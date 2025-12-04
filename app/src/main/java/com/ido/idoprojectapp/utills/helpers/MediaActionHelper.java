package com.ido.idoprojectapp.utills.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Locale;

public class MediaActionHelper {
    public static final int REQUEST_VOICE_INPUT = 11;
    public static final int REQUEST_PICK_IMAGE = 2;
    public static final int REQUEST_CAPTURE_IMAGE = 3;
    public static final int REQUEST_CAMERA_PERMISSION = 10;

    private final Activity activity;

    public MediaActionHelper(Activity activity) {
        this.activity = activity;
    }

    public void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try {
            activity.startActivityForResult(intent, REQUEST_VOICE_INPUT);
        } catch (Exception e) {
            UIHelper.showError(activity, "Voice input not supported");
        }
    }

    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    public void openCamera() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
            }
        }
    }

    public void showImageSourceDialog(Runnable onCamera, Runnable onGallery) {
        String[] options = {"Camera", "Gallery"};
        CustomDialogHelper.showOptionsDialog(activity, "Select Image Source", options, index -> {
            if (index == 0) onCamera.run();
            else onGallery.run();
        });
    }
}