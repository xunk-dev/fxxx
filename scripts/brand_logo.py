#!/usr/bin/env python3
"""Single source for the Frame Out logo art: loads scripts/brand/frame_out_logo.xml and recolours it per accent.

How the recolour works (mirrored 1:1 in app/.../splash/LogoPalette.kt, keep both in sync):
  The logo gradients walk a ramp  cyan -> blue -> violet -> magenta.  Those four colours are the "original anchors".
  Every accent supplies four new anchors (c, a, b, b2) taken from Accent.kt.  A logo colour is located on the ramp by
  its hue, replaced by the matching point of the accent ramp, and keeps its own shading (the difference between the
  colour and the original ramp point is added back).  White / grey stays as is.  Warm colours (sun, glow on the ring)
  are mapped between two "pop" anchors.  Indigo is the identity (the logo exactly as designed).
"""
import colorsys, os, re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
LOGO = os.path.join(HERE, 'brand', 'frame_out_logo.xml')
ACCENT_KT = os.path.join(ROOT, 'designsystem/src/main/java/com/frameout/designsystem/theme/Accent.kt')

# ------------------------------------------------------------------ colour helpers (rgb 0..255 tuples)
def hx(c):
    c = c.lstrip('#'); return tuple(float(int(c[i:i + 2], 16)) for i in (0, 2, 4))
def th(t): return '%02X%02X%02X' % tuple(max(0, min(255, int(round(v)))) for v in t)
def mix(a, b, t): return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))
def shade(a, k): return tuple(v * k for v in a)
WHITE = (255.0, 255.0, 255.0)
def hue(c):
    return colorsys.rgb_to_hls(c[0] / 255, c[1] / 255, c[2] / 255)[0] * 360
def lightness(c): return (max(c) + min(c)) / 510
def chroma(c): return (max(c) - min(c)) / 255

# ------------------------------------------------------------------ original logo
O_RAMP = [hx('01F6FD'), hx('2A44FB'), hx('7725FB'), hx('D02AFC')]      # c, a, b, b2
O_POP = [hx('FCA84B'), hx('FB5686')]                                   # sun top-left, sun bottom-right
O_RAMP_H = [hue(c) for c in O_RAMP]
O_POP_H0, O_POP_H1 = hue(O_POP[0]), hue(O_POP[1]) - 360                # 33 .. -17.5 (unwrapped)
RAMP_MIN_H, RAMP_MAX_H = 168, 300
NEUTRAL_CHROMA = 0.08

def parse_color(s):
    s = s.lstrip('#')
    if len(s) == 8: return hx(s[2:]), int(s[:2], 16) / 255      # Android ARGB
    return hx(s), 1.0

def load_logo():
    txt = open(LOGO, encoding='utf-8').read()
    layers = []
    for m in re.finditer(r'<!--\s*(\w+)\s*-->\s*<path(.*?)</path>', txt, re.S):
        name, body = m.groups()
        g = re.search(r'<gradient(.*?)>(.*?)</gradient>', body, re.S)
        at = dict(re.findall(r'android:(\w+)="([^"]*)"', g.group(1)))
        layers.append(dict(
            name=name,
            d=re.search(r'android:pathData="([^"]+)"', body).group(1),
            grad=tuple(float(at[k]) for k in ('startX', 'startY', 'endX', 'endY')),
            stops=[(float(o), c) for o, c in re.findall(r'android:offset="([^"]+)"\s+android:color="([^"]+)"', g.group(2))]))
    return layers

LAYERS = load_logo()
LAYER = {l['name']: l for l in LAYERS}
VIEW = 1254.0
BOUNDS = (168.0, 247.0, 1127.0, 955.0)          # content box of the artwork inside the 1254 viewport
CENTER = ((BOUNDS[0] + BOUNDS[2]) / 2, (BOUNDS[1] + BOUNDS[3]) / 2)
# launcher foreground transform (identical to the supplied ic_launcher_foreground.xml)
FG_SCALE, FG_TX, FG_TY = 0.06475, 13.389, 13.312

