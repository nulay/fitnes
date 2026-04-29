package com.fitness.spine.presentation.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitness.spine.data.model.Exercise;
import com.fitness.spine.data.model.LessonInfo;
import com.fitness.spine.data.model.LessonsCatalog;
import com.fitness.spine.data.model.Workout;
import com.fitness.spine.data.repository.LessonsRepository;
import com.fitness.spine.domain.usecase.WorkoutIterator;
import com.fitness.spine.domain.usecase.WorkoutService;

public class MainViewModel extends AndroidViewModel {

    private LessonsRepository lessonsRepository;
    private Workout currentWorkout;
    private WorkoutService workoutService;

    private final MutableLiveData<LessonsCatalog> catalogLiveData = new MutableLiveData<>();
    private final MutableLiveData<LessonInfo> selectedLessonLiveData = new MutableLiveData<>();
    private final MutableLiveData<Workout> workoutLiveData = new MutableLiveData<>();
    private final MutableLiveData<WorkoutService.WorkoutState> stateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Exercise> currentExerciseLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentRepetitionLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalRepetitionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> statusLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ttsReadyLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> sttReadyLiveData = new MutableLiveData<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public void initialize(String serverBaseUrl) {
        lessonsRepository = new LessonsRepository(getApplication(), serverBaseUrl);
    }

    public void setServices(com.fitness.spine.service.TtsService ttsService,
                            com.fitness.spine.service.SttService sttService) {
        workoutService = new WorkoutService(currentWorkout, ttsService, sttService);
        workoutService.setCallback(new WorkoutService.WorkoutCallback() {
            @Override
            public void onStateChanged(WorkoutService.WorkoutState state) {
                mainHandler.post(() -> stateLiveData.setValue(state));
            }

            @Override
            public void onExerciseChanged(Exercise exercise, int repetition, int total) {
                mainHandler.post(() -> {
                    currentExerciseLiveData.setValue(exercise);
                    currentRepetitionLiveData.setValue(repetition);
                    totalRepetitionsLiveData.setValue(total);
                });
            }

            @Override
            public void onCountdown(int seconds) {
            }

            @Override
            public void onCountdownFinished() {
            }

            @Override
            public void onExerciseCompleted() {
            }

            @Override
            public void onWorkoutCompleted() {
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> errorLiveData.setValue(error));
            }
        });
    }

    public void loadCatalog() {
        loadingLiveData.setValue(true);
        statusLiveData.setValue("Загрузка каталога...");

        lessonsRepository.loadCatalogFromAllSources(new LessonsRepository.LoadCallback() {
            @Override
            public void onSuccess(LessonsCatalog catalog) {
                mainHandler.post(() -> {
                    catalogLiveData.setValue(catalog);
                    loadingLiveData.setValue(false);
                    statusLiveData.setValue("Каталог загружен: " + catalog.getLessonCount() + " уроков");
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue(error);
                    statusLiveData.setValue(error);
                });
            }
        });
    }

    public void loadLesson(LessonInfo lesson) {
        loadingLiveData.setValue(true);
        selectedLessonLiveData.setValue(lesson);
        statusLiveData.setValue("Загрузка: " + lesson.getTitle());

        lessonsRepository.loadWorkout(lesson.getFile(), new LessonsRepository.LoadWorkoutCallback() {
            @Override
            public void onSuccess(Workout workout) {
                mainHandler.post(() -> {
                    currentWorkout = workout;
                    workoutLiveData.setValue(workout);
                    loadingLiveData.setValue(false);
                    statusLiveData.setValue(workout.getAppName() + " - " + workout.getTotalExercises() + " упражнений");
                });

                if (workoutService != null) {
                    workoutService.updateWorkout(workout);
                }
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue(error);
                    statusLiveData.setValue(error);
                });
            }
        });
    }

    public void loadLessonById(String lessonId) {
        LessonsCatalog catalog = catalogLiveData.getValue();
        if (catalog != null && catalog.getLessons() != null) {
            for (LessonInfo lesson : catalog.getLessons()) {
                if (lesson.getId().equals(lessonId)) {
                    loadLesson(lesson);
                    return;
                }
            }
        }
        errorLiveData.setValue("Урок не найден: " + lessonId);
    }

    public void loadWorkoutFromJson(String json) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            Workout workout = gson.fromJson(json, Workout.class);
            currentWorkout = workout;
            workoutLiveData.setValue(workout);
            statusLiveData.setValue(workout.getAppName() + " - " + workout.getTotalExercises() + " упражнений");
            if (workoutService != null) {
                workoutService.updateWorkout(workout);
            }
        } catch (Exception e) {
            errorLiveData.setValue("Ошибка парсинга JSON: " + e.getMessage());
        }
    }

    public void loadWorkoutFromUrl(String url) {
        loadingLiveData.setValue(true);
        statusLiveData.setValue("Загрузка...");

        lessonsRepository.loadWorkoutFromServer(url, new LessonsRepository.LoadWorkoutCallback() {
            @Override
            public void onSuccess(Workout workout) {
                mainHandler.post(() -> {
                    currentWorkout = workout;
                    workoutLiveData.setValue(workout);
                    loadingLiveData.setValue(false);
                    statusLiveData.setValue(workout.getAppName());
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue(error);
                });
            }
        });
    }

    public void startWorkout() {
        if (workoutService != null && currentWorkout != null) {
            workoutService.start();
        } else {
            if (currentWorkout == null) {
                errorLiveData.setValue("Сначала выберите урок");
            }
        }
    }

    public void nextExercise() {
        if (workoutService != null) {
            workoutService.nextExercise();
        }
    }

    public void pauseWorkout() {
        if (workoutService != null) {
            workoutService.pause();
        }
    }

    public void resumeWorkout() {
        if (workoutService != null) {
            workoutService.resume();
        }
    }

