package com.ido.idoprojectapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AiActivity extends AppCompatActivity {
    Button logOut;
    TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai);

        //inisializing variables
        logOut = findViewById(R.id.button);
        welcomeText = findViewById(R.id.TVguest);

        //welcome text with username
        PrefsHelper prefs = new PrefsHelper(this);
        String username = prefs.getUsername();
        welcomeText.setText(username);

        //log out test function delete and move later
        logOut.setOnClickListener(v -> {
                    prefs.clearCardensials();
                    finish();
                });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}