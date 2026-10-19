package dev.pipten.module;

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class PipActions {
    private static final String MARKER = "PiP10 Back 10s";
    private PipActions() {}
    static PictureInPictureParams inject(Context host, PictureInPictureParams original) {
        if (host == null || original == null) return original;
        List<RemoteAction> actions = original.getActions();
        for (RemoteAction action : actions)
            if (MARKER.contentEquals(action.getTitle())) return original;
        String pkg = host.getPackageName();
        RemoteAction mid = null;
        for (RemoteAction action : actions) {
            String title = action.getTitle().toString().toLowerCase(Locale.ROOT);
            if (title.contains("play") || title.contains("pause") ||
                title.contains("resume") || title.contains("播放") || title.contains("暫停")) {
                mid = action; break;
            }
        }
        if (mid == null) mid = make(host, pkg, 0,
            R.drawable.ic_toggle, "PiP10 Play or pause");
        return new PictureInPictureParams.Builder(original).setActions(Arrays.asList(
            make(host, pkg, -1, R.drawable.ic_back, MARKER),
            mid,
            make(host, pkg, 1, R.drawable.ic_forward, "PiP10 Forward 10s")
        )).build();
    }
    private static RemoteAction make(Context ctx, String pkg, int direction,
            int icon, String label) {
        Intent intent = new Intent();
        intent.setClassName(HookEntry.PACKAGE, HookEntry.PACKAGE + ".MediaActionReceiver");
        intent.setAction("dev.pipten.ACTION_" + direction);
        intent.putExtra(MediaActionReceiver.EXTRA_PKG, pkg);
        intent.putExtra(MediaActionReceiver.EXTRA_ACTION, direction);
        PendingIntent pending = PendingIntent.getBroadcast(ctx,
            31 * pkg.hashCode() + direction, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteAction action = new RemoteAction(Icon.createWithResource(HookEntry.PACKAGE, icon),
            label, label, pending);
        action.setEnabled(true);
        return action;
    }
}
