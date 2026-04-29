package com.fitness.spine.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SttService {

    private static final String TAG = "SttService";

private SpeechRecognizer recognizer;
    private Context context;
    private boolean isListening = false;
    private boolean isAvailable = false;
    private SttCallback callback;
    private SttCallback tempCallback;
    private Handler mainHandler;

    private static final String[] SUPPORTED_COMMANDS = {
        "следующее", "дальше", "следующий",
        "повторить", "повтори", "снова",
        "стоп", "остановить", "закончить",
        "пауза",
        "начать", "старт",
        "да", "готов"
    };

    public interface SttCallback {
        void onSttAvailable();
        void onSttUnavailable(String error);
        void onCommandRecognized(String command);
        void onSpeechResult(ArrayList<String> matches);
        void onError(int errorCode, String errorMessage);
    }

    public SttService(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setCallback(SttCallback callback) {
        this.callback = callback;
    }

    public void startListening(SttCallback callback) {
        this.tempCallback = callback;
        startListening("ru-RU");
    }

    public void initialize() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available");
            if (callback != null) {
                callback.onSttUnavailable("Speech recognition not available on this device");
            }
            return;
        }

        mainHandler.post(() -> {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isListening = true;
                    Log.i(TAG, "Ready for speech");
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    isListening = false;
                }

                @Override
                public void onError(int error) {
                    isListening = false;
                    String errorMessage = getErrorMessage(error);
                    Log.e(TAG, "Speech recognition error: " + error);

                    if (callback != null) {
                        callback.onError(error, errorMessage);
                    }

                    if (isListening) {
                        restartListening();
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    isListening = false;
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String command = findCommand(matches);
                        Log.i(TAG, "Recognized: " + command);
                        
                        // Temp callback first
                        if (tempCallback != null) {
                            tempCallback.onSpeechResult(matches);
                            tempCallback = null;
                        } else if (callback != null) {
                            if (command != null) {
                                callback.onCommandRecognized(command);
                            }
                            callback.onSpeechResult(matches);
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String command = findCommand(matches);
                        if (command != null && callback != null) {
                            callback.onCommandRecognized(command);
                        }
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });

            isAvailable = true;
            Log.i(TAG, "STT initialized successfully");

            if (callback != null) {
                mainHandler.post(() -> callback.onSttAvailable());
            }
        });
    }

    public void startListening() {
        startListening("ru-RU");
    }

    public void startListening(String language) {
        if (recognizer == null || !isAvailable) {
            Log.w(TAG, "STT not initialized");
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        mainHandler.post(() -> recognizer.startListening(intent));
    }

    public void stopListening() {
        if (recognizer != null) {
            mainHandler.post(() -> recognizer.stopListening());
            isListening = false;
        }
    }

    public void cancel() {
        if (recognizer != null) {
            mainHandler.post(() -> recognizer.cancel());
            isListening = false;
        }
    }

    private void restartListening() {
        mainHandler.postDelayed(() -> {
            if (!isListening) {
                startListening();
            }
        }, 500);
    }

    private String findCommand(ArrayList<String> matches) {
        if (matches == null || matches.isEmpty()) {
            return null;
        }

        for (String match : matches) {
            String lowerMatch = match.toLowerCase(Locale.getDefault());
            for (String cmd : SUPPORTED_COMMANDS) {
                if (lowerMatch.contains(cmd)) {
                    return normalizeCommand(cmd);
                }
            }
        }

        return null;
    }

    private String normalizeCommand(String rawCommand) {
        if (rawCommand.contains("следующ") || rawCommand.contains("next")) {
            return "next";
        } else if (rawCommand.contains("повтор") || rawCommand.contains("repeat") || rawCommand.contains("снова")) {
            return "repeat";
        } else if (rawCommand.contains("стоп") || rawCommand.contains("закончить") || rawCommand.contains("stop")) {
            return "stop";
        } else if (rawCommand.contains("пауза") || rawCommand.contains("pause")) {
            return "pause";
        } else if (rawCommand.contains("начать") || rawCommand.contains("start")) {
            return "start";
        }
        return rawCommand;
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech detected";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public boolean isListening() {
        return isListening;
    }

    public void destroy() {
        stopListening();
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        isAvailable = false;
    }
}