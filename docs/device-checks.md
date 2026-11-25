# Device checks

Checks that can only be done on a real device. Reference device: NVIDIA Shield Tablet K1,
Android 7.0, WebView 101. Run the checks of a milestone before closing it, and record the date.

## M0

- [ ] `adb install -r app/build/outputs/apk/debug/app-debug.apk` succeeds.
- [ ] The app opens to a blank screen from its launcher icon.

## M1

Run on 2026-11-25 (Shield K1, Android 7.0, WebView 101): all passed.

1. [x] Launch from the icon: the clock ticks; the device line shows `Chromium 101` and the same battery
   level as `adb shell dumpsys battery`.
2. [x] Brightness slider at minimum and maximum changes the window brightness (`sbrt=0.0` / `1.0` in
   `dumpsys window`); "Auto" removes the override.
3. [x] "Apps" opens the drawer; Settings opens from it; Back returns to the drawer, then to the panel.
4. [x] Back on the panel opens the drawer.
5. [x] `http://127.0.0.1:8770/panel/` in Chrome on the tablet: the clock ticks and "Running outside the
   app" shows (the bridge is not injected outside the app).
6. [x] From the PC, `curl.exe -m 5 http://<tablet-ip>:8770/` does not connect (server is loopback only).
7. [x] `adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME`
   lists `com.android.launcher3/.Launcher` and `io.github.llwmht.sidepanel/.PanelActivity`.
8. [x] Port taken: `adb shell am force-stop io.github.llwmht.sidepanel`, `adb reverse tcp:8770 tcp:8770`,
   launch: "The panel server could not start: Address already in use"; "Open apps" works.
   `adb reverse --remove tcp:8770`, "Try again": the panel loads.
   Note: right after a force-stop, `adb reverse` can fail with "Address already in use" for up to a
   minute while old connections time out; retry.
9. [x] Crash path (debug build): `adb shell am start -n io.github.llwmht.sidepanel/.PanelActivity --ez crash true`;
   the process is gone within about two seconds. Launch again: "The panel did not start properly last
   time, so it was skipped." "Try again": the panel loads and stays.
10. [x] `adb logcat -d | Select-String sidepanel` shows no exception other than the one forced in check 9.

## M1 — home app (Task 8)

Run on 2026-11-25: all passed except where noted.

- [x] Way back rehearsed first: `adb shell cmd package set-home-activity com.android.launcher3/.Launcher`
  sets the stock launcher as home without the chooser (works on Android 7.0). Last resort:
  `adb uninstall io.github.llwmht.sidepanel`.
- [x] `adb shell cmd package set-home-activity io.github.llwmht.sidepanel/.PanelActivity`, then reboot:
  the tablet boots into the panel (behind the swipe lock screen; see below).
  Wait about 15 seconds before rebooting: the setting is written to disk with a delay, and an
  immediate `adb reboot` loses it.
- [x] Crash while default home (debug build: `adb shell am start -n io.github.llwmht.sidepanel/.PanelActivity --ez crash true`,
  then Home): the panel comes back, no "choose a home app" dialog, the app is still the default home.
- [x] `adb shell am force-stop io.github.llwmht.sidepanel`, then Home: the panel comes back.
- [x] Charge limiter still running after reboot (`device/shield-k1/check.sh`).
- [x] Back to stock launcher with the command above; Fully Kiosk re-enabled.

Findings to handle later:
- The swipe lock screen shows over the panel after boot (Fully Kiosk used to dismiss it). Planned for M4.
- A package disabled with `pm disable-user` and enabled again stays in the "stopped" state and gets no
  boot broadcast until it is opened once by hand.
