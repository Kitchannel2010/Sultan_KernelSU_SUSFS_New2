# PiP10 LSPosed v0.3.0-alpha

**Corrected the three PiP controls:** -10s, Play/Pause (no borrowed headphones/media routing action), +10s.
Refreshes icons after host MediaSession playback-state updates.

**Optional SystemUI artwork enlargement:** enable `com.android.systemui` in LSPosed scope and reboot. The module attempts to find AOSP `PipMenuView.updateActionViews`, enlarging PiP10 artwork from the default 20dp to 32dp, while leaving hitboxes unchanged. Not enabled until explicitly selected. If a ROM does not contain the target class, nothing is enlarged. This is an experimental system process hook; if SystemUI becomes unstable, disable its LSPosed scope.

No Notification Access permission is necessary (removed the v0.1 cross-app approach).
Required app scope: `com.google.android.youtube` or `app.revanced.android.youtube`.
Install APK over v0.2, force stop YouTube and reboot before testing.

To diagnose controls: `adb logcat -d -s PiP10:I`.
- "In-process control receiver registered": buttons have a receiver.
- "PiP action clicked": PendingIntent was delivered.
- "No active in-process MediaSession": YouTube playback requires a different session control path.

Only the independent pip10-build branch in the user's kernel repository is modified. This artifact is a debug-signed, device-untested alpha.
