package com.fitness.spine.data.repository;

import android.content.Context;
import android.util.Log;

import com.fitness.spine.data.model.Exercise;
import com.fitness.spine.data.model.Workout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WorkoutRepositoryImpl implements WorkoutRepository {

    private static final String TAG = "WorkoutRepository";
    private final Context context;
    private final Gson gson;

    public WorkoutRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new GsonBuilder().create();
    }

    @Override
    public Workout loadDefaultWorkout() {
        String json = "{\"app_name\":\"LFK Trainer Voice\",\"version\":\"1.0\"," +
            "\"exercise_groups\":[" +
            "{\"group_name\":\"Упражнения для укрепления мышц шеи\",\"description\":\"Силу давления увеличивать постепенно. Время задержки 5-7 секунд.\"," +
            "\"exercises\":[" +
            "{\"id\":\"neck_1\",\"title\":\"Давление на лоб\",\"instruction\":\"Положите кисти на лоб. Надавливайте лбом на кисти, сопротивляясь движению.\",\"action_voice\":\"Надавите лбом на ладони\",\"hold_seconds\":7,\"repeats\":7,\"breath_instruction\":\"Дыхание ровное, не задерживайте\"}," +
            "{\"id\":\"neck_2\",\"title\":\"Давление на висок\",\"instruction\":\"Прижмите правую ладонь к правому виску. Надавливайте головой на кисть.\",\"action_voice\":\"Надавите виском на правую руку\",\"hold_seconds\":7,\"repeats\":7,\"side_switch\":\"Повторите то же самое левой рукой\"}," +
            "{\"id\":\"neck_3\",\"title\":\"Давление на затылок\",\"instruction\":\"Соедините руки на затылке. Надавливайте затылком на кисти.\",\"action_voice\":\"Надавите затылком на сцепленные руки\",\"hold_seconds\":7,\"repeats\":7}" +
            "]}" +
            ",{\"group_name\":\"Упражнения для плечевого пояса\",\"exercises\":[" +
            "{\"id\":\"shoulder_1\",\"title\":\"Подъем плеч\",\"instruction\":\"На глубоком вдохе поднимите плечи максимально вверх, на выдохе опустите.\",\"action_voice\":\"На вдохе поднимаем плечи вверх\",\"hold_seconds\":3,\"repeats\":10,\"breath_instruction\":\"Глубокий вдох при подъеме, ��олный выдох при опускании\"}," +
            "{\"id\":\"shoulder_2\",\"title\":\"Круговые вращения\",\"instruction\":\"Руки вдоль тела. Делайте круговые движения плечами назад, затем вперед.\",\"action_voice\":\"Вращаем плечи назад\",\"repeats\":10}" +
            "]}" +
            ",{\"group_name\":\"Упражнения для позвоночника на растягивание\",\"exercises\":[" +
            "{\"id\":\"stretch_1\",\"title\":\"Колени к груди\",\"instruction\":\"Лежа на спине, согните ноги, обхватите колени руками и притяните к груди.\",\"action_voice\":\"Притяните колени к груди и зафиксируйте\",\"hold_seconds\":5,\"repeats\":8}," +
            "{\"id\":\"stretch_2\",\"title\":\"Поза Кошки\",\"instruction\":\"Встаньте на четвереньки. Плавно выгните спину вверх, затем прогнитесь вниз.\",\"action_voice\":\"Выгибаем спину вверх, голову вниз\",\"hold_seconds\":5,\"repeats\":10,\"breath_instruction\":\"Выдох на прогибе вверх\"}" +
            "]}" +
            ",{\"group_name\":\"Упражнения для укрепления мышц живота и спины\",\"exercises\":[" +
            "{\"id\":\"core_1\",\"title\":\"Подъем головы и лопаток\",\"instruction\":\"Лежа на спине, руки вдоль туловища. Приподнимите голову и плечи.\",\"action_voice\":\"Приподнимаем плечи и фиксируем\",\"hold_seconds\":5,\"repeats\":10}," +
            "{\"id\":\"core_2\",\"title\":\"Попеременный подъем ног\",\"instruction\":\"Лежа на животе, руки под подбородком. Поочередно поднимайте прямые ноги.\",\"action_voice\":\"Поднимите правую ногу, держите\",\"hold_seconds\":5,\"repeats\":10}" +
            "]}" +
            "]}";
        return loadFromJson(json);
    }

    @Override
    public Workout loadFromJson(String json) {
        try {
            return gson.fromJson(json, Workout.class);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
            return loadDefaultWorkout();
        }
    }

    @Override
    public Workout loadFromFile(String filePath) {
        try {
            File file = new File(filePath);
            StringBuilder json = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();
            return loadFromJson(json.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error loading from file: " + filePath, e);
            return loadDefaultWorkout();
        }
    }

    @Override
    public Workout loadFromUrl(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return loadFromJson(response.toString());
            } else {
                Log.w(TAG, "HTTP error: " + responseCode);
                return loadDefaultWorkout();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading from URL: " + urlString, e);
            return loadDefaultWorkout();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void saveToFile(Workout workout, String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            String json = gson.toJson(workout);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(json);
            writer.close();
            Log.i(TAG, "Workout saved to: " + filePath);
        } catch (Exception e) {
            Log.e(TAG, "Error saving to file: " + filePath, e);
        }
    }
}