#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FILE="$ROOT_DIR/test-switch.properties"
VALUE="off"
if [ -f "$FILE" ]; then
  VALUE="$(grep -E '^DEVICE_TESTS=' "$FILE" | tail -1 | cut -d= -f2 | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]' || true)"
fi
case "$VALUE" in
  on|true|1) ENABLED=true ;;
  *) ENABLED=false ;;
esac
echo "device tests switch: $ENABLED"
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "enabled=$ENABLED" >> "$GITHUB_OUTPUT"
fi
[ "$ENABLED" = "true" ]
