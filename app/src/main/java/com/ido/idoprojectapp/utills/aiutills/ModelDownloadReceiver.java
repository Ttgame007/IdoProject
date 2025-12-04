package com.ido.idoprojectapp.utills.aiutills;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;

import com.ido.idoprojectapp.services.DownloadService;

public class ModelDownloadReceiver extends BroadcastReceiver {

    private final DownloadListener listener;

    public interface DownloadListener {
        void onDownloadProgress(String filename);
        void onDownloadComplete(String filename);
        void onDownloadFailed(String filename);
    }

    public ModelDownloadReceiver(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String filename = intent.getStringExtra(DownloadService.EXTRA_FILENAME);

        if (filename == null || action == null || listener == null) return;

        switch (action) {
            case DownloadService.ACTION_PROGRESS:
                listener.onDownloadProgress(filename);
                break;
            case DownloadService.ACTION_COMPLETE:
                listener.onDownloadComplete(filename);
                break;
            case DownloadService.ACTION_FAILED:
                listener.onDownloadFailed(filename);
                break;
        }
    }

    public void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_PROGRESS);
        filter.addAction(DownloadService.ACTION_COMPLETE);
        filter.addAction(DownloadService.ACTION_FAILED);
        ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    public void unregister(Context context) {
        try {
            context.unregisterReceiver(this);
        } catch (IllegalArgumentException e) {
            // Receiver not registered, ignore
        }
    }
}