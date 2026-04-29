package com.fitness.spine.data.model;

import java.util.List;

public class Block {
    private String blockName;
    private List<Exercise> exercises;

    public Block() {}

    public Block(String blockName, List<Exercise> exercises) {
        this.blockName = blockName;
        this.exercises = exercises;
    }

    public String getBlockName() { return blockName; }
    public void setBlockName(String blockName) { this.blockName = blockName; }

    public List<Exercise> getExercises() { return exercises; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }

    public int getExerciseCount() {
        return exercises != null ? exercises.size() : 0;
    }
}