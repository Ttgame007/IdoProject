package com.ido.idoprojectapp.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.core.splashscreen.SplashScreen;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ido.idoprojectapp.R;
import com.ido.idoprojectapp.utills.helpers.UIHelper;
import com.ido.idoprojectapp.deta.db.HelperUserDB;
import com.ido.idoprojectapp.deta.prefs.PrefsHelper;

public class MainActivity extends AppCompatActivity {

    Button signIn, signUp;
    ImageButton thwakz;
    ImageView SignInForeground;

    ConstraintLayout mainContent;
    View splashBackground;
    ImageView waterDrop;

    Boolean isSign;
    LinearLayout layout;

    TextInputEditText usernameET, passET;
    TextInputLayout usrInputLayout, passInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        PrefsHelper prefs = new PrefsHelper(this);
        if (prefs.isLoggedIn()) {
            startActivity(new Intent(this, AiActivity.class));
            finish();
            return;
        }
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        isSign = false;
        initViews();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        prefsLogIn(prefs);
        setupListeners();

        startWaterDropAnimation();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContent), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        mainContent = findViewById(R.id.mainContent);
        splashBackground = findViewById(R.id.splashBackground);
        waterDrop = findViewById(R.id.waterDrop);

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


    private void startWaterDropAnimation() {
        waterDrop.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            waterDrop.setVisibility(View.VISIBLE);
            waterDrop.setTranslationY(-1000f);
            waterDrop.setAlpha(0f);
            waterDrop.setScaleX(0.5f);
            waterDrop.setScaleY(0.5f);

            waterDrop.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(800)
                    .setInterpolator(new BounceInterpolator())
                    .withEndAction(this::startRippleReveal)
                    .start();
        });
    }

    private void startRippleReveal() {
        if (isFinishing() || isDestroyed() || mainContent == null || !ViewCompat.isAttachedToWindow(mainContent)) {
            return;
        }

        int cx = mainContent.getWidth() / 2;
        int cy = mainContent.getHeight() / 2;

        if (cx == 0 || cy == 0) {
            mainContent.setVisibility(View.VISIBLE);
            splashBackground.setVisibility(View.GONE);
            waterDrop.setVisibility(View.GONE);
            return;
        }

        float finalRadius = (float) Math.hypot(cx, cy);

        try {
            Animator anim = ViewAnimationUtils.createCircularReveal(mainContent, cx, cy, 0f, finalRadius);
            anim.setDuration(600);
            anim.setInterpolator(new AccelerateInterpolator());

            mainContent.setVisibility(View.VISIBLE);
            waterDrop.animate().alpha(0f).setDuration(200).start();

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!isFinishing()) {
                        splashBackground.setVisibility(View.GONE);
                        waterDrop.setVisibility(View.GONE);
                    }
                }
            });

            anim.start();

        } catch (Exception e) {
            mainContent.setVisibility(View.VISIBLE);
            splashBackground.setVisibility(View.GONE);
            waterDrop.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        setupErrorClearer(usernameET, usrInputLayout);
        setupErrorClearer(passET, passInputLayout);

        signUp.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));
        findViewById(R.id.forgotText).setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        thwakz.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.thwakz.org"))));

        signIn.setOnClickListener(v -> {
            String username = usernameET.getText().toString().trim();
            String password = passET.getText().toString().trim();

            if (isSign) {
                HelperUserDB hudb = new HelperUserDB(this);
                if (hudb.checkUser(username, password)){
                    logIn(new PrefsHelper(this), username, password);
                } else {
                    UIHelper.showError(this, passInputLayout, "Invalid username or password");
                    UIHelper.showError(this, usrInputLayout, "Invalid username or password");
                }

            } else {
                isSign = true;

                layout.setVisibility(View.VISIBLE);
                SignInForeground.setVisibility(View.VISIBLE);

                float distanceToMove = SignInForeground.getHeight();
                if (distanceToMove == 0) distanceToMove = 1000f; // Fallback

                SignInForeground.setTranslationY(distanceToMove);
                layout.setTranslationY(distanceToMove);

                SignInForeground.setAlpha(0f);
                layout.setAlpha(0f);

                long duration = 500;
                DecelerateInterpolator interpolator = new DecelerateInterpolator();

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
        });
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