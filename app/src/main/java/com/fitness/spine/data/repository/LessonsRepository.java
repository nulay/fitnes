package com.fitness.spine.data.repository;

import android.content.Context;
import android.util.Log;

import com.fitness.spine.data.model.LessonInfo;
import com.fitness.spine.data.model.LessonsCatalog;
import com.fitness.spine.data.model.Workout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LessonsRepository {

    private static final String TAG = "LessonsRepository";

    private final Context context;
    private final Gson gson;
    private final String serverBaseUrl;

    public interface LoadCallback {
        void onSuccess(LessonsCatalog catalog);
        void onError(String error);
    }

    public interface LoadWorkoutCallback {
        void onSuccess(Workout workout);
        void onError(String error);
    }

    public LessonsRepository(Context context) {
        this(context, "");
    }

    public LessonsRepository(Context context, String serverBaseUrl) {
        this.context = context.getApplicationContext();
        this.gson = new GsonBuilder().create();
        this.serverBaseUrl = serverBaseUrl;
    }

    public void loadCatalogFromAssets(LoadCallback callback) {
        try {
            Reader reader = new InputStreamReader(context.getAssets().open("lessons_catalog.json"));
            LessonsCatalog catalog = gson.fromJson(reader, LessonsCatalog.class);
            reader.close();
            callback.onSuccess(catalog);
        } catch (Exception e) {
            Log.e(TAG, "Error loading catalog from assets", e);
            callback.onError("Ошибка загрузки каталога: " + e.getMessage());
        }
    }

    public void loadCatalogFromInternalStorage(LoadCallback callback) {
        try {
            File file = new File(context.getFilesDir(), "lessons_catalog.json");
            if (!file.exists()) {
                loadCatalogFromAssets(callback);
                return;
            }
            Reader reader = new FileReader(file);
            LessonsCatalog catalog = gson.fromJson(reader, LessonsCatalog.class);
            reader.close();
            callback.onSuccess(catalog);
        } catch (Exception e) {
            Log.e(TAG, "Error loading catalog from storage", e);
            loadCatalogFromAssets(callback);
        }
    }

    public void loadCatalogFromServer(LoadCallback callback) {
        if (serverBaseUrl == null || serverBaseUrl.isEmpty()) {
            callback.onError("URL сервера не указан");
            return;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(serverBaseUrl + "/lessons_catalog.json");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Reader reader = new InputStreamReader(connection.getInputStream(), "UTF-8");
                LessonsCatalog catalog = gson.fromJson(reader, LessonsCatalog.class);
                reader.close();
                callback.onSuccess(catalog);
            } else {
                callback.onError("Ошибка сервера: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading catalog from server", e);
            callback.onError("Ошибка загрузки: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void loadCatalogFromAllSources(LoadCallback callback) {
        loadCatalogFromServer(new LoadCallback() {
            @Override
            public void onSuccess(LessonsCatalog catalog) {
                callback.onSuccess(catalog);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Server failed, trying storage: " + error);
                loadCatalogFromInternalStorage(callback);
            }
        });
    }

    public void loadWorkoutFromAssets(String fileName, LoadWorkoutCallback callback) {
        try {
            Reader reader = new InputStreamReader(context.getAssets().open(fileName));
            Workout workout = gson.fromJson(reader, Workout.class);
            reader.close();
            callback.onSuccess(workout);
        } catch (Exception e) {
            Log.e(TAG, "Error loading workout from assets: " + fileName, e);
            callback.onError("Ошибка загрузки: " + e.getMessage());
        }
    }

    public void loadWorkoutFromInternalStorage(String fileName, LoadWorkoutCallback callback) {
        try {
            File file = new File(context.getFilesDir(), fileName);
            if (!file.exists()) {
                loadWorkoutFromAssets(fileName, callback);
                return;
            }
            Reader reader = new FileReader(file);
            Workout workout = gson.fromJson(reader, Workout.class);
            reader.close();
            callback.onSuccess(workout);
        } catch (Exception e) {
            Log.e(TAG, "Error loading workout from storage: " + fileName, e);
            loadWorkoutFromAssets(fileName, callback);
        }
    }

    public void loadWorkoutFromServer(String fileName, LoadWorkoutCallback callback) {
        if (serverBaseUrl == null || serverBaseUrl.isEmpty()) {
            callback.onError("URL сервера не указан");
            return;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(serverBaseUrl + "/" + fileName);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Reader reader = new InputStreamReader(connection.getInputStream(), "UTF-8");
                Workout workout = gson.fromJson(reader, Workout.class);
                reader.close();
                callback.onSuccess(workout);
            } else {
                callback.onError("Ошибка сервера: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading workout from server: " + fileName, e);
            callback.onError("Ошибка загрузки: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void loadWorkout(String fileName, LoadWorkoutCallback callback) {
        loadWorkoutFromServer(fileName, new LoadWorkoutCallback() {
            @Override
            public void onSuccess(Workout workout) {
                callback.onSuccess(workout);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Server failed, trying storage: " + error);
                loadWorkoutFromInternalStorage(fileName, callback);
            }
        });
    }

    public void saveCatalogToStorage(LessonsCatalog catalog) {
        try {
            File file = new File(context.getFilesDir(), "lessons_catalog.json");
            String json = gson.toJson(catalog);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(json);
            writer.close();
            Log.i(TAG, "Catalog saved to storage");
        } catch (Exception e) {
            Log.e(TAG, "Error saving catalog", e);
        }
    }

    public void saveWorkoutToStorage(Workout workout, String fileName) {
        try {
            File file = new File(context.getFilesDir(), fileName);
            String json = gson.toJson(workout);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(json);
            writer.close();
            Log.i(TAG, "Workout saved to storage: " + fileName);
        } catch (Exception e) {
            Log.e(TAG, "Error saving workout: " + fileName, e);
        }
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public void setServerBaseUrl(String url) {
    }
}