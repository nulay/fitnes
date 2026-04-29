package com.fitness.spine.domain.usecase;

import com.fitness.spine.data.model.Block;
import com.fitness.spine.data.model.Exercise;
import com.fitness.spine.data.model.Workout;

public class WorkoutIterator {

    private Workout workout;
    private int currentBlockIndex = 0;
    private int currentExerciseIndex = 0;
    private int currentRepetition = 0;
    private int currentSideIndex = 0;
    private int totalRepetitions = 0;

    public WorkoutIterator(Workout workout) {
        this.workout = workout;
        reset();
    }

    public void reset() {
        currentBlockIndex = 0;
        currentExerciseIndex = 0;
        currentRepetition = 0;
        currentSideIndex = 0;
        totalRepetitions = 0;
    }

    public void updateWorkout(Workout workout) {
        this.workout = workout;
        reset();
    }

    public boolean hasNext() {
        if (workout == null || workout.getBlocks() == null) {
            return false;
        }

        if (currentBlockIndex >= workout.getBlocks().size()) {
            return false;
        }

        Block block = workout.getBlocks().get(currentBlockIndex);
        if (block == null || block.getExercises() == null) {
            return false;
        }

        return currentExerciseIndex < block.getExercises().size();
    }

    public boolean hasNextRepetition() {
        Exercise current = getCurrentExercise();
        if (current == null) {
            return false;
        }
        return currentRepetition < current.getRepeatsCount();
    }

    public boolean isLastRepetition() {
        Exercise current = getCurrentExercise();
        if (current == null) {
            return false;
        }
        return currentRepetition == current.getRepeatsCount() - 1;
    }

    public Exercise getCurrentExercise() {
        if (!hasNext()) {
            return null;
        }
        return workout.getBlocks()
            .get(currentBlockIndex)
            .getExercises()
            .get(currentExerciseIndex);
    }

    public Block getCurrentBlock() {
        if (workout == null || workout.getBlocks() == null) {
            return null;
        }
        if (currentBlockIndex >= workout.getBlocks().size()) {
            return null;
        }
        return workout.getBlocks().get(currentBlockIndex);
    }

    public int getCurrentRepetition() {
        return currentRepetition;
    }

    public int getCurrentSideIndex() {
        return currentSideIndex;
    }

    public int getCurrentBlockIndex() {
        return currentBlockIndex;
    }

    public void resetCurrentRepetition() {
        currentRepetition = 0;
        currentSideIndex = 0;
    }

    public boolean hasMoreSides() {
        Exercise ex = getCurrentExercise();
        if (ex == null || !ex.hasSides()) {
            return false;
        }
        int sidesCount = ex.getSides().size();
        return currentSideIndex < sidesCount - 1;
    }

    public void nextSide() {
        currentSideIndex++;
    }

    public String getCurrentSide() {
        Exercise ex = getCurrentExercise();
        if (ex == null || !ex.hasSides() || ex.getSides() == null) {
            return null;
        }
        if (currentSideIndex < ex.getSides().size()) {
            return ex.getSides().get(currentSideIndex);
        }
        return null;
    }

    public void nextRepetition() {
        Exercise ex = getCurrentExercise();
        if (ex != null && ex.hasSides()) {
            if (currentSideIndex < ex.getSides().size() - 1) {
                currentSideIndex++;
            } else {
                currentSideIndex = 0;
                currentRepetition++;
            }
        } else {
            currentRepetition++;
        }
    }

    public boolean next() {
        if (!hasNext()) {
            return false;
        }

        currentExerciseIndex++;
        currentRepetition = 0;
        currentSideIndex = 0;

        while (currentBlockIndex < workout.getBlocks().size()) {
            Block block = workout.getBlocks().get(currentBlockIndex);
            if (block == null || block.getExercises() == null) {
                currentBlockIndex++;
                currentExerciseIndex = 0;
                continue;
            }

            if (currentExerciseIndex < block.getExercises().size()) {
                return true;
            }

            currentBlockIndex++;
            currentExerciseIndex = 0;
        }

        return false;
    }

    public int getGlobalPosition() {
        return totalRepetitions;
    }

    public void incrementTotalRepetitions() {
        totalRepetitions++;
    }

    public int getTotalRepetitions() {
        return totalRepetitions;
    }

    public int getTotalExercisesInWorkout() {
        if (workout == null || workout.getBlocks() == null) {
            return 0;
        }

        int count = 0;
        for (Block block : workout.getBlocks()) {
            if (block != null && block.getExercises() != null) {
                count += block.getExercises().size();
            }
        }
        return count;
    }

    public int getTotalRepetitionsInWorkout() {
        if (workout == null || workout.getBlocks() == null) {
            return 0;
        }

        int count = 0;
        for (Block block : workout.getBlocks()) {
            if (block != null && block.getExercises() != null) {
                for (Exercise ex : block.getExercises()) {
                    if (ex != null) {
                        count += ex.getRepeatsCount();
                    }
                }
            }
        }
        return count;
    }

    public Workout getWorkout() {
        return workout;
    }
}