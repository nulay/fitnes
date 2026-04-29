package com.fitness.spine.data.model;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Workout {
    private String appName;
    private String dataSource;
    private List<Block> blocks;

    public Workout() {}

    public Workout(String appName, String dataSource, List<Block> blocks) {
        this.appName = appName;
        this.dataSource = dataSource;
        this.blocks = blocks;
    }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public List<Block> getBlocks() { return blocks; }
    public void setBlocks(List<Block> blocks) { this.blocks = blocks; }

    public int getTotalExercises() {
        if (blocks == null) return 0;
        int count = 0;
        for (Block block : blocks) {
            if (block.getExercises() != null) {
                count += block.getExercises().size();
            }
        }
        return count;
    }

    private static final Gson GSON = new Gson();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Workout fromJson(String json) {
        try {
            return GSON.fromJson(json, Workout.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<Workout> fromJsonList(String json) {
        try {
            Type type = new TypeToken<List<Workout>>(){}.getType();
            return GSON.fromJson(json, type);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static String toJson(List<Workout> workouts) {
        return GSON.toJson(workouts);
    }
}