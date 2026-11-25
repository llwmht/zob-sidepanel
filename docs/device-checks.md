# Device checks

Checks that can only be done on a real device. Reference device: NVIDIA Shield Tablet K1,
Android 7.0, WebView 101. Run the checks of a milestone before closing it, and record the date.

## M0

- [ ] `adb install -r app/build/outputs/apk/debug/app-debug.apk` succeeds.
- [ ] The app opens to a blank screen from its launcher icon.
