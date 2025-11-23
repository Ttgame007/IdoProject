package com.ido.idoprojectapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";

    public static final String ACTION_DOWNLOAD = "com.ido.idoprojectapp.action.DOWNLOAD";
    public static final String ACTION_CANCEL_DOWNLOAD = "com.ido.idoprojectapp.action.CANCEL";
    public static final String ACTION_PROGRESS = "com.ido.idoprojectapp.broadcast.PROGRESS";
    public static final String ACTION_COMPLETE = "com.ido.idoprojectapp.broadcast.COMPLETE";
    public static final String ACTION_FAILED = "com.ido.idoprojectapp.broadcast.FAILED";

    public static final String EXTRA_MODEL = "com.ido.idoprojectapp.extra.MODEL";
    public static final String EXTRA_FILENAME = "com.ido.idoprojectapp.extra.FILENAME";
    public static final String EXTRA_PROGRESS = "com.ido.idoprojectapp.extra.PROGRESS";

    private static final String CHANNEL_ID = "DownloadChannel";
    private static final int NOTIFICATION_ID = 1;

    private static final Map<String, Integer> ongoingDownloads = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Call> activeCalls = Collections.synchronizedMap(new HashMap<>());

    public static Map<String, Integer> getOngoingDownloads() {
        return ongoingDownloads;
    }

    private OkHttpClient okHttpClient;
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;

    // ====== Lifecycle ======

    @Override
    public void onCreate() {
        super.onCreate();
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.MINUTES)
                .build();

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::ModelDownloadWakelockTag");

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        switch (intent.getAction()) {
            case ACTION_DOWNLOAD:
                Model model = (Model) intent.getSerializableExtra(EXTRA_MODEL);
                if (model != null && !ongoingDownloads.containsKey(model.getFilename())) {
                    startForeground(NOTIFICATION_ID, createNotification("Preparing...", 0));
                    downloadModel(model);
                }
                break;
            case ACTION_CANCEL_DOWNLOAD:
                String filenameToCancel = intent.getStringExtra(EXTRA_FILENAME);
                if (filenameToCancel != null) {
                    cancelDownload(filenameToCancel);
                }
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ====== Download Logic ======

    private void downloadModel(final Model model) {
        wakeLock.acquire();
        ongoingDownloads.put(model.getFilename(), 0);
        broadcastProgress(model.getFilename(), 0);

        File destinationFile = new File(getFilesDir(), model.getFilename());
        Request request = new Request.Builder().url(model.getPath()).build();
        Call call = okHttpClient.newCall(request);
        activeCalls.put(model.getFilename(), call);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) {
                    Log.d(TAG, "Download cancelled for " + model.getName());
                } else {
                    Log.e(TAG, "Download failed for " + model.getName(), e);
                }
                cleanupAndBroadcastFailure(model.getFilename());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful() || response.body() == null) {
                    cleanupAndBroadcastFailure(model.getFilename());
                    return;
                }

                try (ResponseBody body = response.body()) {
                    long fileLength = body.contentLength();
                    try (InputStream input = body.byteStream(); OutputStream output = new FileOutputStream(destinationFile)) {
                        byte[] data = new byte[8192];
                        long total = 0;
                        int count;
                        int lastProgress = -1;

                        while ((count = input.read(data)) != -1) {
                            if (call.isCanceled()) {
                                destinationFile.delete();
                                return;
                            }
                            total += count;
                            output.write(data, 0, count);
                            if (fileLength > 0) {
                                int progress = (int) (total * 100 / fileLength);
                                if (progress > lastProgress) {
                                    broadcastProgress(model.getFilename(), progress);
                                    notificationManager.notify(NOTIFICATION_ID, createNotification(model.getName() + " " + progress + "%", progress));
                                    lastProgress = progress;
                                }
                            }
                        }
                        output.flush();
                    }
                    broadcastComplete(model.getFilename());
                } catch (Exception e) {
                    Log.e(TAG, "File writing failed", e);
                    cleanupAndBroadcastFailure(model.getFilename());
                } finally {
                    activeCalls.remove(model.getFilename());
                    releaseWakeLockAndStopService();
                }
            }
        });
    }

    private void cancelDownload(String filename) {
        Call call = activeCalls.get(filename);
        if (call != null) {
            call.cancel();
        }
        cleanupAndBroadcastFailure(filename);
    }

    private void cleanupAndBroadcastFailure(String filename) {
        ongoingDownloads.remove(filename);
        activeCalls.remove(filename);
        broadcastFailure(filename);
        releaseWakeLockAndStopService();
    }

    private void releaseWakeLockAndStopService() {
        if (activeCalls.isEmpty()) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            stopSelf();
        }
    }

    // ====== Broadcast & Notification ======

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Download Service Channel", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification(String text, int progress) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Downloading Model")
                .setContentText(text)
                .setSmallIcon(R.drawable.thwakz_ai_logo)
                .setProgress(100, progress, false)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void broadcastProgress(String filename, int progress) {
        ongoingDownloads.put(filename, progress);
        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_FILENAME, filename);
        intent.putExtra(EXTRA_PROGRESS, progress);
        sendBroadcast(intent);
    }

    private void broadcastComplete(String filename) {
        ongoingDownloads.remove(filename);
        Intent intent = new Intent(ACTION_COMPLETE);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_FILENAME, filename);
        sendBroadcast(intent);
    }

    private void broadcastFailure(String filename) {
        ongoingDownloads.remove(filename);
        Intent intent = new Intent(ACTION_FAILED);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_FILENAME, filename);
        sendBroadcast(intent);
    }
}