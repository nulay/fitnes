package com.fitness.spine.domain.usecase;

import android.os.Handler;
import android.os.Looper;

import com.fitness.spine.data.model.Exercise;
import com.fitness.spine.data.model.Workout;
import com.fitness.spine.service.SttService;
import com.fitness.spine.service.TtsService;

public class WorkoutService {

    private static final String TAG = "WorkoutService";

    private final TtsService ttsService;
    private final SttService sttService;
    private WorkoutIterator iterator;

    private WorkoutState state = WorkoutState.IDLE;
    private WorkoutCallback callback;
    private Handler handler;
    private Runnable countdownRunnable;
    private boolean isHolding = false;
    private int countdownSeconds = 3;

    public enum WorkoutState {
        IDLE,
        EXPLAINING,
        EXERCISING,
        HOLDING,
        AWAITING_COMMAND,
        PAUSED,
        COMPLETED,
        COUNTDOWN
    }

    public interface WorkoutCallback {
        void onStateChanged(WorkoutState state);
        void onExerciseChanged(Exercise exercise, int repetition, int totalRepetitions);
        void onCountdown(int seconds);
        void onCountdownFinished();
        void onExerciseCompleted();
        void onWorkoutCompleted();
        void onError(String error);
    }

    public WorkoutService(Workout workout, TtsService ttsService, SttService sttService) {
        this.ttsService = ttsService;
        this.sttService = sttService;
        this.iterator = new WorkoutIterator(workout);
        this.handler = new Handler(Looper.getMainLooper());

        setupSttCallback();
    }

    public void setCallback(WorkoutCallback callback) {
        this.callback = callback;
    }

    private void setupSttCallback() {
        sttService.setCallback(new SttService.SttCallback() {
            @Override
            public void onSttAvailable() {
            }

            @Override
            public void onSttUnavailable(String error) {
                if (callback != null) {
                    callback.onError("Голосовое распознавание недоступно: " + error);
                }
            }

            @Override
            public void onCommandRecognized(String command) {
                handleCommand(command);
            }

            @Override
            public void onSpeechResult(java.util.ArrayList<String> matches) {
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
            }
        });
    }

    public void updateWorkout(Workout workout) {
        if (workout != null) {
            this.iterator = new WorkoutIterator(workout);
        }
    }

    public void start() {
        if (state != WorkoutState.IDLE && state != WorkoutState.COMPLETED) {
            return;
        }

        ttsService.speak("Привет! Вы готовы начать упражнения?");
        ttsService.speak("Скажите \"да\" или \"готов\", чтобы начать тренировку.");

        state = WorkoutState.AWAITING_COMMAND;
        notifyStateChanged();
        sttService.startListening();
    }

    public void confirmStart() {
        ttsService.speak("Начинаем тренировку!");
        iterator.reset();
        nextExercise();
    }

    public void nextExercise() {
        if (!iterator.hasNext()) {
            completeWorkout();
            return;
        }

        state = WorkoutState.EXPLAINING;
        notifyStateChanged();
        notifyExerciseChanged();

        Exercise exercise = iterator.getCurrentExercise();
        String blockName = iterator.getCurrentBlock().getBlockName();

        String announcement = "Блок " + (iterator.getCurrentBlockIndex() + 1) + ". " + blockName + ". ";
        announcement += "Упражнение " + (iterator.getGlobalPosition() + 1) + ": " + exercise.getTitle() + ". ";

        if (exercise.getFullText() != null && !exercise.getFullText().isEmpty()) {
            announcement += exercise.getFullText() + ". ";
        }

        announcement += "Повторим " + exercise.getRepeats() + " раз.";

        ttsService.speak(announcement, "exercise_intro");

        if (exercise.hasHoldTime()) {
            handler.postDelayed(() -> startExerciseRepetition(), 5000);
        } else {
            handler.postDelayed(() -> startExerciseRepetition(), 3000);
        }
    }

    private void startExerciseRepetition() {
        if (state == WorkoutState.IDLE || state == WorkoutState.PAUSED) {
            return;
        }

        state = WorkoutState.EXERCISING;
        notifyStateChanged();

        Exercise exercise = iterator.getCurrentExercise();
        int rep = iterator.getCurrentRepetition() + 1;
        notifyExerciseChanged();

        String currentSide = iterator.getCurrentSide();
        if (currentSide != null) {
            ttsService.speak("Повторение " + rep + ". " + currentSide, "rep_" + rep);
        } else {
            ttsService.speak("Повторение " + rep, "rep_" + rep);
        }

        if (exercise.getAction() != null && !exercise.getAction().isEmpty()) {
            ttsService.speak(exercise.getAction(), "action_" + rep);
        }

        if (exercise.hasHoldTime()) {
            performHold(exercise.getHoldTime());
        } else {
            int holdTime = exercise.getHoldTime() > 0 ? exercise.getHoldTime() : 3;
            performHold(holdTime);
        }
    }

