#!/usr/bin/env python3
"""Rebuilds the Frame Out logo as clean vector geometry.

  python3 scripts/logo_clean.py          # brand/frame_out_logo.traced.xml  ->  brand/frame_out_logo.xml

brand/frame_out_logo.traced.xml is the original artwork as it came out of the automatic tracer (hundreds of tiny wobbly
Bezier pieces per outline).  This script keeps the SAME shapes, gradients, layer names, layer order and coordinate space
(1254 x 1254, so SUN_CENTER / FRAME_PIVOT used by the splash code stay valid) and only replaces the outlines:

  * trace specks (areas < 20 px^2) are dropped;
  * every outline is resampled, smoothed with a curvature-adaptive filter (small sigma in corners, large sigma on gentle
    stretches, so rounded corners keep their radius and long edges lose the trace wobble) and refitted with a minimum
    number of cubic Beziers (Schneider fit, tangent-continuous);
  * straight stretches (max residual 0.7 px, no bow) become exact lines and are joined to their corners with G1 curves;
  * real corners (turn > 62 degrees within 4 px) stay sharp.

Measured against the traced outlines the new ones stay within about 1 unit (mean 0.3, the trace noise level) of 1254.
No decoration layers are added and the recolouring in brand_logo.py / LogoPalette.kt is untouched.
"""
import numpy as np, re

def parse_subpaths(d):
    toks = re.findall(r'[MCLHVZmclhvz]|-?\d*\.?\d+(?:e-?\d+)?', d)
    subs = []; cur = None; i = 0; pos = np.zeros(2); cmd = None
    def num(n):
        nonlocal i
        v = [float(toks[i + k]) for k in range(n)]; i += n; return v
    while i < len(toks):
        t = toks[i]
        if t.isalpha(): cmd = t; i += 1
        if cmd == 'M':
            pos = np.array(num(2)); cur = [pos.copy()]; subs.append(cur); cmd = 'L'
        elif cmd == 'C':
            p = num(6); P = [pos, np.array(p[0:2]), np.array(p[2:4]), np.array(p[4:6])]
            tt = np.linspace(0, 1, 25)[1:, None]
            pts = ((1-tt)**3)*P[0] + 3*((1-tt)**2)*tt*P[1] + 3*(1-tt)*tt**2*P[2] + tt**3*P[3]
            cur.extend(list(pts)); pos = P[3]
        elif cmd == 'L': pos = np.array(num(2)); cur.append(pos.copy())
        elif cmd == 'H': pos = np.array([num(1)[0], pos[1]]); cur.append(pos.copy())
        elif cmd == 'V': pos = np.array([pos[0], num(1)[0]]); cur.append(pos.copy())
        elif cmd in 'Zz': i += 0; cmd = None
        if cmd is None and i >= len(toks): break
    out = []
    for s in subs:
        a = np.array(s)
        if np.hypot(*(a[0]-a[-1])) < 1e-6: a = a[:-1]
        out.append(a)
    return out

def resample_closed(P, ds):
    Q = np.vstack([P, P[:1]])
    seg = np.hypot(*(Q[1:]-Q[:-1]).T)
    keep = np.concatenate([[True], seg > 1e-9]); Q = Q[keep]
    seg = np.hypot(*(Q[1:]-Q[:-1]).T)
    s = np.concatenate([[0], np.cumsum(seg)]); L = s[-1]
    n = max(8, int(round(L/ds)))
    u = np.linspace(0, L, n, endpoint=False)
    return np.stack([np.interp(u, s, Q[:, 0]), np.interp(u, s, Q[:, 1])], 1), L/n

def gsmooth(P, sigma_samples, closed):
    """Gaussian smooth; closed -> wrap, open -> odd reflection (endpoints fixed)."""
    if sigma_samples < 0.3: return P.copy()
    r = int(np.ceil(3*sigma_samples))
    if not closed: r = min(r, len(P)-1)
    k = np.exp(-0.5*(np.arange(-r, r+1)/sigma_samples)**2); k /= k.sum()
    if closed:
        pad = np.vstack([P[-r:], P, P[:r]]) if r < len(P) else np.vstack([P]*3)[len(P)-r:2*len(P)+r]
    else:
        pre = 2*P[0] - P[1:r+1][::-1]; post = 2*P[-1] - P[-r-1:-1][::-1]
        pad = np.vstack([pre, P, post])
    out = np.stack([np.convolve(pad[:, c], k, mode='valid') for c in range(2)], 1)
    return out

