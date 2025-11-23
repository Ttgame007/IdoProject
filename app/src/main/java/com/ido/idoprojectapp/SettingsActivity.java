package com.ido.idoprojectapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial darkModeSwitch, ttsSwitch;
    private PrefsHelper prefs;

    // ====== Lifecycle ======

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PrefsHelper(this);

        initializeViews();
        loadCurrentSettings();
        setupListeners();
    }

    // ====== UI Setup ======

    private void initializeViews() {
        ImageButton backButton = findViewById(R.id.backButton);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        ttsSwitch = findViewById(R.id.ttsSwitch);

        backButton.setOnClickListener(v -> finish());
    }

    private void loadCurrentSettings() {
        darkModeSwitch.setChecked(prefs.isNightModeEnabled());
        ttsSwitch.setChecked(prefs.isTtsEnabled());
    }

    // ====== Logic ======

    private void setupListeners() {
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setNightModeEnabled(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        ttsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setTtsEnabled(isChecked);
            if(isChecked){
                Toast.makeText(this, "AI will now speak responses", Toast.LENGTH_SHORT).show();
            }
        });
    }
}