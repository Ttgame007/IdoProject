package com.ido.idoprojectapp.utills.helpers;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.ido.idoprojectapp.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UIHelper {

    private static View currentPopupView;

    public static void showError(Activity activity, String message) {
        if (activity == null) return;
        triggerVibration(activity);
        showFloatingCard(activity, message, true);
    }
    public static void showError(Activity activity, View targetView, String message) {
        if (activity == null) return;

        triggerVibration(activity);

        if (targetView != null) {
            Animation shake = AnimationUtils.loadAnimation(activity, R.anim.shake);
            targetView.startAnimation(shake);
        }

        showFloatingCard(activity, message, true);
    }

    public static void showError(Activity activity, TextInputLayout inputLayout, String message) {
        if (activity == null) return;

        triggerVibration(activity);

        if (inputLayout != null) {
            Animation shake = AnimationUtils.loadAnimation(activity, R.anim.shake);
            inputLayout.startAnimation(shake);

            inputLayout.setError(" ");
            inputLayout.setErrorEnabled(true);

            if (inputLayout.getEditText() != null) {
                inputLayout.getEditText().requestFocus();
            }
        }

        showFloatingCard(activity, message, true); // true = isError
    }

    public static void showInfo(Activity activity, String message) {
        if (activity == null) return;

        triggerVibration(activity);
        showFloatingCard(activity, message, false);
    }

    private static void triggerVibration(Activity activity) {
        Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(50);
            }
        }
    }

    private static void showFloatingCard(Activity activity, String message, boolean isError) {
        ViewGroup rootLayout = activity.findViewById(android.R.id.content);

        if (currentPopupView != null) {
            rootLayout.removeView(currentPopupView);
            currentPopupView = null;
        }

        LayoutInflater inflater = LayoutInflater.from(activity);

        int layoutId = isError ? R.layout.layout_error : R.layout.layout_info;
        View popupView = inflater.inflate(layoutId, rootLayout, false);

        int textId = isError ? R.id.tvErrorMessage : R.id.tvInfoMessage;
        TextView tvMessage = popupView.findViewById(textId);
        tvMessage.setText(message);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP;
        params.topMargin = 120;
        params.leftMargin = 32;
        params.rightMargin = 32;
        popupView.setLayoutParams(params);

        rootLayout.addView(popupView);
        currentPopupView = popupView;

        popupView.setAlpha(0f);
        popupView.setTranslationY(-100f);
        popupView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentPopupView == popupView) {
                popupView.animate()
                        .alpha(0f)
                        .translationY(-100f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            rootLayout.removeView(popupView);
                            if (currentPopupView == popupView) {
                                currentPopupView = null;
                            }
                        })
                        .start();
            }
        }, 3000);
    }

    public static void clearError(TextInputLayout inputLayout) {
        if (inputLayout != null) {
            inputLayout.setError(null);
            inputLayout.setErrorEnabled(false);
        }
    }

    // <=== Image Utills ===>

    public static byte[] bitmapToBytes(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static Bitmap bytesToBitmap(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }
}