package com.ido.idoprojectapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    Button signIn;
    Button signUp;

    ImageView SignInForeground;

    LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        signIn = findViewById(R.id.signIn);
        signUp = findViewById(R.id.signUpText);
        SignInForeground = findViewById(R.id.signInForeground);
        layout = findViewById(R.id.layout);
        layout.setVisibility(View.INVISIBLE);
        SignInForeground.setVisibility(View.INVISIBLE);
        signUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUp.class);
            startActivity(intent);
        });
        signIn.setOnClickListener(v -> {
            layout.setVisibility(View.VISIBLE);
            SignInForeground.setVisibility(View.VISIBLE);
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}