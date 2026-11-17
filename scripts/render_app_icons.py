#!/usr/bin/env python3
"""Renders the launcher-icon art at the EXACT pixel size of every screen density (lossless WebP) plus the store icons.

  python3 scripts/render_app_icons.py     # needs: pip install playwright pillow  (and a Chromium)

Why one file per density: the adaptive-icon foreground is 108 x 108 dp.  A single big bitmap in drawable-nodpi gets scaled
down by the system with a bilinear filter, which is what made the old icon look jagged.  Files in drawable-<density> are
used 1:1 on a matching screen (and scaled by < 1.4x on in-between screens such as 2.625x / 3.5x), so the edges stay crisp.

Writes
  app/src/main/res/drawable-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_fg_<accent>_<dark|light>.webp    108/162/216/324/432 px
  store/ic_launcher_512.png, store/ic_launcher_1024.png            full-bleed square, no shadow, no rounded corners (Play masks it)
and removes the old drawable-nodpi/ic_launcher_fg_*.webp.  Dynamic-colour art cannot be rasterised (system colours) and
stays vector (gen_app_icons.py).
"""
import glob, io, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import brand_logo as BL
import gen_app_icons as G
from PIL import Image
from playwright.sync_api import sync_playwright

DENSITIES = {'mdpi': 108, 'hdpi': 162, 'xhdpi': 216, 'xxhdpi': 324, 'xxxhdpi': 432}   # 108 dp x density factor


def tile(bg):
    t0, t1 = G.tile_stops(bg)       # the same diagonal gradient the launcher background and BrandTile use
    return (f'<linearGradient id="tile" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="108" y2="108">'
            f'<stop offset="0" stop-color="#{t0}"/><stop offset="1" stop-color="#{t1}"/></linearGradient>',
            '<rect width="108" height="108" fill="url(#tile)"/>')


def svg(accent, variant, px, bg=None):
    return BL.svg_logo(accent, variant, px=px, bg=tile(bg) if bg else None)


def shot(page, markup, px):
    page.set_viewport_size({'width': px, 'height': px})
    page.set_content(f'<html><body style="margin:0;background:transparent">{markup}</body></html>')
    return Image.open(io.BytesIO(page.screenshot(omit_background=True, clip={'x': 0, 'y': 0, 'width': px, 'height': px}))).convert('RGBA')


def main():
    for f in glob.glob(os.path.join(G.RES, 'drawable-nodpi', 'ic_launcher_fg_*.webp')): os.remove(f)
    store = os.path.join(G.ROOT, 'store'); os.makedirs(store, exist_ok=True)
    with sync_playwright() as pw:
        b = pw.chromium.launch(); page = b.new_page()
        for dens, px in DENSITIES.items():
            out = os.path.join(G.RES, f'drawable-{dens}'); os.makedirs(out, exist_ok=True)
            for a in G.ACCENTS:
                for v in ('dark', 'light'):
                    im = shot(page, svg(a, v, px), px)
                    im.save(os.path.join(out, f'ic_launcher_fg_{a}_{v}.webp'), lossless=True, exact=True, quality=100, method=6)
        for px in (512, 1024):          # rendered natively at each size (no resampling)
            shot(page, svg('indigo', 'dark', px, bg='indigo'), px).convert('RGB').save(os.path.join(store, f'ic_launcher_{px}.png'), optimize=True)
        b.close()
    print('done')


if __name__ == '__main__':
    main()
