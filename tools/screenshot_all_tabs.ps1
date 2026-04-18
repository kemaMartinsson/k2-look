param(
    [switch]$Build,
    [string]$Device = "",
    [string]$OutputDir = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ProjectRoot

if (-not $OutputDir) {
    $OutputDir = Join-Path $ProjectRoot "docs\images"
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
Write-Host "Screenshots -> $OutputDir" -ForegroundColor Cyan

function Invoke-Adb {
    param([string[]]$AdbArgs)
    $all = if ($Device) { @("-s", $Device) + $AdbArgs } else { $AdbArgs }
    & adb @all
}

$devList = (& adb devices) -join "`n"
if ($devList -notmatch "(?m)^\S+\s+device$") {
    Write-Error "No ADB device found. Connect a device or start an emulator."
    exit 1
}
Write-Host "Device connected: OK" -ForegroundColor Green

if ($Build) {
    Write-Host "Building and installing..." -ForegroundColor Yellow
    & .\gradlew.bat installDebug -x test -x testDebugUnitTest -x lintDebug
    if ($LASTEXITCODE -ne 0) { Write-Error "Build failed"; exit 1 }
    Write-Host "Build complete" -ForegroundColor Green
}

$Package  = "com.kema.k2look"
$Activity = "$Package.MainActivity"

# Tab bounds from uiautomator dump (480x800 portrait):
#   Status   [0,124][240,209]  -> center 120,166
#   Fields   [240,124][480,209] -> center 360,166
#   Gestures [0,209][240,299]  -> center 120,254
#   About    [240,209][480,299] -> center 360,254
$TabTapCoords = @{
    0 = @(120, 166)
    1 = @(360, 166)
    2 = @(120, 254)
    3 = @(360, 254)
}
$TabNames = @("Status", "Fields", "Gestures", "About")

Write-Host "Launching K2Look..." -ForegroundColor Cyan
Invoke-Adb @("shell", "am", "start", "-n", "$Package/.MainActivity") | Out-Null
Start-Sleep -Seconds 3

function Take-Screenshot([string]$Filename) {
    $remote = "/sdcard/k2look_ss.png"
    Invoke-Adb @("shell", "screencap", "-p", $remote) | Out-Null
    $local = Join-Path $OutputDir $Filename
    Invoke-Adb @("pull", $remote, $local) | Out-Null
    Invoke-Adb @("shell", "rm", $remote) | Out-Null
    Write-Host "  Saved: $Filename" -ForegroundColor White
}

foreach ($i in 0..3) {
    $name = $TabNames[$i]
    $x    = $TabTapCoords[$i][0]
    $y    = $TabTapCoords[$i][1]
    Write-Host "Tab $i - $name  (tap $x,$y)" -ForegroundColor Magenta
    Invoke-Adb @("shell", "input", "tap", "$x", "$y") | Out-Null
    Start-Sleep -Milliseconds 800
    Take-Screenshot -Filename "tab${i}_${name}.png"

    # After Fields tab: also tap the Active Profile cog icon (bounds [338,344][428,434])
    if ($i -eq 1) {
        Write-Host "  -> Tapping Active Profile cog (383,389)" -ForegroundColor Magenta
        Invoke-Adb @("shell", "input", "tap", "383", "389") | Out-Null
        Start-Sleep -Milliseconds 1000
        Take-Screenshot -Filename "tab1_Fields_ActiveProfile.png"
        # Press Back to dismiss and return to Fields tab
        Invoke-Adb @("shell", "input", "keyevent", "4") | Out-Null
        Start-Sleep -Milliseconds 600
    }
}

Write-Host ""
Write-Host "All screenshots captured: $OutputDir" -ForegroundColor Green
Start-Process explorer.exe $OutputDir

Pop-Location

