#!/system/bin/sh
# Shield K1 charge limiter: stops charging at 60 %, resumes at 55 %.
# Installed in /data/adb/service.d/: Magisk runs it as root at every boot.
# If the tablet no longer charges: reboot, or as root:
#   echo enabled > /sys/devices/platform/7000c400.i2c/i2c-1/1-006b/charging_state
f=/sys/devices/platform/7000c400.i2c/i2c-1/1-006b/charging_state   # bq2419x charger
b=/sys/class/power_supply/battery/capacity
until [ -e "$f" ]; do sleep 5; done
state=enabled
while true; do
  c=$(cat $b)
  if [ "$c" -ge 60 ]; then state=disabled; elif [ "$c" -le 55 ]; then state=enabled; fi
  # written on every pass: the driver re-enables charging when the cable is replugged
  echo $state > $f
  sleep 60
done
