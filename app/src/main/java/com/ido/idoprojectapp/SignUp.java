package com.ido.idoprojectapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.regex.Pattern;

public class SignUp extends AppCompatActivity {

    ImageButton thwakz;
    EditText usernameET, emailET, passET, rePassET;
    Button signUp;

    HelperUserDB hudb;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +         //at least 1 digit
                    "(?=.*[a-z])" +         //at least 1 lower case letter
                    "(?=.*[A-Z])" +         //at least 1 upper case letter
                    "(?=.*[@#$%^&+=])" +    //at least 1 special character
                    "(?=\\S+$)" +           //no white spaces
                    ".{8,}" +               //at least 8 characters
                    "$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        hudb = new HelperUserDB(this);

        thwakz = findViewById(R.id.thwakzLogo);
        usernameET = findViewById(R.id.usrET);
        emailET = findViewById(R.id.emailET);
        passET = findViewById(R.id.passET);
        rePassET = findViewById(R.id.rePassET);
        signUp = findViewById(R.id.signUp);

        thwakz.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.thwakz.org")));
        });

        signUp.setOnClickListener(v -> {
            validateFields();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean validateEmail() {
        String emailInput = emailET.getText().toString().trim();

        if (emailInput.isEmpty()) {
            emailET.setError("Field can't be empty");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            emailET.setError("Please enter a valid email address");
            return false;
        }         else if (hudb.checkEmail(emailInput)) {
            emailET.setError("Email already exists");
            return false;
        } else {
            emailET.setError(null);
            return true;
        }

    }

    private boolean validatePassword() {
        String passwordInput = passET.getText().toString().trim();

        if (passwordInput.isEmpty()) {
            passET.setError("Field can't be empty");
            return false;
        } else if (!PASSWORD_PATTERN.matcher(passwordInput).matches()) {
            passET.setError("Password too weak. It must contain at least 8 characters, including one uppercase letter, one lowercase letter, one number, and one special character.");
            return false;
        } else {
            passET.setError(null);
            return true;
        }
    }

    private boolean validateRepeatPassword() {
        String passwordInput = passET.getText().toString().trim();
        String repeatPasswordInput = rePassET.getText().toString().trim();

        if (repeatPasswordInput.isEmpty()) {
            rePassET.setError("Field can't be empty");
            return false;
        } else if (!passwordInput.equals(repeatPasswordInput)) {
            rePassET.setError("Passwords do not match");
            return false;
        } else {
            rePassET.setError(null);
            return true;
        }
    }

    private void validateFields() {
        if (validateEmail() && validatePassword() && validateRepeatPassword()) {
            String username = usernameET.getText().toString();
            String password = passET.getText().toString();
            String email = emailET.getText().toString();
            PrefsHelper prefs = new PrefsHelper(this);
            User user = new User(username, email , password);
            hudb.insertUser(user);
            prefs.saveCardensials(username, password);
            finish();
            Intent intent = new Intent(this, AiActivity.class);
            startActivity(intent);
        }
    }
}