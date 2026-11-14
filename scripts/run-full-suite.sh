#!/usr/bin/env bash
set -uo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LABEL="${1:?label}"
FILTER="${2:-all}"
OUT="$ROOT_DIR/build/suite-results/$LABEL"
APP_ID="com.frameout.app"
RUNNER="$APP_ID.test/androidx.test.runner.AndroidJUnitRunner"
mkdir -p "$OUT"
if ! bash "$ROOT_DIR/scripts/test-switch.sh"; then
  echo "device tests are off"
  exit 0
fi
cd "$ROOT_DIR"
# emulators run x86_64; the default build only has arm64-v8a, so the install is rejected; deviceTest builds one APK with x86_64 too
adb uninstall "$APP_ID.test" >/dev/null 2>&1 || true
adb uninstall "$APP_ID" >/dev/null 2>&1 || true
gradle :app:installDebug :app:installDebugAndroidTest -PdeviceTest=on --no-daemon || exit 1
if [ -s "$ROOT_DIR/build/fixtures/manifest.txt" ]; then
  adb shell rm -rf /data/local/tmp/frameout-fixtures
  adb push "$ROOT_DIR/build/fixtures" /data/local/tmp/frameout-fixtures >/dev/null
  adb shell "run-as $APP_ID sh -c 'rm -rf files/fixtures && mkdir -p files/fixtures && cp /data/local/tmp/frameout-fixtures/* files/fixtures/'"
fi
ARGS=(-w -r -e listener com.frameout.app.full.ReportListener -e longFrames 2000 -e maxJankPercent 60)
if [ "$FILTER" = "basic" ]; then ARGS+=(-e notAnnotation com.frameout.app.full.FullSuite); fi
if [ "$FILTER" = "screens" ]; then ARGS+=(-e package com.frameout.app.screens); fi
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
for _ in $(seq 1 60); do
  load="$(adb shell cat /proc/loadavg | awk '{print int($1)}')"
  [ "${load:-9}" -lt 2 ] && break
  sleep 3
done
adb logcat -c
# stream logcat to a file while the tests run, so it survives if the emulator dies mid-run
adb logcat > "$OUT/logcat.txt" 2>&1 &
LOGCAT_PID=$!
trap 'kill "$LOGCAT_PID" 2>/dev/null || true' EXIT
# a hung emulator must fail in minutes, not burn the whole job timeout
run_instrument() { timeout 25m adb shell am instrument "${ARGS[@]}" "$RUNNER" | tee "$OUT/instrument.txt"; }
run_instrument
if grep -q "Process crashed" "$OUT/instrument.txt" && ! grep -q "INSTRUMENTATION_STATUS: test=" "$OUT/instrument.txt"; then
  echo "instrumentation crashed before the first test; retrying once"
  sleep 30
  run_instrument
fi
kill "$LOGCAT_PID" 2>/dev/null || true
# a run that ends normally prints INSTRUMENTATION_CODE; without it the emulator or the process died mid-run
if ! grep -q "^INSTRUMENTATION_CODE:" "$OUT/instrument.txt"; then
  total="$(grep -m1 -oE 'numtests=[0-9]+' "$OUT/instrument.txt" | cut -d= -f2)"
  last="$(grep -E '^INSTRUMENTATION_STATUS: test=' "$OUT/instrument.txt" | tail -1 | cut -d= -f2-)"
  finished="$(grep -c '^INSTRUMENTATION_STATUS_CODE: 0$' "$OUT/instrument.txt")"
  state="$(adb get-state 2>&1 | tr -d '\r')"
  echo "run cut off: $finished/${total:-?} tests finished, last started: ${last:-none}, adb state: $state" | tee "$OUT/cutoff.txt"
fi
adb exec-out run-as "$APP_ID" cat files/fullsuite/results.jsonl > "$OUT/results.jsonl" 2>/dev/null || true
adb exec-out run-as "$APP_ID" cat files/fullsuite/capabilities.json > "$OUT/capabilities.json" 2>/dev/null || true
test -s "$OUT/results.jsonl" || exit 1
if grep -qE "^INSTRUMENTATION_STATUS_CODE: (-1|-2)$|INSTRUMENTATION_RESULT: shortMsg|Process crashed" "$OUT/instrument.txt"; then exit 1; fi
exit 0
