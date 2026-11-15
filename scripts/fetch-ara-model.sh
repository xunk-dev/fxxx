#!/usr/bin/env bash
set -euo pipefail
DEST="${ARA_DEST:-core/src/main/assets/tessdata/ara.traineddata}"
URLS=(
  "https://github.com/tesseract-ocr/tessdata_fast/raw/main/ara.traineddata"
  "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/ara.traineddata"
  "https://cdn.jsdelivr.net/gh/tesseract-ocr/tessdata_fast@main/ara.traineddata"
)
mkdir -p "$(dirname "$DEST")"
if [ -s "$DEST" ]; then
  echo "OK: $DEST already present ($(stat -c%s "$DEST") bytes)"
  exit 0
fi
valid() {
  [ -s "$DEST" ] || return 1
  local size
  size=$(stat -c%s "$DEST")
  [ "$size" -ge 500000 ] && [ "$size" -le 4000000 ]
}
for round in 1 2 3; do
  for url in "${URLS[@]}"; do
    echo "Attempt $round: $url"
    if curl -fsSL --retry 3 --retry-delay 3 --retry-all-errors --connect-timeout 20 --max-time 180 "$url" -o "$DEST" && valid; then
      sha256sum "$DEST"
      echo "OK: fetched tessdata_fast ara ($(stat -c%s "$DEST") bytes)"
      exit 0
    fi
    rm -f "$DEST"
  done
  sleep $((round * 10))
done
echo "FAIL: could not download ara.traineddata from any mirror"
exit 1
