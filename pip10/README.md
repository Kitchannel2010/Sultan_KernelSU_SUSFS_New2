# PiP10 v0.7.1-alpha — iOS-style Frosted Media Card

Designed from the reference image: a **single dark smoky translucent rounded
rectangle** with clean white controls, not separate shiny circles or a bright
silver capsule.

## Differences vs v0.6
- One softly tinted charcoal gray frosted glass panel.
- Three large unframed white icons, evenly spaced across one shared surface.
- No individual circular borders, metallic gradients, blue highlights, glare
  or inset rings.
- A thin low-opacity outline, soft shadow and subtle vertical tint.
- Keep already working v0.5/v0.6 MediaBridge and ±10-second behavior.
- Modern rounded rectangle instead of exaggerated pill radius.

## Glass blur limitations

The floating Accessibility overlay draws a translucent panel. Real iOS
background blur is not available via ordinary View Canvas. Android's
`FLAG_BLUR_BEHIND` blurs the screen behind a window rather than being
clipped to our desired rounded rectangle, so it is intentionally not enabled.
No screen capture, privileged APIs or fake claims of native blur.

## Install
Install over v0.6, keep Accessibility and Notification permissions enabled,
disable any old LSPosed PiP10 module. The panel follows the PiP bounds.

Source: independent `pip10-build` branch. Android 16 Pixel 8 Pro runtime
behavior and aesthetics still require on-device confirmation.


v0.7.1: lowered shadow base opacity so video texture remains subtly visible through the charcoal glass panel.
