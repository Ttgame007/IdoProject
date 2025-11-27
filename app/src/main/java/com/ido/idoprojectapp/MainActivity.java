package com.ido.idoprojectapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.animation.DecelerateInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    Button signIn, signUp;
    ImageButton thwakz;
    ImageView SignInForeground;
    Boolean isSign;
    LinearLayout layout;


    TextInputEditText usernameET, passET;
    TextInputLayout usrInputLayout, passInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsHelper prefs = new PrefsHelper(this);
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        isSign = false;
        initViews();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        prefsLogIn(prefs);
        setupListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        SignInForeground = findViewById(R.id.signInForeground);
        layout = findViewById(R.id.layout);
        signUp = findViewById(R.id.signUpText);
        thwakz = findViewById(R.id.thwakzLogo);
        signIn = findViewById(R.id.signIn);

        usernameET = findViewById(R.id.usrET);
        usrInputLayout = findViewById(R.id.usrInputLayout);

        passET = findViewById(R.id.passET);
        passInputLayout = findViewById(R.id.passInputLayout);

        layout.setVisibility(View.INVISIBLE);
        SignInForeground.setVisibility(View.INVISIBLE);
    }

    private void setupListeners() {
        setupErrorClearer(usernameET, usrInputLayout);
        setupErrorClearer(passET, passInputLayout);

        signUp.setOnClickListener(v -> startActivity(new Intent(this, SignUp.class)));
        findViewById(R.id.forgotText).setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        thwakz.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.thwakz.org"))));

        signIn.setOnClickListener(v -> {
                    String username = usernameET.getText().toString().trim();
                    String password = passET.getText().toString().trim();

                    if (isSign) {
                        HelperUserDB hudb = new HelperUserDB(this);
                        if (hudb.checkUser(username, password)) {
                            logIn(new PrefsHelper(this), username, password);
                        } else {
                            UIHelper.showError(this, passInputLayout, "Invalid username or password");
                            UIHelper.showError(this, usrInputLayout, "Invalid username or password");
                        }

                    } else {
                        isSign = true;

                        layout.setVisibility(View.VISIBLE);
                        SignInForeground.setVisibility(View.VISIBLE);

                        // 2. Determine how far to push them down.
                        // Using the foreground's height ensures they start exactly off the bottom edge.
                        float distanceToMove = SignInForeground.getHeight();

                        // Safety check: if height is 0 (rare, but possible if layout isn't ready), use a safe default
                        if (distanceToMove == 0) distanceToMove = 1000f;

                        // 3. Set the initial position (Pushed down by the same amount)
                        // By moving both by 'distanceToMove', their relative positions stay locked.
                        SignInForeground.setTranslationY(distanceToMove);
                        layout.setTranslationY(distanceToMove);

                        // Optional: Start transparent
                        SignInForeground.setAlpha(0f);
                        layout.setAlpha(0f);

                        // 4. Animate both to 0 (Original Position) with identical settings
                        // NO START DELAY provided to either, so they move in perfect lockstep.

                        long duration = 500; // Same duration for both
                        DecelerateInterpolator interpolator = new DecelerateInterpolator(); // Same interpolator

                        SignInForeground.animate()
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(duration)
                                .setInterpolator(interpolator)
                                .start();

                        layout.animate()
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(duration)
                                .setInterpolator(interpolator)
                                .start();
                    }
                }
        );
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

    public void prefsLogIn(PrefsHelper prefs){
        if (prefs.isLoggedIn()) startActivity(new Intent(this, AiActivity.class));
    }

    public void logIn(PrefsHelper prefs, String username, String password) {
        prefs.saveCardensials(username, password);
        startActivity(new Intent(this, AiActivity.class));
    }
}