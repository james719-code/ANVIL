@echo off
setlocal enabledelayedexpansion

echo =======================================================
echo          ANVIL Perfetto Trace Recorder (Windows)
echo =======================================================

for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set TIMESTAMP=%datetime:~0,4%%datetime:~4,2%%datetime:~6,2%T%datetime:~8,2%%datetime:~10,2%%datetime:~12,2%

set OUTPUT_DIR=..\traces
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

set TRACE_FILE=%OUTPUT_DIR%\anvil-perfetto-%TIMESTAMP%.trace
set CONFIG_FILE=%~dp0perfetto_config.pbtx

echo [1/4] Checking connected ADB devices...
adb devices
if %ERRORLEVEL% NEQ 0 (
    echo Error: ADB not found or device not connected.
    exit /b 1
)

echo [2/4] Pushing trace config to device...
adb push "%CONFIG_FILE%" /data/local/tmp/perfetto_config.pbtx
if %ERRORLEVEL% NEQ 0 (
    echo Error pushing config to /data/local/tmp/perfetto_config.pbtx
    exit /b 1
)

echo [3/4] Recording 10-second Perfetto trace...
echo Perform the actions you want to profile in ANVIL now!
adb shell "perfetto -c /data/local/tmp/perfetto_config.pbtx -o /data/misc/perfetto-traces/trace.perfetto-trace"

echo [4/4] Pulling trace file to %TRACE_FILE%...
adb pull /data/misc/perfetto-traces/trace.perfetto-trace "%TRACE_FILE%"
adb shell "rm /data/local/tmp/perfetto_config.pbtx /data/misc/perfetto-traces/trace.perfetto-trace"

echo =======================================================
echo Trace saved to: %TRACE_FILE%
echo Open in browser at https://ui.perfetto.dev/
echo Or run: python %~dp0analyze_trace.py "%TRACE_FILE%"
echo =======================================================