public void stopWorkout() {
        workoutService.stop();
    }

    public void repeatCurrent() {
        workoutService.repeatCurrent();
    }

    public void setTtsReady(boolean ready) {
        ttsReadyLiveData.setValue(ready);
    }

    public void setSttReady(boolean ready) {
        sttReadyLiveData.setValue(ready);
    }

    public void setStatus(String status) {
        statusLiveData.setValue(status);
    }

    public void clearError() {
        errorLiveData.setValue(null);
    }

    public LiveData<LessonsCatalog> getCatalog() {
        return catalogLiveData;
    }

    public LiveData<LessonInfo> getSelectedLesson() {
        return selectedLessonLiveData;
    }

    public LiveData<Workout> getWorkout() {
        return workoutLiveData;
    }

    public LiveData<WorkoutService.WorkoutState> getState() {
        return stateLiveData;
    }

    public LiveData<Exercise> getCurrentExercise() {
        return currentExerciseLiveData;
    }

    public LiveData<Integer> getCurrentRepetition() {
        return currentRepetitionLiveData;
    }

    public LiveData<Integer> getTotalRepetitions() {
        return totalRepetitionsLiveData;
    }

    public LiveData<String> getStatus() {
        return statusLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> isLoading() {
        return loadingLiveData;
    }

    public LiveData<Boolean> isTtsReady() {
        return ttsReadyLiveData;
    }

    public LiveData<Boolean> isSttReady() {
        return sttReadyLiveData;
    }

    public Workout getCurrentWorkout() {
        return currentWorkout;
    }

    public WorkoutIterator getIterator() {
        return workoutService != null ? workoutService.getIterator() : null;
    }

    public WorkoutService.WorkoutState getCurrentState() {
        return workoutService != null ? workoutService.getState() : WorkoutService.WorkoutState.IDLE;
    }

    public void updateWorkout(Workout workout) {
        this.currentWorkout = workout;
        workoutLiveData.setValue(workout);
        if (workoutService != null) {
            workoutService.updateWorkout(workout);
        }
    }

    public void setNewWorkout(Workout workout) {
        this.currentWorkout = workout;
        workoutLiveData.setValue(workout);
        if (workoutService != null) {
            workoutService.updateWorkout(workout);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (workoutService != null) {
            workoutService.stop();
        }
    }
}