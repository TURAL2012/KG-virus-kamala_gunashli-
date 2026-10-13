package com.example.morsehorror;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends Activity {
    private static final String TARGET = "-.---."; // K (-.-) followed by G (--.)
    private static final long RESET_AFTER_MS = 5000L;
    private static final int OVERLAY_SETTINGS_REQUEST = 441;
    private static final float SWIPE_THRESHOLD_DP = 80f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final StringBuilder entered = new StringBuilder();
    private long downAt;
    private long lastInputAt;
    private float downX, downY;
    private TextView hint;
    private ImageView image;
    private Runnable resetRunnable;
    private boolean challengeRevealed = false;
    private int eyeTapCount = 0;
    private long lastEyeTapAt = 0;
    private boolean settingsRequested = false;
    private boolean musicStarted = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this) && !settingsRequested) {
            settingsRequested = true;
            try {
                // The package URI asks Android to show this app's own overlay-permission page.
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_SETTINGS_REQUEST);
                return;
            } catch (Exception ignored) { }
        }
        showHorrorScreen();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_SETTINGS_REQUEST) showHorrorScreen();
    }

    @Override protected void onResume() {
        super.onResume();
        if (settingsRequested && image == null) showHorrorScreen();
    }

    private void startMusicIfNeeded() {
        if (musicStarted) return;
        musicStarted = true;
        Intent intent = new Intent(this, MusicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    private void showHorrorScreen() {
        if (image != null) return;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        image = new ImageView(this);
        try { image.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("horror.png"))); }
        catch (Exception ignored) { }
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(image, new FrameLayout.LayoutParams(-1, -1));

        hint = new TextView(this);
        hint.setText("");
        hint.setVisibility(View.GONE);
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(15);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(20, 16, 20, 16);
        hint.setBackgroundColor(0xAA000000);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        hp.setMargins(18, 0, 18, 22);
        root.addView(hint, hp);
        setContentView(root);
        startMusicIfNeeded();

        image.setOnTouchListener((v, event) -> {
            if (!challengeRevealed) {
                handleSecretEyeGesture(v, event);
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downAt = System.currentTimeMillis();
                    downX = event.getX();
                    downY = event.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;
                    float threshold = SWIPE_THRESHOLD_DP * getResources().getDisplayMetrics().density;
                    long duration = System.currentTimeMillis() - downAt;
                    if (dx >= threshold && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                        addSymbol('-'); // left-to-right swipe is dash
                    } else if (Math.abs(dx) < threshold / 2 && Math.abs(dy) < threshold / 2 && duration < 700L) {
                        addSymbol('.'); // short tap is dot
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    downAt = 0;
                    return true;
                default:
                    return true;
            }
        });
    }

    // Reveal the Morse challenge only after three two-finger taps landing on both eyes.
    private void handleSecretEyeGesture(View v, MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_POINTER_UP || event.getPointerCount() != 2) return;
        int lifted = event.getActionIndex();
        int other = lifted == 0 ? 1 : 0;
        float x1 = event.getX(lifted) / Math.max(1f, v.getWidth());
        float x2 = event.getX(other) / Math.max(1f, v.getWidth());
        float y1 = event.getY(lifted) / Math.max(1f, v.getHeight());
        float y2 = event.getY(other) / Math.max(1f, v.getHeight());
        boolean leftEye = x1 >= 0.29f && x1 <= 0.51f && y1 >= 0.30f && y1 <= 0.53f;
        boolean rightEye = x2 >= 0.49f && x2 <= 0.72f && y2 >= 0.30f && y2 <= 0.53f;
        boolean reverseLeft = x2 >= 0.29f && x2 <= 0.51f && y2 >= 0.30f && y2 <= 0.53f;
        boolean reverseRight = x1 >= 0.49f && x1 <= 0.72f && y1 >= 0.30f && y1 <= 0.53f;
        long now = System.currentTimeMillis();
        if (now - lastEyeTapAt > 1800L) eyeTapCount = 0;
        if ((leftEye && rightEye) || (reverseLeft && reverseRight)) {
            eyeTapCount++;
            lastEyeTapAt = now;
            if (eyeTapCount >= 3) {
                challengeRevealed = true;
                hint.setVisibility(View.VISIBLE);
                hint.setText("Tap = dot • Swipe left to right = dash\nEnter K then G to exit\n5 seconds idle resets the code");
            }
        } else {
            eyeTapCount = 0;
            lastEyeTapAt = now;
        }
    }

    private void addSymbol(char symbol) {
        long now = System.currentTimeMillis();
        if (lastInputAt != 0 && now - lastInputAt >= RESET_AFTER_MS) entered.setLength(0);
        lastInputAt = now;
        entered.append(symbol);
        if (entered.length() > TARGET.length()) entered.delete(0, entered.length() - TARGET.length());
        hint.setText("Code: " + entered + "\nTap = dot • swipe left to right = dash\n5 seconds idle resets");
        if (entered.toString().equals(TARGET)) {
            hint.setText("CODE ACCEPTED");
            handler.postDelayed(() -> {
                stopService(new Intent(this, MusicService.class));
                finish();
            }, 500);
            return;
        }
        if (resetRunnable != null) handler.removeCallbacks(resetRunnable);
        resetRunnable = () -> {
            entered.setLength(0);
            lastInputAt = 0;
            if (!isFinishing() && hint != null && challengeRevealed)
                hint.setText("Code reset.\nTap = dot • Swipe left to right = dash");
        };
        handler.postDelayed(resetRunnable, RESET_AFTER_MS);
    }

    @Override public void onBackPressed() {
        // Normal Android navigation remains available; the foreground notification controls background audio.
        super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (resetRunnable != null) handler.removeCallbacks(resetRunnable);
        // Deliberately do not stop the music service here; it continues until the user stops it or unlocks.
        super.onDestroy();
    }
}
