package com.ido.idoprojectapp.utills.aiutills;

import android.app.ActivityManager;
import android.content.Context;
import android.os.StatFs;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ido.idoprojectapp.R;
import com.ido.idoprojectapp.deta.model.Model;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModelUtils {

    public static List<Model> loadAvailableModels(Context context) {
        try (InputStream inputStream = context.getResources().openRawResource(R.raw.models);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            Type listType = new TypeToken<ArrayList<Model>>(){}.getType();
            return new Gson().fromJson(response.toString(), listType);
        } catch (Exception e) {
            Log.e("ModelUtils", "Error loading model list", e);
            return new ArrayList<>();
        }
    }

    public static int calculateOptimalContextSize(Context context, int userSavedSize) {
        if (userSavedSize > 0 && userSavedSize != 2048) {
            return userSavedSize;
        }

        ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);

        long availableMemMB = memInfo.totalMem / 1048576L;

        if (availableMemMB > 7500) return 4096;
        if (availableMemMB > 5500) return 2048;
        return 1024;
    }

    public static boolean hasEnoughStorage(Context context, String sizeString) {
        long requiredBytes = parseSizeToBytes(sizeString);
        File path = context.getFilesDir();
        StatFs stat = new StatFs(path.getPath());
        long availableBytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        return availableBytes >= requiredBytes;
    }

    private static long parseSizeToBytes(String size) {
        if (size == null || size.isEmpty()) return 0;
        try {
            String sanitized = size.trim().toUpperCase(Locale.ROOT);
            String[] parts = sanitized.split(" ");
            if (parts.length != 2) return 0;
            double value = Double.parseDouble(parts[0]);
            if (parts[1].equals("GB")) return (long) (value * 1e9);
            if (parts[1].equals("MB")) return (long) (value * 1e6);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}