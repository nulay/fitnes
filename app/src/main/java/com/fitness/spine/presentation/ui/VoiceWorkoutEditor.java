package com.fitness.spine.presentation.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fitness.spine.R;
import com.fitness.spine.data.model.Block;
import com.fitness.spine.data.model.Exercise;
import com.fitness.spine.data.model.Workout;
import com.fitness.spine.service.SttService;
import com.fitness.spine.service.TtsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoiceWorkoutEditor {

    private static Context context;
    private static TtsService ttsService;
    private static SttService sttService;
    private static Handler mainHandler;
    private static EditText etTitle;
    private static EditText etInstruction;
    private static EditText etHoldTime;
    private static EditText etRepeats;
    private static TextView tvVoiceStatus;
    private static Button btnVoiceInput;
    private static AlertDialog currentDialog;

    public interface OnExerciseSaveListener {
        void onExerciseSaved(Exercise exercise);
    }

    public static void showCreateExerciseDialog(Context ctx, TtsService tts, SttService stt, OnExerciseSaveListener listener) {
        context = ctx;
        ttsService = tts;
        sttService = stt;
        mainHandler = new Handler(Looper.getMainLooper());

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_voice_create_exercise, null);

        etTitle = dialogView.findViewById(R.id.etExerciseTitle);
        etInstruction = dialogView.findViewById(R.id.etExerciseInstruction);
        etHoldTime = dialogView.findViewById(R.id.etHoldTime);
        etRepeats = dialogView.findViewById(R.id.etRepeats);
        tvVoiceStatus = dialogView.findViewById(R.id.tvVoiceStatus);
        btnVoiceInput = dialogView.findViewById(R.id.btnVoiceInput);
        Button btnSave = dialogView.findViewById(R.id.btnSaveExercise);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelExercise);

        currentDialog = new AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create();

        btnVoiceInput.setOnClickListener(v -> startVoiceInput());

        btnCancel.setOnClickListener(v -> currentDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String instruction = etInstruction.getText().toString().trim();
            String holdTimeStr = etHoldTime.getText().toString().trim();
            String repeatsStr = etRepeats.getText().toString().trim();

            if (title.isEmpty()) {
                etTitle.setError("Введите название");
                return;
            }

            Exercise exercise = new Exercise();
            exercise.setId("ex_" + System.currentTimeMillis());
            exercise.setTitle(title);
            exercise.setFullText(instruction);
            exercise.setHoldTime(holdTimeStr.isEmpty() ? 5 : Integer.parseInt(holdTimeStr));
            exercise.setRelaxTime(5);
            exercise.setRepeats(repeatsStr.isEmpty() ? "10" : repeatsStr);

            listener.onExerciseSaved(exercise);
            currentDialog.dismiss();
        });

        currentDialog.show();

        ttsService.speak("Скажите название упр��жнения, например: Подъем ног, или давление на лоб");
    }

    private static void startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Распознавание речи недоступно", Toast.LENGTH_SHORT).show();
            return;
        }

        tvVoiceStatus.setText("Слушаю...");
        btnVoiceInput.setEnabled(false);

        SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                mainHandler.post(() -> tvVoiceStatus.setText("Слушаю... говорите"));
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spoken = matches.get(0);
                    mainHandler.post(() -> processVoiceInput(spoken));
                }
                mainHandler.post(() -> {
                    tvVoiceStatus.setText("");
                    btnVoiceInput.setEnabled(true);
                });
            }

            @Override
            public void onError(int error) {
                mainHandler.post(() -> {
                    tvVoiceStatus.setText("Не распознано. Попробуйте снова");
                    btnVoiceInput.setEnabled(true);
                });
            }

            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {}
            @Override
            public void onPartialResults(Bundle partialResults) {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizer.startListening(intent);
    }

    private static void processVoiceInput(String spoken) {
        String text = spoken.toLowerCase(Locale.getDefault());

        if (etTitle.getText().toString().isEmpty()) {
            etTitle.setText(capitalize(spoken));
            ttsService.speak("Название: " + spoken + ". Теперь скажите инструкцию, или нажмите сохранить если достаточно");
            return;
        }

        if (etInstruction.getText().toString().isEmpty()) {
            etInstruction.setText(spoken);
            ttsService.speak("Инструкция сохранена. Скажите время задержки в секундах");
            return;
        }

        if (etHoldTime.getText().toString().isEmpty()) {
            try {
                int holdTime = extractNumber(text);
                if (holdTime > 0) {
                    etHoldTime.setText(String.valueOf(holdTime));
                    ttsService.speak("Время задержки " + holdTime + " секунд. Скажите количество повторений");
                    return;
                }
            } catch (Exception e) {}
            ttsService.speak("Не понял. Скажите число");
            return;
        }

        if (etRepeats.getText().toString().isEmpty()) {
            try {
                int repeats = extractNumber(text);
                if (repeats > 0) {
                    etRepeats.setText(String.valueOf(repeats));
                    ttsService.speak("Повторений: " + repeats + ". Нажмите сохранить");
                    return;
                }
            } catch (Exception e) {}
            ttsService.speak("Не понял. Скажите число");
        }
    }

    private static int extractNumber(String text) {
        String[] words = text.split(" ");
        for (String word : words) {
            try {
                return Integer.parseInt(word.replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                if (word.contains("десят")) return 10;
                if (word.contains("двадцат")) return 20;
                if (word.contains("тридцат")) return 30;
                if (word.contains("сорок")) return 40;
                if (word.contains("пять") || word.contains("пяти")) return 5;
                if (word.contains("шесть") || word.contains("шести")) return 6;
                if (word.contains("семь") || word.contains("семи")) return 7;
                if (word.contains("восемь") || word.contains("восьми")) return 8;
                if (word.contains("девять") || word.contains("девяти")) return 9;
                if (word.contains("одиннадцат")) return 11;
                if (word.contains("двенадцат")) return 12;
            }
        }
        return 0;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}