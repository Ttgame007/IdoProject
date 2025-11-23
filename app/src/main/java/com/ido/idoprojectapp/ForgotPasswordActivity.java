package com.ido.idoprojectapp;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.regex.Pattern;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etNewPass, etConfirmPass;
    private Button btnReset;
    private HelperUserDB userDb;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +         // at least 1 digit
                    "(?=.*[a-z])" +         // at least 1 lower case letter
                    "(?=.*[A-Z])" +         // at least 1 upper case letter
                    "(?=.*[@#$%^&+=])" +    // at least 1 special character
                    "(?=\\S+$)" +           // no white spaces
                    ".{8,}" +               // at least 8 characters
                    "$");

    // ====== Lifecycle ======

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        userDb = new HelperUserDB(this);

        initViews();

        btnReset.setOnClickListener(v -> attemptReset());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ====== UI Setup ======

    private void initViews() {
        etEmail = findViewById(R.id.etResetEmail);
        etNewPass = findViewById(R.id.etNewPassword);
        etConfirmPass = findViewById(R.id.etConfirmNewPassword);
        btnReset = findViewById(R.id.btnResetPassword);
    }

    // ====== Logic ======

    private void attemptReset() {
        String email = etEmail.getText().toString().trim();
        String pass = etNewPass.getText().toString().trim();
        String confirmPass = etConfirmPass.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Field can't be empty");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            return;
        }

        if (!userDb.checkEmail(email)) {
            etEmail.setError("This email is not registered");
            return;
        }

        if (pass.isEmpty()) {
            etNewPass.setError("Field can't be empty");
            return;
        }
        if (!PASSWORD_PATTERN.matcher(pass).matches()) {
            etNewPass.setError("Password too weak. Must contain 8+ chars, 1 Upper, 1 Lower, 1 Digit, 1 Special.");
            return;
        }

        if (!pass.equals(confirmPass)) {
            etConfirmPass.setError("Passwords do not match");
            return;
        }

        boolean success = userDb.updatePassword(email, pass);

        if (success) {
            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error updating password.", Toast.LENGTH_SHORT).show();
        }
    }
}