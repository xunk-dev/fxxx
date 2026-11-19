#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="$HERE/../app/src/main/cpp"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
g++ -std=c++17 -fsanitize=address,undefined -g -I"$SRC" "$HERE/guard_test/harness.cpp" "$SRC/guard_core.cpp" -o "$WORK/harness"
python3 -I "$HERE/inject-text-hash.py" "$WORK/harness"
cd "$WORK"
python3 -I "$HERE/guard_test/verify.py" "$WORK/harness"
