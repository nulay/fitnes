package com.fitness.spine.presentation.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fitness.spine.R;
import com.fitness.spine.data.model.Exercise;
import com.fitness.spine.data.model.LessonInfo;
import com.fitness.spine.data.model.LessonsCatalog;
import com.fitness.spine.data.model.Workout;
import com.fitness.spine.data.repository.LessonsRepository;
import com.fitness.spine.domain.usecase.WorkoutService;
import com.fitness.spine.presentation.viewmodel.MainViewModel;
import com.fitness.spine.service.SttService;
import com.fitness.spine.service.TtsService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String SERVER_URL = ""; // заглушка

    private TextView tvTitle;
    private TextView tvStatus;
    private TextView tvExercise;
    private TextView tvCount;
    private ListView lvLessons;
    private Button btnStart;
    private Button btnStop;
    private Button btnNew;
    private Button btnVoice;
    private ImageButton btnSettings;

    private MainViewModel viewModel;
    private TtsService ttsService;
    private SttService sttService;
    private LessonsAdapter lessonsAdapter;

    private boolean ttsInitialized = false;
    private boolean catalogLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initServices();
        initViewModel();
        setupListeners();
        checkPermissions();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvStatus = findViewById(R.id.tvStatus);
        tvExercise = findViewById(R.id.tvExercise);
        tvCount = findViewById(R.id.tvCount);
        lvLessons = findViewById(R.id.lvLessons);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnNew = findViewById(R.id.btnNew);
        btnVoice = findViewById(R.id.btnVoice);
        btnSettings = findViewById(R.id.btnSettings);
    }

    private void initServices() {
        ttsService = new TtsService(this);
        ttsService.setCallback(new TtsService.TtsCallback() {
            @Override
            public void onTtsInitialized() {
                ttsInitialized = true;
                runOnUiThread(() -> viewModel.setTtsReady(true));
                ttsService.speak("Приложение готово. Выберите урок.");
            }

            @Override
            public void onTtsError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка TTS: " + error, Toast.LENGTH_SHORT).show();
                    viewModel.setTtsReady(false);
                });
            }
        });

        sttService = new SttService(this);
        sttService.setCallback(new SttService.SttCallback() {
            @Override
            public void onSttAvailable() {
                runOnUiThread(() -> viewModel.setSttReady(true));
            }

            @Override
            public void onSttUnavailable(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "STT недоступно: " + error, Toast.LENGTH_SHORT).show();
                    viewModel.setSttReady(false);
                });
            }

            @Override
            public void onCommandRecognized(String command) {
            }

            @Override
            public void onSpeechResult(ArrayList<String> matches) {
                if (matches != null && !matches.isEmpty()) {
                    String spoken = matches.get(0).toLowerCase();
                    if (spoken.contains("стоп") || spoken.contains("закончить")) {
                        viewModel.stopWorkout();
                    } else if (spoken.contains("пауза") || spoken.contains("приостановить")) {
                        viewModel.pauseWorkout();
                    } else if (spoken.contains("начать") || spoken.contains("старт") || spoken.contains("продолжить")) {
                        if (checkPermissions()) {
                            viewModel.startWorkout();
                        } else {
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                        }
                    } else if (spoken.contains("следующее") || spoken.contains("дальше")) {
                        viewModel.nextExercise();
                    } else if (spoken.contains("повтор") || spoken.contains("снова")) {
                        viewModel.repeatCurrent();
                    }
                }
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
            }
        });
        sttService.initialize();
    }

    private void initViewModel() {
        viewModel = new MainViewModel(getApplication());
        viewModel.initialize(SERVER_URL);
        viewModel.setServices(ttsService, sttService);

        viewModel.getCatalog().observe(this, catalog -> {
            if (catalog != null) {
                catalogLoaded = true;
                updateLessonsList(catalog);
            }
        });

        viewModel.getSelectedLesson().observe(this, lesson -> {
            if (lesson != null) {
                tvTitle.setText(lesson.getTitle());
                tvStatus.setText(lesson.getDescription());
            }
        });

        viewModel.getWorkout().observe(this, workout -> {
            if (workout != null) {
                btnStart.setEnabled(true);
            }
        });

        viewModel.getState().observe(this, state -> {
            if (state != null) {
                updateUI(state);
            }
        });

        viewModel.getCurrentExercise().observe(this, exercise -> {
            if (exercise != null) {
                tvExercise.setText(exercise.getTitle());
            }
        });

        viewModel.getCurrentRepetition().observe(this, repetition -> {
            Integer total = viewModel.getTotalRepetitions().getValue();
            if (repetition != null && total != null) {
                tvCount.setText(repetition + " / " + total);
            } else if (repetition != null) {
                tvCount.setText(String.valueOf(repetition));
            }
        });

        viewModel.getStatus().observe(this, status -> {
            if (status != null && !status.isEmpty()) {
                if (!catalogLoaded) {
                    tvTitle.setText(status);
                }
                tvStatus.setText(status);
            } else {
                tvStatus.setText("");
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.isLoading().observe(this, loading -> {
            btnStart.setEnabled(!(loading != null && loading));
            if (loading != null && !loading) {
                tvStatus.setText("");
            } else if (loading != null && loading) {
                tvStatus.setText("Загрузка...");
            }
        });

        viewModel.isTtsReady().observe(this, ready -> {
            if (ready != null && ready) {
                viewModel.loadCatalog();
            }
        });
    }

    private void setupListeners() {
        lvLessons.setOnItemClickListener((parent, view, position, id) -> {
            LessonInfo lesson = (LessonInfo) parent.getItemAtPosition(position);
            if (lesson != null) {
                viewModel.loadLesson(lesson);
            }
        });

        lvLessons.setOnItemLongClickListener((parent, view, position, id) -> {
            LessonInfo lesson = (LessonInfo) parent.getItemAtPosition(position);
            if (lesson != null) {
                showLessonOptions(lesson);
            }
            return true;
        });

        btnNew.setOnClickListener(v -> {
            WorkoutEditorDialog.showCreateNewDialog(this, ttsService, sttService, newWorkout -> {
                viewModel.setNewWorkout(newWorkout);
                ttsService.speak("Создана новая тренировка");
            });
        });

        btnStart.setOnClickListener(v -> {
            WorkoutService.WorkoutState state = viewModel.getCurrentState();
            if (state == WorkoutService.WorkoutState.IDLE || state == WorkoutService.WorkoutState.COMPLETED) {
                if (checkPermissions()) {
                    viewModel.startWorkout();
                } else {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                }
            } else if (state == WorkoutService.WorkoutState.PAUSED) {
                viewModel.resumeWorkout();
            }
        });

        btnStop.setOnClickListener(v -> {
            viewModel.stopWorkout();
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        btnVoice.setOnClickListener(v -> {
            if (sttService != null && sttService.isAvailable()) {
                sttService.startListening();
                tvStatus.setText("Слушаю...");
            } else {
                Toast.makeText(this, "Голос недоступен", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLessonsList(LessonsCatalog catalog) {
        List<LessonInfo> lessons = catalog.getLessons();
        if (lessons == null || lessons.isEmpty()) {
            lessonsAdapter = new LessonsAdapter(this, new ArrayList<>());
        } else {
            lessonsAdapter = new LessonsAdapter(this, lessons);
        }
        lvLessons.setAdapter(lessonsAdapter);
    }

    private void updateUI(WorkoutService.WorkoutState state) {
        switch (state) {
            case IDLE:
                btnStart.setText("Старт");
                btnStart.setEnabled(ttsInitialized && catalogLoaded);
                break;

            case EXPLAINING:
                tvTitle.setText("Слушайте инструкцию...");
                btnStart.setEnabled(false);
                break;

            case EXERCISING:
            case HOLDING:
                btnStart.setEnabled(false);
                break;

            case AWAITING_COMMAND:
                tvTitle.setText("Скажите \"следующее\"");
                btnStart.setEnabled(false);
                break;

            case PAUSED:
                tvTitle.setText("Пауза");
                btnStart.setText("Продолжить");
                btnStart.setEnabled(true);
                break;

            case COMPLETED:
                tvTitle.setText("Тренировка завершена!");
                btnStart.setText("Старт");
                btnStart.setEnabled(true);
                break;
        }
    }

private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private void showLessonOptions(LessonInfo lesson) {
        String[] options = {"Мои комплексы", "Удалить", "Отмена"};

        new AlertDialog.Builder(this)
            .setTitle(lesson.getTitle())
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    startActivityForResult(new Intent(this, WorkoutsActivity.class), 100);
                } else if (which == 1) {
                    new AlertDialog.Builder(this)
                        .setTitle("Удалить урок?")
                        .setPositiveButton("Удалить", (d, w) -> {
                            Toast.makeText(this, "Урок удалён", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
                }
            })
            .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                       @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.loadCatalog();
            } else {
                Toast.makeText(this, "Требуется разрешение на микрофон", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsService != null) {
            ttsService.shutdown();
        }
        if (sttService != null) {
            sttService.destroy();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String json = data.getStringExtra("workout");
            if (json != null) {
                Workout w = Workout.fromJson(json);
                if (w != null) {
                    viewModel.setNewWorkout(w);
                    tvTitle.setText(w.getAppName());
                    tvExercise.setText(w.getAppName() + " - " + w.getTotalExercises() + " упражнений");
                    ttsService.speak("Загружен комплекс: " + w.getAppName());
                }
            }
        }
    }
}