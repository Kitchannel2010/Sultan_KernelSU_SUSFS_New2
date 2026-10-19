# PiP10 v0.4.0-alpha — custom-painted PiP controls

This version **renders the control appearance from scratch**, rather than
asking Android's built-in ImageView to display oversized vector icons.

- Left: translucent 48dp circle, counterclockwise 10-second arrow.
- Middle: 48dp play/pause with the host playback state.
- Right: clockwise 10-second arrow.
- Uses the *existing* SystemUI action view & already-functional PendingIntents,
  preserving native window management and touch input.
- Does **not** replace the video content, which is rendered by Android PiP.
- Without the SystemUI hook, normal native-sized controls remain available.

## Required LSPosed scope
Tick BOTH:
- `com.google.android.youtube` or `app.revanced.android.youtube`
- `com.android.systemui`
Then reboot. Install over v0.3.1 (same package name and debug signing for builds
from same project; if signatures differ Android may require uninstall).

## How to check the hook
```shell
adb logcat -d | grep -i 'PiP10'
```
In LSPosed logs, look for:
- `custom controls installed hooks=1` (or more): found SystemUI class
- `custom 48dp PiP10 controls painted successfully`: actually displayed
If `hooks=0`, the Pixel's ROM uses a different PiP menu implementation;
do not assume the enlarged appearance works.

## Safety
This is a device-untested debug APK. Injecting SystemUI can affect its stability.
If it crashes, disable SystemUI scope from LSPosed or boot into safe mode. It
never flashes system partitions or touches the original kernel branches.

PiP ±10-second seek still uses the v0.3.1 in-process MediaSession bridge.
Look for `dispatchMs=` in logcat to understand command dispatch speed.
