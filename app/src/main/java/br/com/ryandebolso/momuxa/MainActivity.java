package br.com.ryandebolso.momuxa;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/** Offline-only native WebView shell for the exact approved Momuxo HTML. */
public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 123;
    private WebView browser;
    private ImageView launchSplash;
    private boolean pendingEnableReminder;
    private boolean pendingNotificationTest;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(255, 247, 240));
        getWindow().setNavigationBarColor(Color.rgb(255, 247, 240));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        FrameLayout rootView = new FrameLayout(this);
        browser = new WebView(this);
        browser.setBackgroundColor(Color.rgb(255, 247, 240));
        browser.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        browser.setOverScrollMode(View.OVER_SCROLL_NEVER);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setAllowFileAccess(true);
        browser.getSettings().setAllowContentAccess(false);
        browser.getSettings().setAllowFileAccessFromFileURLs(false);
        browser.getSettings().setAllowUniversalAccessFromFileURLs(false);
        browser.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        browser.getSettings().setSupportMultipleWindows(false);
        browser.addJavascriptInterface(new RyanBridge(), "RyanAndroid");
        browser.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Brief branded intro while the offline game initializes.
                if (launchSplash != null) launchSplash.postDelayed(() -> {
                    if (launchSplash != null && launchSplash.getParent() == rootView) {
                        rootView.removeView(launchSplash);
                        launchSplash = null;
                    }
                }, 950);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri.toString().startsWith("file:///android_asset/")) return false;
                // No remote browsing inside the pet; open user-triggered URLs outside it.
                if (request.isForMainFrame()) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
                }
                return true;
            }
        });
        rootView.addView(browser, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        launchSplash = new ImageView(this);
        launchSplash.setImageResource(R.drawable.momuxo_capa);
        launchSplash.setScaleType(ImageView.ScaleType.FIT_CENTER);
        launchSplash.setBackgroundColor(Color.rgb(255, 241, 235));
        rootView.addView(launchSplash, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(rootView);
        browser.loadUrl("file:///android_asset/index.html");
    }

    private class RyanBridge {
        @JavascriptInterface public boolean areRemindersEnabled() {
            return ReminderScheduler.areEnabled(MainActivity.this);
        }
        @JavascriptInterface public void toggleReminders() {
            runOnUiThread(() -> {
                boolean on = ReminderScheduler.areEnabled(MainActivity.this);
                if (on) {
                    ReminderScheduler.setEnabled(MainActivity.this, false);
                    toast("Recadinhos desativados.");
                    notifyPage();
                } else if (!ReminderScheduler.notificationsAllowed(MainActivity.this)) {
                    pendingEnableReminder = true;
                    requestNotificationPermission();
                } else {
                    ReminderScheduler.setEnabled(MainActivity.this, true);
                    toast("Recadinhos ativados para o fim da tarde. 💗");
                    notifyPage();
                }
            });
        }
        @JavascriptInterface public void testReminder() {
            runOnUiThread(() -> {
                if (!ReminderScheduler.notificationsAllowed(MainActivity.this)) {
                    pendingNotificationTest = true;
                    requestNotificationPermission();
                } else {
                    ReminderReceiver.show(MainActivity.this);
                }
            });
        }
        /** Automatically saved in the app's private storage, on every state update. */
        @JavascriptInterface public String loadProgress() {
            return getSharedPreferences("ryan_progress", MODE_PRIVATE).getString("state", "");
        }
        @JavascriptInterface public void saveProgress(String data) {
            if (data == null || data.length() > 1000000 || data.length() < 10) return;
            // commit() makes each saved care action durable before the call returns.
            getSharedPreferences("ryan_progress", MODE_PRIVATE).edit().putString("state", data).commit();
        }

    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATION_PERMISSION) return;
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            if (pendingEnableReminder) {
                ReminderScheduler.setEnabled(this, true);
                toast("Recadinhos ativados. 💗");
            }
            if (pendingNotificationTest) ReminderReceiver.show(this);
        } else {
            toast("Tudo bem! O Ryan não precisa enviar notificações.");
        }
        pendingEnableReminder = false;
        pendingNotificationTest = false;
        notifyPage();
    }

    private void notifyPage() {
        if (browser != null) browser.evaluateJavascript("window.ryanNativeNotificationChanged && window.ryanNativeNotificationChanged()", null);
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    @Override @Deprecated public void onBackPressed() {
        if (browser == null) { super.onBackPressed(); return; }
        browser.evaluateJavascript("(function(){" +
            "var a=document.getElementById('hubLayer58');if(a&&!a.hidden){document.getElementById('hubClose58').click();return true;}" +
            "var m=document.getElementById('modalLayer');if(m&&!m.classList.contains('hidden')){document.getElementById('modalClose').click();return true;}" +
            "var c=document.getElementById('careSet');if(c&&!c.hidden){document.getElementById('closeCareScene').click();return true;}" +
            "var h=document.querySelector('.bottom-nav [data-go=home]');if(h&&!h.classList.contains('active')){h.click();return true;}" +
            "return false;})()", value -> {
                if (!"true".equals(value)) moveTaskToBack(true);
            });
    }

    @Override protected void onPause() {
        if (browser != null) {
            browser.evaluateJavascript("window.ryanSaveNow && window.ryanSaveNow()", null);
            browser.onPause();
        }
        super.onPause();
    }
    @Override protected void onResume() { super.onResume(); if (browser != null) browser.onResume(); }
    @Override protected void onDestroy() {
        if (browser != null) { browser.removeJavascriptInterface("RyanAndroid"); browser.destroy(); browser = null; }
        super.onDestroy();
    }
}