# ------------------------------------------------------------------ accents (from Accent.kt)
def read_accents():
    src = open(ACCENT_KT, encoding='utf-8').read()
    out = {}
    for blk in src.split('palette(')[1:]:
        k = re.search(r'key = "(\w+)"', blk)
        s = re.search(r'stops = longArrayOf\(([^)]*)\)', blk)
        if not (k and s): continue
        cols = lambda t: [hx(x[4:]) for x in re.findall(r'0xFF\w{6}', t)]
        ls = re.search(r'lightStops = longArrayOf\(([^)]*)\)', blk)
        out[k.group(1)] = dict(stops=cols(s.group(1)), light=cols(ls.group(1)) if ls else None,
                               neutral='neutral = true' in blk)
    return out

ACCENTS = read_accents()
assert set(ACCENTS) == {'indigo', 'ocean', 'emerald', 'sunset', 'rose', 'gold', 'black', 'crimson', 'lime', 'violet', 'mocha'}, ACCENTS.keys()

def anchors(accent, variant):
    """-> dict(ramp=[c,a,b,b2], pop=[w0,w1], deep=ring colour on light tiles, neutral=bool)"""
    info = ACCENTS[accent]
    if accent == 'indigo' and variant == 'dark':
        return dict(ramp=O_RAMP, pop=O_POP, deep=None, neutral=False)
    a, b, c = (info['light'] or info['stops']) if variant == 'light' else info['stops']
    # same order as Accent.kt: stops = a, b, c
    if variant == 'dark':
        C, A, B = mix(c, WHITE, .10), a, b
        B2 = mix(b, WHITE, .18)
        pop = [mix(c, WHITE, .30), mix(B2, WHITE, .25)]
        deep = None
    else:
        C, A, B = shade(c, .78), shade(a, .88), shade(b, .88)
        B2 = shade(mix(b, WHITE, .12), .88)
        pop = [shade(mix(c, WHITE, .10), .85), shade(B2, .95)]
        deep = shade(mix(a, b, .5), .62)
    if accent == 'indigo':          # keep the designed warm sun on the default colour in light mode too
        pop = [shade(O_POP[0], .92), shade(O_POP[1], .92)]
    return dict(ramp=[C, A, B, B2], pop=pop, deep=deep, neutral=info['neutral'])

def remap(x, an, variant):
    """x = rgb tuple of a logo colour -> rgb tuple for the accent."""
    if chroma(x) <= NEUTRAL_CHROMA:
        if variant == 'light' and an['deep'] is not None and lightness(x) >= .85: return an['deep']
        return x
    h = hue(x)
    if RAMP_MIN_H <= h <= RAMP_MAX_H:
        hh = min(max(h, O_RAMP_H[0]), O_RAMP_H[3])
        i = 0
        while i < 2 and hh > O_RAMP_H[i + 1]: i += 1
        u = (hh - O_RAMP_H[i]) / (O_RAMP_H[i + 1] - O_RAMP_H[i])
        oi = mix(O_RAMP[i], O_RAMP[i + 1], u)
        ni = mix(an['ramp'][i], an['ramp'][i + 1], u)
    else:
        if h > 180: h -= 360
        u = min(max((O_POP_H0 - h) / (O_POP_H0 - O_POP_H1), 0), 1)
        oi = mix(O_POP[0], O_POP[1], u)
        ni = mix(an['pop'][0], an['pop'][1], u)
    res = tuple(min(255.0, max(0.0, ni[k] + (x[k] - oi[k]))) for k in range(3))
    if an['neutral']:
        g = 0.2126 * res[0] + 0.7152 * res[1] + 0.0722 * res[2]; res = (g, g, g)
    return res

def layer_stops(layer, accent, variant):
    """[(offset, '#RRGGBB', opacity)] of one layer for this accent/variant."""
    an = anchors(accent, variant); out = []
    for o, c in layer['stops']:
        rgb, a = parse_color(c)
        out.append((o, '#' + th(remap(rgb, an, variant)), a))
    return out

