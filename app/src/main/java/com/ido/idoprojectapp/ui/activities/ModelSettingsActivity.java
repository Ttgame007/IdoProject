package com.ido.idoprojectapp.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ido.idoprojectapp.R;
import com.ido.idoprojectapp.deta.model.Model;
import com.ido.idoprojectapp.deta.prefs.PrefsHelper;
import com.ido.idoprojectapp.services.DownloadService;
import com.ido.idoprojectapp.services.LLMW;
import com.ido.idoprojectapp.ui.adapters.ModelAdapter;
import com.ido.idoprojectapp.utills.aiutills.ModelDownloadReceiver;
import com.ido.idoprojectapp.utills.aiutills.ModelUtils;
import com.ido.idoprojectapp.utills.helpers.UIHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModelSettingsActivity extends AppCompatActivity implements ModelDownloadReceiver.DownloadListener {

    private TextView currentModelName, currentModelDescription;
    private RecyclerView modelsRecyclerView;
    private ProgressBar listProgressBar;
    private ModelAdapter adapter;
    private final List<Model> modelOptions = new ArrayList<>();
    private PrefsHelper prefs;
    private ModelDownloadReceiver downloadReceiver;
    private final ActivityResultLauncher<String> notifPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) UIHelper.showInfo(this, "Notification permission needed for progress.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_settings);

        prefs = new PrefsHelper(this);
        downloadReceiver = new ModelDownloadReceiver(this);

        initViews();
        setupRecyclerView();

        loadModels();
        updateCurrentModelStatus();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        downloadReceiver.register(this);
        refreshDownloadStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        downloadReceiver.unregister(this);
    }

    private void initViews() {
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

    private void loadModels() {
        listProgressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<Model> models = ModelUtils.loadAvailableModels(this);
            runOnUiThread(() -> {
                modelOptions.clear();
                modelOptions.addAll(models);
                adapter.notifyDataSetChanged();
                listProgressBar.setVisibility(View.GONE);
            });
        }).start();
    }

    private void updateCurrentModelStatus() {
        Model activeModel = prefs.getCurrentModel();
        boolean exists = activeModel != null && new File(getFilesDir(), activeModel.getFilename()).exists();

        currentModelName.setText(exists ? activeModel.getName() : "No Model Selected");
        currentModelDescription.setText(exists ? activeModel.getDescription() : "Please download a model from the list below.");

        adapter.setCurrentModelFilename(prefs.getCurrentModelFilename());
        adapter.notifyDataSetChanged();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // <=== Model Operations ===>

    private void loadModel(Model model, int position) {
        UIHelper.showInfo(this, "Loading " + model.getName() + "...");

        View itemView = modelsRecyclerView.getLayoutManager().findViewByPosition(position);
        if (itemView != null) itemView.findViewById(R.id.downloadButton).setEnabled(false);

        new Thread(() -> {
            try {
                LLMW.Companion.unloadModel();
                File modelFile = new File(getFilesDir(), model.getFilename());
                int contextSize = ModelUtils.calculateOptimalContextSize(this, prefs.getContextSize());

                LLMW.Companion.getInstance(modelFile.getAbsolutePath(), contextSize);

                runOnUiThread(() -> {
                    prefs.setCurrentModel(model);
                    prefs.setContextSize(contextSize);
                    setResult(RESULT_OK);
                    updateCurrentModelStatus();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    UIHelper.showError(this, "Failed to load model.");
                    if (itemView != null) itemView.findViewById(R.id.downloadButton).setEnabled(true);
                });
            }
        }).start();
    }

    private void deleteModel(Model model, int position) {
        File file = new File(getFilesDir(), model.getFilename());
        if (file.exists() && file.delete()) {
            UIHelper.showInfo(this, "Deleted " + model.getName());
            if (model.getFilename().equals(prefs.getCurrentModelFilename())) {
                LLMW.Companion.unloadModel();
                prefs.clearCurrentModel();
                updateCurrentModelStatus();
            }
            adapter.notifyItemChanged(position);
        } else {
            UIHelper.showError(this, "Could not delete file.");
        }
    }

    // <=== Download Logic ===>

    private void startDownload(Model model, int position) {
        if (!ModelUtils.hasEnoughStorage(this, model.getSize())) {
            UIHelper.showError(this, "Not enough storage space.");
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

    private void refreshDownloadStatus() {
        adapter.setDownloadProgressMap(DownloadService.getOngoingDownloads());
        adapter.notifyDataSetChanged();
    }

    // <=== Broadcast Receiver Callbacks ===>

    @Override
    public void onDownloadProgress(String filename) {
        adapter.setDownloadProgressMap(DownloadService.getOngoingDownloads());
        int position = findModelPosition(filename);
        if (position != -1) {
            adapter.notifyItemChanged(position, "update_progress");
        }
    }

    @Override
    public void onDownloadComplete(String filename) {
        adapter.setDownloadProgressMap(DownloadService.getOngoingDownloads());
        int position = findModelPosition(filename);
        if (position != -1) {
            loadModel(modelOptions.get(position), position);
        }
    }

    @Override
    public void onDownloadFailed(String filename) {
        adapter.setDownloadProgressMap(DownloadService.getOngoingDownloads());
        int position = findModelPosition(filename);
        if (position != -1) {
            adapter.notifyItemChanged(position);
            UIHelper.showError(this, "Download failed for " + modelOptions.get(position).getName());
        }
    }

    private int findModelPosition(String filename) {
        for (int i = 0; i < modelOptions.size(); i++) {
            if (modelOptions.get(i).getFilename().equals(filename)) return i;
        }
        return -1;
    }
}