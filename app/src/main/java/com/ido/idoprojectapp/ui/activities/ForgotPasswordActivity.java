package com.ido.idoprojectapp.ui.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ido.idoprojectapp.R;
import com.ido.idoprojectapp.utills.helpers.UIHelper;
import com.ido.idoprojectapp.deta.db.HelperUserDB;

import java.util.regex.Pattern;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etNewPass, etConfirmPass;
    private TextInputLayout emailInputLayout, newPassInputLayout, confirmPassInputLayout;
    private Button btnReset;
    private HelperUserDB userDb;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^" +
            "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    // ===== Lifecycle ======

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        userDb = new HelperUserDB(this);

        initViews();
        setupListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ===== UI Setup ======

    private void initViews() {
        etEmail = findViewById(R.id.etResetEmail);
        emailInputLayout = findViewById(R.id.emailInputLayout);

        etNewPass = findViewById(R.id.etNewPassword);
        newPassInputLayout = findViewById(R.id.newPassInputLayout);

        etConfirmPass = findViewById(R.id.etConfirmNewPassword);
        confirmPassInputLayout = findViewById(R.id.confirmPassInputLayout);

        btnReset = findViewById(R.id.btnResetPassword);
    }

    private void setupListeners() {
        setupErrorClearer(etEmail, emailInputLayout);
        setupErrorClearer(etNewPass, newPassInputLayout);
        setupErrorClearer(etConfirmPass, confirmPassInputLayout);
        btnReset.setOnClickListener(v -> attemptReset());
    }

    private void setupErrorClearer(EditText et, TextInputLayout til) {
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (til != null) {
                    UIHelper.clearError(til);
                }
            }
            public void afterTextChanged(Editable s) {}
        });
    }

    // ===== Logic ======

    private void attemptReset() {
        String email = etEmail.getText().toString().trim();
        String pass = etNewPass.getText().toString().trim();
        String confirmPass = etConfirmPass.getText().toString().trim();

        if (email.isEmpty()) {
            UIHelper.showError(this, emailInputLayout, "Field can't be empty");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            UIHelper.showError(this, emailInputLayout, "Invalid email address");
            return;
        }
        if (!userDb.checkEmail(email)) {
            UIHelper.showError(this, emailInputLayout, "Email not found");
            return;
        }

        if (pass.isEmpty()) {
            UIHelper.showError(this, newPassInputLayout, "Field can't be empty");
            return;
        }
        if (!PASSWORD_PATTERN.matcher(pass).matches()) {
            UIHelper.showError(this, newPassInputLayout, "Weak Password: 8+ chars, Upper, Lower, Digit, Special");
            return;
        }
        if (!pass.equals(confirmPass)) {
            UIHelper.showError(this, confirmPassInputLayout, "Passwords do not match");
            return;
        }

        if (userDb.updatePassword(email, pass)) {
            UIHelper.showInfo(this, "Password updated succesfully");
            finish();
        } else {
            UIHelper.showError(this, null, "Error updating password");
        }
    }
}