package dev.pipten.module;

import android.app.RemoteAction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Optional SystemUI-only hook. Enlarges ONLY the artwork within PiP10's
 * own action views from the AOSP default 20dp to 36dp. Tap targets stay 48dp.
 * If the ROM uses different class names, this hook quietly does nothing.
 */
final class SystemUiTweaks {
    private static final String TAG = "PiP10";
    private static final Map<ImageView, int[]> ORIGINAL = new WeakHashMap<>();
    private static boolean resizeLogged = false;

    private SystemUiTweaks() {}

    static void install(ClassLoader loader) {
        String[] candidates = {
            "com.android.wm.shell.pip.phone.PipMenuView",
            "com.android.wm.shell.pip2.phone.PipMenuView"
        };
        int installed = 0;
        for (String name : candidates) {
            try {
                Class<?> clazz = XposedHelpers.findClassIfExists(name, loader);
                if (clazz == null) continue;
                installed += XposedBridge.hookAllMethods(clazz, "updateActionViews",
                    new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam p) {
                            try { adjust(p.thisObject); }
                            catch (Throwable t) {
                                XposedBridge.log(TAG + " optional UI resize failed: " + t);
                            }
                        }
                    }).size();
            } catch (Throwable t) {
                XposedBridge.log(TAG + " cannot hook " + name + ": " + t);
            }
        }
        XposedBridge.log(TAG + " optional SystemUI hooks installed=" + installed);
    }

    private static void adjust(Object pipMenu) {
        Object group = XposedHelpers.getObjectField(pipMenu, "mActionsGroup");
        Object actions = XposedHelpers.getObjectField(pipMenu, "mActions");
        if (!(group instanceof ViewGroup)) return;
        ViewGroup row = (ViewGroup) group;
        boolean ours = false;
        if (actions instanceof List) {
            for (Object item : (List<?>) actions) {
                if (item instanceof RemoteAction &&
                    PipActions.BACK_LABEL.contentEquals(((RemoteAction) item).getTitle())) {
                    ours = true; break;
                }
            }
        }
        for (int i = 0; i < row.getChildCount(); i++) {
            View v = row.getChildAt(i);
            ImageView icon = findIcon(v);
            if (icon == null) continue;
            int[] saved = ORIGINAL.get(icon);
            ViewGroup.LayoutParams lp = icon.getLayoutParams();
            if (lp == null) continue;
            if (ours && i < 3 && v.getVisibility() == View.VISIBLE) {
                if (saved == null) {
                    ORIGINAL.put(icon, new int[] {lp.width, lp.height});
                }
                int pixels = Math.round(icon.getResources().getDisplayMetrics().density * 36f);
                if (lp.width != pixels || lp.height != pixels) {
                    lp.width = pixels;
                    lp.height = pixels;
                    icon.setLayoutParams(lp);
                    if (!resizeLogged) {
                        resizeLogged = true;
                        XposedBridge.log(TAG + " resized PiP10 icon to 36dp; touch target unchanged");
                    }
                }
            } else if (saved != null) {
                if (lp.width != saved[0] || lp.height != saved[1]) {
                    lp.width = saved[0];
                    lp.height = saved[1];
                    icon.setLayoutParams(lp);
                }
                ORIGINAL.remove(icon);
            }
        }
    }

    private static ImageView findIcon(View view) {
        if (view == null) return null;
        // On AOSP PipMenuActionView is a FrameLayout with TWO ImageViews:
        // mCustomCloseBackground (first, often GONE) and mImageView.
        // v0.3 erroneously enlarged the first ImageView, not the actual glyph.
        try {
            Object actual = XposedHelpers.getObjectField(view, "mImageView");
            if (actual instanceof ImageView) return (ImageView) actual;
        } catch (Throwable ignored) {}
        if (view instanceof ImageView) return (ImageView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            ImageView found = findIcon(child);
            if (found != null) return found;
        }
        return null;
    }
}
