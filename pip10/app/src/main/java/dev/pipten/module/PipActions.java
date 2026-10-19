package dev.pipten.module;

import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Context;
import android.graphics.drawable.Icon;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class PipActions {
    private static final String MARKER = "PiP10 Back 10s";
    private PipActions() {}

    static PictureInPictureParams inject(Context host, PictureInPictureParams original) {
        if (host == null || original == null) return original;
        if (!HostPlaybackBridge.ensureReceiver(host)) return original;
        List<RemoteAction> actions = original.getActions();
        if (actions != null) for (RemoteAction action : actions)
            if (MARKER.contentEquals(action.getTitle())) return original;
        RemoteAction mid = null;
        if (actions != null) for (RemoteAction action : actions) {
            String title = action.getTitle().toString().toLowerCase(Locale.ROOT);
            if (title.contains("play") || title.contains("pause") ||
                title.contains("resume") || title.contains("播放") ||
                title.contains("暫停")) {
                mid = action; break;
            }
        }
        if (mid == null) mid = make(host, 0, R.drawable.ic_toggle, "PiP10 Play or pause");
        return new PictureInPictureParams.Builder(original).setActions(Arrays.asList(
            make(host, -1, R.drawable.ic_back, MARKER),
            mid,
            make(host, 1, R.drawable.ic_forward, "PiP10 Forward 10s")
        )).build();
    }

    private static RemoteAction make(Context ctx, int direction, int icon, String label) {
        RemoteAction a = new RemoteAction(
            Icon.createWithResource(HookEntry.PACKAGE, icon),
            label, label, HostPlaybackBridge.pendingIntent(ctx, direction));
        a.setEnabled(true);
        return a;
    }
}
