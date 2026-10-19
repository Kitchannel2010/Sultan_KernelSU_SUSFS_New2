package dev.pipten.module;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private TextView status;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32,32,32,32);
        TextView instructions = new TextView(this);
        instructions.setText("PiP 10s Controls\n\nEnable LSPosed module for YouTube / Morphe, force stop YouTube, then grant Notification access.\nExperimental Android 16 build.");
        instructions.setTextSize(18);
        layout.addView(instructions);
        Button access = new Button(this);
        access.setText("Grant Notification access");
        access.setOnClickListener(v -> startActivity(
            new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        layout.addView(access);
        Button test = new Button(this);
        test.setText("Check media sessions");
        test.setOnClickListener(v -> showStatus());
        layout.addView(test);
        status = new TextView(this);
        layout.addView(status);
        setContentView(layout);
    }
    @Override protected void onResume() { super.onResume(); showStatus(); }
    private void showStatus() {
        try {
            ComponentName component = new ComponentName(this, MediaNotificationListener.class);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            boolean granted = nm != null && nm.isNotificationListenerAccessGranted(component);
            StringBuilder sb = new StringBuilder("Notification access: " + granted);
            if (granted) {
                MediaSessionManager manager = (MediaSessionManager)
                    getSystemService(MEDIA_SESSION_SERVICE);
                if (manager != null) for (MediaController c : manager.getActiveSessions(component)) {
                    PlaybackState p = c.getPlaybackState();
                    sb.append("\n").append(c.getPackageName())
                        .append(" seekTo=").append(p != null &&
                        (p.getActions() & PlaybackState.ACTION_SEEK_TO) != 0);
                }
            }
            status.setText(sb.toString());
        } catch (Exception e) { status.setText(e.toString()); }
    }
}
