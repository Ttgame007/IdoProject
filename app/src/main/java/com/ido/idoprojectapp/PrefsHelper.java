package com.ido.idoprojectapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;

public class PrefsHelper {
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_CURRENT_MODEL = "current_model_json";

    private final SharedPreferences sharedPreferences;

    public PrefsHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveCardensials(String username, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
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
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_PASSWORD);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    public void setCurrentModel(Model model) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
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
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_CURRENT_MODEL).apply();
    }
}