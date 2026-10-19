package dev.pipten.module;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runs inside the scoped YouTube process via LSPosed. Unlike v0.1.1, PiP
 * actions and session control never cross into the module application's UID.
 */
final class HostPlaybackBridge {
    static final String ACTION = "dev.pipten.module.PIP_CONTROL";
    static final String EXTRA_DIRECTION = "direction";
    private static final String TAG = "PiP10";
    private static final CopyOnWriteArrayList<WeakReference<MediaSession>> SESSIONS =
        new CopyOnWriteArrayList<>();
    private static volatile BroadcastReceiver receiver;
    private static volatile WeakReference<MediaSession> lastSeekSession =
        new WeakReference<>(null);
    private static volatile long lastSeekPosition = 0;
    private static volatile long lastSeekTime = 0;

    private HostPlaybackBridge() {}

    static void track(MediaSession s) {
        if (s == null) return;
        for (WeakReference<MediaSession> w : SESSIONS) {
            MediaSession old = w.get();
            if (old == s) return;
            if (old == null) SESSIONS.remove(w);
        }
        SESSIONS.add(new WeakReference<>(s));
        if (SESSIONS.size() > 32) SESSIONS.remove(0);
        Log.i(TAG, "Tracked host MediaSession");
    }

    static void released(MediaSession s) {
        for (WeakReference<MediaSession> w : SESSIONS)
            if (w.get() == null || w.get() == s) SESSIONS.remove(w);
    }

    static synchronized boolean ensureReceiver(Context ctx) {
        if (receiver != null) return true;
        Context app = ctx.getApplicationContext();
        if (app == null) app = ctx;
        final Context target = app;
        BroadcastReceiver newReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent intent) {
                if (intent == null || !ACTION.equals(intent.getAction())) return;
                int dir = intent.getIntExtra(EXTRA_DIRECTION, 99);
                if (dir < -1 || dir > 1) return;
                Log.i(TAG, "PiP action clicked: " + dir);
                perform(target, dir);
            }
        };
        try {
            target.registerReceiver(newReceiver, new IntentFilter(ACTION),
                Context.RECEIVER_NOT_EXPORTED);
            receiver = newReceiver;
            Log.i(TAG, "In-process control receiver registered");
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "Cannot register in-process control receiver", e);
            return false;
        }
    }

    static PendingIntent pendingIntent(Context host, int dir) {
        Intent action = new Intent(ACTION).setPackage(host.getPackageName())
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        action.putExtra(EXTRA_DIRECTION, dir);
        return PendingIntent.getBroadcast(host, 0x50315000 + dir, action,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static MediaSession findSession() {
        MediaSession fallback = null;
        for (int i = SESSIONS.size() - 1; i >= 0; i--) {
            MediaSession session = SESSIONS.get(i).get();
            if (session == null) continue;
            try {
                if (!session.isActive()) continue;
                if (fallback == null) fallback = session;
                PlaybackState p = session.getController().getPlaybackState();
                if (p != null && (p.getState() == PlaybackState.STATE_PLAYING ||
                    p.getState() == PlaybackState.STATE_PAUSED ||
                    p.getState() == PlaybackState.STATE_BUFFERING)) {
                    return session;
                }
            } catch (Throwable t) { Log.w(TAG, "Session selection failed", t); }
        }
        return fallback;
    }

    static boolean isPlaying() {
        try {
            MediaSession session = findSession();
            if (session == null) return false;
            PlaybackState state = session.getController().getPlaybackState();
            return state != null && state.getState() == PlaybackState.STATE_PLAYING;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void perform(Context ctx, int dir) {
        try {
            MediaSession session = findSession();
            if (session == null) {
                Log.w(TAG, "No active in-process MediaSession");
                notice(ctx, "PiP10: no active media session");
                return;
            }
            MediaController controller = session.getController();
            MediaController.TransportControls controls = controller.getTransportControls();
            PlaybackState p = controller.getPlaybackState();
            if (p == null) {
                Log.w(TAG, "MediaSession has no PlaybackState");
                notice(ctx, "PiP10: playback state unavailable");
                return;
            }
            if (dir == 0) {
                if (p.getState() == PlaybackState.STATE_PLAYING) controls.pause();
                else controls.play();
                Log.i(TAG, "Sent play/pause to host session");
                return;
            }
            long now = SystemClock.elapsedRealtime();
            long pos = Math.max(0, p.getPosition());
            if (p.getState() == PlaybackState.STATE_PLAYING &&
                p.getLastPositionUpdateTime() > 0) {
                pos += (long)(Math.max(0, Math.min(30000,
                    now - p.getLastPositionUpdateTime())) * p.getPlaybackSpeed());
            }
            if (lastSeekSession.get() == session && now - lastSeekTime < 2500) {
                pos = lastSeekPosition;
                if (p.getState() == PlaybackState.STATE_PLAYING)
                    pos += (long)(Math.max(0, now - lastSeekTime) * p.getPlaybackSpeed());
            }
            MediaMetadata meta = controller.getMetadata();
            long duration = meta == null ? -1 :
                meta.getLong(MediaMetadata.METADATA_KEY_DURATION);
            long dest = Math.max(0, pos + (dir * 10000L));
            if (duration > 0) dest = Math.min(duration, dest);
            lastSeekSession = new WeakReference<>(session);
            lastSeekPosition = dest;
            lastSeekTime = now;
            long dispatchStart = SystemClock.elapsedRealtime();
            controls.seekTo(dest);
            long dispatchMs = SystemClock.elapsedRealtime() - dispatchStart;
            Log.i(TAG, "Host session seekTo " + dest + " from " + pos +
                " dispatchMs=" + dispatchMs +
                " actions=" + p.getActions());
            if ((p.getActions() & PlaybackState.ACTION_SEEK_TO) == 0)
                Log.w(TAG, "Seek requested but MediaSession does not advertise ACTION_SEEK_TO");
        } catch (Throwable t) {
            Log.e(TAG, "In-process PiP action error", t);
            notice(ctx, "PiP10: control failed, see LSPosed log");
        }
    }

    private static void notice(Context ctx, String msg) {
        try { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show(); }
        catch (Throwable t) { Log.w(TAG, msg, t); }
    }
}
