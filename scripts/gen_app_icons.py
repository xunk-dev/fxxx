#!/usr/bin/env python3
"""Generates the launcher-icon resources: adaptive icons, tile colours, dynamic-colour vector art, the themed (monochrome)
icon, the notification icon, SplashPaths.kt and the manifest aliases.

The art is the Frame Out logo (scripts/brand/frame_out_logo.xml) recoloured per accent by scripts/brand_logo.py.
The fixed-accent foreground art is rasterised by scripts/render_app_icons.py (lossless WebP, 864 px).

  python3 scripts/gen_app_icons.py            # writes into app/src/main/res (+ engine notification icon, SplashPaths.kt)
  python3 scripts/render_app_icons.py         # webp foregrounds + store icons

Accents x modes:  indigo ocean emerald sunset rose gold black crimson lime violet mocha dynamic  x  dark light amoled
"""
import os, sys, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'app/src/main/res')

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import brand_logo as BL

# ---------------------------------------------------------------- colours
def hx(c): c = c.lstrip('#'); return tuple(int(c[i:i+2], 16) for i in (0, 2, 4))
def th(t): return '%02x%02x%02x' % tuple(max(0, min(255, round(v))) for v in t)
def mix(a, b, t): A, B = hx(a), hx(b); return th(tuple(A[i]*(1-t) + B[i]*t for i in range(3)))
def shade(a, k): return th(tuple(v*k for v in hx(a)))

# Single source of truth: the in-app accent palette (designsystem/.../Accent.kt).
# The logo is recoloured from it by brand_logo.py (and by LogoPalette.kt in the app: the splash, About and onboarding mark).
INK = '05060F'                      # BrandTile.kt INK

def _app_accents():
    src = open(os.path.join(ROOT, 'designsystem/src/main/java/com/frameout/designsystem/theme/Accent.kt'), encoding='utf-8').read()
    out = {}
    for m in re.finditer(r'palette\(\s*key = "(\w+)",\s*stops = longArrayOf\(0xFF(\w{6}),\s*0xFF(\w{6}),\s*0xFF(\w{6})\)', src):
        out[m.group(1)] = tuple(c.lower() for c in m.group(2, 3, 4))   # a, b, c
    return out

APP = _app_accents()
assert set(APP) == {'indigo', 'ocean', 'emerald', 'sunset', 'rose', 'gold', 'black', 'crimson', 'lime', 'violet', 'mocha'}, APP
LIGHT_TILE = {'indigo': 'E3E7FA', 'ocean': 'DCEEFA', 'emerald': 'DAF2E8', 'sunset': 'FBE3D8', 'rose': 'FBDFE9', 'gold': 'F9EDCB', 'black': 'E6E6E6', 'crimson': 'FBDADA', 'lime': 'EAF5D3', 'violet': 'F0E3FB', 'mocha': 'F0E2D3'}

def tile_stops(accent):
    """Same two colours BrandTile.drawTileBody blends (INK towards b at 20%, towards a at 8%)."""
    if accent == 'indigo': return '01092C', '01092C'      # the logo's own background colour
    a, b, c = APP[accent]
    return mix(INK, b, .20), mix(INK, a, .08)

ACCENTS = {}
for _k, (_a, _b, _c) in APP.items():
    _t0, _t1 = tile_stops(_k)
    # (lo, mid, hi, dark tile (flat fallback = midpoint), light tile)
    ACCENTS[_k] = (_b, _a, _c, mix(_t0, _t1, .5).upper(), LIGHT_TILE[_k])
# neutral accent: the light tile needs charcoal art (shading light greys gives too little contrast)
LIGHT_ART = {'black': ('222222', '444444', '767676')}
MODES = ('dark', 'light', 'amoled')
ORDER = list(ACCENTS) + ['dynamic']
SYS = '@android:color/system_'

def write(path, text):
    full = os.path.join(RES, path); os.makedirs(os.path.dirname(full), exist_ok=True)
    open(full, 'w', encoding='utf-8').write(text)

def bg_name(accent, mode): return 'ic_launcher_bg_amoled' if mode == 'amoled' else f'ic_launcher_bg_{accent}_{mode}'
def bg_ref(accent, mode):
    """Adaptive-icon background: the dark tile is the same diagonal gradient the in-app BrandTile draws."""
    if mode == 'dark' and accent in APP: return f'@drawable/ic_launcher_tile_{accent}'
    return '@color/' + bg_name(accent, mode)

def tile_xml(accent):
    t0, t1 = tile_stops(accent)
    return ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            '    <path android:pathData="M0 0H108V108H0Z">\n        <aapt:attr name="android:fillColor">\n'
            '            <gradient android:type="linear" android:startX="0" android:startY="0" android:endX="108" android:endY="108">\n'
            f'                <item android:offset="0" android:color="#FF{t0.upper()}" />\n'
            f'                <item android:offset="1" android:color="#FF{t1.upper()}" />\n'
            '            </gradient>\n        </aapt:attr>\n    </path>\n</vector>\n')
