package com.ido.idoprojectapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;

public class PrefsHelper {
    private static final String PREFS_NAME = "user_prefs";

    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_PROFILE_URI = "profile_uri";

    private static final String KEY_CURRENT_MODEL = "current_model_json";
    private static final String KEY_CONTEXT_SIZE = "context_size";
    private static final String KEY_MAX_RESPONSE_TOKENS = "max_response_tokens";

    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_TOP_P = "top_p";
    private static final String KEY_TOP_K = "top_k";
    private static final String KEY_REPEAT_PENALTY = "repeat_penalty";
    private static final String KEY_REPEAT_LAST_N = "repeat_last_n";

    private static final String KEY_USE_SMART_CONTEXT = "use_smart_context";
    private static final String KEY_MAX_CONTEXT_MESSAGES = "max_context_messages";
    private static final String KEY_QUALITY_MODE = "quality_mode";

    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_TTS_ENABLED = "tts_enabled";


    private static final String KEY_USER_PERSONA = "user_persona_description";


    private static final String KEY_USER_SYSTEM_PROMPT = "user_defined_system_prompt";


    private static final String BASE_CONSTRAINTS =
            "SYSTEM FORMATTING RULES:\n" +
                    "1. ADOPT THE PERSONA defined by the user in the 'ASSISTANT PERSONA' section below. If told to be a specific character, animal, or object, you must STAY IN CHARACTER.\n" +
                    "2. DO NOT generate the User's response. You must STOP writing immediately after you have finished your turn.\n" +
                    "3. DO NOT generate lines starting with 'User:', 'Human:', or '###'.\n" +
                    "4. The user's specific instructions below override any default behavior.";

    private static final String DEFAULT_USER_PROMPT =
            "You are a helpful and intelligent assistant.";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public PrefsHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // ====== User Credentials ======

