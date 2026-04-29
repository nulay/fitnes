package com.fitness.spine.data.repository;

import com.fitness.spine.data.model.Workout;

public interface WorkoutRepository {
    Workout loadDefaultWorkout();
    Workout loadFromJson(String json);
    Workout loadFromFile(String filePath);
    Workout loadFromUrl(String url);
}