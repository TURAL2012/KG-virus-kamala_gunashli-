package com.example.morsehorror;

import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/** Full-screen photo overlay with a hidden Morse unlock gesture. */
public class OverlayService extends Service {
    private static final String TARGET = "-.---.";
    private static final long RESET_AFTER_MS = 5000L;
    private static final long EYE_TAP_WINDOW_MS = 1800L;
    private static final float SWIPE_DASH_MIN_DP = 70f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final StringBuilder entered = new StringBuilder();

    private WindowManager windowManager;
    private FrameLayout overlay;
    private ImageView image;
    private TextView hint;
    private boolean challengeRevealed;
    private boolean ignoreCurrentGesture;
    private int eyeTapCount;
    private long lastEyeTapAt;
    private float downX;
    private float downY;
    private long downAt;
    private long lastInputAt;
    private Runnable resetRunnable;

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

            image = new ImageView(this);
            image.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("horror.png")));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            overlay.addView(image, new FrameLayout.LayoutParams(-1, -1));

            hint = new TextView(this);
            hint.setText("");
            hint.setVisibility(View.GONE);
            hint.setTextColor(Color.WHITE);
            hint.setTextSize(15);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(dp(20), dp(16), dp(20), dp(16));
            hint.setBackgroundColor(0xAA000000);
            FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            hp.setMargins(dp(18), 0, dp(18), dp(22));
            overlay.addView(hint, hp);

            image.setOnTouchListener((v, event) -> {
                if (!challengeRevealed) {
                    handleSecretEyeGesture(v, event);
                    return true;
                }

                if (ignoreCurrentGesture) {
                    if (event.getActionMasked() == MotionEvent.ACTION_UP
                            || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                        ignoreCurrentGesture = false;
                    }
                    return true;
                }

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        downAt = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_UP:
                        float dx = event.getX() - downX;
                        float dy = event.getY() - downY;
                        float minSwipe = SWIPE_DASH_MIN_DP * getResources().getDisplayMetrics().density;
                        long duration = System.currentTimeMillis() - downAt;

                        // Swipe left-to-right anywhere on the screen to enter a dash.
                        if (dx >= minSwipe && Math.abs(dy) <= Math.max(minSwipe, dx * 0.75f)) {
                            addSymbol('-');
                        } else if (Math.abs(dx) < minSwipe / 2f
                                && Math.abs(dy) < minSwipe / 2f && duration < 700L) {
                            // A short tap enters a dot.
                            addSymbol('.');
                        } else {
                            hint.setText("Qısa toxunuş = .   |   Soldan sağa sürüşdür = -\nKod: " + entered);
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        downAt = 0;
                        return true;

                    default:
                        return true;
                }
            });

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

    // Three quick two-finger taps, one finger on each eye, reveal Morse mode.
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
        if (now - lastEyeTapAt > EYE_TAP_WINDOW_MS) eyeTapCount = 0;

        if ((leftEye && rightEye) || (reverseLeft && reverseRight)) {
            eyeTapCount++;
            lastEyeTapAt = now;
            if (eyeTapCount >= 3) {
                challengeRevealed = true;
                ignoreCurrentGesture = true;
                hint.setVisibility(View.VISIBLE);
                hint.setText("Qısa toxunuş = NÖQTƏ (.)\nSoldan sağa sürüşdür = TİRE (-)\nKod: K sonra G");
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

        hint.setText("Kod: " + entered
                + "\nToxunuş = .  |  Soldan sağa sürüşdür = -"
                + "\n5 saniyə fəaliyyətsizlikdən sonra sıfırlanır");

        if (TARGET.contentEquals(entered)) {
            hint.setText("CODE ACCEPTED");
            handler.postDelayed(() -> {
                stopService(new Intent(this, MusicService.class));
                stopSelf();
            }, 400L);
            return;
        }

        if (resetRunnable != null) handler.removeCallbacks(resetRunnable);
        resetRunnable = () -> {
            entered.setLength(0);
            lastInputAt = 0;
            if (hint != null && challengeRevealed && overlay != null) {
                hint.setText("Kod sıfırlandı.\nToxunuş = .  |  Soldan sağa sürüşdür = -");
            }
        };
        handler.postDelayed(resetRunnable, RESET_AFTER_MS);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        if (resetRunnable != null) handler.removeCallbacks(resetRunnable);
        if (overlay != null && windowManager != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) { }
            overlay = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
