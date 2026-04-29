package com.fitness.spine.data.model;

import java.util.ArrayList;
import java.util.List;

public class AIConfig {
    private String id;
    private String name;
    private String apiKey;
    private String model;
    private String customUrl;
    private boolean isActive;

    public AIConfig() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public AIConfig(String name, String apiKey, String model, String customUrl) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.apiKey = apiKey;
        this.model = model;
        this.customUrl = customUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getCustomUrl() { return customUrl; }
    public void setCustomUrl(String customUrl) { this.customUrl = customUrl; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}