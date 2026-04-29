package com.fitness.spine.data.model;

import java.util.List;

public class Exercise {
    private String id;
    private String title;
    private String fullText;
    private int holdTime;
    private int relaxTime;
    private String repeats;
    private String breathing;
    private String action;
    private List<String> sides;

    public Exercise() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }

    public int getHoldTime() { return holdTime; }
    public void setHoldTime(int holdTime) { this.holdTime = holdTime; }

    public int getRelaxTime() { return relaxTime; }
    public void setRelaxTime(int relaxTime) { this.relaxTime = relaxTime; }

    public String getRepeats() { return repeats; }
    public void setRepeats(String repeats) { this.repeats = repeats; }

    public String getBreathing() { return breathing; }
    public void setBreathing(String breathing) { this.breathing = breathing; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public List<String> getSides() { return sides; }
    public void setSides(List<String> sides) { this.sides = sides; }

    public boolean hasHoldTime() {
        return holdTime > 0;
    }

    public boolean hasSides() {
        return sides != null && !sides.isEmpty();
    }

    public int getRepeatsCount() {
        if (repeats == null || repeats.isEmpty()) {
            return 10;
        }
        if (repeats.contains("-")) {
            try {
                return Integer.parseInt(repeats.split("-")[0]);
            } catch (Exception e) {
                return 10;
            }
        }
        try {
            return Integer.parseInt(repeats);
        } catch (Exception e) {
            return 10;
        }
    }

    public String getDisplayTitle() {
        return title != null ? title : "";
    }
}