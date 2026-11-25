# zob-sidepanel

Turn an old Android phone or tablet into an always-on widget panel next to your computer.

zob-sidepanel replaces the Android home screen with pages of widgets: a clock, the weather, any web
page, your music. It works on its own with just Wi-Fi and a charger. Later it will also pair with a
small agent on your PC for hardware stats and macro buttons.

**Status: early development.** Nothing is usable yet. The design and roadmap are in
[docs/PLAN.md](docs/PLAN.md).

- Android 7.0 or newer, no root needed (root unlocks extras such as a charge limit).
- Widgets are plain HTML and JavaScript.
- Layouts are edited from a browser on another device.

## Try it (developers)

Needs JDK 17+ and the Android SDK. Build and install the debug APK on a device with USB debugging:

```powershell
.\gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Make it the home app, and go back to the stock launcher:

```powershell
adb shell cmd package set-home-activity io.github.llwmht.sidepanel/.PanelActivity
adb shell cmd package set-home-activity com.android.launcher3/.Launcher
```

The second command uses the stock launcher of the reference device; use your launcher's component
(`adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME`).
On the device: Settings, Apps, gear icon, Home app. Uninstalling the app also restores the previous home.

## Layouts

The panel shows pages of widgets on a 12 x 8 grid, described by a JSON layout (format in
[docs/PLAN.md](docs/PLAN.md), section 5, and the default in `web/panel/default-layout.json`).
Edit it from a browser on the same Wi-Fi: on the device, press Back to open the app drawer, then
**Edit from another device**. Open the address it shows on your computer and enter the code. The editor
shows your pages with the real widgets; drag to move, drag the corner to resize, select a widget to change
its settings, then **Preview on device** or **Save**. Editing turns itself off after 15 idle minutes;
a paired browser stays paired for 30 days. Five wrong codes lock pairing until you reopen the screen.

Plain HTTP is used on the local network, so only edit on a network you trust.

For scripted setups, a layout file can also be pushed to a debug build:

```powershell
.\device\push-layout.ps1 path\to\layout.json
```

Built-in widgets: `clock`, `battery`, `weather` (Open-Meteo, set `latitude`/`longitude`),
`web-page` (any page that allows being framed).

## Credits

Inspired by [phonebox](https://github.com/ersinkoc/phonebox) by Ersin Koç (MIT license), which turns
an old phone into a desk panel driven by a PC agent. zob-sidepanel takes the widget-panel idea and
makes standalone use the default.

## License

[MIT](LICENSE)
