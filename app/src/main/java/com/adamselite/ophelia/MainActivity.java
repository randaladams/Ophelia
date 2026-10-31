package com.adamselite.ophelia;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** Ophelia runs the card game page from the app's assets in a full-screen WebView. */
public class MainActivity extends ComponentActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#173326"));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);   // keeps the win/loss record and settings
        settings.setTextZoom(100);
        webView.addJavascriptInterface(new Bridge(this), "Android");
        // A long press must never turn into text selection or a menu; the game treats it as a tap.
        webView.setLongClickable(false);
        webView.setOnLongClickListener(v -> true);
        webView.setHapticFeedbackEnabled(false);
        setContentView(webView);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl("file:///android_asset/index.html");
        }

        // Back closes the rules, settings or results screen first; otherwise it leaves the game.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                webView.evaluateJavascript("window.handleBack ? window.handleBack() : false", value -> {
                    if (!"true".equals(value)) {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                        setEnabled(true);
                    }
                });
            }
        });

        hideSystemBars();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    /** Lets the game page buzz the phone and choose how the screen turns. */
    static class Bridge {
        private final ComponentActivity activity;
        private final Context context;

        Bridge(ComponentActivity activity) {
            this.activity = activity;
            this.context = activity.getApplicationContext();
        }

        /** Two short buzzes: a match is waiting. */
        @JavascriptInterface
        public void vibrate() {
            Vibrator vibrator = context.getSystemService(Vibrator.class);
            if (vibrator == null || !vibrator.hasVibrator()) return;
            vibrator.vibrate(VibrationEffect.createWaveform(new long[] {0, 90, 60, 90}, -1));
        }

        /**
         * "landscape" keeps the table sideways, "portrait" keeps it upright, anything else turns with
         * the device. Large screens such as tablets may ignore a fixed choice; the game handles both.
         */
        @JavascriptInterface
        public void setOrientation(String mode) {
            final int o = "landscape".equals(mode) ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    : "portrait".equals(mode) ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
            activity.runOnUiThread(() -> activity.setRequestedOrientation(o));
        }
    }
}
