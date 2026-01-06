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
import com.ido.idoprojectapp.deta.model.User;
import com.ido.idoprojectapp.deta.prefs.PrefsHelper;

import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    TextInputEditText usernameET, emailET, passET, rePassET;
    TextInputLayout usrInputLayout, emailInputLayout, passInputLayout, rePassInputLayout;
    Button signUp;
    HelperUserDB hudb;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^" +
            "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        hudb = new HelperUserDB(this);

        initializeViews();
        setupListeners();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        signUp = findViewById(R.id.signUp);

        usernameET = findViewById(R.id.usrET);
        usrInputLayout = findViewById(R.id.usrInputLayout);

        emailET = findViewById(R.id.emailET);
        emailInputLayout = findViewById(R.id.emailInputLayout);

        passET = findViewById(R.id.passET);
        passInputLayout = findViewById(R.id.passInputLayout);

        rePassET = findViewById(R.id.rePassET);
        rePassInputLayout = findViewById(R.id.rePassInputLayout);
    }

    private void setupListeners() {
        setupErrorClearer(usernameET, usrInputLayout);
        setupErrorClearer(emailET, emailInputLayout);
        setupErrorClearer(passET, passInputLayout);
        setupErrorClearer(rePassET, rePassInputLayout);

        signUp.setOnClickListener(v -> validateFields());
    }

    private void setupErrorClearer(EditText et, TextInputLayout til) {
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (til != null) {
                    til.setError(null);
                    til.setErrorEnabled(false);
                }
            }
            public void afterTextChanged(Editable s) {}
        });
    }
    private boolean validateUsername(){
        String usernameInput = usernameET.getText().toString().trim();
        if (usernameInput.isEmpty()) {
            UIHelper.showError(this, usrInputLayout, "Field can't be empty");
            return false;
        }
        return true;
    }

    private boolean validateEmail() {
        String emailInput = emailET.getText().toString().trim();
        if (emailInput.isEmpty()) {
            UIHelper.showError(this, emailInputLayout, "Field can't be empty");            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            UIHelper.showError(this, emailInputLayout, "Invalid email address");
            return false;
        } else if (hudb.checkEmail(emailInput)) {
            UIHelper.showError(this, emailInputLayout, "Email already exists");
            return false;
        }
        return true;
    }

    private boolean validatePassword() {
        String passwordInput = passET.getText().toString().trim();
        if (passwordInput.isEmpty()) {
            UIHelper.showError(this, passInputLayout, "Field can't be empty");
            return false;
        } else if (!PASSWORD_PATTERN.matcher(passwordInput).matches()) {
            UIHelper.showError(this, passInputLayout, "Weak Password: 8+ chars, Upper, Lower, Digit, Special");
            return false;
        }
        return true;
    }

    private boolean validateRepeatPassword() {
        String passwordInput = passET.getText().toString().trim();
        String repeatPasswordInput = rePassET.getText().toString().trim();
        if (repeatPasswordInput.isEmpty()) {
            UIHelper.showError(this, rePassInputLayout, "Field can't be empty");
            return false;
        } else if (!passwordInput.equals(repeatPasswordInput)) {
            UIHelper.showError(this, rePassInputLayout,"Passwords do not match");
            return false;
        }
        return true;
    }

    private void validateFields() {
        if (validateUsername() && validateEmail() && validatePassword() && validateRepeatPassword()) {
            String username = usernameET.getText().toString();
            String password = passET.getText().toString();
            String email = emailET.getText().toString();
            byte[] defaultAvatar = HelperUserDB.convertDrawableToByteArray(this, R.drawable.ic_default_avatar);

            User user = new User(username, email, password);
            user.setProfilePicture(defaultAvatar);
            hudb.insertUser(user);
            new PrefsHelper(this).saveCardensials(username, password);

            finish();
            startActivity(new Intent(this, AiActivity.class));
        }
    }
}