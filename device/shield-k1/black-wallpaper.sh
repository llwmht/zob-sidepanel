#!/system/bin/sh
# Black wallpaper (run as root over adb, then reboot). Expects /data/local/tmp/black.png.
# Android 7 reads wallpaper_orig (source) and wallpaper (cropped copy) at boot.
d=/data/system/users/0
for f in wallpaper_orig wallpaper; do
  cp /data/local/tmp/black.png $d/$f
  chown system:system $d/$f; chmod 600 $d/$f
  chcon u:object_r:wallpaper_file:s0 $d/$f
done
ls -lZ $d | grep wallpaper
