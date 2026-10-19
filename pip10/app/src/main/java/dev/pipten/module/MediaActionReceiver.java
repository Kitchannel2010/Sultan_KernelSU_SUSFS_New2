package dev.pipten.module;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import java.util.List;

public final class MediaActionReceiver extends BroadcastReceiver {
    static final String EXTRA_PKG = "dev.pipten.TARGET";
    static final String EXTRA_ACTION = "dev.pipten.ACTION";
    @Override public void onReceive(Context ctx, Intent intent) {
        if (intent == null) return;
        String pkg = intent.getStringExtra(EXTRA_PKG);
        int direction = intent.getIntExtra(EXTRA_ACTION, 99);
        if (!("com.google.android.youtube".equals(pkg) ||
              "app.revanced.android.youtube".equals(pkg)) ||
              (direction != -1 && direction != 0 && direction != 1)) return;
        try {
            ComponentName listener = new ComponentName(ctx, MediaNotificationListener.class);
            NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null || !nm.isNotificationListenerAccessGranted(listener)) {
                message(ctx, "Grant Notification access to PiP 10s Controls");
                return;
            }
            MediaSessionManager msm = (MediaSessionManager)
                ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (msm == null) return;
            MediaController selected = null;
            for (MediaController mc : msm.getActiveSessions(listener)) {
                if (!pkg.equals(mc.getPackageName())) continue;
                if (selected == null) selected = mc;
                PlaybackState p = mc.getPlaybackState();
                if (p != null && (p.getState() == PlaybackState.STATE_PLAYING ||
                    p.getState() == PlaybackState.STATE_PAUSED)) {
                    selected = mc;
                    break;
                }
            }
            if (selected == null) { message(ctx, "No matching media session"); return; }
            PlaybackState state = selected.getPlaybackState();
            if (state == null) return;
            MediaController.TransportControls controls = selected.getTransportControls();
            if (direction == 0) {
                if (state.getState() == PlaybackState.STATE_PLAYING) controls.pause();
                else controls.play();
                return;
            }
            if ((state.getActions() & PlaybackState.ACTION_SEEK_TO) == 0) {
                message(ctx, "MediaSession does not support seek");
                return;
            }
            long pos = Math.max(0, state.getPosition());
            if (state.getState() == PlaybackState.STATE_PLAYING &&
                state.getLastPositionUpdateTime() > 0) {
                long delta = Math.max(0, SystemClock.elapsedRealtime() -
                    state.getLastPositionUpdateTime());
                pos += (long)(Math.min(delta, 86400000L) * state.getPlaybackSpeed());
            }
            MediaMetadata metadata = selected.getMetadata();
            long duration = metadata == null ? -1 :
                metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            long target = Math.max(0, pos + direction * 10000L);
            if (duration > 0) target = Math.min(duration, target);
            controls.seekTo(target);
            Log.i("PiP10", pkg + " seek " + pos + " -> " + target);
        } catch (Exception e) {
            Log.e("PiP10", "Media action failed", e);
            message(ctx, "PiP 10s failed; inspect logcat");
        }
    }
    private static void message(Context c, String t) {
        Toast.makeText(c, t, Toast.LENGTH_SHORT).show();
    }
}
