#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Checks the exported STL files against the dimensions from the .scad.

`verify.py` checks the numbers before OpenSCAD touches them. This script
checks what OpenSCAD made of them, and it exists because of one bug the
numbers could not have caught: the three openings in the frame's right rail
each cut the whole rail, and the rail between two of them ended up attached
to nothing. Two loose blocks, 12 vertices each, in a file that looked right
in every preview and printed as two little pieces rattling on the bed.

So the first thing it counts is shells.

    openscad -o /tmp/body.stl  -D 'part="body"'  case/zeigmal-case.scad
    openscad -o /tmp/frame.stl -D 'part="frame"' case/zeigmal-case.scad
    python3 case/check-stl.py /tmp/body.stl /tmp/frame.stl

Exit code 0 = everything fine, 1 = at least one check failed.
"""

import math
import os
import re
import struct
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from verify import SCAD, Report, load_parameters  # noqa: E402


def load_stl(path):
    """The triangles, as three (x, y, z) tuples each. ASCII or binary."""
    with open(path, 'rb') as f:
        d = f.read()
    if d[:5].lower() == b'solid' and b'facet' in d[:2000]:
        vs = [tuple(float(x) for x in m.groups())
              for m in re.finditer(rb'vertex\s+(\S+)\s+(\S+)\s+(\S+)', d)]
        return [tuple(vs[i:i + 3]) for i in range(0, len(vs), 3)]
    n = struct.unpack('<I', d[80:84])[0]
    tris, off = [], 84
    for _ in range(n):
        v = struct.unpack('<12f', d[off:off + 48])
        tris.append(((v[3], v[4], v[5]), (v[6], v[7], v[8]), (v[9], v[10], v[11])))
        off += 50
    return tris


def bbox(tris):
    xs = [v[0] for t in tris for v in t]
    ys = [v[1] for t in tris for v in t]
    zs = [v[2] for t in tris for v in t]
    return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))


def shells(tris, grid=1e-4):
    """How many pieces the solid falls into, largest first.

    Vertices are snapped to a grid before they are joined: OpenSCAD writes
    the same corner with the same bits from every triangle that meets there,
    but rounding keeps a stray last digit from splitting a solid in two.
    """
    parent = {}

    def key(v):
        return tuple(round(c / grid) for c in v)

    def find(a):
        while parent[a] != a:
            parent[a] = parent[parent[a]]
            a = parent[a]
        return a

    def union(a, b):
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[ra] = rb

    for t in tris:
        ks = [key(v) for v in t]
        for k in ks:
            parent.setdefault(k, k)
        union(ks[0], ks[1])
        union(ks[1], ks[2])
    sizes = {}
    for k in parent:
        r = find(k)
        sizes[r] = sizes.get(r, 0) + 1
    return sorted(sizes.values(), reverse=True)


def inside(tris, pt, eps=1e-7):
    """Is `pt` inside the solid? A ray straight up, crossings counted.

    Needed because not every downward-facing facet is an overhang: where two
    solids of the model meet, the exporter can leave the shared face in the
    file, and below it there is material, not air.
    """
    x, y, z = pt
    crossings = 0
    for a, b, c in tris:
        # does the triangle's shadow contain (x, y)?
        d1 = (x - b[0]) * (a[1] - b[1]) - (a[0] - b[0]) * (y - b[1])
        d2 = (x - c[0]) * (b[1] - c[1]) - (b[0] - c[0]) * (y - c[1])
        d3 = (x - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (y - a[1])
        if (min(d1, d2, d3) < -eps) and (max(d1, d2, d3) > eps):
            continue
        den = ((b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1]))
        if abs(den) < eps:
            continue
        l1 = ((b[1] - c[1]) * (x - c[0]) + (c[0] - b[0]) * (y - c[1])) / den
        l2 = ((c[1] - a[1]) * (x - c[0]) + (a[0] - c[0]) * (y - c[1])) / den
        l3 = 1 - l1 - l2
        zt = l1 * a[2] + l2 * b[2] + l3 * c[2]
        if zt > z:
            crossings += 1
    return crossings % 2 == 1


def span(tris, pt, reach=40.0, step=0.5):
    """How far the air at `pt` reaches, across the narrower of the two axes.

    A ceiling with a wall close by on both sides is a bridge and the printer
    handles it; one that runs out into the open is a ledge and it does not.
    Walks outward until it meets material or gives up at `reach`.
    """
    out = []
    for axis in (0, 1):
        widths = []
        for sign in (1, -1):
            d = step
            while d <= reach:
                probe = list(pt)
                probe[axis] += sign * d
                if inside(tris, tuple(probe)):
                    break
                d += step
            widths.append(d)
        out.append(sum(widths) if max(widths) <= reach else float('inf'))
    return min(out)


def overhangs(tris, min_area=2.0, limit=45.0, drop=0.3, bridge=25.0):
    """Downward-facing faces shallower than `limit` that are not bridges.

    Returns (area, degrees, centroid, span) per offending facet, largest
    first. `drop` is how far below the facet the air is tested — one layer
    and a half, so a facet that merely touches another solid does not count.
    A gap narrower than `bridge` between walls is left to the printer.
    """
    out = []
    for a, b, c in tris:
        u = (b[0] - a[0], b[1] - a[1], b[2] - a[2])
        v = (c[0] - a[0], c[1] - a[1], c[2] - a[2])
        n = (u[1] * v[2] - u[2] * v[1],
             u[2] * v[0] - u[0] * v[2],
             u[0] * v[1] - u[1] * v[0])
        area = math.sqrt(sum(k * k for k in n)) / 2
        if area < min_area or n[2] >= 0:
            continue
        deg = math.degrees(math.atan2(math.hypot(n[0], n[1]), -n[2]))
        if deg >= limit:
            continue
        cen = tuple((a[i] + b[i] + c[i]) / 3 for i in range(3))
        if cen[2] - drop <= 0:          # the first layer, on the bed
            continue
        under = (cen[0], cen[1], cen[2] - drop)
        if inside(tris, under):
            continue                    # material under it, not an overhang
        w = span(tris, under)
        if w <= bridge:
            continue                    # a wall on either side: the printer bridges it
        out.append((area, deg, cen, w))
    return sorted(out, reverse=True)


def check(path, p, b):
    g = os.path.basename(path)
    tris = load_stl(path)
    lo, hi = bbox(tris)
    size = tuple(hi[i] - lo[i] for i in range(3))
    sz = shells(tris)

    b.check(g, 'one piece, nothing loose', float(len(sz)), '==', 1.0, unit='',
            note='loose shells of %s vertices' % ', '.join(str(s) for s in sz[1:]))
    b.check(g, 'sits on the bed', lo[2], '==', 0.0,
            note='the part floats or digs in; it will not slice as modelled')
    b.info(g, 'size', '%.1f x %.1f x %.1f mm' % size)

    expect_x = p['plate_x_right'] - p['plate_x_left'] if 'body' in g else p['x_right'] - p['x_left']
    b.check(g, 'as long as the model says', size[0], '==', round(expect_x, 3))
    b.check(g, 'fits the bed (x)', size[0], '<=', 220.0)
    b.check(g, 'fits the bed (y)', size[1], '<=', 220.0)
    b.check(g, 'fits the bed (z)', size[2], '<=', 250.0)

    over = overhangs(tris)
    b.check(g, 'no ledge hangs in the air under 45 degrees', float(len(over)), '==', 0.0, unit='',
            note='%.0f mm2 at %.0f deg over a %.0f mm gap, around x=%.0f y=%.0f z=%.0f'
                 % (over[0][0], over[0][1], over[0][3], *over[0][2]) if over else '')
    for area, deg, cen, w in over[:5]:
        b.info(g, '  %.0f mm2 at %.0f deg' % (area, deg),
               'x %.0f  y %.0f  z %.0f, gap %.0f mm - support goes there' % (cen + (w,)))
    return b


def main():
    paths = sys.argv[1:]
    if not paths:
        raise SystemExit(__doc__.strip().split('\n\n')[-2])
    p = load_parameters(SCAD)
    b = Report()
    for path in paths:
        check(path, p, b)
    return b.emit()


if __name__ == '__main__':
    sys.exit(main())
