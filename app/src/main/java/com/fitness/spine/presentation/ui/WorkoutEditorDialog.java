package com.fitness.spine.presentation.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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

public class WorkoutEditorDialog {

    public interface OnWorkoutSaveListener {
        void onWorkoutSaved(Workout workout);
    }

    public static void showEditDialog(Context context, TtsService tts, SttService stt, Workout workout, OnWorkoutSaveListener listener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_workout_editor, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvEditorTitle);
        EditText etAppName = dialogView.findViewById(R.id.etAppName);
        EditText etDataSource = dialogView.findViewById(R.id.etDataSource);
        TextView tvExercisesCount = dialogView.findViewById(R.id.tvExercisesCount);
        ListView lvExercises = dialogView.findViewById(R.id.lvExercises);

        Button btnVoiceAdd = dialogView.findViewById(R.id.btnVoiceAdd);
        Button btnVoiceEdit = dialogView.findViewById(R.id.btnVoiceEdit);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);
        Button btnEdit = dialogView.findViewById(R.id.btnEdit);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (tvTitle != null) tvTitle.setText("РЕДАКТОР КОМПЛЕКСА");

        etAppName.setText(workout.getAppName());
        etDataSource.setText(workout.getDataSource());

        List<Exercise> exercises = new ArrayList<>();
        for (Block block : workout.getBlocks()) {
            if (block.getExercises() != null) {
                exercises.addAll(block.getExercises());
            }
        }

        int count = exercises.size();
        tvExercisesCount.setText("Упражнений: " + count);

        ExerciseAdapter adapter = new ExerciseAdapter(exercises);
        lvExercises.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnVoiceAdd.setOnClickListener(v -> {
            VoiceWorkoutEditor.showCreateExerciseDialog(context, tts, stt, exercise -> {
                exercises.add(exercise);
                updateWorkout(workout, exercises, etAppName.getText().toString(), etDataSource.getText().toString());
                tvExercisesCount.setText("Упражнений: " + exercises.size());
                adapter.notifyDataSetChanged();
            });
        });

        btnVoiceEdit.setOnClickListener(v -> {
            int selected = lvExercises.getCheckedItemPosition();
            if (selected >= 0 && selected < exercises.size()) {
                VoiceWorkoutEditor.showCreateExerciseDialog(context, tts, stt, updated -> {
                    exercises.set(selected, updated);
                    updateWorkout(workout, exercises, etAppName.getText().toString(), etDataSource.getText().toString());
                    adapter.notifyDataSetChanged();
                });
            } else {
                Toast.makeText(context, "Выберите упражнение", Toast.LENGTH_SHORT).show();
            }
        });

        btnAdd.setOnClickListener(v -> {
            Exercise newEx = new Exercise();
            newEx.setTitle("Новое");
            newEx.setHoldTime(5);
            newEx.setRepeats("10");
            exercises.add(newEx);
            updateWorkout(workout, exercises, etAppName.getText().toString(), etDataSource.getText().toString());
            tvExercisesCount.setText("Упражнений: " + exercises.size());
            adapter.notifyDataSetChanged();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                .setTitle("Удалить комплекс?")
                .setPositiveButton("Удалить", (d, w) -> dialog.dismiss())
                .setNegativeButton("Отмена", null)
                .show();
        });

        btnSave.setOnClickListener(v -> {
            workout.setAppName(etAppName.getText().toString().trim());
            workout.setDataSource(etDataSource.getText().toString().trim());
            updateWorkout(workout, exercises, workout.getAppName(), workout.getDataSource());
            listener.onWorkoutSaved(workout);
            dialog.dismiss();
        });

        lvExercises.setOnItemLongClickListener((parent, view, position, id) -> {
            showExerciseOptions(context, exercises.get(position), updated -> {
                if (updated != null) {
                    exercises.set(position, updated);
                } else {
                    exercises.remove(position);
                }
                updateWorkout(workout, exercises, etAppName.getText().toString(), etDataSource.getText().toString());
                tvExercisesCount.setText("Упражнений: " + exercises.size());
                adapter.notifyDataSetChanged();
            });
            return true;
        });

        dialog.show();
    }

    private static void updateWorkout(Workout workout, List<Exercise> exercises, String name, String desc) {
        List<Block> blocks = new ArrayList<>();
        if (!exercises.isEmpty()) {
            Block block = new Block();
            block.setBlockName("Блок 1");
            block.setExercises(exercises);
            blocks.add(block);
        }
        workout.setAppName(name);
        workout.setDataSource(desc);
        workout.setBlocks(blocks);
    }

    private static void showExerciseOptions(Context context, Exercise exercise, OnExerciseCallback callback) {
        String[] options = {"Изменить", "Удалить", "Отмена"};
        new AlertDialog.Builder(context)
            .setTitle(exercise.getTitle())
            .setItems(options, (d, which) -> {
                if (which == 0) callback.onResult(exercise);
                else if (which == 1) callback.onResult(null);
            })
            .show();
    }

    public interface OnExerciseCallback {
        void onResult(Exercise exercise);
    }

    public static void showCreateNewDialog(Context context, TtsService tts, SttService stt, OnWorkoutSaveListener listener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_workout, null);

        EditText etName = dialogView.findViewById(R.id.etNewWorkoutName);
        EditText etDescription = dialogView.findViewById(R.id.etNewWorkoutDescription);
        Button btnVoiceCreate = dialogView.findViewById(R.id.btnVoiceCreate);
        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelNew);

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnVoiceCreate.setOnClickListener(v -> {
            dialog.dismiss();
            VoiceWorkoutEditor.showCreateExerciseDialog(context, tts, stt, exercise -> {
                listener.onWorkoutSaved(createWorkoutFromExercise(exercise));
            });
        });

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Введите название");
                return;
            }
            listener.onWorkoutSaved(createEmptyWorkout(name, desc));
            dialog.dismiss();
        });

        dialog.show();
    }

    private static Workout createEmptyWorkout(String name, String description) {
        List<Block> blocks = new ArrayList<>();
        List<Exercise> exercises = new ArrayList<>();
        Exercise ex = new Exercise();
        ex.setId("ex_1");
        ex.setTitle("Новое упражнение");
        ex.setFullText("Описание");
        ex.setHoldTime(5);
        ex.setRepeats("10");
        exercises.add(ex);
        Block block = new Block();
        block.setBlockName("Блок 1");
        block.setExercises(exercises);
        blocks.add(block);
        Workout w = new Workout();
        w.setAppName(name);
        w.setDataSource(description);
        w.setBlocks(blocks);
        return w;
    }

    private static Workout createWorkoutFromExercise(Exercise exercise) {
        List<Block> blocks = new ArrayList<>();
        List<Exercise> exercises = new ArrayList<>();
        exercises.add(exercise);
        Block block = new Block();
        block.setBlockName("Блок 1");
        block.setExercises(exercises);
        blocks.add(block);
        Workout w = new Workout();
        w.setAppName(exercise.getTitle());
        w.setDataSource(exercise.getFullText());
        w.setBlocks(blocks);
        return w;
    }

    static class ExerciseAdapter extends BaseAdapter {
        private List<Exercise> exercises;
        ExerciseAdapter(List<Exercise> exercises) { this.exercises = exercises; }
        @Override public int getCount() { return exercises.size(); }
        @Override public Object getItem(int pos) { return exercises.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            Exercise ex = exercises.get(position);
            ((android.widget.TextView) view.findViewById(android.R.id.text1)).setText((position + 1) + ". " + ex.getTitle());
            return view;
        }
    }
}