package dev.pipten.module;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/** Supplies all three controls; never borrows an unrelated YouTube RemoteAction. */
final class PipActions {
    static final String BACK_LABEL = "PiP10 Back 10s";
    static final String FORWARD_LABEL = "PiP10 Forward 10s";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> lastActivity = new WeakReference<>(null);
    private static PictureInPictureParams lastParams;
    private static long lastRefresh;
    private static boolean refreshQueued;

    private PipActions() {}

    static PictureInPictureParams inject(Context host, PictureInPictureParams original) {
        if (host == null || original == null) return original;
        if (!HostPlaybackBridge.ensureReceiver(host)) return original;
        if (host instanceof Activity) {
            lastActivity = new WeakReference<>((Activity) host);
            lastParams = original;
        }
        boolean playing = HostPlaybackBridge.isPlaying();
        RemoteAction back = make(host, -1, R.drawable.ic_back, BACK_LABEL);
        RemoteAction center = make(host, 0,
                playing ? R.drawable.ic_pause : R.drawable.ic_toggle,
                playing ? "PiP10 Pause" : "PiP10 Play");
        RemoteAction forward = make(host, 1, R.drawable.ic_forward, FORWARD_LABEL);
        return new PictureInPictureParams.Builder(original)
                .setActions(Arrays.asList(back, center, forward)).build();
    }

    static void requestRefresh() {
        synchronized (PipActions.class) {
            if (refreshQueued) return;
            refreshQueued = true;
        }
        MAIN.postDelayed(() -> {
            synchronized (PipActions.class) { refreshQueued = false; }
            Activity a = lastActivity.get();
            PictureInPictureParams old = lastParams;
            if (a == null || old == null || a.isFinishing() || a.isDestroyed()
                    || !a.isInPictureInPictureMode()) return;
            try {
                a.setPictureInPictureParams(old);
            } catch (Throwable ignored) {}
        }, 350);
    }

    private static RemoteAction make(Context ctx, int direction, int icon, String label) {
        RemoteAction a = new RemoteAction(Icon.createWithResource(HookEntry.PACKAGE, icon),
                label, label, HostPlaybackBridge.pendingIntent(ctx, direction));
        a.setEnabled(true);
        return a;
    }
}
