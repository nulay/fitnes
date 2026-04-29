package com.fitness.spine.data.model;

import java.util.List;

public class LessonsCatalog {
    private String catalogVersion;
    private List<LessonInfo> lessons;

    public LessonsCatalog() {}

    public LessonsCatalog(String catalogVersion, List<LessonInfo> lessons) {
        this.catalogVersion = catalogVersion;
        this.lessons = lessons;
    }

    public String getCatalogVersion() { return catalogVersion; }
    public void setCatalogVersion(String catalogVersion) { this.catalogVersion = catalogVersion; }

    public List<LessonInfo> getLessons() { return lessons; }
    public void setLessons(List<LessonInfo> lessons) { this.lessons = lessons; }

    public int getLessonCount() {
        return lessons != null ? lessons.size() : 0;
    }
}