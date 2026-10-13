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
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends Activity {
    private static final String TARGET = "-.---."; // K (-.-) followed by G (--.)
    private static final long RESET_AFTER_MS = 5000L;
    private static final long DASH_MS = 3000L;
    private static final int OVERLAY_SETTINGS_REQUEST = 441;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final StringBuilder entered = new StringBuilder();
    private long downAt;
    private long lastInputAt;
    private MediaPlayer player;
    private TextView hint;
    private ImageView image;
    private Runnable resetRunnable;
    private boolean challengeRevealed = false;
    private int eyeTapCount = 0;
    private long lastEyeTapAt = 0;
    private boolean settingsOpened = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        // Android requires the user to grant overlay access manually in system settings.
        // We open that settings page once on launch, but never use an overlay to trap the user.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this) && !settingsOpened) {
            settingsOpened = true;
            try {
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
        if (settingsOpened && image == null) showHorrorScreen();
    }

    private void showHorrorScreen() {
        if (image != null) return;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        image = new ImageView(this);
        try {
            image.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("horror.png")));
        } catch (Exception ignored) { }
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

        image.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (!challengeRevealed) {
                    handleSecretEyeGesture(v, event);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downAt = System.currentTimeMillis();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    long duration = System.currentTimeMillis() - downAt;
                    if (duration >= DASH_MS) addSymbol('-');
                    else if (duration < 700L) addSymbol('.');
                    else hint.setText("Tap briefly for dot; hold for 3 seconds for dash.\n" + entered);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    downAt = 0;
                    return true;
                }
                return true;
            }
        });

        try {
            player = new MediaPlayer();
            player.setDataSource(getAssets().openFd("music_kg.mp3"));
            player.setLooping(true);
            player.setOnPreparedListener(mp -> mp.start());
            player.prepareAsync();
        } catch (Exception e) {
            if (player != null) { player.release(); player = null; }
        }
    }

    // Reveal Morse instructions only after three two-finger taps landing on both eyes.
    private void handleSecretEyeGesture(View v, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_POINTER_UP || event.getPointerCount() != 2) return;
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
                hint.setText("TAP = DOT  •  HOLD 3s = DASH\nEnter K then G to exit\n5 seconds idle resets the code");
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
        hint.setText("Code: " + entered + "\nTap = dot • hold 3s = dash\n5 seconds idle resets");
        if (entered.toString().equals(TARGET)) {
            if (player != null) { try { player.stop(); } catch (Exception ignored) {} player.release(); player = null; }
            hint.setText("CODE ACCEPTED");
            handler.postDelayed(this::finish, 500);
            return;
        }
        if (resetRunnable != null) handler.removeCallbacks(resetRunnable);
        resetRunnable = () -> {
            entered.setLength(0);
            lastInputAt = 0;
            if (!isFinishing() && hint != null && challengeRevealed)
                hint.setText("Code reset.\nTAP = DOT  •  HOLD 3s = DASH\nEnter K then G");
        };
        handler.postDelayed(resetRunnable, RESET_AFTER_MS);
    }

    @Override public void onBackPressed() {
        // Keep the app normally escapable; leaving also stops its audio.
        super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (resetRunnable != null) handler.removeCallbacks(resetRunnable);
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) { }
            player.release(); player = null;
        }
        super.onDestroy();
    }
}
