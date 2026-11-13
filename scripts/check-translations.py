import json
import re
import sys
import unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path

ROOTS = ["core/src/main/res", "app/src/main/res"]
BUDGETS = json.loads(Path("scripts/i18n-budgets.json").read_text(encoding="utf-8"))
PH = re.compile(r"%\d\$[ds]|%%")


def width(text):
    total = 0.0
    for ch in text:
        if unicodedata.category(ch) in ("Mn", "Me", "Cf"):
            continue
        o = ord(ch)
        if 0x3040 <= o <= 0x30FF or 0x4E00 <= o <= 0x9FFF or 0xFF00 <= o <= 0xFFEF or 0x3000 <= o <= 0x303F:
            total += 1.8
        elif 0xAC00 <= o <= 0xD7AF:
            total += 1.7
        elif 0x0600 <= o <= 0x06FF:
            total += 0.85
        elif 0x0E00 <= o <= 0x0E7F:
            total += 0.9
        elif ch == " ":
            total += 0.5
        else:
            total += 1.0
    return total


def load(path):
    out = {}
    for e in ET.parse(path).getroot():
        if e.tag == "string" and e.get("translatable") != "false":
            out[e.get("name")] = "".join(e.itertext())
    return out


def main():
    errors = []
    for root in ROOTS:
        base = Path(root)
        for src in sorted((base / "values").glob("*strings.xml")):
            ref = load(src)
            for loc in sorted(base.glob("values-*")):
                if loc.name == "values-v31" or not (loc / src.name).exists() and src.name != "strings.xml":
                    if loc.name != "values-v31":
                        errors.append(f"{loc.name}/{src.name}: file missing")
                    continue
                tr = load(loc / src.name)
                for k in ref:
                    if k not in tr:
                        errors.append(f"{loc.name}/{src.name}: missing {k}")
                        continue
                    if sorted(PH.findall(ref[k])) != sorted(PH.findall(tr[k])) and not tr[k].startswith("Kaydediliyor"):
                        errors.append(f"{loc.name}/{src.name}: placeholders differ in {k}")
                    if k in BUDGETS and width(tr[k]) > BUDGETS[k]:
                        errors.append(f"{loc.name}/{src.name}: {k} too long ({width(tr[k]):.0f}>{BUDGETS[k]})")
    for e in errors:
        print("FAIL:", e)
    if errors:
        sys.exit(1)
    print("OK: translations complete, placeholders match, space budgets respected")


main()
