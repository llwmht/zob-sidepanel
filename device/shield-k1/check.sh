#!/system/bin/sh
# Checks the charge limiter (run as root over adb).
# Uses /proc instead of pgrep -f, and [x] in the patterns: otherwise the command finds itself.
running() { cat /proc/[0-9]*/cmdline 2>/dev/null | tr '\0' ' ' | grep -q "$1"; }
running 'service.d/charge-limi[t]' && echo "limiter running" || echo "limiter STOPPED"
running 'vr25/ac[c]' && echo "ACC still present" || echo "ACC absent"
echo "switch: $(cat /sys/devices/platform/7000c400.i2c/i2c-1/1-006b/charging_state)"
# current in µA, negative = charging; 6 samples over 30 s to smooth out spikes
i=0; while [ $i -lt 6 ]; do
  echo "battery: $(cat /sys/class/power_supply/battery/capacity)% current $(cat /sys/class/power_supply/battery/current_now)"
  i=$((i+1)); [ $i -lt 6 ] && sleep 5
done
