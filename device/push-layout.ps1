# Pushes a layout file into the debug build of zob-sidepanel on the connected device, then restarts the panel.
#   .\device\push-layout.ps1 path\to\layout.json
# Needs a debug build (uses run-as). The layout format is described in docs/PLAN.md, section 5.
param([Parameter(Mandatory)][string]$Path)
$ErrorActionPreference = 'Stop'
$pkg = 'io.github.llwmht.sidepanel'
if (-not (Test-Path $Path)) { throw "Layout file not found: $Path" }
$null = Get-Content $Path -Raw | ConvertFrom-Json   # fail early on invalid JSON

adb push $Path /data/local/tmp/sidepanel-layout.json | Out-Null
adb shell "run-as $pkg mkdir -p files && run-as $pkg cp /data/local/tmp/sidepanel-layout.json files/layout.json && rm /data/local/tmp/sidepanel-layout.json"
if ($LASTEXITCODE -ne 0) { throw "Could not copy the layout into the app (is a debug build installed?)" }
adb shell "am force-stop $pkg; am start -n $pkg/.PanelActivity" | Out-Null
"Layout pushed and panel restarted."
