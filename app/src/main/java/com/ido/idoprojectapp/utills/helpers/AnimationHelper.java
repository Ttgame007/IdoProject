package com.ido.idoprojectapp.utills.helpers;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;

public class AnimationHelper {
    private AnimatorSet thinkingAnimator;

    public void toggleThinkingAnimation(View targetView, boolean isThinking) {
        if (isThinking) {
            if (thinkingAnimator != null && thinkingAnimator.isRunning()) return;

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(targetView, "scaleX", 1f, 0.9f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(targetView, "scaleY", 1f, 0.9f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(targetView, "alpha", 1f, 0.5f);

            scaleX.setRepeatCount(ObjectAnimator.INFINITE);
            scaleX.setRepeatMode(ObjectAnimator.REVERSE);
            scaleY.setRepeatCount(ObjectAnimator.INFINITE);
            scaleY.setRepeatMode(ObjectAnimator.REVERSE);
            alpha.setRepeatCount(ObjectAnimator.INFINITE);
            alpha.setRepeatMode(ObjectAnimator.REVERSE);

            thinkingAnimator = new AnimatorSet();
            thinkingAnimator.playTogether(scaleX, scaleY, alpha);
            thinkingAnimator.setDuration(800);
            thinkingAnimator.start();
        } else {
            if (thinkingAnimator != null) {
                thinkingAnimator.cancel();
                thinkingAnimator = null;
            }
            targetView.setScaleX(1f);
            targetView.setScaleY(1f);
            targetView.setAlpha(1f);
        }
    }
}