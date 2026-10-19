package dev.pipten.module;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private TextView status;
    @Override public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(36, 36, 36, 36);
        TextView text = new TextView(this);
        text.setTextSize(18f);
        text.setText("PiP10 v0.5.0 — Standalone iOS-style controls\n\n" +
            "This build draws large floating 10-second controls from scratch. " +
            "No LSPosed, root or SystemUI hook is needed.\n\n" +
            "Enable BOTH permissions below, then start a normal YouTube video in PiP. " +
            "The controls follow the video window.\n\n" +
            "Privacy: the service reads PiP window bounds and active media playback " +
            "state only. It does not save or upload notifications or screen contents.\n\n" +
            "IMPORTANT: disable PiP10's old LSPosed scope before using this standalone version.");
        content.addView(text);
        Button a = new Button(this);
        a.setText("1. Enable accessibility overlay");
        a.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        content.addView(a);
        Button n = new Button(this);
        n.setText("2. Enable notification access for media control");
        n.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        content.addView(n);
        Button refresh = new Button(this);
        refresh.setText("Check permissions and MediaSession");
        refresh.setOnClickListener(v -> renderStatus());
        content.addView(refresh);
        status = new TextView(this);
        content.addView(status);
        scroll.addView(content);
        setContentView(scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        renderStatus();
    }

    private void renderStatus() {
        if (status == null) return;
        boolean a = false;
        try {
            String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String component = new ComponentName(
                this, OverlayAccessibilityService.class).flattenToString();
            a = enabled != null &&
                enabled.toLowerCase().contains(component.toLowerCase());
        } catch (Throwable ignored) {}
        status.setText("\nAccessibility enabled: " + a +
                "\n" + MediaBridge.diagnostics(this) +
                "\n\nDebug: adb logcat -d -s PiP10:I");
    }
}
