# PiP10 LSPosed for Android 16 (experimental)

v0.2.0: Try in-process playback control to fix v0.1.1's dead buttons.
The scoped YouTube/Morphe application now receives its own PiP broadcast and
uses its own MediaSession.getController() rather than requiring notification
access to locate a session in a second application. Icon artwork is larger,
but Android SystemUI still fixes button size / touch target.

**Scope: YouTube or app.revanced.android.youtube only. Do not scope SystemUI.**
After upgrading, force-stop YouTube / Morphe, optionally reboot to ensure old
LSPosed code is unloaded, then test a normal (non-live) video in PiP.

Failures can be diagnosed using logcat filter PiP10 (from the YouTube process).
If "No active in-process MediaSession", the app may host its playback session
in a separate process; a future revision must resolve its own cross-process
session or use NotificationListener with explicit user permission.

Security: listener is registered as RECEIVER_NOT_EXPORTED in the host app.
The prior exported module receiver remains in the APK for backward
compatibility but is no longer used by the new actions.

Built as a debug-signed APK by .github/workflows/pip10-build.yml.
