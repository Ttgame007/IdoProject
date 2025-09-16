package com.ido.idoprojectapp;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsHelper {
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    public PrefsHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    public void saveCardensials(String username, String password) {
        editor.putString("username", username);
        editor.putString("password", password);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUsername() {
        return prefs.getString("username", null);
    }
    public void clearCardensials() {
        editor.remove("username");
        editor.remove("password");
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }
}
