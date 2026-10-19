package dev.pipten.module;

import android.app.Activity;
import android.app.Application;
import android.app.PictureInPictureParams;
import android.app.AndroidAppHelper;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {
    static final String PACKAGE = "dev.pipten.module";
    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
        "com.google.android.youtube", "app.revanced.android.youtube"));

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if ("com.android.systemui".equals(lp.packageName)) {
            SystemUiTweaks.install(lp.classLoader);
            return;
        }
        if (!ALLOWED.contains(lp.packageName)) return;
        try {
            XposedBridge.hookAllConstructors(MediaSession.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    HostPlaybackBridge.track((MediaSession) p.thisObject);
                }
            });
            XposedHelpers.findAndHookMethod(MediaSession.class, "setPlaybackState",
                PlaybackState.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        HostPlaybackBridge.track((MediaSession) p.thisObject);
                        PipActions.requestRefresh();
                    }
                });
            XposedHelpers.findAndHookMethod(MediaSession.class, "setActive",
                boolean.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if ((boolean) p.args[0])
                            HostPlaybackBridge.track((MediaSession) p.thisObject);
                        PipActions.requestRefresh();
                    }
                });
            XposedHelpers.findAndHookMethod(MediaSession.class, "release",
                new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        HostPlaybackBridge.released((MediaSession) p.thisObject);
                    }
                });
        } catch (Throwable t) { XposedBridge.log("PiP10 session hook: " + t); }
        try {
            XposedHelpers.findAndHookMethod(PictureInPictureParams.Builder.class, "build",
                new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if (BUSY.get()) return;
                        Application app = AndroidAppHelper.currentApplication();
                        if (app != null) patchResult(p, app);
                    }
                });
        } catch (Throwable t) { XposedBridge.log("PiP10 builder hook: " + t); }
        try {
            XC_MethodHook hook = new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (BUSY.get() || !(p.args[0] instanceof PictureInPictureParams)) return;
                    try {
                        BUSY.set(true);
                        p.args[0] = PipActions.inject((Activity) p.thisObject,
                            (PictureInPictureParams) p.args[0]);
                    } catch (Throwable t) { XposedBridge.log("PiP10 activity hook: " + t); }
                    finally { BUSY.set(false); }
                }
            };
            XposedHelpers.findAndHookMethod(Activity.class, "setPictureInPictureParams",
                PictureInPictureParams.class, hook);
            XposedHelpers.findAndHookMethod(Activity.class, "enterPictureInPictureMode",
                PictureInPictureParams.class, hook);
        } catch (Throwable t) { XposedBridge.log("PiP10 activity hooks: " + t); }
        XposedBridge.log("PiP10 v0.3.0 loaded: " + lp.packageName);
    }

    private static void patchResult(XC_MethodHook.MethodHookParam p, Application app) {
        try {
            BUSY.set(true);
            PictureInPictureParams original = (PictureInPictureParams) p.getResult();
            if (original != null) p.setResult(PipActions.inject(app, original));
        } catch (Throwable t) { XposedBridge.log("PiP10 build hook: " + t); }
        finally { BUSY.set(false); }
    }
}
