# PiP10 v0.6.0-alpha — Liquid Glass controls

Compact, iOS-inspired **custom-painted glass controls** for Android PiP.

## v0.6 appearance changes

- Removed the giant full-width bottom overlay from v0.5.
- Added a centered translucent capsule with rounded corners (up to 260dp).
- Added multi-layer transparent glass highlights, lens rims, gradients and shadows.
- Three smaller, precise glass lenses with back 10s / play-pause / forward 10s.
- The touch area now covers only the compact capsule, not the whole PiP width.
- MediaSession +/-10s and play/pause logic unchanged from the WORKING v0.5 build.
- No LSPosed / SystemUI hooks, root or screen capture required.

**About actual blur:** Android Accessibility overlays cannot generally blur
individual underlying app pixels through a Canvas paint operation without
screen capture or privileged system APIs. This build uses a convincing
translucent glass *simulation* rather than claiming true live backdrop blur.

## Updating from v0.5

1. Install APK over v0.5 (same application ID, versionCode increased).
2. Keep accessibility and Notification access enabled.
3. Disable any older PiP10 LSPosed scope if it is still enabled.
4. Open YouTube PiP; the new glass capsule should follow the window.

## Notes

The capsule is always visible while PiP is visible, so it may still overlap
subtitles directly beneath it. PiP controls require a seek-capable MediaSession.
This is a device-untested alpha even if Gradle compilation succeeds.
For diagnostics use `adb logcat -d -s PiP10:I`.

Only the independent pip10-build branch is changed.
