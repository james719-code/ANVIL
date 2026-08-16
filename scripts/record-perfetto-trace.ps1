# ANVIL Perfetto Trace Recorder (PowerShell)
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$tracesDir = Join-Path $projectRoot "traces"

if (-not (Test-Path $tracesDir)) {
    New-Item -ItemType Directory -Path $tracesDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMddTHHmmss"
$traceFile = Join-Path $tracesDir "anvil-perfetto-$timestamp.trace"
$configFile = Join-Path $scriptDir "perfetto_config.pbtx"

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "         ANVIL Perfetto Trace Recorder (PowerShell)    " -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

Write-Host "[1/4] Checking ADB devices..." -ForegroundColor Yellow
adb devices

Write-Host "[2/4] Pushing Perfetto configuration to device..." -ForegroundColor Yellow
adb push "$configFile" /data/local/tmp/perfetto_config.pbtx

Write-Host "[3/4] Recording 10-second Perfetto trace..." -ForegroundColor Green
Write-Host ">>> Perform actions in ANVIL now (scrolling, adding tasks, resetting)... <<<" -ForegroundColor Green
adb shell "perfetto -c /data/local/tmp/perfetto_config.pbtx -o /data/misc/perfetto-traces/trace.perfetto-trace"

Write-Host "[4/4] Pulling trace file to $traceFile..." -ForegroundColor Yellow
adb pull /data/misc/perfetto-traces/trace.perfetto-trace "$traceFile"
adb shell "rm /data/local/tmp/perfetto_config.pbtx /data/misc/perfetto-traces/trace.perfetto-trace"

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "Trace saved to: $traceFile" -ForegroundColor Green
Write-Host "To view visually: Open https://ui.perfetto.dev/ and load the trace." -ForegroundColor Cyan
Write-Host "To analyze via SQL: python $scriptDir\analyze_trace.py `"$traceFile`"" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
