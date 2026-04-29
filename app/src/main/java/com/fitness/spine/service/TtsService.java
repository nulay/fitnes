package com.fitness.spine.service;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TtsService implements TextToSpeech.OnInitListener {

    private static final String TAG = "TtsService";

    private TextToSpeech tts;
    private boolean isInitialized = false;
    private boolean isSpeaking = false;
    private final Queue<SpeechTask> speechQueue = new ConcurrentLinkedQueue<>();
    private TtsCallback callback;

    public interface TtsCallback {
        void onTtsInitialized();
        void onTtsError(String error);
    }

    public static class SpeechTask {
        public final String text;
        public final String utteranceId;
        public final boolean interrupt;

        public SpeechTask(String text, String utteranceId, boolean interrupt) {
            this.text = text;
            this.utteranceId = utteranceId;
            this.interrupt = interrupt;
        }
    }

    public TtsService(Context context) {
        tts = new TextToSpeech(context, this);
    }

    public void setCallback(TtsCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Locale russianLocale = new Locale("ru", "RU");
            int result = tts.setLanguage(russianLocale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Russian language not supported, trying default");
                result = tts.setLanguage(Locale.getDefault());
            }

            tts.setSpeechRate(1.0f);
            tts.setPitch(1.0f);

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    isSpeaking = true;
                }

                @Override
                public void onDone(String utteranceId) {
                    isSpeaking = false;
                    processQueue();
                }

                @Override
                public void onError(String utteranceId) {
                    isSpeaking = false;
                    Log.e(TAG, "TTS error: " + utteranceId);
                    processQueue();
                }

                @Override
                public void onError(String utteranceId, int errorCode) {
                    isSpeaking = false;
                    Log.e(TAG, "TTS error code: " + errorCode);
                    processQueue();
                }
            });

            isInitialized = true;
            Log.i(TAG, "TTS initialized successfully");

            if (callback != null) {
                callback.onTtsInitialized();
            }
        } else {
            Log.e(TAG, "TTS initialization failed");
            if (callback != null) {
                callback.onTtsError("TTS initialization failed");
            }
        }
    }

    public void speak(String text) {
        speak(text, null, false);
    }

    public void speak(String text, String utteranceId) {
        speak(text, utteranceId, false);
    }

    public void speak(String text, String utteranceId, boolean interrupt) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized yet");
            return;
        }

        SpeechTask task = new SpeechTask(text, utteranceId, interrupt);
        if (!isSpeaking && speechQueue.isEmpty()) {
            executeSpeech(task);
        } else if (interrupt) {
            tts.stop();
            speechQueue.clear();
            executeSpeech(task);
        } else {
            speechQueue.add(task);
        }
    }

    private void executeSpeech(SpeechTask task) {
        if (task == null || task.text == null) return;

        String utteranceId = task.utteranceId != null ? task.utteranceId : "utterance_" + System.currentTimeMillis();
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

        tts.speak(task.text, TextToSpeech.QUEUE_FLUSH, params);
    }

    private void processQueue() {
        if (!speechQueue.isEmpty() && !isSpeaking) {
            SpeechTask nextTask = speechQueue.poll();
            executeSpeech(nextTask);
        }
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
            speechQueue.clear();
            isSpeaking = false;
        }
    }

    public boolean isAvailable() {
        return isInitialized;
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    public void shutdown() {
        stop();
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
    }

    public void speakCountdown(int seconds) {
        String[] words = {"раз", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять", "десять"};
        for (int i = 1; i <= seconds && i <= words.length; i++) {
            final int index = i;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                speak(words[index - 1]);
            }, i * 1000L);
        }
    }
}