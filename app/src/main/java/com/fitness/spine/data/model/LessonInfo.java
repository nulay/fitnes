package com.fitness.spine.data.model;

public class LessonInfo {
    private String id;
    private String title;
    private String description;
    private String difficulty;
    private int durationMinutes;
    private String file;

    public LessonInfo() {}

    public LessonInfo(String id, String title, String description, String difficulty, int durationMinutes, String file) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.durationMinutes = durationMinutes;
        this.file = file;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public String getDifficultyDisplay() {
        if (difficulty == null) return "";
        switch (difficulty) {
            case "beginner": return "Начальный";
            case "intermediate": return "Средний";
            case "advanced": return "Продвинутый";
            default: return difficulty;
        }
    }
}