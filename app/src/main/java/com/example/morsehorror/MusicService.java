package com.example.morsehorror;

import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/** User-started photo overlay. A visible close control is always available. */
public class OverlayService extends Service {
    private WindowManager windowManager;
    private FrameLayout overlay;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (overlay != null) return START_NOT_STICKY;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow Display over other apps, then open KG App again.", Toast.LENGTH_LONG).show();
            stopService(new Intent(this, MusicService.class));
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            overlay = new FrameLayout(this);
            overlay.setBackgroundColor(Color.BLACK);

            ImageView image = new ImageView(this);
            image.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("horror.png")));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            overlay.addView(image, new FrameLayout.LayoutParams(-1, -1));

            TextView close = new TextView(this);
            close.setText("×");
            close.setTextColor(Color.WHITE);
            close.setTextSize(34);
            close.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            close.setGravity(Gravity.CENTER);
            close.setContentDescription("Close photo overlay");
            GradientDrawable background = new GradientDrawable();
            background.setColor(0xDD111111);
            background.setShape(GradientDrawable.OVAL);
            background.setStroke(dp(1), 0xFFFFFFFF);
            close.setBackground(background);
            int size = dp(56);
            FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.END);
            closeParams.setMargins(0, dp(20), dp(12), 0);
            overlay.addView(close, closeParams);
            close.setOnClickListener(v -> closeOverlay());

            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            windowManager.addView(overlay, params);
        } catch (Exception e) {
            Toast.makeText(this, "Could not show the photo overlay. Check its permission.", Toast.LENGTH_LONG).show();
            stopService(new Intent(this, MusicService.class));
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void closeOverlay() {
        stopService(new Intent(this, MusicService.class));
        stopSelf();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        if (overlay != null && windowManager != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) { }
            overlay = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
