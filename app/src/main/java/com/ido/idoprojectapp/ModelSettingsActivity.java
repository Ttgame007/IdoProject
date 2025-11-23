package com.ido.idoprojectapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModelSettingsActivity extends AppCompatActivity {

    private static final String TAG = "ModelSettingsActivity";

    private TextView currentModelName, currentModelDescription;
    private RecyclerView modelsRecyclerView;
    private ProgressBar listProgressBar;

    private ModelAdapter adapter;
    private List<Model> modelOptions = new ArrayList<>();
    private PrefsHelper prefs;
    private DownloadUpdateReceiver downloadUpdateReceiver;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notification permission is needed for download progress.", Toast.LENGTH_LONG).show();
                }
            });

    // ====== Lifecycle ======

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_settings);

        prefs = new PrefsHelper(this);
        findViews();
        setupRecyclerView();
        loadModelListFromLocal();
        updateCurrentModelStatus();
        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerDownloadReceiver();
        adapter.setDownloadProgressMap(DownloadService.getOngoingDownloads());
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(downloadUpdateReceiver);
    }

    // ====== UI Setup ======

    private void findViews() {
        currentModelName = findViewById(R.id.currentModelName);
        currentModelDescription = findViewById(R.id.currentModelDescription);
        modelsRecyclerView = findViewById(R.id.modelsRecyclerView);
        listProgressBar = findViewById(R.id.listProgressBar);
    }

    private void setupRecyclerView() {
        adapter = new ModelAdapter(this, modelOptions, prefs.getCurrentModelFilename(),
                this::startDownload,
                this::cancelDownload,
                this::loadModel,
                this::deleteModel
        );
        modelsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        modelsRecyclerView.setAdapter(adapter);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void loadModelListFromLocal() {
        listProgressBar.setVisibility(View.VISIBLE);
        modelsRecyclerView.setVisibility(View.GONE);
        try (InputStream inputStream = getResources().openRawResource(R.raw.models);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            Type listType = new TypeToken<ArrayList<Model>>(){}.getType();
            List<Model> fetchedModels = new Gson().fromJson(response.toString(), listType);
            modelOptions.clear();
            modelOptions.addAll(fetchedModels);
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "Error loading model list.", e);
            Toast.makeText(this, "Error: Could not load model list.", Toast.LENGTH_LONG).show();
        } finally {
            listProgressBar.setVisibility(View.GONE);
            modelsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateCurrentModelStatus() {
        Model activeModel = prefs.getCurrentModel();
        if (activeModel != null && new File(getFilesDir(), activeModel.getFilename()).exists()) {
            currentModelName.setText(activeModel.getName());
            currentModelDescription.setText(activeModel.getDescription());
        } else {
            currentModelName.setText("No Model Selected");
            currentModelDescription.setText("Please download a model from the list below.");
        }
    }

    // ====== Model Logic ======

    private void loadModel(Model model, int position) {
        Toast.makeText(this, "Loading " + model.getName() + "...", Toast.LENGTH_SHORT).show();

        View itemView = modelsRecyclerView.getLayoutManager().findViewByPosition(position);
        if (itemView != null) {
            itemView.findViewById(R.id.downloadButton).setEnabled(false);
        }

        new Thread(() -> {
            try {
                LLMW.Companion.unloadModel();
                File modelFile = new File(getFilesDir(), model.getFilename());

                int contextSize = calculateOptimalContextSize();
                LLMW.Companion.getInstance(modelFile.getAbsolutePath(), contextSize);

                runOnUiThread(() -> {
                    prefs.setCurrentModel(model);
                    Toast.makeText(this, model.getName() + " loaded with " + contextSize + " token context!", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    updateCurrentModelStatus();
                    adapter.setCurrentModelFilename(prefs.getCurrentModelFilename());
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to load model into LLMW.", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "ERROR: Failed to load " + model.getName(), Toast.LENGTH_LONG).show();
                    if (itemView != null) {
                        itemView.findViewById(R.id.downloadButton).setEnabled(true);
                    }
                });
            }
        }).start();
    }

    private int calculateOptimalContextSize() {
        android.app.ActivityManager activityManager =
                (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo memoryInfo =
                new android.app.ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        long availableMemMB = memoryInfo.availMem / 1048576L;

        int savedContextSize = prefs.getContextSize();
        if (savedContextSize > 0) {
            return savedContextSize;
        }

        if (availableMemMB > 4000) {
            return 4096;
        } else if (availableMemMB > 2000) {
            return 2048;
        } else {
            return 1024;
        }
    }

    private void deleteModel(Model model, int position) {
        File file = new File(getFilesDir(), model.getFilename());
        if (file.exists() && file.delete()) {
            Toast.makeText(this, "Deleted " + model.getName(), Toast.LENGTH_SHORT).show();

            if (model.getFilename().equals(prefs.getCurrentModelFilename())) {
                LLMW.Companion.unloadModel();
                prefs.clearCurrentModel();
                updateCurrentModelStatus();
                adapter.setCurrentModelFilename(null);
            }
            adapter.notifyItemChanged(position);
        } else {
            Toast.makeText(this, "Could not delete file.", Toast.LENGTH_SHORT).show();
        }
    }

    // ====== Download Handling ======

    private void startDownload(Model model, int position) {
        long requiredBytes = parseSizeToBytes(model.getSize());
        long availableBytes = getAvailableInternalMemorySize();
        if (availableBytes < requiredBytes) {
            Toast.makeText(this, "Not enough storage.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(DownloadService.ACTION_DOWNLOAD);
        intent.putExtra(DownloadService.EXTRA_MODEL, model);
        startService(intent);
    }

    private void cancelDownload(Model model, int position) {
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(DownloadService.ACTION_CANCEL_DOWNLOAD);
        intent.putExtra(DownloadService.EXTRA_FILENAME, model.getFilename());
        startService(intent);
    }

    private void registerDownloadReceiver() {
        downloadUpdateReceiver = new DownloadUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadService.ACTION_PROGRESS);
        intentFilter.addAction(DownloadService.ACTION_COMPLETE);
        intentFilter.addAction(DownloadService.ACTION_FAILED);

        ContextCompat.registerReceiver(this, downloadUpdateReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private class DownloadUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String filename = intent.getStringExtra(DownloadService.EXTRA_FILENAME);
            if (filename == null || intent.getAction() == null) return;
            String action = intent.getAction();
            adapter.setDownloadProgressMap(DownloadService.getOngoingDownloads());

            if (action.equals(DownloadService.ACTION_COMPLETE)) {
                int position = findModelPositionByFilename(filename);
                if (position != -1) {
                    loadModel(modelOptions.get(position), position);
                }
                return;
            }
            int position = findModelPositionByFilename(filename);
            if (position != -1) {
                adapter.notifyItemChanged(position, "update_progress");
            }
        }
    }

    // ====== Utils ======

    private int findModelPositionByFilename(String filename) {
        for (int i = 0; i < modelOptions.size(); i++) {
            if (modelOptions.get(i).getFilename().equals(filename)) {
                return i;
            }
        }
        return -1;
    }

    private long getAvailableInternalMemorySize() {
        File path = getFilesDir();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    private long parseSizeToBytes(String size) {
        if (size == null || size.isEmpty()) return 0;
        try {
            String sanitizedSize = size.trim().toUpperCase(Locale.ROOT);
            String[] parts = sanitizedSize.split(" ");
            if (parts.length != 2) return 0;
            double value = Double.parseDouble(parts[0]);
            if (parts[1].equals("GB")) {
                return (long) (value * 1e9);
            } else if (parts[1].equals("MB")) {
                return (long) (value * 1e6);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not parse model size string: " + size, e);
        }
        return 0;
    }
}