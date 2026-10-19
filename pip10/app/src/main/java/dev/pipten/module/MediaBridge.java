package dev.pipten.module;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import java.util.List;

/**
 * Controls a public MediaSession through user-approved Notification access.
 * No private YouTube internals, no SystemUI hook and no media recording.
 */
final class MediaBridge {
    private static final String TAG = "PiP10";
    private static final String YOUTUBE = "com.google.android.youtube";
    private static final String REVANCED = "app.revanced.android.youtube";
    private static String lastPackage;
    private static long lastSeekTarget;
    private static long lastSeekAt;
    private MediaBridge() {}

    static boolean hasAccess(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationListenerAccessGranted(
                new ComponentName(ctx, PiPMediaListener.class));
    }

    private static MediaController findController(Context ctx) {
        if (!hasAccess(ctx)) return null;
        MediaSessionManager mgr = (MediaSessionManager)
                ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (mgr == null) return null;
        List<MediaController> sessions =
                mgr.getActiveSessions(new ComponentName(ctx, PiPMediaListener.class));
        MediaController chosen = null;
        for (MediaController c : sessions) {
            String pkg = c.getPackageName();
            if (!YOUTUBE.equals(pkg) && !REVANCED.equals(pkg)) continue;
            if (chosen == null) chosen = c;
            PlaybackState state = c.getPlaybackState();
            if (state != null && (state.getState() == PlaybackState.STATE_PLAYING ||
                    state.getState() == PlaybackState.STATE_PAUSED ||
                    state.getState() == PlaybackState.STATE_BUFFERING)) {
                chosen = c;
                break;
            }
        }
        return chosen;
    }

    static boolean isPlaying(Context ctx) {
        try {
            MediaController c = findController(ctx);
            PlaybackState p = c == null ? null : c.getPlaybackState();
            return p != null && p.getState() == PlaybackState.STATE_PLAYING;
        } catch (Throwable t) {
            Log.w(TAG, "Playback state unavailable", t);
            return false;
        }
    }

    static String diagnostics(Context ctx) {
        if (!hasAccess(ctx)) return "Notification access: disabled";
        try {
            MediaController c = findController(ctx);
            if (c == null) return "Notification access: enabled; no YouTube MediaSession";
            PlaybackState p = c.getPlaybackState();
            return "Notification access: enabled\nSession: " + c.getPackageName() +
                    "\nPlayback: " + (p == null ? "unavailable" : p.getState()) +
                    "\nSeek supported: " + (p != null &&
                       (p.getActions() & PlaybackState.ACTION_SEEK_TO) != 0);
        } catch (Throwable t) { return t.toString(); }
    }

    static void control(Context ctx, int dir) {
        try {
            MediaController controller = findController(ctx);
            if (controller == null) {
                warn(ctx, "No YouTube MediaSession — check Notification access");
                return;
            }
            PlaybackState state = controller.getPlaybackState();
            if (state == null) {
                warn(ctx, "Playback position unavailable");
                return;
            }
            MediaController.TransportControls controls = controller.getTransportControls();
            long before = SystemClock.elapsedRealtime();
            if (dir == 0) {
                if (state.getState() == PlaybackState.STATE_PLAYING) controls.pause();
                else controls.play();
                Log.i(TAG, "Standalone play/pause dispatchMs=" +
                        (SystemClock.elapsedRealtime() - before));
                return;
            }
            if ((state.getActions() & PlaybackState.ACTION_SEEK_TO) == 0) {
                warn(ctx, "This YouTube MediaSession does not support seek");
                return;
            }
            long position = Math.max(0, state.getPosition());
            if (state.getState() == PlaybackState.STATE_PLAYING &&
                    state.getLastPositionUpdateTime() > 0) {
                long dt = Math.max(0, Math.min(30000,
                        before - state.getLastPositionUpdateTime()));
                position += (long)(dt * state.getPlaybackSpeed());
            }
            String pkg = controller.getPackageName();
            // Some players update PlaybackState asynchronously; maintain consecutive seeks.
            if (pkg.equals(lastPackage) && before - lastSeekAt < 2000) {
                position = lastSeekTarget;
                if (state.getState() == PlaybackState.STATE_PLAYING)
                    position += (long)((before - lastSeekAt) * state.getPlaybackSpeed());
            }
            long target = Math.max(0, position + dir * 10000L);
            MediaMetadata metadata = controller.getMetadata();
            long duration = metadata == null ? -1 :
                    metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            if (duration > 0) target = Math.min(duration, target);
            controls.seekTo(target);
            lastPackage = pkg;
            lastSeekTarget = target;
            lastSeekAt = before;
            Log.i(TAG, "Standalone seekTo " + target + " from " + position +
                    " dispatchMs=" + (SystemClock.elapsedRealtime() - before));
        } catch (Throwable t) {
            Log.e(TAG, "Standalone control failed", t);
            warn(ctx, "PiP10 failed — see logcat");
        }
    }

    private static void warn(Context ctx, String message) {
        Log.w(TAG, message);
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
    }
}
