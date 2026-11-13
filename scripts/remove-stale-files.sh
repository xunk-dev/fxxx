#!/usr/bin/env bash
# Removes leftovers from before the FrameX -> Frame out (com.frameout) rename.
# Old com/framex source dirs keep importing com.framex.*.R, which no longer
# exists once the namespace is com.frameout.*, giving "Unresolved reference 'R'".
set -euo pipefail
find . -type f \( -name "SampleSelector.kt" -o -name "SampleSelectorTest.kt" -o -name "PaywallMarquee.kt" -o -name "ImageSearchPanel.kt" \) ! -path "*/build/*" ! -path "./.git/*" -print -delete
find . -type d -path "*/src/*/com/framex" ! -path "*/build/*" ! -path "./.git/*" -prune -print -exec rm -rf {} +
find codecpack/src/main/cpp -maxdepth 1 -type f \( -name "framex_ffmpeg.*" -o -name "FramexJni*" \) -print -delete 2>/dev/null || true
find . -type d -path "*/src/*/com/frameout/app/quick" ! -path "*/build/*" ! -path "./.git/*" -prune -print -exec rm -rf {} +
find . -type f -path "*/src/main/java/com/frameout/app/trim/*" \( -name "CollectSection.kt" -o -name "QueueRow.kt" -o -name "ToolsSheet.kt" -o -name "MarkerJump.kt" -o -name "PlaybackRow.kt" \) ! -path "*/build/*" ! -path "./.git/*" -print -delete
exit 0
