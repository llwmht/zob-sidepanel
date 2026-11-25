#!/system/bin/sh
# Night mode: screen off from 00:00 to 08:00, back on at 08:00. Installed in /data/adb/service.d/.
# Acts only when the period changes: a screen turned back on at night is not turned off again right away.
OFF=0; ON=8
mode=""
while true; do
  h=$(date +%H); h=${h#0}
  if [ "$h" -ge $OFF ] && [ "$h" -lt $ON ]; then want=night; else want=day; fi
  if [ "$want" != "$mode" ]; then
    # first pass after boot: never wake the screen, only turn it off if it is night
    if [ "$want" = night ]; then input keyevent KEYCODE_SLEEP
    elif [ -n "$mode" ]; then input keyevent KEYCODE_WAKEUP; fi
    mode=$want
  fi
  sleep 60
done
