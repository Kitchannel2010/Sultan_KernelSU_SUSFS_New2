package dev.pipten.module;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

/**
 * A narrow transparent accessibility window that follows Android PiP.
 * The single-piece dark frosted card is painted by ControlStrip.
 */
public final class OverlayAccessibilityService extends AccessibilityService {
    private static final String TAG = "PiP10";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager manager;
    private WindowManager.LayoutParams params;
    private ControlStrip controls;
    private boolean attached;
    private final Rect previous = new Rect();

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            try { updateOverlay(); }
            catch (Throwable t) { Log.e(TAG, "Overlay update failed", t); }
            handler.postDelayed(this, 450);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo inf = getServiceInfo();
        if (inf != null) {
            inf.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            inf.eventTypes |= AccessibilityEvent.TYPE_WINDOWS_CHANGED |
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
            inf.notificationTimeout = 100;
            setServiceInfo(inf);
        }
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        controls = new ControlStrip(this);
        params = new WindowManager.LayoutParams(
                1, 1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.setTitle("PiP10 Frosted Media Controls");
        handler.post(poll);
        Log.i(TAG, "PiP10 frosted overlay connected");
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e != null && (e.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))
            handler.post(this::updateOverlay);
    }
    @Override public void onInterrupt() {}

    private void updateOverlay() {
        if (manager == null || controls == null) return;
        Rect pip = findPipWindow();
        if (pip == null || pip.width() < dp(150) || pip.height() < dp(80)) {
            removeOverlay();
            return;
        }
        // Compact card (not a full-width video scrim).
        int width = Math.min(dp(285), pip.width() - dp(24));
        int height = Math.min(dp(75), Math.max(dp(58), pip.height() / 3));
        int x = pip.left + (pip.width() - width) / 2;
        int y = pip.bottom - height - dp(9);
        controls.updatePlayState(MediaBridge.isPlaying(this));

        if (attached && previous.left == x && previous.top == y &&
                previous.right == width && previous.bottom == height) return;
        params.x = x;
        params.y = y;
        params.width = width;
        params.height = height;
        try {
            if (attached) manager.updateViewLayout(controls, params);
            else {
                manager.addView(controls, params);
                attached = true;
                Log.i(TAG, "Flat glass controls attached " + pip.toShortString());
            }
            previous.set(x, y, width, height);
        } catch (Throwable t) {
            Log.e(TAG, "Overlay update failed", t);
            removeOverlay();
        }
    }

    private Rect findPipWindow() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return null;
        Rect selected = null;
        for (AccessibilityWindowInfo w : windows) {
            if (w == null || !w.isInPictureInPictureMode()) continue;
            Rect bounds = new Rect();
            w.getBoundsInScreen(bounds);
            if (!bounds.isEmpty() &&
                    (selected == null || bounds.width() > selected.width()))
                selected = bounds;
        }
        return selected;
    }

    private int dp(float value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void removeOverlay() {
        if (!attached) return;
        try { manager.removeViewImmediate(controls); }
        catch (Throwable t) { Log.w(TAG, "Remove overlay failed", t); }
        attached = false;
        previous.setEmpty();
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        removeOverlay();
        super.onDestroy();
    }
}
