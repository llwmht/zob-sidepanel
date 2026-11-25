# NVIDIA Shield Tablet K1 profile

Scripts used on the reference device: Shield Tablet K1, Android 7.0, rooted with Magisk.
The `.sh` scripts run as root on the tablet; the `.ps1` scripts run on a Windows PC with `adb`.

| Script | Where it runs | What it does |
|---|---|---|
| `charge-limit.sh` | tablet, `/data/adb/service.d/` | Keeps the battery between 55 % and 60 % by toggling the charger. |
| `night.sh` | tablet, `/data/adb/service.d/` | Turns the screen off at 00:00 and back on at 08:00. |
| `check.sh` | tablet, run by hand | Reports whether the limiter runs, the switch state and six current samples. |
| `black-wallpaper.sh` | tablet, run by hand, then reboot | Installs `/data/local/tmp/black.png` as the wallpaper. |
| `shield-dashboard.ps1` | PC | Safe tweaks (animations off, stay awake while charging), lists and disables bloat, `-Restore` undoes it. |
| `ui.ps1` | PC | Lists on-screen elements through `uiautomator`, or taps one by its text. |

Scripts in `service.d` need mode `755`. Magisk starts them at every boot.

## Measured facts

- Charger switch: `/sys/devices/platform/7000c400.i2c/i2c-1/1-006b/charging_state` (bq2419x), values `enabled` / `disabled`.
- `battery/current_now` is negative while charging and positive while discharging.
- The battery `status` keeps reporting `Charging` after the switch is disabled, so read the current, not the status.
- The driver can turn charging back on when the cable is replugged, which is why the limiter rewrites the switch every minute.
- ACC (Advanced Charging Controller) did not work on this device: it misread the current polarity and reset its config on restart.

If the tablet stops charging: reboot it, or run as root
`echo enabled > /sys/devices/platform/7000c400.i2c/i2c-1/1-006b/charging_state`.
