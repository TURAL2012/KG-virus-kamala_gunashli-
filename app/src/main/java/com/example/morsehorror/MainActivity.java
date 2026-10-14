package com.example.morsehorror;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.IntentFilter;
import androidx.appcompat.app.AppCompatActivity;



public class MainActivity extends AppCompatActivity {
    // Alıcı nesnemizi tanımlıyoruz
    private ForceMaxVolumeReceiver volumeReceiver = new ForceMaxVolumeReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ses değişim aksiyonunu dinlemek için filtre oluşturuyoruz
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(volumeReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Uygulama arka plana geçtiğinde hafıza sızıntısını önlemek için dinleyiciyi kapatıyoruz
        unregisterReceiver(volumeReceiver);
    }
}













public class MainActivity extends Activity {
    private static final int OVERLAY_SETTINGS_REQUEST = 441;
    private boolean startedOverlay = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        if (canDrawOverlay()) {
            startOverlayOnce();
        } else {
            showPermissionScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (canDrawOverlay()) startOverlayOnce();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(music);
        else startService(music);
        startService(new Intent(this, OverlayService.class));
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
