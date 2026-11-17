# Usage: python3 scripts/audit_text_fit.py . build/strings_width_audit.csv
# Lists every string of every language with its width compared to English (CJK counts double).
import sys, os, re, glob, csv, unicodedata
import xml.etree.ElementTree as ET
root = sys.argv[1]; out = sys.argv[2]
def width(s):
    w = 0.0
    for c in s:
        ea = unicodedata.east_asian_width(c)
        if ea in ('W','F'): w += 2.0
        elif unicodedata.category(c).startswith('M'): w += 0
        elif ord(c) >= 0x0E00 and ord(c) <= 0x0E7F: w += 1.0
        else: w += 1.0
    return w
def load(path):
    d = {}
    try: t = ET.parse(path)
    except Exception as e: return d
    for el in t.getroot():
        n = el.get('name')
        if not n or el.get('translatable') == 'false': continue
        if el.tag == 'string': d[n] = ''.join(el.itertext())
        elif el.tag == 'plurals':
            for it in el: d[f"{n}#{it.get('quantity')}"] = ''.join(it.itertext())
        elif el.tag == 'string-array':
            for i, it in enumerate(el): d[f"{n}[{i}]"] = ''.join(it.itertext())
    return d
data = {}  # locale -> key -> text
for mod in ('app','core'):
    base = os.path.join(root, mod, 'src/main/res')
    for vd in sorted(glob.glob(base + '/values*')):
        name = os.path.basename(vd)
        if name == 'values-v31': continue
        loc = 'en' if name == 'values' else name[len('values-'):]
        for f in glob.glob(vd + '/*.xml'):
            for k, v in load(f).items():
                data.setdefault(loc, {})[f"{mod}:{k}"] = v
en = data['en']
rows = []
for loc, d in data.items():
    if loc == 'en': continue
    for k, v in d.items():
        e = en.get(k)
        if not e or not v: continue
        ew, vw = width(re.sub(r'%\d?\$?[sd]','00',e)), width(re.sub(r'%\d?\$?[sd]','00',v))
        if ew < 3: continue
        rows.append((loc, k, round(vw/ew,2), int(vw), int(ew), v, e))
rows.sort(key=lambda r: -r[2])
with open(out, 'w', newline='', encoding='utf-8') as f:
    w = csv.writer(f); w.writerow(['locale','key','width_ratio_vs_en','width','en_width','text','english'])
    w.writerows(rows)
print('locales:', len(data), 'en strings:', len(en))
per = {}
for r in rows: per.setdefault(r[0], []).append(r[2])
for loc in sorted(per): 
    v = per[loc]; print(f"{loc:8s} n={len(v):5d} avg={sum(v)/len(v):.2f} >1.5x={sum(1 for x in v if x>1.5)}")
print('--- top 25 overall (width>=12)')
for r in [r for r in rows if r[3]>=12][:25]: print(r[0], r[1], r[2], r[5][:70])
