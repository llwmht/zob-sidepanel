# Helper to drive the tablet's screen over adb.
#   .\ui.ps1                -> lists visible elements (text + centre)
#   .\ui.ps1 -Tap "French"  -> taps the first element whose text contains this word
param([string]$Tap)
$env:Path = [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')
adb shell uiautomator dump /data/local/tmp/ui.xml | Out-Null
[xml]$x = (adb shell cat /data/local/tmp/ui.xml) -join ''
$nodes = $x.SelectNodes('//node') | Where-Object { $_.text -or $_.'content-desc' } | ForEach-Object {
    $b = [regex]::Matches($_.bounds, '\d+') | ForEach-Object { [int]$_.Value }
    [pscustomobject]@{ Text = "$($_.text)$($_.'content-desc')"; Checked = $_.checked; X = [int](($b[0]+$b[2])/2); Y = [int](($b[1]+$b[3])/2) }
}
if (-not $Tap) { $nodes | Format-Table -AutoSize | Out-String -Width 200; return }
$n = $nodes | Where-Object Text -like "*$Tap*" | Select-Object -First 1
if (-not $n) { throw "Not found on screen: $Tap" }
adb shell input tap $n.X $n.Y; "tap '$($n.Text)' ($($n.X),$($n.Y))"
