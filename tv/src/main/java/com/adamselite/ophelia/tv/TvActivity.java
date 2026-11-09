package com.adamselite.ophelia.tv;

import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Ophelia for Fire TV: the same game page as the phone app, full screen, with tv.js added
 * after the page loads so the remote's arrows, OK and media buttons play the game.
 */
public class TvActivity extends ComponentActivity {

    private WebView webView;
    private String tvScript = "";

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
        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        tvScript = readAsset("tv.js");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(tvScript, null);
            }
        });
        setContentView(webView);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl("file:///android_asset/index.html");
        }

        // Back closes the rules, settings or results, or unpicks a card; otherwise it leaves the game.
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
        webView.requestFocus();
    }

    /** Play/Pause lays down your card, Rewind undoes, Menu shows the rules. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        String name = null;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                name = "play";
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                name = "rewind";
                break;
            case KeyEvent.KEYCODE_MENU:
                name = "menu";
                break;
            default:
                break;
        }
        if (name != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                webView.evaluateJavascript("window.tvKey && window.tvKey('" + name + "')", null);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
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

    private String readAsset(String name) {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /** What the game page can ask of the app. A TV has no vibration and doesn't turn. */
    static class Bridge {
        /** The version shown in settings and on the opening screen, e.g. 1.0.33. */
        @JavascriptInterface
        public String versionName() {
            return BuildConfig.VERSION_NAME;
        }
    }
}
