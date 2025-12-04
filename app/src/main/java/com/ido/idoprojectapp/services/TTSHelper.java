package com.ido.idoprojectapp.services;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.ido.idoprojectapp.deta.prefs.PrefsHelper;

import java.util.Locale;

public class TTSHelper {
    private TextToSpeech textToSpeech;
    private boolean isReady = false;
    private final PrefsHelper prefs;

    public TTSHelper(Context context) {
        prefs = new PrefsHelper(context);
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                } else {
                    isReady = true;
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });
    }

    public void speak(String text) {
        if (!isReady || !prefs.isTtsEnabled() || text == null || text.isEmpty()) return;
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}