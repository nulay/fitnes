package com.fitness.spine.presentation.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fitness.spine.R;
import com.fitness.spine.data.model.AIConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "lfk_trainer_prefs";
    private static final String KEY_VOICE_MODE = "voice_mode";
    private static final String KEY_CONFIGS = "ai_configs";
    private static final String KEY_ACTIVE_CONFIG = "active_config";

    public static final int MODE_SIMPLE = 0;
    public static final int MODE_AI = 1;

    private RadioGroup rgVoiceMode;
    private RadioButton rbSimple, rbAI;
    private ListView lvConfigs;
    private Button btnAddConfig;
    private ImageButton btnBack;
    private SharedPreferences prefs;
    private Gson gson;
    private List<AIConfig> configs;
    private ArrayAdapter<AIConfig> adapter;

    private static final int COLOR_INACTIVE = 0xFF3D4554;
    private static final int COLOR_ACTIVE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_INACTIVE = 0xFFA4B0BE;
    private static final int COLOR_TEXT_ACTIVE = 0xFF1E272E;

    private Handler handler = new Handler(Looper.getMainLooper());

    private static String getDefaultUrl(String model) {
        if (model == null) return "";
        switch (model) {
            case "gpt-4o-mini":
            case "gpt-3.5-turbo": return "https://api.openai.com/v1/chat/completions";
            case "claude-3-haiku": return "https://api.anthropic.com/v1/messages";
            case "yandexgpt": return "https://llm.api.yandex.ru/v1/completion";
            case "gemini-2.0-flash-exp":
            case "gemini-1.5-flash": return "https://generativelanguage.googleapis.com/v1/models/" + model + ":generateContent";
            default: return "";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();

        initViews();
        loadConfigs();
        setupListeners();
    }

    private void initViews() {
        rgVoiceMode = findViewById(R.id.rgVoiceMode);
        rbSimple = findViewById(R.id.rbSimple);
        rbAI = findViewById(R.id.rbAI);
        lvConfigs = findViewById(R.id.lvConfigs);
        btnAddConfig = findViewById(R.id.btnAddConfig);
        btnBack = findViewById(R.id.btnBack);

        int mode = prefs.getInt(KEY_VOICE_MODE, MODE_SIMPLE);

        String json = prefs.getString(KEY_CONFIGS, "[]");
        Type type = new TypeToken<List<AIConfig>>(){}.getType();
        List<AIConfig> savedConfigs = gson.fromJson(json, type);
        boolean hasConfigs = savedConfigs != null && !savedConfigs.isEmpty();

        if (mode == MODE_AI) {
            if (hasConfigs) {
                rbAI.setChecked(true);
            } else {
                rbSimple.setChecked(true);
                prefs.edit().putInt(KEY_VOICE_MODE, MODE_SIMPLE).apply();
                Toast.makeText(this, "Настройте конфигурацию API!", Toast.LENGTH_LONG).show();
                handler.postDelayed(() -> showConfigDialog(null, -1), 500);
            }
        } else {
            rbSimple.setChecked(true);
            if (!hasConfigs) {
                prefs.edit().putInt(KEY_VOICE_MODE, MODE_SIMPLE).apply();
            }
        }
        updateListColors(mode == MODE_AI);
    }

    private void updateListColors(boolean aiMode) {
        int bgColor = aiMode ? COLOR_ACTIVE : COLOR_INACTIVE;
        lvConfigs.setBackgroundColor(bgColor);
    }

    private void loadConfigs() {
        String json = prefs.getString(KEY_CONFIGS, "[]");
        Type type = new TypeToken<List<AIConfig>>(){}.getType();
        configs = gson.fromJson(json, type);
        if (configs == null) configs = new ArrayList<>();

        adapter = new ArrayAdapter<AIConfig>(this, R.layout.item_config, configs) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.item_config, parent, false);
                }

                AIConfig config = getItem(position);
                TextView tvName = view.findViewById(R.id.tvConfigName);
                TextView tvModel = view.findViewById(R.id.tvConfigModel);
                Switch switchActive = view.findViewById(R.id.switchActive);

                tvName.setText(config.getName());
                tvModel.setText(config.getModel());
                switchActive.setChecked(config.isActive());

                switchActive.setOnCheckedChangeListener((button, isChecked) -> {
                    if (isChecked) {
                        setActiveConfig(config.getId());
                    }
                });

                view.setOnLongClickListener(v -> {
                    showConfigOptions(position);
                    return true;
                });

                return view;
            }
        };

        lvConfigs.setAdapter(adapter);
    }

    private void setupListeners() {
        rgVoiceMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAI) {
                if (!hasValidAIConfig()) {
                    rbSimple.setChecked(true);
                    Toast.makeText(this, "Настройте хотя бы один API ключ!", Toast.LENGTH_LONG).show();
                    showConfigDialog(null, -1);
                    return;
                }
                prefs.edit().putInt(KEY_VOICE_MODE, MODE_AI).apply();
                updateListColors(true);
            } else {
                prefs.edit().putInt(KEY_VOICE_MODE, MODE_SIMPLE).apply();
                updateListColors(false);
            }
        });

        lvConfigs.setOnItemClickListener((parent, view, position, id) -> {
            AIConfig config = configs.get(position);
            setActiveConfig(config.getId());
        });

        lvConfigs.setOnItemLongClickListener((parent, view, position, id) -> {
            showConfigOptions(position);
            return true;
        });

        btnAddConfig.setOnClickListener(v -> showConfigDialog(null, -1));
        btnBack.setOnClickListener(v -> finish());
    }

    private boolean hasValidAIConfig() {
        for (AIConfig c : configs) {
            String key = c.getApiKey();
            if (key != null && !key.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void setActiveConfig(String configId) {
        for (AIConfig c : configs) {
            c.setActive(c.getId().equals(configId));
        }
        prefs.edit().putString(KEY_ACTIVE_CONFIG, configId).apply();
        adapter.notifyDataSetChanged();

        for (AIConfig c : configs) {
            if (c.isActive()) {
                Toast.makeText(this, "Выбрано: " + c.getName(), Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    private void showConfigOptions(int position) {
        String[] options = {"Редактировать", "Удалить", "Отмена"};

        new AlertDialog.Builder(this)
            .setTitle(configs.get(position).getName())
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    showConfigDialog(configs.get(position), position);
                } else if (which == 1) {
                    deleteConfig(position);
                }
            })
            .show();
    }

    private void showConfigDialog(AIConfig existingConfig, int position) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_config_edit, null);

        EditText etName = dialogView.findViewById(R.id.etConfigName);
        EditText etApiKey = dialogView.findViewById(R.id.etConfigApiKey);
        Spinner spinnerModel = dialogView.findViewById(R.id.spinnerModel);
        EditText etCustomUrl = dialogView.findViewById(R.id.etCustomUrl);
        Button btnSave = dialogView.findViewById(R.id.btnSaveConfig);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelConfig);

        List<String> models = Arrays.asList(
            "gemini-2.0-flash-exp",
            "gemini-1.5-flash",
            "gpt-4o-mini",
            "gpt-3.5-turbo",
            "claude-3-haiku",
            "yandexgpt",
            "Другая..."
        );

        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, models);
        spinnerModel.setAdapter(modelAdapter);

        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selected = models.get(pos);
                if ("Другая...".equals(selected)) {
                    etCustomUrl.setEnabled(true);
                    etCustomUrl.setHint("https://api.custom.com/v1");
                } else {
                    etCustomUrl.setEnabled(false);
                    etCustomUrl.setText(getDefaultUrl(selected));
                    etCustomUrl.setHint("URL (авто)");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (existingConfig != null) {
            etName.setText(existingConfig.getName());
            etApiKey.setText(existingConfig.getApiKey());
            etCustomUrl.setText(existingConfig.getCustomUrl());
            String model = existingConfig.getModel();
            int pos = modelAdapter.getPosition(model);
            if (pos >= 0) {
                spinnerModel.setSelection(pos);
            } else {
                spinnerModel.setSelection(models.indexOf("Другая..."));
                etCustomUrl.setEnabled(true);
            }
        } else {
            etCustomUrl.setEnabled(false);
            etCustomUrl.setText(getDefaultUrl("gemini-2.0-flash-exp"));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String apiKey = etApiKey.getText().toString().trim();
            String model = spinnerModel.getSelectedItem().toString();
            String customUrl = etCustomUrl.getText().toString().trim();

            if (name.isEmpty()) {
                name = model;
                if ("Другая...".equals(model)) {
                    name = "Custom";
                }
                etName.setText(name);
            }

            if (apiKey.isEmpty()) {
                etApiKey.setError("Введите API ключ");
                return;
            }

            AIConfig savedConfig;
            if (position >= 0) {
                savedConfig = configs.get(position);
                savedConfig.setName(name);
                savedConfig.setApiKey(apiKey);
                savedConfig.setModel(model);
                savedConfig.setCustomUrl(customUrl);
            } else {
                savedConfig = new AIConfig(name, apiKey, model, customUrl);
                configs.add(0, savedConfig);
            }

            // Активировать и переключить на AI режим
            savedConfig.setActive(true);
            for (int i = 0; i < configs.size(); i++) {
                if (i != configs.indexOf(savedConfig)) {
                    configs.get(i).setActive(false);
                }
            }
            prefs.edit()
                .putInt(KEY_VOICE_MODE, MODE_AI)
                .putString(KEY_ACTIVE_CONFIG, savedConfig.getId())
                .apply();

            rbAI.setChecked(true);
            updateListColors(true);

            saveConfigs();
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteConfig(int position) {
        AIConfig deleting = configs.get(position);
        boolean wasActive = deleting.isActive();
        configs.remove(position);

        if (configs.isEmpty()) {
            prefs.edit()
                .putInt(KEY_VOICE_MODE, MODE_SIMPLE)
                .remove(KEY_ACTIVE_CONFIG)
                .apply();
            rbSimple.setChecked(true);
            updateListColors(false);
            Toast.makeText(this, "Переключено на простой режим", Toast.LENGTH_SHORT).show();
        } else if (wasActive) {
            String activeId = configs.get(0).getId();
            configs.get(0).setActive(true);
            prefs.edit().putString(KEY_ACTIVE_CONFIG, activeId).apply();
        }

        saveConfigs();
        adapter.notifyDataSetChanged();
    }

    private void saveConfigs() {
        String json = gson.toJson(configs);
        prefs.edit().putString(KEY_CONFIGS, json).apply();
    }

    public static String getApiKey(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String activeId = p.getString(KEY_ACTIVE_CONFIG, "");

        String json = p.getString(KEY_CONFIGS, "[]");
        Type type = new TypeToken<List<AIConfig>>(){}.getType();
        List<AIConfig> list = new Gson().fromJson(json, type);

        if (list != null) {
            for (AIConfig c : list) {
                String key = c.getApiKey();
                if ((c.isActive() || (activeId.isEmpty() && list.indexOf(c) == 0))
                        && key != null && !key.trim().isEmpty()) {
                    return key;
                }
            }
        }
        return "";
    }

    public static String getModel(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String activeId = p.getString(KEY_ACTIVE_CONFIG, "");

        String json = p.getString(KEY_CONFIGS, "[]");
        Type type = new TypeToken<List<AIConfig>>(){}.getType();
        List<AIConfig> list = new Gson().fromJson(json, type);

        if (list != null) {
            for (AIConfig c : list) {
                String key = c.getApiKey();
                if ((c.isActive() || (activeId.isEmpty() && list.indexOf(c) == 0))
                        && key != null && !key.trim().isEmpty()) {
                    return c.getModel();
                }
            }
        }
        return "gemini-2.0-flash-exp";
    }

    public static String getCustomUrl(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String activeId = p.getString(KEY_ACTIVE_CONFIG, "");

        String json = p.getString(KEY_CONFIGS, "[]");
        Type type = new TypeToken<List<AIConfig>>(){}.getType();
        List<AIConfig> list = new Gson().fromJson(json, type);

        if (list != null) {
            for (AIConfig c : list) {
                String key = c.getApiKey();
                if ((c.isActive() || (activeId.isEmpty() && list.indexOf(c) == 0))
                        && key != null && !key.trim().isEmpty()) {
                    return c.getCustomUrl();
                }
            }
        }
        return "";
    }

    public static int getVoiceMode(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return p.getInt(KEY_VOICE_MODE, MODE_SIMPLE);
    }

    public static boolean isAIMode(Context context) {
        return getVoiceMode(context) == MODE_AI;
    }
}