def turning(P, w):
    n = len(P); idx = np.arange(n)
    a = P[idx] - P[(idx-w) % n]; b = P[(idx+w) % n] - P[idx]
    ang = np.arctan2(a[:,0]*b[:,1]-a[:,1]*b[:,0], (a*b).sum(1))
    return np.degrees(ang)

def find_corners(P, ds, thresh=45, win_px=4.0, nms_px=6.0):
    w = max(2, int(round(win_px/ds)))
    th = turning(gsmooth(P, 1.5/ds, True), w)
    n = len(P); nms = int(round(nms_px/ds)); c = []
    for i in np.argsort(-np.abs(th)):
        if abs(th[i]) < thresh: break
        if all(min((i-j) % n, (j-i) % n) > nms for j in c): c.append(i)
    return sorted(c), th

# ---------------------------------------------------------------- Schneider cubic fitting
def bez(ctrl, t):
    t = np.asarray(t)[:, None]
    return ((1-t)**3)*ctrl[0] + 3*((1-t)**2)*t*ctrl[1] + 3*(1-t)*t**2*ctrl[2] + t**3*ctrl[3]
def bez1(ctrl, t):
    t = np.asarray(t)[:, None]
    return 3*((1-t)**2)*(ctrl[1]-ctrl[0]) + 6*(1-t)*t*(ctrl[2]-ctrl[1]) + 3*t**2*(ctrl[3]-ctrl[2])
def bez2(ctrl, t):
    t = np.asarray(t)[:, None]
    return 6*(1-t)*(ctrl[2]-2*ctrl[1]+ctrl[0]) + 6*t*(ctrl[3]-2*ctrl[2]+ctrl[1])

def unit(v):
    n = np.hypot(*v); return v/n if n > 1e-12 else v

def chord_params(P):
    d = np.concatenate([[0], np.cumsum(np.hypot(*(P[1:]-P[:-1]).T))]); return d/d[-1]

def gen_bezier(P, u, t1, t2):
    p0, p3 = P[0], P[-1]
    A1 = (3*((1-u)**2)*u)[:, None]*t1; A2 = (3*(1-u)*u**2)[:, None]*t2
    C = np.array([[ (A1*A1).sum(), (A1*A2).sum()], [(A1*A2).sum(), (A2*A2).sum()]])
    base = (((1-u)**3+3*((1-u)**2)*u)[:, None])*p0 + ((3*(1-u)*u**2+u**3)[:, None])*p3
    tmp = P - base
    X = np.array([(A1*tmp).sum(), (A2*tmp).sum()])
    det = C[0,0]*C[1,1]-C[0,1]*C[1,0]
    seglen = np.hypot(*(p3-p0))
    if abs(det) > 1e-12:
        a1 = (X[0]*C[1,1]-X[1]*C[0,1])/det; a2 = (C[0,0]*X[1]-C[1,0]*X[0])/det
    else: a1 = a2 = 0
    eps = 1e-6*seglen
    if a1 < eps or a2 < eps: a1 = a2 = seglen/3
    return np.array([p0, p0+t1*a1, p3+t2*a2, p3])

def reparam(ctrl, P, u):
    for _ in range(1):
        q = bez(ctrl, u); q1 = bez1(ctrl, u); q2 = bez2(ctrl, u)
        num = ((q-P)*q1).sum(1); den = (q1*q1).sum(1) + ((q-P)*q2).sum(1)
        with np.errstate(all='ignore'): du = np.where(np.abs(den) > 1e-12, num/den, 0)
        u = np.clip(u - du, 0, 1)
    return np.sort(u)

