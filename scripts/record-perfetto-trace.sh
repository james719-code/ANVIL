#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TRACES_DIR="$PROJECT_ROOT/traces"
mkdir -p "$TRACES_DIR"

TIMESTAMP=$(date +"%Y%m%dT%H%M%S")
TRACE_FILE="$TRACES_DIR/anvil-perfetto-$TIMESTAMP.trace"
CONFIG_FILE="$SCRIPT_DIR/perfetto_config.pbtx"

echo "======================================================="
echo "         ANVIL Perfetto Trace Recorder (Bash)          "
echo "======================================================="

echo "[1/4] Checking ADB devices..."
adb devices

echo "[2/4] Pushing trace config to device..."
adb push "$CONFIG_FILE" /data/local/tmp/perfetto_config.pbtx

echo "[3/4] Recording 10-second Perfetto trace..."
echo ">>> Perform actions in ANVIL now! <<<"
adb shell "perfetto -c /data/local/tmp/perfetto_config.pbtx -o /data/misc/perfetto-traces/trace.perfetto-trace"

echo "[4/4] Pulling trace file to $TRACE_FILE..."
adb pull /data/misc/perfetto-traces/trace.perfetto-trace "$TRACE_FILE"
adb shell "rm /data/local/tmp/perfetto_config.pbtx /data/misc/perfetto-traces/trace.perfetto-trace"

echo "======================================================="
echo "Trace saved to: $TRACE_FILE"
echo "View in browser: https://ui.perfetto.dev/"
echo "Analyze via SQL: python3 $SCRIPT_DIR/analyze_trace.py \"$TRACE_FILE\""
echo "======================================================="
