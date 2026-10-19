package dev.pipten.module;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
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
        text.setText("PiP10 v0.6.0 — Liquid Glass controls\n\n" +
            "A compact translucent glass capsule with iOS-style 10-second " +
            "rewind, play/pause and forward buttons. No LSPosed or root needed.\n\n" +
            "Updating from v0.5? Your Accessibility and Notification permissions " +
            "should still be enabled.\n\n" +
            "The glass effect uses gradients and highlights; it does not " +
            "record the display or blur protected video content.\n\n" +
            "Disable any old PiP10 LSPosed scope to avoid duplicate controls.");
        content.addView(text);
        Button a = new Button(this);
        a.setText("Accessibility service settings");
        a.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        content.addView(a);
        Button n = new Button(this);
        n.setText("Notification access settings");
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
        boolean accessible = false;
        try {
            String enabled = Settings.Secure.getString(
                    getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String component = new ComponentName(
                    this, OverlayAccessibilityService.class).flattenToString();
            accessible = enabled != null &&
                    enabled.toLowerCase().contains(component.toLowerCase());
        } catch (Throwable ignored) {}
        status.setText("\nAccessibility enabled: " + accessible +
                "\n" + MediaBridge.diagnostics(this) +
                "\n\nDebug: adb logcat -d -s PiP10:I");
    }
}
