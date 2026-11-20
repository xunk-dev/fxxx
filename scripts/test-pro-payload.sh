#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$HERE/.."
GRADLE_BIN="${GRADLE_BIN:-gradle}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
FP="53a6d464bb615003eab4706fff8283344677b025df34f2bbaf2b233a84613956"
python3 -I "$HERE/pro_payload_test/make_project.py" "$ROOT/buildSrc/src/main/kotlin/EncryptProPayload.kt" "$ROOT/app/payload/pro_filters.txt" "$WORK"
cd "$WORK"
"$GRADLE_BIN" --no-daemon -q sealed plain "-Pcert=$FP"
if "$GRADLE_BIN" --no-daemon -q badCert "-Pcert=$FP" >/dev/null 2>&1; then
  echo "FAILED: a malformed certificate must stop the task"
  exit 1
fi
java "$HERE/pro_payload_test/Verify.java" build/sealed/pro_payload.bin build/plain/pro_payload.bin "$ROOT/app/payload/pro_filters.txt"
