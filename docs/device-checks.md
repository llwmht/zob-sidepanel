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

## M2

Run on 2026-11-25 (Shield K1, Android 7.0, WebView 101) with the owner's layout pushed by
`device/push-layout.ps1`: page 1 = web-page widget with the owner's dashboard (120 s), page 2 =
clock + weather + battery (30 s).

1. [x] Page 1 shows the dashboard with live data inside the frame.
   It keeps its own settings (theme, mode) apart from Fully Kiosk's; change them by tapping it.
2. [x] After 120 s, page 2: clock ticking, weather with real values, battery equal to `dumpsys battery`.
3. [x] After 30 s, back to page 1; the dashboard was not reloaded (it kept refreshing while hidden).
4. [x] Swipe from the right edge goes to the next page.
5. [x] Panel started with Wi-Fi off: "Weather unavailable" and the WebView error page in the frame.
   Wi-Fi back on: weather recovers within the 2-minute retry, the dashboard frame reloads by itself.
6. [x] Layout `{"schema":1,"pages":[]}` pushed: default layout plus the banner
   "Layout ignored: layout has no pages. Showing the default layout."
7. [x] Back opens the drawer; no exception from the app in logcat.
8. [ ] Memory after 30 minutes of rotation: see the M2 pull request.
9. [x] Fully Kiosk disabled, panel set as home, reboot: the tablet boots into the panel showing the dashboard
   (after the swipe lock screen, until M4). Charge limiter still running.

Found on the device and fixed in M2:
- Android 7.0 does not trust ISRG Root X1, so Let's Encrypt sites (the dashboard, Open-Meteo) failed with
  `net_error -202`. The app now adds ISRG Root X1 to its own trust anchors (network security config).
- WebView does not follow connectivity on its own: the app now calls `setNetworkAvailable` from a
  network callback, so the `online` event fires and widgets can recover.

## M3

Run on 2026-11-26 (Shield K1, Android 7.0) with a browser on a PC on the same Wi-Fi.

1. [x] With LAN editing off, the PC cannot connect to `http://<tablet-ip>:8770/`.
2. [x] Drawer, "Edit from another device": the screen shows the address and a 6-digit code.
3. [x] From the PC (curl):
   - `GET /api/layout` without a session → 401; `GET /editor/` → 302 to `/editor/pair.html`; the pairing page → 200.
   - `POST /api/pair` without `X-Sidepanel: 1` → 403.
   - Five wrong codes → the code on the device changes; the old code then → 401.
   - Right code → 200 with a session cookie; `GET /api/layout` with it → 200.
   - Same cookie with `Host: evil.example:8770` → 403; `PUT /api/preview` without `X-Sidepanel` → 403;
     `PUT /api/layout` with `{"schema":2}` → 400.
4. [x] In a browser: the pairing page, then the editor with the stored layout and the real widgets (including the
   framed dashboard) at the device's aspect ratio.
5. [x] Resize (grip) and move (drag) snap to the grid; selecting a widget shows its settings from the manifest.
6. [x] "Preview on device": the device shows the edited page; `layout.json` is unchanged. "Discard preview": back to the saved layout.
7. [x] A new empty page is refused in the browser ("page … has no widgets") before any request.
8. [x] Adding a widget and "Save": `layout.json` on the device contains the change.
9. [ ] LAN editing turns itself off after 15 idle minutes: see the M3 pull request.

Notes:
- While the app is being updated (`adb install -r`), Android shows the stock launcher until Home is pressed.
- The night script turns the screen off from 00:00 to 08:00, which also applies while testing.
