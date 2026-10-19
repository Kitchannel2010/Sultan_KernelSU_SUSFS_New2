# PiP10 LSPosed Android 16 (experimental)

Branch pip10-build is isolated from kernel development; only pip10/ and a dedicated .github workflow are added.

Build through GitHub Actions: Build PiP10 APK. Expected artifact app-debug.apk.

Install APK, enable module LSPosed with YouTube scope, restart YouTube, grant Notification listener access from module app. PiP controls will offer -10 / play / +10 depending on app and SystemUI behavior.

Known limitations: Not device-tested; actions may be overridden by video app or SystemUI; Requires MediaSession ACTION_SEEK_TO and active media notification. This prototype receiver is exported for host-app PendingIntents; don't use in sensitive environments. No changes to system partition.

Source lives in pip10/app. Debug-only build.
