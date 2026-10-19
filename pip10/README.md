# PiP10 — v0.5.0-alpha: fully standalone large PiP controls

This version replaces unsuccessful attempts at resizing hidden AOSP SystemUI
PiP glyphs with a **genuine custom Android View** on top of the bottom of the
PiP window. The 3 large dark circular buttons are Canvas rendered and are
not limited by SystemUI's 20dp icons.

**No LSPosed, Magisk, root, or modified YouTube is required.** Disable the old
LSPosed module/scope after updating. The old Xposed entrypoint asset was removed.

## Setup
1. Install APK.
2. Open PiP 10s Controls app.
3. Tap 'Enable accessibility overlay' and switch on PiP10 floating controls.
4. Tap 'Enable notification access for media control' and switch on PiP10.
5. Open YouTube (including Morphe) and enter normal video PiP.
6. Custom buttons appear inside the bottom section of PiP and follow it.

Accessibility reads window bounds with `isInPictureInPictureMode()`; notification
access uses `MediaSessionManager.getActiveSessions()` to control playback.
They require explicit user consent. No notifications are logged or transmitted.

## Limitations / troubleshooting
- The controls currently stay visible whenever a PiP window is present.
- They cover the lower ~56–74dp of the PiP and can overlap subtitles.
- Original PiP menu may show behind the custom controls.
- Works only where Android exposes PiP windows to Accessibility.
- MediaSession needs to support `ACTION_SEEK_TO`; player buffering/decoder delays
  are outside our control.
- Android may restrict accessibility permission to sideloaded apps; you may need
  App Info → three dots → Allow restricted settings before enabling it.
- Debug using `adb logcat -d -s PiP10:I`.
- If this build does not display overlays on Pixel Android 16, inspect PiP
  accessibility window output and adapt to actual device implementation.

To stop the overlay: disable PiP10 Accessibility service in Android Settings.
No framework components or system partitions are changed.
