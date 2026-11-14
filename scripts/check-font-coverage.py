import glob
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

try:
    from fontTools.ttLib import TTFont
except ImportError:
    print("FAIL: fonttools is required (see scripts/fonttools-requirements.txt)")
    sys.exit(1)

FONT = "designsystem/src/main/res/font/ibm_plex_sans_arabic_regular.ttf"
SCRIPT = Path("designsystem/src/main/java/com/frameout/designsystem/theme/FrameScript.kt").read_text(encoding="utf-8")
ALIAS = {"zh-rTW": "zh", "in": "id"}
plex = set(re.findall(r'"(\w+)"', re.search(r"val plexLanguages[^=]*=\s*setOf\(([^)]*)\)", SCRIPT).group(1)))
cmap = set(TTFont(FONT).getBestCmap())
missing = {}
for path in glob.glob("*/src/main/res/values*/*.xml"):
    folder = path.split("/")[-2]
    if folder == "values" or folder == "values-v31":
        lang = "en" if folder == "values" else None
    else:
        lang = folder[len("values-"):]
    if lang is None or not Path(path).name.endswith("strings.xml") and "plurals" not in path:
        continue
    lang = ALIAS.get(lang, lang)
    for e in ET.parse(path).getroot().iter():
        if e.tag in ("string", "item"):
            for ch in "".join(e.itertext()):
                if ord(ch) > 0x7F and ch.isalpha() and ord(ch) not in cmap:
                    missing.setdefault(lang, set()).add(ch)
errors = [f"{lang} uses the bundled font but it lacks {''.join(sorted(chars))[:20]}" for lang, chars in sorted(missing.items()) if lang in plex]
for e in errors:
    print("FAIL:", e)
if errors:
    sys.exit(1)
print("OK: every language that uses the bundled font has all its letters in it")