    public void saveCardensials(String username, String password) {
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    public void clearCardensials() {
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_PASSWORD);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    // ====== Model Management ======

    public void setCurrentModel(Model model) {
        String json = new Gson().toJson(model);
        editor.putString(KEY_CURRENT_MODEL, json).apply();
    }

    public Model getCurrentModel() {
        String json = sharedPreferences.getString(KEY_CURRENT_MODEL, null);
        if (json == null) return null;
        return new Gson().fromJson(json, Model.class);
    }

    public String getCurrentModelFilename() {
        Model model = getCurrentModel();
        return (model != null) ? model.getFilename() : null;
    }

    public void clearCurrentModel() {
        editor.remove(KEY_CURRENT_MODEL).apply();
    }

    // ====== Profile Image ======

    public void setProfileImageUri(String uriString) {
        editor.putString(KEY_PROFILE_URI, uriString);
        editor.apply();
    }

    public String getProfileImageUri() {
        return sharedPreferences.getString(KEY_PROFILE_URI, null);
    }

    public void clearProfileImageUri() {
        editor.remove(KEY_PROFILE_URI);
        editor.apply();
    }

    // ====== Token Management ======

    public int getContextSize() {
        return sharedPreferences.getInt(KEY_CONTEXT_SIZE, 2048);
    }

    public void setContextSize(int size) {
        editor.putInt(KEY_CONTEXT_SIZE, size);
        editor.apply();
    }

    public int getMaxResponseTokens() {
        return sharedPreferences.getInt(KEY_MAX_RESPONSE_TOKENS, 256);
    }

    public void setMaxResponseTokens(int tokens) {
        editor.putInt(KEY_MAX_RESPONSE_TOKENS, tokens);
        editor.apply();
    }

    // ====== Generation Parameters ======


    public String getUserSystemPrompt() {
        return sharedPreferences.getString(KEY_USER_SYSTEM_PROMPT, DEFAULT_USER_PROMPT);
    }


    public void setUserSystemPrompt(String prompt) {
        editor.putString(KEY_USER_SYSTEM_PROMPT, prompt);
        editor.apply();
    }


    public String getUserPersona() {
        return sharedPreferences.getString(KEY_USER_PERSONA, ""); // Default is blank
    }

    public void setUserPersona(String persona) {
        editor.putString(KEY_USER_PERSONA, persona);
        editor.apply();
    }


    public String getEffectiveSystemPrompt() {
        String aiPersona = getUserSystemPrompt();
        String userDescription = getUserPersona();
        String username = getUsername();

        StringBuilder finalPrompt = new StringBuilder();

        finalPrompt.append(BASE_CONSTRAINTS).append("\n\n");

        finalPrompt.append("### ASSISTANT PERSONA\n").append(aiPersona).append("\n\n");

        finalPrompt.append("### USER INFO\n");
        finalPrompt.append("User's Name: ").append(username).append("\n");
        if (!userDescription.isEmpty()) {
            finalPrompt.append("User Context: ").append(userDescription).append("\n");
        }

        return finalPrompt.toString();
    }


    public float getTemperature() {
        return sharedPreferences.getFloat(KEY_TEMPERATURE, 0.7f);
    }

    public void setTemperature(float temp) {
        editor.putFloat(KEY_TEMPERATURE, temp);
        editor.apply();
    }

    public float getTopP() {
        return sharedPreferences.getFloat(KEY_TOP_P, 0.9f);
    }

    public void setTopP(float topP) {
        editor.putFloat(KEY_TOP_P, topP);
        editor.apply();
    }

    public int getTopK() {
        return sharedPreferences.getInt(KEY_TOP_K, 40);
    }

    public void setTopK(int topK) {
        editor.putInt(KEY_TOP_K, topK);
        editor.apply();
    }

    public float getRepeatPenalty() {
        return sharedPreferences.getFloat(KEY_REPEAT_PENALTY, 1.15f);
    }

    public void setRepeatPenalty(float penalty) {
        editor.putFloat(KEY_REPEAT_PENALTY, penalty);
        editor.apply();
    }

    public int getRepeatLastN() {
        return sharedPreferences.getInt(KEY_REPEAT_LAST_N, 64);
    }

    public void setRepeatLastN(int n) {
        editor.putInt(KEY_REPEAT_LAST_N, n);
        editor.apply();
    }

    // ====== Quality Settings ======

    public boolean getUseSmartContext() {
        return sharedPreferences.getBoolean(KEY_USE_SMART_CONTEXT, true);
    }

    public void setUseSmartContext(boolean use) {
        editor.putBoolean(KEY_USE_SMART_CONTEXT, use);
        editor.apply();
    }

    public int getMaxContextMessages() {
        return sharedPreferences.getInt(KEY_MAX_CONTEXT_MESSAGES, 4);
    }

    public void setMaxContextMessages(int max) {
        editor.putInt(KEY_MAX_CONTEXT_MESSAGES, max);
        editor.apply();
    }

    public String getQualityMode() {
        return sharedPreferences.getString(KEY_QUALITY_MODE, "balanced");
    }

    public void setQualityMode(String mode) {
        editor.putString(KEY_QUALITY_MODE, mode);
        editor.apply();
    }

    // ====== App Settings ======

    public boolean isNightModeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setNightModeEnabled(boolean enabled) {
        editor.putBoolean(KEY_DARK_MODE, enabled);
        editor.apply();
    }

    public boolean isTtsEnabled() {
        return sharedPreferences.getBoolean(KEY_TTS_ENABLED, false);
    }

    public void setTtsEnabled(boolean enabled) {
        editor.putBoolean(KEY_TTS_ENABLED, enabled);
        editor.apply();
    }

    // ====== Presets ======

    public void applyFastMode() {
        editor.putFloat(KEY_TEMPERATURE, 0.5f);
        editor.putFloat(KEY_TOP_P, 0.8f);
        editor.putInt(KEY_TOP_K, 30);
        editor.putFloat(KEY_REPEAT_PENALTY, 1.1f);
        editor.putInt(KEY_MAX_RESPONSE_TOKENS, 150);
        editor.putInt(KEY_MAX_CONTEXT_MESSAGES, 2);
        editor.putString(KEY_QUALITY_MODE, "fast");
        editor.apply();
    }

    public void applyBalancedMode() {
        editor.putFloat(KEY_TEMPERATURE, 0.7f);
        editor.putFloat(KEY_TOP_P, 0.9f);
        editor.putInt(KEY_TOP_K, 40);
        editor.putFloat(KEY_REPEAT_PENALTY, 1.15f);
        editor.putInt(KEY_MAX_RESPONSE_TOKENS, 256);
        editor.putInt(KEY_MAX_CONTEXT_MESSAGES, 4);
        editor.putString(KEY_QUALITY_MODE, "balanced");
        editor.apply();
    }

    public void applyQualityMode() {
        editor.putFloat(KEY_TEMPERATURE, 0.8f);
        editor.putFloat(KEY_TOP_P, 0.95f);
        editor.putInt(KEY_TOP_K, 50);
        editor.putFloat(KEY_REPEAT_PENALTY, 1.2f);
        editor.putInt(KEY_MAX_RESPONSE_TOKENS, 512);
        editor.putInt(KEY_MAX_CONTEXT_MESSAGES, 6);
        editor.putString(KEY_QUALITY_MODE, "quality");
        editor.apply();
    }

    public void resetToDefaults() {
        applyBalancedMode();
        editor.putInt(KEY_CONTEXT_SIZE, 2048);
        editor.putBoolean(KEY_USE_SMART_CONTEXT, true);
        editor.remove(KEY_USER_SYSTEM_PROMPT); // Reset to default user prompt
        editor.apply();
    }
}