# ------------------------------------------------------------------ SVG (used for webp / store / previews)
def svg_logo(accent, variant, px=864, view=108, transform=None, layers=None, bg=None, solid=None):
    """Whole logo as an SVG.  transform=(scale, tx, ty) in view units; default = launcher foreground placement."""
    s, tx, ty = transform or (FG_SCALE, FG_TX, FG_TY)
    defs, body, back = [], [], ''
    if bg: defs.append(bg[0]); back = bg[1]
    for l in LAYERS:
        if layers and l['name'] not in layers: continue
        if solid:
            if l['name'] == 'band_shade': continue
            body.append(f'<path fill-rule="evenodd" fill="{solid}" d="{l["d"]}"/>'); continue
        gid = 'g_' + l['name']
        st = ''.join(f'<stop offset="{o:g}" stop-color="{c}" stop-opacity="{a:.3f}"/>' for o, c, a in layer_stops(l, accent, variant))
        x1, y1, x2, y2 = l['grad']
        defs.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">{st}</linearGradient>')
        body.append(f'<path fill-rule="evenodd" fill="url(#{gid})" d="{l["d"]}"/>')
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {view} {view}" width="{px}" height="{px}">'
            f'<defs>{"".join(defs)}</defs>{back}<g transform="translate({tx:.3f} {ty:.3f}) scale({s})">{"".join(body)}</g></svg>')

# ------------------------------------------------------------------ Android vectors
VEC_HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n    android:width="{dp}dp"\n    android:height="{dp}dp"\n'
            '    android:viewportWidth="{vp}"\n    android:viewportHeight="{vp}">\n'
            '    <group\n        android:pivotX="0"\n        android:pivotY="0"\n        android:scaleX="{s}"\n'
            '        android:scaleY="{s}"\n        android:translateX="{tx:.3f}"\n        android:translateY="{ty:.3f}">\n')
VEC_TAIL = '    </group>\n</vector>\n'

def argb(rgb_hex, a):
    return '#%02X%s' % (round(a * 255), rgb_hex.lstrip('#').upper())

def vector(color_fn, layers=None, solid=None, dp=108, vp=108, transform=None):
    """color_fn(layer, stop_rgb, opacity, offset) -> android colour string.  solid -> flat fill for every layer."""
    s, tx, ty = transform or (FG_SCALE, FG_TX, FG_TY)
    x = VEC_HEAD.format(dp=dp, vp=vp, s=s, tx=tx, ty=ty)
    for l in LAYERS:
        if layers and l['name'] not in layers: continue
        if solid:
            if l['name'] == 'band_shade': continue
            x += f'        <path android:fillColor="{solid}" android:fillType="evenOdd" android:pathData="{l["d"]}" />\n'
            continue
        x1, y1, x2, y2 = l['grad']
        x += f'        <path android:fillType="evenOdd" android:pathData="{l["d"]}">\n            <aapt:attr name="android:fillColor">\n'
        x += f'                <gradient android:type="linear" android:startX="{x1:g}" android:startY="{y1:g}" android:endX="{x2:g}" android:endY="{y2:g}">\n'
        for o, c in l['stops']:
            rgb, a = parse_color(c)
            x += f'                    <item android:offset="{o:.2f}" android:color="{color_fn(l, rgb, a, o)}" />\n'
        x += '                </gradient>\n            </aapt:attr>\n        </path>\n'
    return x + VEC_TAIL

# Material You (dynamic): pick a system tonal palette from the ramp position and a tone from the lightness
TONES = [0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000]
def _tone(l, variant, lift):
    raw = 1000 * (1 - min(1, l + lift)) if variant == 'dark' else 1000 * (1 - max(0, l - .10))
    return min(TONES, key=lambda t: abs(t - raw))

def dynamic_color_fn(variant):
    def fn(layer, rgb, a, o):
        if layer['name'] == 'band_shade': return '#%02X000000' % round(a * 255 * .85)
        l = lightness(rgb)
        if chroma(rgb) <= NEUTRAL_CHROMA:
            return '@android:color/white' if variant == 'dark' else '@android:color/system_accent1_800'
        h = hue(rgb)
        if RAMP_MIN_H <= h <= RAMP_MAX_H:
            hh = min(max(h, O_RAMP_H[0]), O_RAMP_H[3])
            t = next((i + (hh - O_RAMP_H[i]) / (O_RAMP_H[i + 1] - O_RAMP_H[i]) for i in range(3) if hh <= O_RAMP_H[i + 1]), 3.0)
            pal = 'accent3' if t < .5 else 'accent2' if t < 1.5 else 'accent1'
            return f'@android:color/system_{pal}_{_tone(l, variant, .12)}'
        return f'@android:color/system_accent3_{_tone(l, variant, .20 if variant == "dark" else .0)}'
    return fn

