package com.fitness.spine.presentation.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fitness.spine.R;
import com.fitness.spine.data.model.Workout;
import com.fitness.spine.service.SttService;
import com.fitness.spine.service.TtsService;

import java.util.ArrayList;
import java.util.List;

public class WorkoutsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "lfk_trainer_prefs";
    private static final String KEY_WORKOUTS = "custom_workouts";

    private ImageButton btnBack;
    private EditText etNewWorkoutName;
    private EditText etNewWorkoutDesc;
    private Button btnVoiceCreate;
    private ListView lvWorkouts;

    private TtsService ttsService;
    private SttService sttService;
    private List<Workout> workouts;
    private WorkoutAdapter adapter;
    private SharedPreferences prefs;

    public interface OnWorkoutSelectedListener {
        void onWorkoutSelected(Workout workout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workouts);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        ttsService = new TtsService(this);
        sttService = new SttService(this);

        initViews();
        loadWorkouts();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etNewWorkoutName = findViewById(R.id.etNewWorkoutName);
        etNewWorkoutDesc = findViewById(R.id.etNewWorkoutDesc);
        Button btnSaveWorkout = findViewById(R.id.btnSaveWorkout);
        btnVoiceCreate = findViewById(R.id.btnVoiceCreate);
        lvWorkouts = findViewById(R.id.lvWorkouts);

        btnSaveWorkout.setOnClickListener(v -> {
            String name = etNewWorkoutName.getText().toString().trim();
            String desc = etNewWorkoutDesc.getText().toString().trim();
            if (name.isEmpty()) {
                etNewWorkoutName.setError("Введите название");
                etNewWorkoutName.requestFocus();
                return;
            }
            saveNewWorkout(name, desc);
            etNewWorkoutName.setText("");
            etNewWorkoutDesc.setText("");
        });
    }

    private void loadWorkouts() {
        String json = prefs.getString(KEY_WORKOUTS, "[]");
        workouts = Workout.fromJsonList(json);
        if (workouts == null) workouts = new ArrayList<>();

        adapter = new WorkoutAdapter(workouts);
        lvWorkouts.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnVoiceCreate.setOnClickListener(v -> {
            etNewWorkoutName.requestFocus();
            ttsService.speak("Введите название");
            new android.os.Handler().postDelayed(() -> {
                sttService.startListening(new SttService.SttCallback() {
                    @Override
                    public void onSpeechResult(ArrayList<String> matches) {
                        if (matches != null && !matches.isEmpty()) {
                            String voiceName = matches.get(0);
                            runOnUiThread(() -> {
                                etNewWorkoutName.setText(voiceName);
                                etNewWorkoutDesc.requestFocus();
                            });
                            ttsService.speak("Описание");
                        } else {
                            runOnUiThread(() -> etNewWorkoutDesc.requestFocus());
                            ttsService.speak("Описание");
                        }
                    }
                    @Override
                    public void onError(int errorCode, String error) {
                        runOnUiThread(() -> etNewWorkoutDesc.requestFocus());
                        ttsService.speak("Описание");
                    }
                    @Override public void onSttAvailable() {}
                    @Override public void onSttUnavailable(String e) {}
                    @Override public void onCommandRecognized(String c) {}
                });
            }, 2500);
        });

        lvWorkouts.setOnItemClickListener((parent, view, position, id) -> {
            Workout w = workouts.get(position);
            Intent result = new Intent();
            result.putExtra("workout", w.toJson());
            setResult(RESULT_OK, result);
            finish();
        });

        lvWorkouts.setOnItemLongClickListener((parent, view, position, id) -> {
            showWorkoutOptions(position);
            return true;
        });
    }

    private void showWorkoutOptions(int position) {
        String[] options = {"Редактировать", "Удалить", "Отмена"};

        new AlertDialog.Builder(this)
            .setTitle(workouts.get(position).getAppName())
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    editWorkout(position);
                } else if (which == 1) {
                    deleteWorkout(position);
                }
            })
            .show();
    }

    private void editWorkout(int position) {
        Workout workout = workouts.get(position);
        WorkoutEditorDialog.showEditDialog(this, ttsService, sttService, workout, updated -> {
            workouts.set(position, updated);
            saveWorkouts();
            adapter.notifyDataSetChanged();
        });
    }

    private void deleteWorkout(int position) {
        new AlertDialog.Builder(this)
            .setTitle("Удалить комплекс?")
            .setPositiveButton("Удалить", (d, which) -> {
                workouts.remove(position);
                saveWorkouts();
                adapter.notifyDataSetChanged();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void saveWorkouts() {
        String json = Workout.toJson(workouts);
        prefs.edit().putString(KEY_WORKOUTS, json).apply();
    }

    private Workout createWorkout(String name, com.fitness.spine.data.model.Exercise exercise) {
        List<com.fitness.spine.data.model.Block> blocks = new ArrayList<>();
        List<com.fitness.spine.data.model.Exercise> exercises = new ArrayList<>();
        exercises.add(exercise);

        com.fitness.spine.data.model.Block block = new com.fitness.spine.data.model.Block();
        block.setBlockName("Блок 1");
        block.setExercises(exercises);
        blocks.add(block);

        Workout workout = new Workout();
        workout.setAppName(name);
        workout.setDataSource(exercise.getFullText());
        workout.setBlocks(blocks);

return workout;
    }

private void createWorkoutFromVoice(String name) {
        ttsService.speak("Скажите упражнение");
        new android.os.Handler().postDelayed(() -> {
            sttService.startListening(new SttService.SttCallback() {
                @Override
                public void onSpeechResult(ArrayList<String> matches) {
                    if (matches != null && !matches.isEmpty()) {
                        String exerciseText = matches.get(0);
                        createAndSaveWorkout(name, exerciseText);
                    }
                }
                @Override
                public void onError(int errorCode, String error) {}
                @Override public void onSttAvailable() {}
                @Override public void onSttUnavailable(String e) {}
                @Override public void onCommandRecognized(String c) {}
            });
        }, 2000);
    }

    private void createAndSaveWorkout(String name, String exerciseText) {
        com.fitness.spine.data.model.Exercise ex = new com.fitness.spine.data.model.Exercise();
        ex.setTitle(exerciseText);
        ex.setHoldTime(5);
        ex.setRepeats("10");

        Workout w = createWorkout(name, ex);
        w.setAppName("");
        workouts.add(0, w);
        saveWorkouts();
        adapter.notifyDataSetChanged();

        runOnUiThread(() -> {
            etNewWorkoutName.setText("");
            ttsService.speak("Комплекс " + name + " создан");
        });
    }

    private void saveNewWorkout(String name, String desc) {
        com.fitness.spine.data.model.Exercise ex = new com.fitness.spine.data.model.Exercise();
        ex.setTitle("Новое упражнение");
        ex.setHoldTime(5);
        ex.setRepeats("10");

        Workout w = createWorkout(name, ex);
        w.setAppName(desc);
        workouts.add(0, w);
        saveWorkouts();
        adapter.notifyDataSetChanged();
        ttsService.speak("Комплекс " + name + " сохранён");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsService != null) ttsService.shutdown();
        if (sttService != null) sttService.destroy();
    }

    static class WorkoutAdapter extends BaseAdapter {
        private List<Workout> workouts;

        WorkoutAdapter(List<Workout> workouts) {
            this.workouts = workouts;
        }

        @Override
        public int getCount() { return workouts.size(); }

        @Override
        public Object getItem(int position) { return workouts.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
            }

            Workout w = workouts.get(position);
            ((TextView) view.findViewById(R.id.tvWorkoutName)).setText(w.getAppName());
            ((TextView) view.findViewById(R.id.tvWorkoutDesc)).setText(w.getDataSource());
            ((TextView) view.findViewById(R.id.tvExercisesCount)).setText("Упражнений: " + w.getTotalExercises());

            return view;
        }
    }
}