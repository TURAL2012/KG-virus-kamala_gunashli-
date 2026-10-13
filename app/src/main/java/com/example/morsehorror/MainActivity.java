package com.example.morsehorror;

import android.app.Activity;
import android.os.Bundle;
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

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TARGET = "-.---."; // K (-.-) followed by G (--.)
    private static final long RESET_AFTER_MS = 5000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private StringBuilder entered = new StringBuilder();
    private long downAt;
    private long lastInputAt;
    private MediaPlayer player;
    private TextView hint;
    private Runnable resetRunnable;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        ImageView image = new ImageView(this);
        try {
            image.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("horror.png")));
        } catch (Exception ignored) { }
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(image, new FrameLayout.LayoutParams(-1, -1));

        hint = new TextView(this);
        hint.setText("TAP = DOT  •  HOLD 3s = DASH\nEnter K then G to exit\n5 seconds idle resets the code");
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(15);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(20, 16, 20, 16);
        hint.setBackgroundColor(0xAA000000);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        hp.setMargins(18, 0, 18, 22);
        root.addView(hint, hp);
        setContentView(root);

        image.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downAt = System.currentTimeMillis();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                long duration = System.currentTimeMillis() - downAt;
                if (duration >= 3000L) addSymbol('-');
                else if (duration < 600L) addSymbol('.');
                else hint.setText("Tap briefly for dot, hold at least 3 seconds for dash.\n" + entered);
                return true;
            }
            return true;
        });

        try {
            player = new MediaPlayer();
            player.setDataSource(getAssets().openFd("music_kg.mp3"));
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            if (player != null) { player.release(); player = null; }
            hint.setText("Music could not start. You can still enter the code.");
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
            if (player != null) { player.stop(); player.release(); player = null; }
            hint.setText("CODE ACCEPTED");
            handler.postDelayed(this::finish, 500);
            return;
        }
        if (resetRunnable != null) handler.removeCallbacks(resetRunnable);
        resetRunnable = () -> {
            entered.setLength(0);
            lastInputAt = 0;
            if (!isFinishing()) hint.setText("Code reset.\nTAP = DOT  •  HOLD 3s = DASH\nEnter K then G");
        };
        handler.postDelayed(resetRunnable, RESET_AFTER_MS);
    }

    @Override protected void onDestroy() {
        if (resetRunnable != null) handler.removeCallbacks(resetRunnable);
        if (player != null) { if (player.isPlaying()) player.stop(); player.release(); player = null; }
        super.onDestroy();
    }
}
