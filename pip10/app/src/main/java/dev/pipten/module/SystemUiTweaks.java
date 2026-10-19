package dev.pipten.module;

import android.app.RemoteAction;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * A custom-drawn control layer inside stock PiP action views.
 *
 * No resizing of framework ImageViews or changing ViewGroup children (which
 * would break AOSP's casts to PipMenuActionView during subsequent updates).
 * We draw our own entire 48dp surface on top using View.setForeground().
 * Original PendingIntents/touch targets remain owned by SystemUI.
 *
 * Only acts on PiP10's own 3 actions; other PiP clients are untouched.
 */
final class SystemUiTweaks {
    private static final String TAG = "PiP10";
    private static final Map<View, Drawable> ORIGINAL = new WeakHashMap<>();
    private static boolean installedLogShown, paintedLogShown;
    private static boolean missingFieldsShown;

    private SystemUiTweaks() {}

    static void install(ClassLoader loader) {
        String[] names = {
            "com.android.wm.shell.pip.phone.PipMenuView",
            "com.android.wm.shell.pip2.phone.PipMenuView",
            "com.android.systemui.pip.phone.PipMenuActivity"
        };
        int count = 0;
        for (String name : names) {
            try {
                Class<?> target = XposedHelpers.findClassIfExists(name, loader);
                if (target == null) continue;
                count += XposedBridge.hookAllMethods(target, "updateActionViews",
                        new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        try { paintControls(p.thisObject); }
                        catch (Throwable t) {
                            XposedBridge.log(TAG + " custom controls failed: " + t);
                        }
                    }
                }).size();
                XposedBridge.log(TAG + " found menu class " + name);
            } catch (Throwable t) {
                XposedBridge.log(TAG + " cannot install menu hook " + name + ": " + t);
            }
        }
        XposedBridge.log(TAG + " custom controls installed hooks=" + count);
    }

    @SuppressWarnings("unchecked")
    private static void paintControls(Object menu) {
        Object groupObj;
        Object actionsObj;
        try {
            groupObj = XposedHelpers.getObjectField(menu, "mActionsGroup");
            actionsObj = XposedHelpers.getObjectField(menu, "mActions");
        } catch (Throwable t) {
            if (!missingFieldsShown) {
                missingFieldsShown = true;
                XposedBridge.log(TAG + " PiP menu action fields unavailable: " + t);
            }
            return;
        }
        if (!(groupObj instanceof ViewGroup) || !(actionsObj instanceof List)) return;
        ViewGroup actionsGroup = (ViewGroup) groupObj;
        List<?> actions = (List<?>) actionsObj;
        boolean matching = actions.size() == 3 &&
                isAction(actions.get(0), PipActions.BACK_LABEL) &&
                isAction(actions.get(2), PipActions.FORWARD_LABEL);
        for (int i = 0; i < actionsGroup.getChildCount(); i++) {
            View target = actionsGroup.getChildAt(i);
            if (matching && i < 3 && target.getVisibility() == View.VISIBLE) {
                int kind;
                if (i == 0) kind = CustomPipGlyph.BACK;
                else if (i == 2) kind = CustomPipGlyph.FORWARD;
                else if (actions.get(1) instanceof RemoteAction &&
                        ((RemoteAction) actions.get(1)).getTitle().toString().contains("Pause"))
                    kind = CustomPipGlyph.PAUSE;
                else kind = CustomPipGlyph.PLAY;

                Drawable existing = target.getForeground();
                if (existing instanceof CustomPipGlyph &&
                        ((CustomPipGlyph) existing).controlType() == kind) continue;
                if (!ORIGINAL.containsKey(target))
                    ORIGINAL.put(target, existing);
                target.setForeground(new CustomPipGlyph(kind));
                if (!paintedLogShown) {
                    paintedLogShown = true;
                    XposedBridge.log(TAG + " custom 48dp PiP10 controls painted successfully");
                }
            } else if (ORIGINAL.containsKey(target)) {
                target.setForeground(ORIGINAL.remove(target));
            }
        }
    }

    private static boolean isAction(Object obj, String title) {
        return obj instanceof RemoteAction &&
                title.contentEquals(((RemoteAction) obj).getTitle());
    }
}
