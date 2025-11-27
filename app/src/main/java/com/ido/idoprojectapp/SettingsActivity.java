package com.ido.idoprojectapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity {

    private PrefsHelper prefs;
    private HelperUserDB userDb;

    private View backButton;

    private ShapeableImageView profileImage;
    private TextView changePhotoText;

    private TextInputLayout usernameInputLayout;
    private TextInputEditText editUsernameET;
    private LinearLayout usernameButtonsLayout;
    private Button saveUsernameBtn, cancelUsernameBtn;

    private Button btnChangePassword;
    private LinearLayout changePasswordLayout;
    private TextInputEditText etOldPass, etNewPass, etConfirmPass;
    private TextInputLayout oldPassInputLayout, newPassInputLayout, confirmPassInputLayout;
    private Button btnSavePassword, btnCancelPassword;

    private SwitchMaterial darkModeSwitch, ttsSwitch;
    private TextInputEditText etUserPersona;
    private Button btnSaveUserPersona;


    private TextInputEditText etSystemPrompt;
    private Button btnSaveSystemPrompt;
    private Slider sliderTemperature, sliderMaxTokens, sliderContextMsgs;
    private TextView tvTempDisplay, tvMaxTokensDisplay, tvContextMsgsDisplay;

    private Button btnDeleteData, btnDeleteAccount, btnLogOut;

    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_CAPTURE_IMAGE = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 10;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^" +
            "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    // ====== Lifecycle ======

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PrefsHelper(this);
        userDb = new HelperUserDB(this);

        initializeViews();
        loadCurrentSettings();
        loadProfileData();
        setupListeners();
    }

    // ====== Initialization ======

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);

        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        ttsSwitch = findViewById(R.id.ttsSwitch);

        profileImage = findViewById(R.id.settingsProfileImage);
        changePhotoText = findViewById(R.id.changePhotoText);

        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        editUsernameET = findViewById(R.id.editUsernameET);
        usernameButtonsLayout = findViewById(R.id.usernameButtonsLayout);
        saveUsernameBtn = findViewById(R.id.saveUsernameBtn);
        cancelUsernameBtn = findViewById(R.id.cancelUsernameBtn);

        btnChangePassword = findViewById(R.id.btnChangePassword);
        changePasswordLayout = findViewById(R.id.changePasswordLayout);

        etOldPass = findViewById(R.id.etOldPass);
        oldPassInputLayout = findViewById(R.id.oldPassInputLayout);

        etNewPass = findViewById(R.id.etNewPass);
        newPassInputLayout = findViewById(R.id.newPassInputLayout);

        etConfirmPass = findViewById(R.id.etConfirmPass);
        confirmPassInputLayout = findViewById(R.id.confirmPassInputLayout);

        btnSavePassword = findViewById(R.id.btnSavePassword);
        btnCancelPassword = findViewById(R.id.btnCancelPassword);

        etSystemPrompt = findViewById(R.id.etSystemPrompt);
        btnSaveSystemPrompt = findViewById(R.id.btnSaveSystemPrompt);
        etUserPersona = findViewById(R.id.etUserPersona);
        btnSaveUserPersona = findViewById(R.id.btnSaveUserPersona);
        sliderTemperature = findViewById(R.id.sliderTemperature);
        sliderMaxTokens = findViewById(R.id.sliderMaxTokens);
        sliderContextMsgs = findViewById(R.id.sliderContextMsgs);
        tvTempDisplay = findViewById(R.id.tvTempDisplay);
        tvMaxTokensDisplay = findViewById(R.id.tvMaxTokensDisplay);
        tvContextMsgsDisplay = findViewById(R.id.tvContextMsgsDisplay);



        btnDeleteData = findViewById(R.id.btnDeleteData);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnLogOut = findViewById(R.id.btnLogOut);
    }

    private void loadCurrentSettings() {
        darkModeSwitch.setChecked(prefs.isNightModeEnabled());
        ttsSwitch.setChecked(prefs.isTtsEnabled());

        etSystemPrompt.setText(prefs.getUserSystemPrompt());
        etUserPersona.setText(prefs.getUserPersona());

        float temp = prefs.getTemperature();
        if (temp < 0.1f) temp = 0.1f;
        if (temp > 1.0f) temp = 1.0f;
        sliderTemperature.setValue(temp);
        tvTempDisplay.setText(String.format("%.1f", temp));

        int maxTokens = prefs.getMaxResponseTokens();
        if (maxTokens < 50) maxTokens = 50;
        if (maxTokens > 1024) maxTokens = 1024;
        sliderMaxTokens.setValue((float) maxTokens);
        tvMaxTokensDisplay.setText(String.valueOf(maxTokens));

        int contextMsgs = prefs.getMaxContextMessages();
        if (contextMsgs < 0) contextMsgs = 0;
        if (contextMsgs > 20) contextMsgs = 20;
        sliderContextMsgs.setValue((float) contextMsgs);
        tvContextMsgsDisplay.setText(String.valueOf(contextMsgs));

    }

    private void loadProfileData() {
        String username = prefs.getUsername();
        editUsernameET.setText(username);
        toggleUsernameEdit(false);

        byte[] profilePicBytes = userDb.getProfilePicture(username);
        if (profilePicBytes != null && profilePicBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(profilePicBytes, 0, profilePicBytes.length);
            profileImage.setImageBitmap(bitmap);
        } else {
            profileImage.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    // ====== Listeners ======

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setNightModeEnabled(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        ttsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setTtsEnabled(isChecked));

        profileImage.setOnClickListener(v -> showImageSourceDialog());
        changePhotoText.setOnClickListener(v -> showImageSourceDialog());

        editUsernameET.setOnClickListener(v -> toggleUsernameEdit(true));

        cancelUsernameBtn.setOnClickListener(v -> {
            editUsernameET.setText(prefs.getUsername());
            toggleUsernameEdit(false);
            UIHelper.clearError(usernameInputLayout);
        });

        saveUsernameBtn.setOnClickListener(v -> saveNewUsername());

        btnChangePassword.setOnClickListener(v -> togglePasswordEdit(true));

        btnCancelPassword.setOnClickListener(v -> {
            togglePasswordEdit(false);
            etOldPass.setText("");
            etNewPass.setText("");
            etConfirmPass.setText("");
        });

        btnSavePassword.setOnClickListener(v -> saveNewPassword());


        btnSaveSystemPrompt.setOnClickListener(v -> {
            String prompt = etSystemPrompt.getText().toString();
            prefs.setUserSystemPrompt(prompt); // Save only user part
            UIHelper.showInfo(this, "System Prompt Updated");
            hideKeyboard(etSystemPrompt);
            etSystemPrompt.clearFocus();
        });
        btnSaveUserPersona.setOnClickListener(v -> {
            String persona = etUserPersona.getText().toString();
            prefs.setUserPersona(persona);
            UIHelper.showInfo(this, "User Persona Updated");
            hideKeyboard(etUserPersona);
            etUserPersona.clearFocus();
        });

        sliderTemperature.addOnChangeListener((slider, value, fromUser) -> {
            tvTempDisplay.setText(String.format("%.1f", value));
        });
        sliderTemperature.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                prefs.setTemperature(slider.getValue());
            }
        });

        sliderMaxTokens.addOnChangeListener((slider, value, fromUser) -> {
            tvMaxTokensDisplay.setText(String.valueOf((int) value));
        });
        sliderMaxTokens.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                prefs.setMaxResponseTokens((int) slider.getValue());
            }
        });

        sliderContextMsgs.addOnChangeListener((slider, value, fromUser) -> {
            tvContextMsgsDisplay.setText(String.valueOf((int) value));
        });
        sliderContextMsgs.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                prefs.setMaxContextMessages((int) slider.getValue());
            }
        });

        btnLogOut.setOnClickListener(v -> performLogout());

        btnDeleteData.setOnClickListener(v -> showConfirmationDialog(
                "Delete Chat History?",
                "This will permanently delete all your chats and messages. This cannot be undone.",
                "Delete",
                this::deleteChatData));

        btnDeleteAccount.setOnClickListener(v -> showConfirmationDialog(
                "Delete Account?",
                "This will delete your user, password, and all data. You will be logged out immediately.",
                "Delete Account",
                this::deleteAccount));
    }

    // ====== Profile Logic ======

    private void toggleUsernameEdit(boolean editing) {
        if (editing) {

            usernameInputLayout.setBoxStrokeColor(getColor(R.color.blue));
            usernameInputLayout.setBoxBackgroundColor(getColor(R.color.surface_color));

            editUsernameET.setFocusableInTouchMode(true);
            editUsernameET.setFocusable(true);
            editUsernameET.setCursorVisible(true);
            editUsernameET.requestFocus();

            if (editUsernameET.getText() != null) {
                editUsernameET.setSelection(editUsernameET.getText().length());
            }

            usernameButtonsLayout.setVisibility(View.VISIBLE);
        } else {
            usernameInputLayout.setBoxStrokeColor(android.graphics.Color.TRANSPARENT);
            usernameInputLayout.setBoxBackgroundColor(android.graphics.Color.TRANSPARENT);

            editUsernameET.setFocusable(false);
            editUsernameET.setFocusableInTouchMode(false);
            editUsernameET.setCursorVisible(false);

            usernameButtonsLayout.setVisibility(View.GONE);

            editUsernameET.clearFocus();
            hideKeyboard(editUsernameET);
        }
    }

    private void saveNewUsername() {
        String currentName = prefs.getUsername();
        String newName = editUsernameET.getText().toString().trim();

        UIHelper.clearError(usernameInputLayout);

        if (newName.isEmpty()) {
            UIHelper.showError(this, usernameInputLayout, "Username cannot be empty");
            return;
        }

        if (newName.equals(currentName)) {
            toggleUsernameEdit(false);
            return;
        }

        boolean dbSuccess = userDb.updateUsername(currentName, newName);
        if (dbSuccess) {
            JsonHelper.renameUserDirectory(this, currentName, newName);

            SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String currentPass = sp.getString("password", "");

            prefs.saveCardensials(newName, currentPass);

            UIHelper.showInfo(this, "Username updated");
            toggleUsernameEdit(false);
        } else {
            UIHelper.showError(this, usernameInputLayout, "Username already taken");
        }
    }


    private void togglePasswordEdit(boolean show) {
        if (show) {
            btnChangePassword.setVisibility(View.GONE);
            changePasswordLayout.setVisibility(View.VISIBLE);
            etOldPass.requestFocus();

            UIHelper.clearError(oldPassInputLayout);
            UIHelper.clearError(newPassInputLayout);
            UIHelper.clearError(confirmPassInputLayout);
        } else {
            changePasswordLayout.setVisibility(View.GONE);
            btnChangePassword.setVisibility(View.VISIBLE);
            hideKeyboard(etOldPass);
        }
    }

    private void saveNewPassword() {
        String oldPass = etOldPass.getText().toString().trim();
        String newPass = etNewPass.getText().toString().trim();
        String confirmPass = etConfirmPass.getText().toString().trim();
        String username = prefs.getUsername();

        UIHelper.clearError(oldPassInputLayout);
        UIHelper.clearError(newPassInputLayout);
        UIHelper.clearError(confirmPassInputLayout);

        if (!userDb.checkUser(username, oldPass)) {
            UIHelper.showError(this, oldPassInputLayout, "Incorrect current password");
            return;
        }

        if (!PASSWORD_PATTERN.matcher(newPass).matches()) {
            UIHelper.showError(this, newPassInputLayout, "Weak Password: 8+ chars, Upper, Lower, Digit, Special");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            UIHelper.showError(this, confirmPassInputLayout, "Passwords do not match");
            return;
        }

        boolean success = userDb.updatePasswordByUsername(username, newPass);
        if (success) {
            prefs.saveCardensials(username, newPass);
            UIHelper.showInfo(this, "Password updated successfully");
            togglePasswordEdit(false);
            etOldPass.setText("");
            etNewPass.setText("");
            etConfirmPass.setText("");
        } else {
            UIHelper.showError(this, oldPassInputLayout, "failed to update password");
        }
    }


    private void performLogout() {
        CustomDialogHelper.showConfirmation(
                this,
                "Log Out",
                "Are you sure you want to log out of Thwakz AI?",
                "Log Out",
                "Stay",
                () -> {
                    prefs.clearCardensials();
                    LLMW.Companion.unloadModel();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
        );
    }

    private void deleteChatData() {
        String username = prefs.getUsername();
        boolean deleted = JsonHelper.deleteUserDirectory(this, username);
        if (deleted) {
            UIHelper.showInfo(this, "Cleared Chat history");
        } else {
            UIHelper.showError(this,btnDeleteData, "No data found or delete failed");
        }
    }

    private void deleteAccount() {
        String username = prefs.getUsername();
        JsonHelper.deleteUserDirectory(this, username);
        userDb.deleteUser(username);
        performLogout();
    }

    private void showConfirmationDialog(String title, String message, String positiveBtnText, Runnable onConfirm) {
        CustomDialogHelper.showConfirmation(
                this,
                title,
                message,
                positiveBtnText,
                "Cancel",
                onConfirm::run
        );
    }


    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};
        CustomDialogHelper.showOptionsDialog(
                this,
                "Change Profile Picture",
                options,
                (index) -> {
                    if (index == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        } else {
                            openCamera();
                        }
                    } else {
                        openGallery();
                    }
                }
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        Bitmap imageBitmap = null;

        if (requestCode == REQUEST_PICK_IMAGE) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == REQUEST_CAPTURE_IMAGE) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                imageBitmap = (Bitmap) extras.get("data");
            }
        }

        if (imageBitmap != null) {
            byte[] bytes = convertBitmapToByteArray(imageBitmap);
            boolean success = userDb.updateProfilePicture(prefs.getUsername(), bytes);
            if (success) {
                profileImage.setImageBitmap(imageBitmap);
                UIHelper.showInfo(this,"Profile picture updated");

            }
        }
    }

    private byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    // ====== Utils ======

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}