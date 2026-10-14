package com.example.morsehorror;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

public class MusicService extends Service {
    public static final String ACTION_STOP = "com.example.morsehorror.STOP_MUSIC";
    private static final String CHANNEL_ID = "kg_music_channel";
    private static final int NOTIFICATION_ID = 7321;
    private static final long RAMP_DURATION_MS = 7000L;
    private static final long RAMP_STEP_MS = 100L;

    private final Handler rampHandler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private long rampStartedAt;
    private Runnable rampRunnable;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();

        Intent stopIntent = new Intent(this, MusicService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        Notification notification = builder
                .setContentTitle("KG App is playing music")
                .setContentText("Tap Stop to stop the background music")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause, "Stop music", stopPending).build())
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        try {
            android.content.res.AssetFileDescriptor afd = getAssets().openFd("music_kg.mp3");
            player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setLooping(true);
            player.setOnPreparedListener(mp -> {
                mp.setVolume(0f, 0f);
                mp.start();
                startVolumeRamp();
            });
            player.prepareAsync();
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void startVolumeRamp() {
        cancelVolumeRamp();
        rampStartedAt = SystemClock.uptimeMillis();
        rampRunnable = new Runnable() {
            @Override public void run() {
                if (player == null) return;
                float progress = Math.min(1f,
                        (float) (SystemClock.uptimeMillis() - rampStartedAt) / RAMP_DURATION_MS);
                float volume = progress * progress;
                try {
                    // Ramps this app's playback volume; does not override the phone's volume controls.
                    player.setVolume(volume, volume);
                } catch (IllegalStateException ignored) {
                    return;
                }
                if (progress < 1f) rampHandler.postDelayed(this, RAMP_STEP_MS);
                else rampRunnable = null;
            }
        };
        rampHandler.post(rampRunnable);
    }

    private void cancelVolumeRamp() {
        if (rampRunnable != null) {
            rampHandler.removeCallbacks(rampRunnable);
            rampRunnable = null;
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "KG App music", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Controls for KG App background music");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override public void onDestroy() {
        cancelVolumeRamp();
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) { }
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}