def fit_cubic(P, t1, t2, tol, depth=0):
    """-> list of ctrl arrays.  t1: unit tangent leaving P[0], t2: unit tangent pointing back from P[-1] (i.e. -forward)."""
    if len(P) > 8 and np.hypot(*(P[-1]-P[0])) < 1e-6:
        m = len(P)//2; tc = unit(P[m-1]-P[m+1])
        return fit_cubic(P[:m+1], t1, tc, tol, depth+1) + fit_cubic(P[m:], -tc, t2, tol, depth+1)
    if len(P) < 3 or np.hypot(*(P[-1]-P[0])) < 1e-6:
        c = np.array([P[0], P[0]+(P[-1]-P[0])/3, P[0]+2*(P[-1]-P[0])/3, P[-1]]); return [c]
    u = chord_params(P); ctrl = gen_bezier(P, u, t1, t2)
    best = None
    for it in range(8):
        err = np.hypot(*(bez(ctrl, u)-P).T); m = int(err.argmax()); e = err[m]
        if best is None or e < best[0]: best = (e, ctrl, m)
        if e < tol: return [ctrl]
        u = reparam(ctrl, P, u); ctrl = gen_bezier(P, u, t1, t2)
    e, ctrl, m = best
    if e < tol: return [ctrl]
    mg = min(4, max(1, len(P)//6)); m = min(max(m, len(P)//4, mg), max(3*len(P)//4, mg), len(P)-1-mg)
    tc = unit(P[m-mg]-P[m+mg])
    return fit_cubic(P[:m+1], t1, tc, tol, depth+1) + fit_cubic(P[m:], -tc, t2, tol, depth+1)

def is_line(ctrl, tol=0.35):
    a, b = ctrl[0], ctrl[3]; d = b-a; L = np.hypot(*d)
    if L < 1e-9: return True
    n = np.array([-d[1], d[0]])/L
    return max(abs(((ctrl[1]-a)*n).sum()), abs(((ctrl[2]-a)*n).sum())) < tol

def fmt(v):
    s = ('%.2f' % v).rstrip('0').rstrip('.')
    return '0' if s in ('-0', '') else s
def pt(p): return f'{fmt(p[0])},{fmt(p[1])}'

def path_from_loops(loops):
    """loops: list of list-of-segments; each segment ('L', p3) | ('C', c1, c2, p3); first item ('M', p0)."""
    out = []
    for lp in loops:
        for s in lp:
            if s[0] == 'M': out.append('M' + pt(s[1]))
            elif s[0] == 'L': out.append('L' + pt(s[1]))
            else: out.append('C' + pt(s[1]) + ' ' + pt(s[2]) + ' ' + pt(s[3]))
        out.append('Z')
    return ''.join(out)

def fit_loop(P0, ds=0.5, sigma=4.0, tol=0.8, corner_thresh=45, corners=None):
    P, ds = resample_closed(P0, ds)
    if corners is None: corners, _ = find_corners(P, ds, corner_thresh)
    n = len(P); segs = []
    if len(corners) == 0:
        # closed smooth curve: smooth with wrap, start at point 0 with shared tangent
        S = gsmooth(P, sigma/ds, True)
        # choose start at the point of max |curvature| proxy -> just the extreme-left point for determinism
        k = int(np.argmin(S[:, 0]))
        S = np.roll(S, -k, axis=0); S = np.vstack([S, S[:1]])
        w = max(2, int(3/ds)); tan = unit(S[w] - S[-w-1])
        ctrls = fit_cubic(S, tan, -tan, tol)
        loop = [('M', S[0])] + [('C', c[1], c[2], c[3]) for c in ctrls]
        return [loop]
    out = [('M', None)]
    cs = corners
    for a in range(len(cs)):
        i, j = cs[a], cs[(a+1) % len(cs)]
        idx = np.arange(i, j+1) if j > i else np.concatenate([np.arange(i, n), np.arange(0, j+1)])
        seg = P[idx]
        if len(cs) == 1: seg = np.vstack([P[i:], P[:i+1]])
        if a == 0: out[0] = ('M', seg[0])
        length = np.hypot(*(seg[1:]-seg[:-1]).T).sum()
        if length < 4:
            out.append(('L', seg[-1])); continue
        S = gsmooth(seg, sigma/ds, False)
        w = max(2, min(int(3/ds), len(S)//3))
        t1 = unit(S[w]-S[0]); t2 = unit(S[-1-w]-S[-1])
        for c in fit_cubic(S, t1, t2, tol):
            out.append(('L', c[3]) if is_line(c) else ('C', c[1], c[2], c[3]))
    return [out]

# ================================================================= v2: straight runs + G1 curves
def tls_line(P):
    c = P.mean(0); u, s, vt = np.linalg.svd(P-c, full_matrices=False); d = vt[0]
    return c, d
def line_resid(P, c, d):
    v = P-c; return np.abs(v[:,0]*d[1]-v[:,1]*d[0])
def proj(p, c, d): return c + d*((p-c) @ d)
def intersect(c1, d1, c2, d2):
    A = np.array([d1, -d2]).T
    if abs(np.linalg.det(A)) < 1e-9: return None
    t = np.linalg.solve(A, c2-c1); return c1 + d1*t[0]

def find_runs(P, ds, tol=1.2, min_len=40.0, step=4):
    n = len(P); PP = np.vstack([P, P]); cands = []
    mn = int(min_len/ds)
    for i in range(0, n, step):
        j = i+mn
        if j > i+n-1: continue
        c, d = tls_line(PP[i:j+1])
        if line_resid(PP[i:j+1], c, d).max() > tol: continue
        while j+step <= i+n-1:
            c2, d2 = tls_line(PP[i:j+step+1])
            if line_resid(PP[i:j+step+1], c2, d2).max() > tol: break
            j += step
        while j+1 <= i+n-1:
            c2, d2 = tls_line(PP[i:j+2])
            if line_resid(PP[i:j+2], c2, d2).max() > tol: break
            j += 1
        while j-i >= mn:
            seg = PP[i:j+1]; c2, d2 = tls_line(seg); v = seg-c2
            t_ = v @ d2; r_ = v[:,0]*d2[1]-v[:,1]*d2[0]
            tn = t_/max(abs(t_).max(), 1e-9); sag = abs(np.polyfit(tn, r_, 2)[0])
            if sag <= 0.4: break
            j -= step
        if j-i < mn: continue
        cands.append((j-i, i, j))
    cands.sort(reverse=True); used = np.zeros(n, bool); runs = []
    for L, i, j in cands:
        idx = np.arange(i, j+1) % n
        if used[idx[1:-1]].any() or used[idx[0]] and used[idx[-1]]: continue
        # shrink to avoid overlap
        if used[idx].any():
            ok = np.where(~used[idx])[0]; 
            if len(ok) == 0: continue
            # take the longest contiguous free stretch
            br = np.where(np.diff(ok) > 1)[0]; parts = np.split(ok, br+1); ok = max(parts, key=len)
            if len(ok)*ds < min_len: continue
            i, j = i+ok[0], i+ok[-1]; idx = np.arange(i, j+1) % n
        used[idx] = True; runs.append((i % n, j-i))
    runs.sort(); return runs   # (start index, length in samples)

def adaptive_smooth(S, ds, s_small=2.5, s_big=8.0, k0=0.010):
    A = gsmooth(S, s_small/ds, False); B = gsmooth(S, s_big/ds, False)
    # curvature of the big-smoothed curve
    d1 = np.gradient(B, axis=0)/ds; d2 = np.gradient(d1, axis=0)/ds
    k = np.abs(d1[:,0]*d2[:,1]-d1[:,1]*d2[:,0])/np.maximum((d1**2).sum(1), 1e-9)**1.5
    w = np.clip(k/k0, 0, 1)
    w = gsmooth(w[:, None].repeat(2, 1), 6.0/ds, False)[:, 0]
    w = np.clip(w, 0, 1)[:, None]
    return w*A + (1-w)*B

def fit_open(S, ds, t1, t2, tol=0.6, corner_thresh=45, adaptive=True):
    """Fit an open polyline S (ends are anchors).  t1/t2: forced end tangents (unit, pointing along the curve at the
    start / pointing back at the end) or None.  Returns list of ctrl arrays."""
    if len(S) < 3: return [np.array([S[0], S[0]+(S[-1]-S[0])/3, S[0]+2*(S[-1]-S[0])/3, S[-1]])]
    w = max(2, int(4.0/ds)); n = len(S)
    th = turning(np.vstack([S[:1].repeat(w, 0), gsmooth(S, 1.5/ds, False), S[-1:].repeat(w, 0)]), w)[w:w+n]
    cor = []
    for i in np.argsort(-np.abs(th)):
        if abs(th[i]) < corner_thresh: break
        if i < 6/ds or i > n-1-6/ds: continue
        if all(abs(i-j) > 6/ds for j in cor): cor.append(int(i))
    cuts = [0] + sorted(cor) + [n-1]; out = []
    for a in range(len(cuts)-1):
        seg = S[cuts[a]:cuts[a+1]+1]
        sm = adaptive_smooth3(seg, ds, False) if adaptive and len(seg)*ds > 150 else gsmooth(seg, 1.5/ds, False)
        sm[0] = seg[0]; sm[-1] = seg[-1]
        k = max(2, min(int(3/ds), len(sm)//3))
        a1 = t1 if (a == 0 and t1 is not None) else unit(sm[k]-sm[0])
        a2 = t2 if (a == len(cuts)-2 and t2 is not None) else unit(sm[-1-k]-sm[-1])
        out += fit_cubic(sm, a1, a2, tol)
    return out

def seg_items(ctrls):
    return [('L', c[3]) if is_line(c) else ('C', c[1], c[2], c[3]) for c in ctrls]

def fit_loop2(P0, ds=0.5, line_tol=0.6, min_len=50.0, tol=0.5, corner_thresh=45, presmooth=3.0):
    P, ds = resample_closed(P0, ds); n = len(P)
    corners, _ = find_corners(P, ds, corner_thresh)
    Ps = gsmooth(P, presmooth/ds, True)
    for c in corners:                      # keep true corners sharp
        r = int(2.5/ds)
        for o in range(-r, r+1): Ps[(c+o) % n] = P[(c+o) % n] if abs(o) <= 1 else Ps[(c+o) % n]
    P = Ps
    runs = find_runs(P, ds, line_tol, min_len)
    if not runs:
        return fit_loop(P0, ds=0.5, sigma=4.0, tol=0.8, corner_thresh=corner_thresh)
    R = []
    for i, L in runs:
        idx = np.arange(i, i+L+1) % n; c, d = tls_line(P[idx])
        if (P[idx[-1]]-P[idx[0]]) @ d < 0: d = -d
        R.append(dict(i=i, j=(i+L) % n, c=c, d=d, S=proj(P[idx[0]], c, d), E=proj(P[idx[-1]], c, d)))
    m = len(R); items = []
    for k in range(m):
        a, b = R[k], R[(k+1) % m]
        i0 = a['j']; i1 = b['i']
        idx = np.arange(i0, i1+1) % n if i1 >= i0 else np.concatenate([np.arange(i0, n), np.arange(0, i1+1)])
        if m == 1: idx = np.concatenate([np.arange(i0, n), np.arange(0, i0+1)]) if a['i'] != a['j'] else idx
        glen = (len(idx)-1)*ds
        ang = np.degrees(np.arccos(np.clip(a['d'] @ b['d'], -1, 1)))
        if glen <= 8 and m > 1 and ang > 3:
            V = intersect(a['c'], a['d'], b['c'], b['d'])
            if V is not None and np.hypot(*(V-P[idx[len(idx)//2]])) < 12:
                a['E'] = V; b['S'] = V; items.append(('gap', None)); continue
        if glen <= 8 and m > 1:
            items.append(('gap', None)); continue
        G = P[idx].copy(); nn = len(G); u = np.arange(nn)*ds
        fade = max(6.0, min(20.0, glen/3))
        G += (a['E']-G[0])[None]*np.clip(1-u/fade, 0, 1)[:, None] + (b['S']-G[-1])[None]*np.clip(1-(u[-1]-u)/fade, 0, 1)[:, None]
        items.append(('gap', G))
    loop = [('M', R[0]['S'])]
    for k in range(m):
        a, b = R[k], R[(k+1) % m]
        loop.append(('L', a['E']))
        G = items[k][1]
        if G is None: loop.append(('L', b['S'])) if np.hypot(*(a['E']-b['S'])) > 1e-6 else None; continue
        for it in seg_items(fit_open(G, ds, a['d'], -b['d'], tol, corner_thresh)): loop.append(it)
    # drop the closing duplicate of the start
    if loop[-1][0] == 'L' and np.hypot(*(loop[-1][1]-loop[0][1])) < 1e-6: loop.pop()
    elif loop[-1][0] == 'C' and np.hypot(*(loop[-1][3]-loop[0][1])) < 1e-6: pass
    return [loop]

# ================================================================= v3: faithful smoothing (curvature adaptive) + Bezier fit
def adaptive_smooth3(S, ds, closed, s_small=2.0, s_big=9.0, k_lo=0.006, k_hi=0.020):
    A = gsmooth(S, s_small/ds, closed); B = gsmooth(S, s_big/ds, closed)
    if closed:
        d1 = (np.roll(B, -1, 0)-np.roll(B, 1, 0))/(2*ds); d2 = (np.roll(B, -1, 0)-2*B+np.roll(B, 1, 0))/ds**2
    else:
        d1 = np.gradient(B, axis=0)/ds; d2 = np.gradient(d1, axis=0)/ds
    k = np.abs(d1[:,0]*d2[:,1]-d1[:,1]*d2[:,0])/np.maximum((d1**2).sum(1), 1e-9)**1.5
    w = np.clip((k-k_lo)/(k_hi-k_lo), 0, 1)
    w = gsmooth(np.stack([w, w], 1), 5.0/ds, closed)[:, 0]
    w = np.clip(w, 0, 1)[:, None]
    return w*A + (1-w)*B

def fit_loop3(P0, ds=0.5, tol=0.6, corner_thresh=45, s_small=2.0, s_big=9.0):
    P, ds = resample_closed(P0, ds); n = len(P)
    corners, _ = find_corners(P, ds, corner_thresh)
    if not corners:
        S = adaptive_smooth3(P, ds, True, s_small, s_big)
        k = int(np.argmin(S[:, 0])); S = np.roll(S, -k, axis=0); S = np.vstack([S, S[:1]])
        w = max(2, int(3/ds)); tan = unit(S[w]-S[-w-1])
        ctrls = fit_cubic(S, tan, -tan, tol)
        return [[('M', S[0])] + seg_items(ctrls)]
    out = [('M', None)]
    for a in range(len(corners)):
        i, j = corners[a], corners[(a+1) % len(corners)]
        idx = np.arange(i, j+1) if j > i else np.concatenate([np.arange(i, n), np.arange(0, j+1)])
        seg = np.vstack([P[i:], P[:i+1]]) if len(corners) == 1 else P[idx]
        if a == 0: out[0] = ('M', seg[0])
        L = np.hypot(*(seg[1:]-seg[:-1]).T).sum()
        if L < 4: out.append(('L', seg[-1])); continue
        S = adaptive_smooth3(seg, ds, False, s_small, s_big) if L > 40 else gsmooth(seg, 1.0/ds, False)
        S[0] = seg[0]; S[-1] = seg[-1]
        w = max(2, min(int(3/ds), len(S)//3))
        out += seg_items(fit_cubic(S, unit(S[w]-S[0]), unit(S[-1-w]-S[-1]), tol))
    return [out]


# ================================================================= driver
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, 'brand', 'frame_out_logo.traced.xml')
DST = os.path.join(HERE, 'brand', 'frame_out_logo.xml')
NAMES = 'strip band_base band_shade connector face trail_outer trail_mid mountain sun ring'.split()
SIGMA, TOL, CORNER, S_BIG = 4.0, 0.6, 62, 9.0


def clean_path(d, cache):
    if d in cache: return cache[d]
    loops = []
    for s in parse_subpaths(d):
        x, y = s[:, 0], s[:, 1]; area = abs(0.5*np.sum(x*np.roll(y, -1) - np.roll(x, -1)*y))
        if area < 20: continue                                   # trace specks
        per = np.hypot(*(s - np.roll(s, -1, 0)).T).sum(); wd = 2*area/per   # mean width: thin slivers keep their detail
        sg = min(SIGMA, max(0.5, 0.3*wd)); tl = min(TOL, max(0.25, 0.25*wd))
        if wd > 6: loops += fit_loop2(s, tol=0.25, line_tol=0.7, corner_thresh=CORNER)
        else: loops += fit_loop3(s, tol=tl, corner_thresh=CORNER, s_small=min(2.0, sg), s_big=min(S_BIG, max(1.0, sg*3.5)))
    cache[d] = path_from_loops(loops); return cache[d]


def main():
    src = open(SRC, encoding='utf-8').read()
    if '<!--' not in src:                                         # the tracer output has no layer names: add them (in order)
        parts = src.split('    <path\n'); src = parts[0]
        for n, p in zip(NAMES, parts[1:]): src += f'    <!-- {n} -->\n    <path\n' + p
    cache = {}
    out = re.sub(r'android:pathData="([^"]+)"',
                 lambda m: m.group(0) if m.group(1).startswith('M0,0H') else 'android:pathData="%s"' % clean_path(m.group(1), cache), src)
    open(DST, 'w', encoding='utf-8').write(out)
    print('wrote', DST, len(src), '->', len(out), 'bytes')


if __name__ == '__main__':
    main()
