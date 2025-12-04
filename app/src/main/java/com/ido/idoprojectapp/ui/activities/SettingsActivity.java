package com.ido.idoprojectapp.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ido.idoprojectapp.R;
import com.ido.idoprojectapp.deta.db.HelperUserDB;
import com.ido.idoprojectapp.deta.prefs.PrefsHelper;
import com.ido.idoprojectapp.services.LLMW;
import com.ido.idoprojectapp.utills.helpers.CustomDialogHelper;
import com.ido.idoprojectapp.deta.db.JsonHelper;
import com.ido.idoprojectapp.utills.helpers.MediaActionHelper;
import com.ido.idoprojectapp.utills.helpers.UIHelper;

import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity {

    private PrefsHelper prefs;
    private HelperUserDB userDb;
    private MediaActionHelper mediaHelper;
    private ShapeableImageView profileImage;
    private TextInputEditText editUsernameET, etOldPass, etNewPass, etConfirmPass, etSystemPrompt, etUserPersona;
    private TextInputLayout usernameInputLayout, oldPassInputLayout, newPassInputLayout, confirmPassInputLayout;
    private LinearLayout changePasswordLayout, usernameButtonsLayout;
    private Button btnChangePassword;

    private static final Pattern PASS_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PrefsHelper(this);
        userDb = new HelperUserDB(this);
        mediaHelper = new MediaActionHelper(this);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        profileImage = findViewById(R.id.settingsProfileImage);
        editUsernameET = findViewById(R.id.editUsernameET);
        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        usernameButtonsLayout = findViewById(R.id.usernameButtonsLayout);

        btnChangePassword = findViewById(R.id.btnChangePassword);
        changePasswordLayout = findViewById(R.id.changePasswordLayout);
        etOldPass = findViewById(R.id.etOldPass);
        etNewPass = findViewById(R.id.etNewPass);
        etConfirmPass = findViewById(R.id.etConfirmPass);
        oldPassInputLayout = findViewById(R.id.oldPassInputLayout);
        newPassInputLayout = findViewById(R.id.newPassInputLayout);
        confirmPassInputLayout = findViewById(R.id.confirmPassInputLayout);

        etSystemPrompt = findViewById(R.id.etSystemPrompt);
        etUserPersona = findViewById(R.id.etUserPersona);
    }

    private void loadSettings() {
        ((SwitchMaterial)findViewById(R.id.darkModeSwitch)).setChecked(prefs.isNightModeEnabled());
        ((SwitchMaterial)findViewById(R.id.ttsSwitch)).setChecked(prefs.isTtsEnabled());

        etSystemPrompt.setText(prefs.getUserSystemPrompt());
        etUserPersona.setText(prefs.getUserPersona());
        setupSliders();

        editUsernameET.setText(prefs.getUsername());
        byte[] pic = userDb.getProfilePicture(prefs.getUsername());
        if (pic != null) profileImage.setImageBitmap(BitmapFactory.decodeByteArray(pic, 0, pic.length));
    }

    private void setupSliders() {
        setupSlider(R.id.sliderTemperature, R.id.tvTempDisplay, prefs.getTemperature(), val -> prefs.setTemperature(val), true);
        setupSlider(R.id.sliderMaxTokens, R.id.tvMaxTokensDisplay, (float)prefs.getMaxResponseTokens(), val -> prefs.setMaxResponseTokens((int)val), false);
        setupSlider(R.id.sliderContextMsgs, R.id.tvContextMsgsDisplay, (float)prefs.getMaxContextMessages(), val -> prefs.setMaxContextMessages((int)val), false);
    }

    private interface SliderCallback { void onSave(float val); }
    private void setupSlider(int sliderId, int textId, float initial, SliderCallback cb, boolean isFloat) {
        Slider s = findViewById(sliderId);
        TextView tv = findViewById(textId);
        s.setValue(initial);
        tv.setText(isFloat ? String.format("%.1f", initial) : String.valueOf((int)initial));
        s.addOnChangeListener((slider, val, fromUser) -> tv.setText(isFloat ? String.format("%.1f", val) : String.valueOf((int)val)));
        s.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            public void onStartTrackingTouch(@NonNull Slider s) {}
            public void onStopTrackingTouch(@NonNull Slider s) { cb.onSave(s.getValue()); }
        });
    }

    private void setupListeners() {
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        ((SwitchMaterial)findViewById(R.id.darkModeSwitch)).setOnCheckedChangeListener((b, chk) -> {
            prefs.setNightModeEnabled(chk);
            AppCompatDelegate.setDefaultNightMode(chk ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
        ((SwitchMaterial)findViewById(R.id.ttsSwitch)).setOnCheckedChangeListener((b, chk) -> prefs.setTtsEnabled(chk));

        View.OnClickListener imgClick = v -> mediaHelper.showImageSourceDialog(mediaHelper::openCamera, mediaHelper::openGallery);
        profileImage.setOnClickListener(imgClick);
        findViewById(R.id.changePhotoText).setOnClickListener(imgClick);

        editUsernameET.setOnClickListener(v -> toggleUsernameEdit(true));
        findViewById(R.id.cancelUsernameBtn).setOnClickListener(v -> {
            editUsernameET.setText(prefs.getUsername());
            toggleUsernameEdit(false);
        });
        findViewById(R.id.saveUsernameBtn).setOnClickListener(v -> saveNewUsername());

        btnChangePassword.setOnClickListener(v -> togglePasswordEdit(true));
        findViewById(R.id.btnCancelPassword).setOnClickListener(v -> togglePasswordEdit(false));
        findViewById(R.id.btnSavePassword).setOnClickListener(v -> saveNewPassword());

        findViewById(R.id.btnSaveSystemPrompt).setOnClickListener(v -> {
            prefs.setUserSystemPrompt(etSystemPrompt.getText().toString());
            UIHelper.showInfo(this, "Saved");
            hideKeyboard(etSystemPrompt);
        });
        findViewById(R.id.btnSaveUserPersona).setOnClickListener(v -> {
            prefs.setUserPersona(etUserPersona.getText().toString());
            UIHelper.showInfo(this, "Saved");
            hideKeyboard(etUserPersona);
        });

        findViewById(R.id.btnLogOut).setOnClickListener(v -> confirmAction("Log Out", "Exit?", () -> {
            prefs.clearCardensials();
            LLMW.Companion.unloadModel();
            startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        }));
        findViewById(R.id.btnDeleteData).setOnClickListener(v -> confirmAction("Delete History", "Cannot be undone.", () -> {
            if(JsonHelper.deleteUserDirectory(this, prefs.getUsername())) UIHelper.showInfo(this, "History Cleared");
        }));
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> confirmAction("Delete Account", "Delete everything?", () -> {
            JsonHelper.deleteUserDirectory(this, prefs.getUsername());
            userDb.deleteUser(prefs.getUsername());
            findViewById(R.id.btnLogOut).performClick();
        }));
    }

    // <=== Logic Sections ===>

    private void saveNewUsername() {
        String newName = editUsernameET.getText().toString().trim();
        if (newName.isEmpty()) { UIHelper.showError(this, usernameInputLayout, "Empty"); return; }
        if (newName.equals(prefs.getUsername())) { toggleUsernameEdit(false); return; }

        if (userDb.updateUsername(prefs.getUsername(), newName)) {
            JsonHelper.renameUserDirectory(this, prefs.getUsername(), newName);

            String currentPassword = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("password", "");
            prefs.saveCardensials(newName, currentPassword);

            toggleUsernameEdit(false);
            UIHelper.showInfo(this, "Updated");
        } else {
            UIHelper.showError(this, usernameInputLayout, "Taken");
        }
    }

    private void saveNewPassword() {
        String old = etOldPass.getText().toString().trim();
        String newP = etNewPass.getText().toString().trim();
        String conf = etConfirmPass.getText().toString().trim();

        if (!userDb.checkUser(prefs.getUsername(), old)) {
            UIHelper.showError(this, oldPassInputLayout, "Wrong Password");
            return;
        }
        if (!PASS_PATTERN.matcher(newP).matches()) {
            UIHelper.showError(this, newPassInputLayout, "Weak Password");
            return;
        }
        if (!newP.equals(conf)) {
            UIHelper.showError(this, confirmPassInputLayout, "Mismatch");
            return;
        }

        if (userDb.updatePasswordByUsername(prefs.getUsername(), newP)) {
            prefs.saveCardensials(prefs.getUsername(), newP);
            togglePasswordEdit(false);
            UIHelper.showInfo(this, "Success");
        }
    }

    // <=== UI Helpers ===>

    private void toggleUsernameEdit(boolean edit) {
        editUsernameET.setFocusable(edit);
        editUsernameET.setFocusableInTouchMode(edit);
        usernameButtonsLayout.setVisibility(edit ? View.VISIBLE : View.GONE);
        if(edit) editUsernameET.requestFocus();
        else hideKeyboard(editUsernameET);
    }

    private void togglePasswordEdit(boolean show) {
        changePasswordLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        btnChangePassword.setVisibility(show ? View.GONE : View.VISIBLE);
        if(!show) hideKeyboard(etOldPass);
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void confirmAction(String t, String m, Runnable r) {
        CustomDialogHelper.showConfirmation(this, t, m, "Yes", "No", r::run);
    }

    // <=== Image Handling ===>

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;
        try {
            Bitmap bm = null;
            if (req == MediaActionHelper.REQUEST_PICK_IMAGE) bm = UIHelper.getBitmapFromUri(this, data.getData());
            else if (req == MediaActionHelper.REQUEST_CAPTURE_IMAGE) bm = (Bitmap) data.getExtras().get("data");

            if (bm != null && userDb.updateProfilePicture(prefs.getUsername(), UIHelper.bitmapToBytes(bm))) {
                profileImage.setImageBitmap(bm);
                UIHelper.showInfo(this, "Updated");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req == MediaActionHelper.REQUEST_CAMERA_PERMISSION && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED)
            mediaHelper.openCamera();
    }
}