def monochrome_xml():
    return vector(None, solid='#FFFFFFFF')

NOTIF_TRANSFORM = (0.0236, 12 - CENTER[0] * 0.0236, 12 - CENTER[1] * 0.0236)
NOTIF_LAYERS = [l['name'] for l in LAYERS if l['name'] != 'trail_mid']      # one trail layer reads better at 24dp
def notification_xml():
    return vector(None, layers=NOTIF_LAYERS, solid='#FFFFFFFF', dp=24, vp=24, transform=NOTIF_TRANSFORM)

# ------------------------------------------------------------------ Kotlin (SplashPaths.kt)
def kotlin_paths():
    names = {'strip': 'STRIP', 'band_base': 'BAND', 'band_shade': 'SHADE', 'connector': 'CONNECTOR', 'face': 'FACE',
             'trail_outer': 'TRAIL_OUTER', 'trail_mid': 'TRAIL_MID', 'mountain': 'MOUNTAIN', 'sun': 'SUN', 'ring': 'RING'}
    camel = lambda n: re.sub(r'_(\w)', lambda m: m.group(1).upper(), n)
    k = ['// GENERATED by scripts/gen_app_icons.py from scripts/brand/frame_out_logo.xml. Do not edit by hand.',
         'package com.frameout.app.splash', '',
         'import androidx.compose.ui.geometry.Offset', 'import androidx.compose.ui.graphics.Path',
         'import androidx.compose.ui.graphics.PathFillType', 'import androidx.compose.ui.graphics.vector.PathParser', '',
         '/** One gradient of the logo, in logo coordinates (1254 x 1254). Stops are ARGB longs, recoloured by [LogoPalette]. */',
         'internal class LogoGradient(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val stops: List<Pair<Float, Long>>)', '']
    for l in LAYERS:
        k.append(f'private const val P_{names[l["name"]]} =\n    "{l["d"]}"')
    k += ['', 'internal object LogoGradients {']
    for l in LAYERS:
        st = ', '.join('%gf to 0x%s' % (o, (c.lstrip('#') if len(c.lstrip('#')) == 8 else 'FF' + c.lstrip('#')).upper() + 'L') for o, c in l['stops'])
        x1, y1, x2, y2 = l['grad']
        k.append(f'    val {camel(l["name"])} = LogoGradient({x1:g}f, {y1:g}f, {x2:g}f, {y2:g}f, listOf({st}))')
    k += ['}', '',
          '// Artwork box inside the 1254 x 1254 logo viewport.',
          f'internal const val LOGO_X = {BOUNDS[0]:g}f', f'internal const val LOGO_Y = {BOUNDS[1]:g}f',
          f'internal const val LOGO_W = {BOUNDS[2] - BOUNDS[0]:g}f', f'internal const val LOGO_H = {BOUNDS[3] - BOUNDS[1]:g}f',
          f'internal const val ICON_W = LOGO_W', f'internal const val ICON_CX = {CENTER[0]:g}f', f'internal const val ICON_CY = {CENTER[1]:g}f',
          'internal val FRAME_PIVOT = Offset(754f, 756f)', 'internal val SUN_CENTER = Offset(868f, 690f)', 'internal const val SUN_R = 48f', '',
          'internal class SplashPaths {']
    for l in LAYERS:
        k.append(f'    val {camel(l["name"])} = parse(P_{names[l["name"]]})')
    k += ['', '    private fun parse(data: String): Path =',
          '        PathParser().parsePathString(data).toPath().apply { fillType = PathFillType.EvenOdd }', '}', '']
    return '\n'.join(k)
