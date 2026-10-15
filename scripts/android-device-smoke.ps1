param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [string]$Package = "dev.zeaz.ultimate.debug",
    [int]$WaitSeconds = 6
)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$android = Join-Path $root "android"
$gradle = Join-Path $android "gradlew.bat"
$apk = Join-Path $android "app/build/outputs/apk/debug/app-debug.apk"
if (-not (Test-Path $gradle)) { throw "Missing Gradle wrapper: $gradle" }
Push-Location $android
try {
    & $gradle ":app:assembleDebug"
    if ($LASTEXITCODE -ne 0) { throw "Android build failed; stale APK will not be installed" }
    if (-not (Test-Path $apk)) { throw "Build did not produce APK" }
    & adb -s $Serial get-state
    if ($LASTEXITCODE -ne 0) { throw "ADB device unavailable" }
    & adb -s $Serial install -r $apk
    if ($LASTEXITCODE -ne 0) { throw "APK installation failed" }
    & adb -s $Serial logcat -c
    if ($LASTEXITCODE -ne 0) { throw "Could not clear crash log" }
    & adb -s $Serial shell am force-stop $Package
    & adb -s $Serial shell am start -W -n "$Package/dev.zeaz.ultimate.MainActivity"
    if ($LASTEXITCODE -ne 0) { throw "Activity launch failed" }
    Start-Sleep -Seconds $WaitSeconds
    $pidText = (& adb -s $Serial shell pidof $Package) -join ""
    if ($LASTEXITCODE -ne 0 -or -not $pidText.Trim()) { throw "App exited after launch" }
    $crashes = (& adb -s $Serial logcat -d -b crash -v time) -join "`n"
    if ($crashes -match [regex]::Escape("Process: $Package")) {
        throw "Fatal Android crash for $Package found in crash buffer"
    }
    Write-Host "PASS: $Package running (PID $($pidText.Trim())); no package crash in cleared crash buffer."
    Write-Host "Manual checks still required: navigation insets, IME, Thai STT/TTS, network failure, offline mode."
} finally {
    Pop-Location
}
