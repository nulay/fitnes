package com.fitness.spine.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.fitness.spine.data.model.Exercise;
import com.fitness.spine.presentation.ui.SettingsActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIVoiceService {

    private static final String TAG = "AIVoiceService";

    public interface AIServiceCallback {
        void onSuccess(List<Exercise> exercises);
        void onError(String error);
    }

    public enum VoiceContext {
        WORKOUT,   // Основное окно - массив
        EXERCISE  // Редактирование упражнения - один объект
    }

    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService executor;

    public AIVoiceService(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void processVoiceInput(String spokenText, VoiceContext voiceContext, AIServiceCallback callback) {
        executor.execute(() -> {
            try {
                String apiKey = SettingsActivity.getApiKey(this.context);
                String model = SettingsActivity.getModel(this.context);

                if (apiKey.isEmpty()) {
                    mainHandler.post(() -> callback.onError("API ключ не настроен"));
                    return;
                }

                String prompt = buildPrompt(spokenText, voiceContext);
                String response = callAI(model, apiKey, prompt);
                List<Exercise> exercises = parseResponse(response, voiceContext);

                if (!exercises.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(exercises));
                } else {
                    mainHandler.post(() -> callback.onError("Не удалось распознать упражнения"));
                }

            } catch (Exception e) {
                Log.e(TAG, "AI error", e);
                mainHandler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
            }
        });
    }

    private String buildPrompt(String spokenText, VoiceContext context) {
        if (context == VoiceContext.EXERCISE) {
            return "Ты ассистент для создания упражнений ЛФК. " +
                    "Пользователь сказал: \"" + spokenText + "\". " +
                    "Создай упражнение и верни ТОЛЬКО JSON объект:\n" +
                    "{\"title\": \"Название\", \"instruction\": \"Инструкция\", \"holdtime\": 5, \"repeats\": \"10\"}\n" +
                    "Если не понимаешь - верни {\"title\": \"\"}";
        }
        // WORKOUT
        return "Ты ассистент для создания упражнений ЛФК. " +
                "Пользователь сказал: \"" + spokenText + "\". " +
                "Создай упражнения и верни ТОЛЬКО JSON массив:\n" +
                "[{\"title\": \"Название\", \"instruction\": \"Инструкция\", \"holdtime\": 5, \"repeats\": \"10\"}]\n" +
                "Если не понимаешь - верни пустой массив []. " +
                "Может быть несколько упражнений.";
    }

    private String callAI(String model, String apiKey, String prompt) throws Exception {
        String customUrl = SettingsActivity.getCustomUrl(context);
        String apiUrl = getApiUrl(model);

        // Использовать свой URL если указан
        if (customUrl != null && !customUrl.isEmpty()) {
            apiUrl = customUrl;
        }

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        String json = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt.replace("\"", "\\\"") + "\"}]}]}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new Exception("HTTP " + code);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        return parseAIResponse(response.toString());
    }

    private String getApiUrl(String model) {
        if (model.equals("deepseek-chat")) {
            return "https://api.deepseek.com/v1/chat/completions";
        }
        if (model.contains("gpt")) {
            return "https://api.openai.com/v1/chat/completions";
        }
        if (model.equals("claude-3-haiku")) {
            return "https://api.anthropic.com/v1/messages";
        }
        if (model.equals("yandexgpt")) {
            return "https://llm.api.yandex.ru/v1/completion";
        }
        // Google (default)
        return "https://generativelanguage.googleapis.com/v1/models/" + model + ":generateContent";
    }

    private String getAuthHeader(String model, String apiKey) {
        if (model.equals("claude-3-haiku")) {
            return " Anthropic-Client-Key";
        }
        if (model.equals("yandexgpt")) {
            return "Api-Key " + apiKey;
        }
        return "Bearer " + apiKey;
    }

    private String parseAIResponse(String json) {
        try {
            if (json.contains("candidates")) {
                int start = json.indexOf("\"text\":\"") + 8;
                int end = json.indexOf("\",\"");
                return json.substring(start, end);
            } else if (json.contains("choices")) {
                int start = json.indexOf("\"content\":\"") + 11;
                int end = json.indexOf("\",\"done");
                return json.substring(start, end);
            }
        } catch (Exception e) {}
        return "";
    }

    private List<Exercise> parseResponse(String json, VoiceContext context) {
        List<Exercise> result = new ArrayList<>();

        try {
            json = json.trim();

            if (context == VoiceContext.EXERCISE) {
                // Один объект
                Exercise ex = parseSingleExercise(json);
                if (ex != null) result.add(ex);
                return result;
            }

            // WORKOUT - массив
            if (!json.startsWith("[")) {
                Exercise ex = parseSingleExercise(json);
                if (ex != null) result.add(ex);
                return result;
            }

            int start = 0;
            while (true) {
                int objStart = json.indexOf("{", start);
                if (objStart < 0 || objStart >= json.length()) break;

                int depth = 0;
                int objEnd = objStart;
                for (int i = objStart; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    if (depth == 0) {
                        objEnd = i + 1;
                        break;
                    }
                }

                String objJson = json.substring(objStart, objEnd);
                Exercise ex = parseSingleExercise(objJson);
                if (ex != null) result.add(ex);

                start = objEnd;
            }

        } catch (Exception e) {
            Log.e(TAG, "Parse error", e);
        }

        return result;
    }

    private Exercise parseSingleExercise(String json) {
        try {
            String title = extractJsonValue(json, "title");
            String instruction = extractJsonValue(json, "instruction");
            String holdtime = extractJsonValue(json, "holdtime");
            String repeats = extractJsonValue(json, "repeats");

            if (title.isEmpty()) return null;

            Exercise ex = new Exercise();
            ex.setId("ex_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000));
            ex.setTitle(title);
            ex.setFullText(instruction);
            ex.setHoldTime(parseIntSafe(holdtime));
            ex.setRepeats(repeats);
            ex.setRelaxTime(5);

            return ex;
        } catch (Exception e) {
            return null;
        }
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 5;
        }
    }

    private String extractJsonValue(String json, String key) {
        try {
            int start = json.indexOf("\"" + key + "\":");
            if (start < 0) return "";
            start = json.indexOf("\"", start + key.length() + 2);
            int end = json.indexOf("\"", start + 1);
            return json.substring(start + 1, end);
        } catch (Exception e) {
            return "";
        }
    }
}