    private void performHold(int seconds) {
        state = WorkoutState.HOLDING;
        notifyStateChanged();

        ttsService.speakCountdown(seconds);

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (state == WorkoutState.HOLDING) {
                    isHolding = false;
                    onHoldFinished();
                }
            }
        };

        handler.postDelayed(countdownRunnable, (long) seconds * 1000 + 500);
    }

    private void onHoldFinished() {
        Exercise exercise = iterator.getCurrentExercise();

        if (iterator.hasMoreSides()) {
            iterator.nextSide();
            ttsService.speak("Расслабляем.", "relax");
            handler.postDelayed(() -> startExerciseRepetition(), 1500);
        } else if (iterator.hasNextRepetition()) {
            iterator.nextRepetition();

            String breathPhase = (iterator.getCurrentRepetition() % 2) == 0 ? "Вдох" : "Выдох";
            ttsService.speak(breathPhase + ". Расслабляем.", "relax");

            int relaxTime = exercise.getRelaxTime() > 0 ? exercise.getRelaxTime() : 3;
            handler.postDelayed(() -> startExerciseRepetition(), relaxTime * 1000L);
        } else {
            completeExercise();
        }
    }

    private void completeExercise() {
        ttsService.speak("Упражнение выполнено.", "exercise_complete");

        iterator.incrementTotalRepetitions();

        if (callback != null) {
            callback.onExerciseCompleted();
        }

        state = WorkoutState.AWAITING_COMMAND;
        notifyStateChanged();

        ttsService.speak("Скажите \"следующее\" для перехода к следующему упражнению.", "await_command");

        sttService.startListening();
    }

    private void handleCommand(String command) {
        switch (command) {
            case "next":
                sttService.stopListening();
                ttsService.speak("Переходим к следующему упражнению.", "confirm_next");
                if (iterator.next()) {
                    nextExercise();
                } else {
                    completeWorkout();
                }
                break;

            case "repeat":
                ttsService.speak("Повторяем упражнение.", "confirm_repeat");
                iterator.resetCurrentRepetition();
                startExerciseRepetition();
                break;

            case "stop":
                ttsService.speak("Тренировка остановлена.", "confirm_stop");
                stop();
                break;

            case "pause":
                ttsService.speak("Пауза.", "confirm_pause");
                pause();
                break;

            case "да":
            case "готов":
            case "start":
                if (state == WorkoutState.AWAITING_COMMAND) {
                    sttService.stopListening();
                    confirmStart();
                }
                break;

            default:
                if (state == WorkoutState.AWAITING_COMMAND) {
                    sttService.startListening();
                }
                break;
        }
    }

    public void pause() {
        state = WorkoutState.PAUSED;
        isHolding = false;
        notifyStateChanged();
    }

    public void resume() {
        if (state == WorkoutState.PAUSED) {
            state = WorkoutState.EXERCISING;
            notifyStateChanged();
            startExerciseRepetition();
        }
    }

    public void stop() {
        sttService.stopListening();
        ttsService.stop();
        state = WorkoutState.IDLE;
        isHolding = false;
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
        notifyStateChanged();
    }

    public void repeatCurrent() {
        Exercise ex = iterator.getCurrentExercise();
        if (ex != null) {
            state = WorkoutState.COUNTDOWN;
            countdownSeconds = 3;
            notifyStateChanged();
            countdownRunnable = new Runnable() {
                @Override
                public void run() {
                    startExerciseRepetition();
                }
            };
            handler.postDelayed(countdownRunnable, (long) countdownSeconds * 1000 + 500);
            ttsService.speak("Повторяем: " + ex.getTitle(), "repeat");
        }
    }

    private void completeWorkout() {
        state = WorkoutState.COMPLETED;
        notifyStateChanged();

        ttsService.speak("Тренировка завершена. Выполнено "
            + iterator.getTotalRepetitions() + " повторений. Молодец!", "workout_complete");

        if (callback != null) {
            callback.onWorkoutCompleted();
        }
    }

    private void notifyStateChanged() {
        if (callback != null) {
            callback.onStateChanged(state);
        }
    }

    private void notifyExerciseChanged() {
        if (callback != null) {
            Exercise ex = iterator.getCurrentExercise();
            if (ex != null) {
                callback.onExerciseChanged(ex,
                    iterator.getCurrentRepetition() + 1,
                    ex.getRepeatsCount());
            }
        }
    }

    public WorkoutState getState() {
        return state;
    }

    public boolean isActive() {
        return state == WorkoutState.EXERCISING
            || state == WorkoutState.HOLDING
            || state == WorkoutState.EXPLAINING;
    }

    public WorkoutIterator getIterator() {
        return iterator;
    }
}