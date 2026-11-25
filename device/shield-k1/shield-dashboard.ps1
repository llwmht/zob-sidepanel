# Tunes a Shield K1 (or another Android tablet) for dashboard use over ADB.
#   .\shield-dashboard.ps1                      -> safe tweaks + list of apps that can be disabled
#   .\shield-dashboard.ps1 -Port 8080           -> + the tablet reaches the PC's port 8080 over the cable (http://localhost:8080)
#   .\shield-dashboard.ps1 -Disable a.b,c.d     -> disables these packages (reversible)
#   .\shield-dashboard.ps1 -Restore             -> re-enables everything this script disabled
param([int]$Port, [string[]]$Disable, [switch]$Restore)
$ErrorActionPreference = 'Stop'
$restoreFile = Join-Path $PSScriptRoot 'disabled-packages.txt'

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { throw "adb not found: winget install Google.PlatformTools, then reopen the terminal." }
$state = (adb get-state 2>&1) -join ''
if ($state -ne 'device') {
    throw "Tablet not ready ($state). Enable USB debugging, plug it in and accept the RSA key shown on the tablet."
}
function Sh([string]$cmd) { (adb shell $cmd) -join "`n" }

if ($Restore) {
    if (-not (Test-Path $restoreFile)) { "Nothing to restore."; return }
    Get-Content $restoreFile | Where-Object { $_ } | ForEach-Object { "enable $_ : " + (Sh "pm enable $_") }
    Remove-Item $restoreFile; return
}

# Packages never to touch: web engine, Play Services, keyboard, active launcher.
$webview  = Sh "settings get global webview_provider"
$launcher = (Sh "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" -split "`n")[-1] -replace '/.*'
$keyboard = (Sh "settings get secure default_input_method") -replace '/.*'
$protected = @($webview, $launcher, $keyboard, 'com.google.android.gms', 'com.android.chrome', 'com.google.android.webview',
               'com.android.webview', 'com.android.vending', 'com.google.android.gsf', 'android', 'com.android.systemui') | Where-Object { $_ -and $_ -ne 'null' }

if ($Disable) {
    foreach ($p in $Disable) {
        # 'launcher' as a safety net: resolve-activity may be missing on Android 7
        if ($protected -contains $p -or $p -match 'launcher|inputmethod|keyboard') { "SKIP $p (protected)"; continue }
        $out = Sh "pm disable-user --user 0 $p"
        "$p : $out"
        if ($out -match 'disabled') { Add-Content $restoreFile $p }
    }
    return
}

# Safe, low-risk tweaks.
Sh "settings put global window_animation_scale 0" | Out-Null
Sh "settings put global transition_animation_scale 0" | Out-Null
Sh "settings put global animator_duration_scale 0" | Out-Null
Sh "settings put global stay_on_while_plugged_in 7" | Out-Null   # screen stays on with AC/USB/wireless power
"OK: animations off, screen stays on while charging."

if ($Port) { adb reverse "tcp:$Port" "tcp:$Port" | Out-Null; "OK: http://localhost:$Port on the tablet = the PC's port $Port (run again after each unplug/reboot)." }

"`nAndroid $(Sh 'getprop ro.build.version.release') | WebView: $webview | Launcher: $launcher | Keyboard: $keyboard"
"`nEnabled apps that could be disabled (nvidia / google / third-party), protected ones excluded:"
$enabled = (Sh "pm list packages -e") -split "`n" | ForEach-Object { ($_ -replace '^package:').Trim() }
$third   = (Sh "pm list packages -e -3") -split "`n" | ForEach-Object { ($_ -replace '^package:').Trim() }
$enabled | Where-Object { ($_ -match 'nvidia|google' -or $third -contains $_) -and $protected -notcontains $_ } | Sort-Object
