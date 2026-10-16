param(
    [string]$EngineRoot = "C:\Program Files\Epic Games\UE_5.8",
    [string]$ArchiveDirectory = (Join-Path $PSScriptRoot "artifacts\android"),
    [ValidateSet("Development", "Shipping")]
    [string]$Configuration = "Development"
)

$ErrorActionPreference = "Stop"
$ProjectFile = Join-Path $PSScriptRoot "ZEAZHumandroid\ZEAZHumandroid.uproject"
$RunUAT = Join-Path $EngineRoot "Engine\Build\BatchFiles\RunUAT.bat"
$Editor = Join-Path $EngineRoot "Engine\Binaries\Win64\UnrealEditor.exe"

foreach ($RequiredPath in @($ProjectFile, $RunUAT, $Editor)) {
    if (-not (Test-Path -LiteralPath $RequiredPath -PathType Leaf)) {
        throw "Required Unreal file is missing: $RequiredPath"
    }
}

New-Item -ItemType Directory -Path $ArchiveDirectory -Force | Out-Null
$ProjectArgument = "-project=$ProjectFile"
$ArchiveArgument = "-archivedirectory=$ArchiveDirectory"
& $RunUAT BuildCookRun $ProjectArgument -platform=Android -cookflavor=ASTC `
    "-clientconfig=$Configuration" -build -cook -stage -pak -package -archive `
    $ArchiveArgument -utf8output -nop4

if ($LASTEXITCODE -ne 0) {
    throw "Unreal Android packaging failed with exit code $LASTEXITCODE"
}

$Apks = @(Get-ChildItem -LiteralPath $ArchiveDirectory -Filter *.apk -Recurse -File)
if ($Apks.Count -eq 0) {
    throw "Unreal Automation Tool completed but produced no APK under $ArchiveDirectory"
}

$Apks | Get-FileHash -Algorithm SHA256 | Format-Table -AutoSize
