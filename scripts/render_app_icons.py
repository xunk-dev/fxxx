#!/usr/bin/env python3
"""Renders the launcher-icon art at full resolution (lossless WebP) from the recoloured Frame Out logo.

  python3 scripts/render_app_icons.py     # needs: pip install playwright pillow  (and a Chromium)

Writes
  app/src/main/res/drawable-nodpi/ic_launcher_fg_<accent>_<dark|light>.webp   864x864 (108dp @ 8x), transparent
  store/ic_launcher_1024.png, store/ic_launcher_512.png                        full-bleed icon for the store listing
Dynamic-colour art cannot be rasterised (it uses system colours) and stays vector (gen_app_icons.py).
"""
import io, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import brand_logo as BL
import gen_app_icons as G
from PIL import Image
from playwright.sync_api import sync_playwright

FG_PX = 864


def svg(accent, variant, px, bg=None):
    tile = None
    if bg:  # bg = accent key: the same diagonal gradient tile the launcher uses
        t0, t1 = G.tile_stops(bg)
        tile = (f'<linearGradient id="tile" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="108" y2="108">'
                f'<stop offset="0" stop-color="#{t0}"/><stop offset="1" stop-color="#{t1}"/></linearGradient>',
                '<rect width="108" height="108" fill="url(#tile)"/>')
    return BL.svg_logo(accent, variant, px=px, bg=tile)


def shot(page, markup, px):
    page.set_viewport_size({'width': px, 'height': px})
    page.set_content(f'<html><body style="margin:0;background:transparent">{markup}</body></html>')
    return Image.open(io.BytesIO(page.screenshot(omit_background=True, clip={'x': 0, 'y': 0, 'width': px, 'height': px}))).convert('RGBA')


def main():
    out = os.path.join(G.RES, 'drawable-nodpi'); os.makedirs(out, exist_ok=True)
    store = os.path.join(G.ROOT, 'store'); os.makedirs(store, exist_ok=True)
    with sync_playwright() as pw:
        b = pw.chromium.launch(); page = b.new_page()
        for a in G.ACCENTS:
            for v in ('dark', 'light'):
                im = shot(page, svg(a, v, FG_PX), FG_PX)
                im.save(os.path.join(out, f'ic_launcher_fg_{a}_{v}.webp'), lossless=True, exact=True, quality=100, method=6)
        big = shot(page, svg('indigo', 'dark', 1024, bg='indigo'), 1024).convert('RGB')
        big.save(os.path.join(store, 'ic_launcher_1024.png'), optimize=True)
        big.resize((512, 512), Image.LANCZOS).save(os.path.join(store, 'ic_launcher_512.png'), optimize=True)
        b.close()
    print('done')


if __name__ == '__main__':
    main()
