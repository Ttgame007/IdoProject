package com.ido.idoprojectapp;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;

public class CustomDialogHelper {

    public interface DialogClickListener {
        void onClick();
    }

    public interface OptionClickListener {
        void onOptionClick(int index);
    }

    public static void showConfirmation(Context context, String title, String message, String positiveText, String negativeText, DialogClickListener onPositive) {
        showDialog(context, title, message, null, positiveText, negativeText, onPositive, null);
    }

    public static void showInfo(Context context, String title, String message, String buttonText) {
        showDialog(context, title, message, null, buttonText, null, null, null);
    }


    public static void showCustomView(Context context, String title, View customView, String positiveText, String negativeText, DialogClickListener onPositive, DialogClickListener onNegative) {
        showDialog(context, title, null, customView, positiveText, negativeText, onPositive, onNegative);
    }


    public static void showOptionsDialog(Context context, String title, String[] options, OptionClickListener listener) {
        LinearLayout optionsLayout = new LinearLayout(context);
        optionsLayout.setOrientation(LinearLayout.VERTICAL);

        final Dialog dialog = createBaseDialog(context);

        android.util.TypedValue outValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        int selectableBackgroundResId = outValue.resourceId;

        for (int i = 0; i < options.length; i++) {
            final int index = i;
            TextView optionView = new TextView(context);
            optionView.setText(options[i]);
            optionView.setTextSize(16);
            optionView.setPadding(30, 40, 30, 40);

            optionView.setTextColor(context.getResources().getColor(R.color.text_primary));

            optionView.setBackgroundResource(selectableBackgroundResId);

            optionView.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onOptionClick(index);
            });

            optionsLayout.addView(optionView);
        }

        setupDialogView(context, dialog, title, null, optionsLayout, "Close", null, null, null);
        dialog.show();
    }

    private static void showDialog(Context context, String title, String message, View customView, String posText, String negText, DialogClickListener onPos, DialogClickListener onNeg) {
        Dialog dialog = createBaseDialog(context);
        setupDialogView(context, dialog, title, message, customView, posText, negText, onPos, onNeg);
        dialog.show();
    }

    private static Dialog createBaseDialog(Context context) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_alert);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    private static void setupDialogView(Context context, Dialog dialog, String title, String message, View customView, String posText, String negText, DialogClickListener onPos, DialogClickListener onNeg) {
        View dialogRoot = dialog.findViewById(R.id.dialogTitle).getRootView();
        Animation scaleIn = AnimationUtils.loadAnimation(context, R.anim.dialog_scale_in);
        dialogRoot.startAnimation(scaleIn);

        TextView titleView = dialog.findViewById(R.id.dialogTitle);
        TextView messageView = dialog.findViewById(R.id.dialogMessage);
        FrameLayout contentContainer = dialog.findViewById(R.id.customContentContainer);
        Button btnPositive = dialog.findViewById(R.id.btnPositive);
        Button btnNegative = dialog.findViewById(R.id.btnNegative);

        titleView.setText(title);

        if (customView != null) {
            messageView.setVisibility(View.GONE);
            contentContainer.setVisibility(View.VISIBLE);
            contentContainer.addView(customView);
        } else {
            messageView.setVisibility(View.VISIBLE);
            contentContainer.setVisibility(View.GONE);
            messageView.setText(message);
        }

        btnPositive.setText(posText);
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            if (onPos != null) onPos.onClick();
        });

        if (negText == null) {
            btnNegative.setVisibility(View.GONE);
        } else {
            btnNegative.setVisibility(View.VISIBLE);
            btnNegative.setText(negText);
            btnNegative.setOnClickListener(v -> {
                dialog.dismiss();
                if (onNeg != null) onNeg.onClick();
            });
        }
    }
}