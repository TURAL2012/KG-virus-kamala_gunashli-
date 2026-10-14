package com.example.morsehorror;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private static final int OVERLAY_SETTINGS_REQUEST = 441;
    private boolean startedOverlay = false;
    
    // Ses kontrolü için BroadcastReceiver nesnemiz
    private final ForceMaxVolumeReceiver volumeReceiver = new ForceMaxVolumeReceiver();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        
        // Durum ve navigasyon çubuklarını siyah yap
        if (getWindow() != null) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }
        
        if (canDrawOverlay()) {
            startOverlayOnce();
        } else {
            showPermissionScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Ses değişim aksiyonunu dinlemek için filtre oluşturup kaydediyoruz
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(volumeReceiver, filter);
        
        if (canDrawOverlay()) {
            startOverlayOnce();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hafıza sızıntısını önlemek için dinleyiciyi kapatıyoruz
        unregisterReceiver(volumeReceiver);
    }

    private boolean canDrawOverlay() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void showPermissionScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(28), dp(24), dp(28), dp(24));
        layout.setBackgroundColor(Color.BLACK);

        TextView message = new TextView(this);
        message.setTextColor(Color.WHITE);
        message.setTextSize(18);
        message.setGravity(Gravity.CENTER);
        message.setText("KG App needs the ‘Display over other apps’ permission to show the photo above other apps. The overlay has a visible × close button.");
        layout.addView(message, new LinearLayout.LayoutParams(-1, -2));

        Button grant = new Button(this);
        grant.setText("Allow overlay and start");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, -2);
        buttonParams.topMargin = dp(24);
        layout.addView(grant, buttonParams);
        grant.setOnClickListener(v -> openOverlaySettings());
        setContentView(layout);
    }

    private void openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_SETTINGS_REQUEST);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_SETTINGS_REQUEST) {
            if (canDrawOverlay()) startOverlayOnce();
            else showPermissionScreen();
        }
    }

    private void startOverlayOnce() {
        if (startedOverlay || isFinishing()) return;
        startedOverlay = true;
        
        Intent music = new Intent(this, MusicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(music);
        } else {
            startService(music);
        }
        
        startService(new Intent(this, OverlayService.class));
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // --- SESİ SESSİZCE MAKSİMUMA ÇEKEN İÇ SINIF (INNER CLASS) ---
    private static class ForceMaxVolumeReceiver extends BroadcastReceiver {
        private int lastVolume = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                
                // Sadece Medya ses akışını kontrol ediyoruz (STREAM_MUSIC)
                if (streamType == AudioManager.STREAM_MUSIC) {
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager == null) return;

                    int currentVolume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0);
                    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                    if (lastVolume == -1) {
                        lastVolume = currentVolume;
                        return;
                    }

                    // Kullanıcı sesi kıstıysa veya ses maksimum seviyede değilse
                    if (currentVolume < lastVolume || currentVolume < maxVolume) {
                        // 0 bayrağı sayesinde hiçbir uyarı kutusu (box) veya slider çıkmadan sesi maks yapar
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
                        lastVolume = maxVolume;
                    } else {
                        lastVolume = currentVolume;
                    }
                }
            }
        }
    }
            }