def fg_name(accent, mode): return f'ic_launcher_fg_{accent}_' + ('light' if mode == 'light' else 'dark')

def main():
    # ---- art: dynamic-colour vectors, themed (monochrome) icon, notification icon, Kotlin paths
    for v in ('dark', 'light'):
        write(f'drawable-v31/ic_launcher_fg_dynamic_{v}.xml', BL.vector(BL.dynamic_color_fn(v)))
        write(f'drawable/ic_launcher_fg_dynamic_{v}.xml',   # < Android 12: falls back to the indigo art
              f'<?xml version="1.0" encoding="utf-8"?>\n<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n'
              f'    <item android:drawable="@drawable/ic_launcher_fg_indigo_{v}" />\n</layer-list>\n')
    # fixed-colour accents are high-resolution WebP made by scripts/render_app_icons.py
    # (drawable-nodpi/ic_launcher_fg_<accent>_<dark|light>.webp); no vector copies of them exist
    write('drawable/ic_launcher_monochrome.xml', BL.monochrome_xml())
    full = os.path.join(ROOT, 'engine/src/main/res/drawable/ic_notification.xml')
    open(full, 'w', encoding='utf-8').write(BL.notification_xml())
    open(os.path.join(ROOT, 'app/src/main/java/com/frameout/app/splash/SplashPaths.kt'), 'w', encoding='utf-8').write(BL.kotlin_paths())
    # ---- gradient tiles (dark mode) shared with the in-app brand tile
    for a in APP: write(f'drawable/ic_launcher_tile_{a}.xml', tile_xml(a))
    # ---- tile colours
    cols = ['<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_bg_amoled">#000000</color>']
    for a, v in ACCENTS.items():
        cols.append(f'    <color name="ic_launcher_bg_{a}_dark">#{v[3]}</color>')
        cols.append(f'    <color name="ic_launcher_bg_{a}_light">#{v[4]}</color>')
    cols.append('    <color name="ic_launcher_bg_dynamic_dark">#030F36</color>')
    cols.append('    <color name="ic_launcher_bg_dynamic_light">#E3E7FA</color>')
    write('values/ic_launcher_colors.xml', '\n'.join(cols) + '\n</resources>\n')
    write('values-v31/ic_launcher_colors.xml',
          '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
          '    <color name="ic_launcher_bg_dynamic_dark">@android:color/system_accent1_900</color>\n'
          '    <color name="ic_launcher_bg_dynamic_light">@android:color/system_accent1_100</color>\n</resources>\n')
    # ---- launcher icons: adaptive (API 26+) and layer-list (API 24-25)
    aliases = []
    for a in ORDER:
        for m in MODES:
            key = f'{a}_{m}'
            write(f'mipmap-anydpi-v26/ic_launcher_{key}.xml',
                  '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                  f'    <background android:drawable="{bg_ref(a, m)}" />\n'
                  f'    <foreground android:drawable="@drawable/{fg_name(a, m)}" />\n'
                  '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n</adaptive-icon>\n')
            write(f'mipmap-anydpi-v24/ic_launcher_{key}.xml',
                  '<?xml version="1.0" encoding="utf-8"?>\n<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n'
                  '    <item>\n        <inset android:insetLeft="7%" android:insetTop="7%" android:insetRight="7%" android:insetBottom="7%">\n'
                  '            <shape android:shape="rectangle">\n'
                  f'                <solid android:color="@color/{bg_name(a, m)}" />\n'
                  '                <corners android:radius="12dp" />\n            </shape>\n        </inset>\n    </item>\n'
                  f'    <item android:drawable="@drawable/{fg_name(a, m)}" />\n</layer-list>\n')
            aliases.append(key)
    # default icon (application icon) = indigo / dark
    write('mipmap-anydpi-v26/ic_launcher.xml',
          '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
          '    <background android:drawable="@drawable/ic_launcher_tile_indigo" />\n'
          '    <foreground android:drawable="@drawable/ic_launcher_fg_indigo_dark" />\n'
          '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n</adaptive-icon>\n')
    write('mipmap-anydpi-v24/ic_launcher.xml', open(os.path.join(RES, 'mipmap-anydpi-v24/ic_launcher_indigo_dark.xml')).read())
    # ---- manifest aliases
    out = []
    for key in aliases:
        cls = ''.join(p.capitalize() for p in key.split('_'))
        on = 'true' if key == 'indigo_dark' else 'false'
        out.append(f'''        <activity-alias
            android:name=".icon.{cls}"
            android:targetActivity=".MainActivity"
            android:enabled="{on}"
            android:exported="true"
            android:icon="@mipmap/ic_launcher_{key}"
            android:label="@string/app_name">
            <meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity-alias>''')
    open(os.path.join(ROOT, 'scripts/icon_aliases.xml.txt'), 'w').write('\n'.join(out) + '\n')
    print('done:', len(aliases), 'aliases')

if __name__ == '__main__':
    main()
