package com.ido.idoprojectapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial darkModeSwitch, ttsSwitch;
    private ShapeableImageView profileImage;
    private TextView usernameText, changePhotoText;
    private ImageButton editUsernameBtn, backButton;
    private Button btnDeleteData, btnDeleteAccount, btnLogOut;

    private PrefsHelper prefs;
    private HelperUserDB userDb;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_CAPTURE_IMAGE = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 10;

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

    // ====== UI Setup ======

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        ttsSwitch = findViewById(R.id.ttsSwitch);

        profileImage = findViewById(R.id.settingsProfileImage);
        usernameText = findViewById(R.id.settingsUsername);
        changePhotoText = findViewById(R.id.changePhotoText);
        editUsernameBtn = findViewById(R.id.editUsernameBtn);

        btnDeleteData = findViewById(R.id.btnDeleteData);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnLogOut = findViewById(R.id.btnLogOut);
    }

    private void loadCurrentSettings() {
        darkModeSwitch.setChecked(prefs.isNightModeEnabled());
        ttsSwitch.setChecked(prefs.isTtsEnabled());
    }

    private void loadProfileData() {
        String username = prefs.getUsername();
        usernameText.setText(username);

        byte[] profilePicBytes = userDb.getProfilePicture(username);
        if (profilePicBytes != null && profilePicBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(profilePicBytes, 0, profilePicBytes.length);
            profileImage.setImageBitmap(bitmap);
        } else {
            profileImage.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    // ====== Logic & Listeners ======

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        // Switches
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setNightModeEnabled(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        ttsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setTtsEnabled(isChecked);
        });

        // Profile Actions
        profileImage.setOnClickListener(v -> showImageSourceDialog());
        changePhotoText.setOnClickListener(v -> showImageSourceDialog());

        editUsernameBtn.setOnClickListener(v -> showEditUsernameDialog());

        // Account Actions
        btnLogOut.setOnClickListener(v -> performLogout());

        btnDeleteData.setOnClickListener(v -> showConfirmationDialog(
                "Delete Chat History?",
                "This will permanently delete all your chats and messages. This cannot be undone.",
                this::deleteChatData));

        btnDeleteAccount.setOnClickListener(v -> showConfirmationDialog(
                "Delete Account?",
                "This will delete your user, password, and all data. You will be logged out immediately.",
                this::deleteAccount));
    }

    // ====== Account Operations ======

    private void performLogout() {
        prefs.clearCardensials();
        LLMW.Companion.unloadModel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void deleteChatData() {
        String username = prefs.getUsername();
        boolean deleted = JsonHelper.deleteUserDirectory(this, username);
        if (deleted) {
            Toast.makeText(this, "Chat history deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No data found or delete failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteAccount() {
        String username = prefs.getUsername();
        JsonHelper.deleteUserDirectory(this, username);
        userDb.deleteUser(username);
        performLogout();
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditUsernameDialog() {
        EditText input = new EditText(this);
        input.setHint("New Username");
        String currentName = prefs.getUsername();
        input.setText(currentName);

        new AlertDialog.Builder(this)
                .setTitle("Change Username")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(currentName)) {
                        boolean dbSuccess = userDb.updateUsername(currentName, newName);
                        if (dbSuccess) {
                            JsonHelper.renameUserDirectory(this, currentName, newName);
                            String pwd = null;
                            getSharedPreferences("user_prefs", MODE_PRIVATE)
                                    .edit().putString("username", newName).apply();
                            usernameText.setText(newName);
                            Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ====== Image Handling ======

    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Change Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
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
                })
                .show();
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
                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
}