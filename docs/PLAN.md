# zob-sidepanel — design and development plan

Status: approved by the owner on 2026-11-24.

## 1. Intent

Turn any old Android phone or tablet into an always-on widget panel that sits next to a computer.
The app replaces the Android home screen, so the device boots straight into the panel.

It has two modes, usable at the same time:

- **Standalone**: the device needs only Wi-Fi and a charger. Widgets get their data from the internet
  (a web page, weather, Spotify) or from the device itself (clock, battery).
- **Connected**: a small agent on a PC feeds PC-only data (hardware stats, what is playing) and runs
  macros when a button is tapped.

Why: the owner revived an NVIDIA Shield Tablet K1 (2015, Android 7.0, rooted) as a dashboard for
a personal web dashboard using Fully Kiosk. The features that make it a real desk panel (motion wake, sleep
schedule, JavaScript interface) are paid there, and the existing open-source reference,
[phonebox](https://github.com/ersinkoc/phonebox) (MIT), cannot run without its PC agent. zob-sidepanel
keeps the widget-panel idea and makes standalone the default.

## 2. Goals, non-goals, anti-goals

Goals

- Runs without root on Android 7.0+ (API 24), any screen size, portrait or landscape.
- Is the launcher: starts at boot, survives a crash, and still lets the user open other apps.
- Pages of widgets on a grid, with swipe and automatic rotation.
- Layout is edited from a browser on another device, with live preview. No account, no cloud service.
- Widgets are plain HTML/CSS/JS, so writing a new one needs no Android knowledge.
- Light enough for 2 GB of RAM and a 2014 GPU.

Non-goals

- iOS. A Play Store listing. A hosted service or user accounts.
- Matching phonebox's 130 widgets. Being a general-purpose launcher with folders and icon packs.

Anti-goals (a failure even if it works)

- The panel is useless when no PC is around.
- Anyone on the Wi-Fi can change the layout or run a macro on the PC.
- The panel can send an arbitrary command to the PC.
- A rooted device is required for the core experience.

## 3. Constraints

- Reference device: Shield Tablet K1, Android 7.0, 1920×1200, 2 GB RAM, system WebView = Chrome 101.
  The owner tests on this device over adb; there is no other test hardware.
- Android 7 has no system dark mode and its WebView is whatever Chrome the user has. The app checks
  the WebView version at startup and shows a clear message below the minimum (Chromium 80).
- Widget code targets ES2019 with no build step, so it runs on old WebViews and can be read on GitHub.
- Everything is in English: UI, code, comments, commits, docs.
- Commits are authored by the owner (Thomas Weill). No AI attribution lines in commits or PRs.
- License MIT. phonebox is credited as inspiration in the README; any code taken from it keeps its
  copyright notice.

## 4. Approaches considered

1. **Kotlin shell + web widgets — chosen.** A small native app owns what only Android can do
   (launcher, screen, sensors, local server) and renders widgets written in HTML/JS inside a WebView.
   The browser editor reuses the same widget code for its preview, and the owner's existing web
   dashboard drops in as a widget. Cost: it depends on the device's WebView.
2. **Fully native (Jetpack Compose).** Smoothest and independent of the WebView, but every widget
   needs Android code, the browser editor has to be written separately, and contributions are harder.
3. **Web app inside Fully Kiosk.** Fastest, but no launcher, and night mode and sensors stay behind
   the paid license. It does not meet the request.

The owner picked 1.

## 5. Architecture

```
┌──────────────── Android device ────────────────┐        ┌──── PC (optional) ────┐
│ Shell (Kotlin)                                 │        │ Agent                 │
│  ├─ Launcher activity + full-screen WebView    │  WSS   │  ├─ hardware metrics  │
│  ├─ Local HTTP server (panel, editor, API)  ◄──┼────────┼─►├─ media session     │
│  ├─ JS bridge: screen, brightness, TTS, apps   │        │  └─ macros (by id)    │
│  └─ Services: boot, night schedule, keep-alive │        └───────────────────────┘
│ Web (HTML/JS): panel runtime · editor · widgets│  ◄── browser on LAN opens the editor
└────────────────────────────────────────────────┘
```

**Shell.** One activity registered for `HOME`, hosting a full-screen WebView that loads the panel from
the local server on `127.0.0.1`. A foreground service keeps the server and schedules alive. A JS
bridge object (`sidepanel`) exposes: screen on/off and brightness, text-to-speech, battery state,
installed apps (list and launch), settings, and secret storage.

**Local server.** Serves the panel, the editor and a small JSON API (layout, settings, widget list).
Loopback is always open. LAN access is off until the user turns on "Edit from another device", which
shows a pairing code on the panel; the browser exchanges it for a token. Every LAN request needs that
token.

**Layout model.** One JSON document: `pages[] → widgets[] {id, type, x, y, w, h, settings}` on a
12-column grid, plus global settings (rotation interval, night schedule, theme). Stored in app
storage, versioned with a `schema` number, exportable and importable as a file.

**Widget contract.** A widget is a folder: `manifest.json` (name, default size, settings schema) and
`widget.js`, an ES module exporting `mount(el, ctx)` and returning an `unmount` function. `ctx` gives
the widget its settings, a `fetch` that goes through the shell (so cross-origin APIs work and secrets
never reach widget code), a `subscribe(source, cb)` for live data, and `sidepanel` actions. The editor
builds each widget's settings form from the manifest schema.

**Editor.** A page served by the device: drag, resize, add, remove, configure, reorder pages. Changes
are pushed to the panel over a WebSocket and shown live; "Save" persists them.

**Other apps.** Because the app is the launcher, an app drawer (a built-in page, also reachable by a
gesture) lists installed apps. Without it the user would be locked out of Settings.

**Night and screen care.** A schedule dims to a black clock page at minimum brightness. Where the
device allows it the screen is really turned off: with root, or with the device-admin permission if
the user grants it. A touch wakes it. A burn-in guard shifts the panel a few pixels every few minutes.

**PC agent (connected mode).** A single executable for Windows first. It advertises itself on the LAN
(mDNS); the device pairs with a code shown on the PC and then holds a WebSocket to it. It streams
metrics and the system media session, and runs macros. **Macros are defined on the PC only**; the
device can send a macro id and nothing else. A device can pair with several PCs and switch between
them, so moving the tablet to another desk means pairing once with that PC's agent.
Language: C#/.NET self-contained, because Windows hardware sensors are best covered by
LibreHardwareMonitorLib. This is the builder's call and can be revisited when M6 starts.

**Root extras.** Optional and per device: charge limit and real screen-off. They ship as device
profiles, starting with the Shield K1 (the charger switch and current polarity were measured on the
owner's tablet; the working scripts are in `device/shield-k1/`).

## 6. Decisions made by the owner

- App replaces the launcher; widget panel in the style of phonebox; standalone and connected modes.
- Target: any old Android device, root optional.
- Layout editing: browser editor served by the device.
- PC agent: Windows first, written so Linux and macOS can follow.
- First usable version: standalone base + Spotify. PC stats/macros and alerts/motion come after.
- A widget to follow crypto and stock prices (asked 2026-11-24; placed in M5 by the builder).
- Spotify: owner has Premium, so the widget has full playback controls. Each user supplies their own
  Spotify developer client id.
- Build: local toolchain on the owner's PC for device testing, GitHub Actions for releases.
- Public repo `llwmht/zob-sidepanel`, MIT.

## 7. Left to the builder

- HTTP/WebSocket library for the shell (small footprint matters more than features).
- Exact grid row height and breakpoints for phones versus tablets.
- Visual design of the panel and editor: dark by default, readable from two metres.
- Agent language, as noted above.

## 8. Milestones

Each milestone ends with something the owner can install and check on the tablet.

**M0 — Repository and toolchain**
- Create the repo: `app/` (Android), `web/` (panel, editor, widgets), `device/shield-k1/` (existing
  scripts), `docs/`, README, LICENSE, CONTRIBUTING with the widget contract.
- Install JDK and Android SDK on the owner's PC. Gradle project that builds an empty debug APK.
- GitHub Actions: build and unit tests on every push; signed release APK on a version tag.
- Done when: CI is green and the empty APK installs on the tablet.

**M1 — Shell**
- Launcher activity, full-screen WebView, start at boot, restart after a crash.
- Local server on loopback serving a placeholder panel. WebView version check.
- JS bridge v1: screen brightness, keep-awake, battery, list and launch apps.
- App drawer page and the gesture to reach it.
- Done when: the tablet boots into the placeholder panel and Settings can still be opened.

**M2 — Panel runtime and first widgets**
- Layout model with schema version, load/save, export/import.
- Grid renderer, pages, swipe, automatic rotation with a per-page duration.
- Widget loader and `ctx` (settings, shell `fetch`, `subscribe`).
- Widgets: Clock, Web page (shows the owner's web dashboard), Weather (Open-Meteo, no key), Battery.
- Done when: a hand-written layout file shows the owner's dashboard, a clock page and a weather page
  rotating on the tablet.

**M3 — Browser editor**
- LAN access toggle, pairing code, token check on every request.
- Editor: add, move, resize, configure (forms from manifests), pages, live preview, save.
- Done when: the owner rebuilds the M2 layout from their PC's browser without touching a file.

**M4 — Night mode, screen care, first-run setup**
- Night schedule (black clock, minimum brightness, real screen-off where allowed), touch to wake.
- Burn-in guard. Settings page. First-run flow: set as home app, pick a starter template.
- Done when: the tablet runs 48 hours unattended, dims at night and is back in the morning.
- **Release v0.1.**

**M5 — Spotify and markets**
- Authorization with PKCE on the device (loopback redirect), token kept in shell secret storage.
- Now-playing widget: cover, title, progress, play/pause, next, previous.
- Markets widget: a watchlist of crypto pairs and stock tickers with price, daily change and a
  sparkline. Crypto prices come from a public exchange API that needs no key. Free stock quotes
  generally need an API key, so the user supplies their own; the provider is chosen when M5 starts,
  after checking current terms and limits.
- Done when: the owner controls Spotify from the tablet with the PC switched off, and a watchlist
  with at least one crypto pair and one stock updates on its own.
- **Release v1.0** (standalone base + Spotify + markets).

**M6 — PC agent and connected mode**
- Agent for Windows: tray icon, pairing, mDNS, WebSocket, settings page for macros.
- Widgets: CPU/GPU/RAM/temperature tiles and chart, Now playing (any PC player), Macro buttons,
  Computers (switch between paired PCs).
- Done when: stats update live, a macro button launches a program, and unplugging the PC leaves the
  standalone widgets working.

**M7 — Alerts and presence**
- Local push endpoint (`/api/notify`) so the owner's own services can make the panel speak or flash.
- Text-to-speech and notification widget. Motion wake using the front camera.
- Done when: an event from the owner's server is announced out loud and the screen wakes when someone walks up at night.

**M8 — Root extras and device profiles**
- Charge-limit and screen-off as optional root features; Shield K1 profile from `device/shield-k1/`.
- Document how to contribute a profile for another device.

Later, not planned: Linux and macOS agents, more widgets (calendar, RSS, Home Assistant, crypto
prices), community widget folder.

## 9. Testing

- `web/`: unit tests for the layout model and widget loader, run with Node's built-in test runner.
- `app/`: unit tests for the server's token check and the layout storage.
- Each milestone has a short manual checklist run by the owner on the Shield K1 (`docs/device-checks.md`).
- Nothing can be tested on the tablet from CI or from a cloud session; device results come from the owner.

## 10. Risks

- **Old WebViews.** A device stuck below Chromium 80 cannot run the panel. Mitigation: version check
  with a plain explanation; keep widget code conservative.
- **Embedding web pages.** Sites that forbid framing will not show in the Web page widget. The owner's
  dashboard allows it (checked 2026-11-24). A native overlay WebView is the fallback if this matters.
- **Spotify on a public app.** Spotify's developer mode limits who can use one client id, so each user
  brings their own. The loopback redirect on the device has not been tried yet; verify first in M5.
- **Launcher lock-out.** A crash loop in a home app can make a device hard to use. Mitigation: the app
  drawer is native, not a widget, and a safe mode skips the web layer when the previous start never
  became healthy. On Android 7 a home app reported as crashed can lose its "default home" status, so
  the app ends its own process instead of letting the system report the crash.
- **Screen-off without root** depends on a permission the user must grant; otherwise night mode only dims.
