#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Recalculation for zeigmal-case.scad.

Why this exists: OpenSCAD shows a preview, and a preview lies kindly. Whether
the slot's stop lands the sticker over the coil, whether the camera pocket
leaves a wall above the slot, whether an M2 x 10 still has thread in the foot
after the floor, or what push at the card's top tips the whole thing — none
of that shows in a preview. Recalculating does.

The script reads the dimensions FROM the .scad (sections 0 to 6) and
recalculates independently. It does not duplicate the numbers, it checks
them. Whoever changes a number in the .scad gets the result without opening
OpenSCAD:

    python3 case/verify.py
    python3 case/verify.py --coil 38 --coil-x 50   # try E4's answer before editing
    python3 case/verify.py --window 1.0            # try E5's answer
    python3 case/verify.py --bed 220 220 250

Exit code 0 = everything fine, 1 = at least one check failed.

The parser and the report harness are the talker's (vorlaut-diy-talker,
case/verify.py), trimmed to what this file needs.
"""

import argparse
import math
import os
import re
import sys

SCAD = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    'zeigmal-case.scad')


# ---------------------------------------------------------------- Parser

def _sin(d): return math.sin(math.radians(d))
def _cos(d): return math.cos(math.radians(d))


def load_parameters(path):
    """Pulls the scalar and list assignments out of the .scad file.

    Only up to the first `module`. `function` lines do not match the
    assignment regex. The two plane-frame helpers py()/pz() are mirrored
    here because derived values in section 6 use them.
    """
    with open(path, encoding='utf-8') as f:
        txt = f.read()
    txt = re.sub(r'/\*.*?\*/', '', txt, flags=re.S)
    txt = re.sub(r'//[^\n]*', '', txt)
    cut = re.search(r'^\s*module\s', txt, re.M)
    if cut:
        txt = txt[:cut.start()]

    ns = {'sqrt': math.sqrt, 'min': min, 'max': max, 'abs': abs,
          'ceil': math.ceil, 'floor': math.floor, 'pow': pow,
          'sin': _sin, 'cos': _cos, 'tan': lambda d: math.tan(math.radians(d)),
          'true': True, 'false': False}
    # mirrored from the .scad: a point (u, n) in the plane frame, in world (y, z)
    ns['py'] = lambda u, n: ns['y0'] + u * ns['s'] - n * ns['c']
    ns['pz'] = lambda u, n: ns['z0'] + u * ns['c'] + n * ns['s']
    ns['u_at'] = lambda z, n: (z - ns['z0'] - n * ns['s']) / ns['c']

    raw = {}
    for m in re.finditer(r'^\s*([a-zA-Z_$]\w*)\s*=\s*([^;]+);', txt, re.M):
        name, expr = m.group(1), ' '.join(m.group(2).split())
        if name in raw or name.startswith('$'):
            continue
        if any(k in expr for k in ('for (', 'concat(', 'len(', 'str(')):
            continue
        tern = re.match(r'^(.+?)\s*\?\s*(.+?)\s*:\s*(.+)$', expr)
        if tern:
            expr = '(%s) if (%s) else (%s)' % (tern.group(2), tern.group(1),
                                               tern.group(3))
        raw[name] = expr

    pending = dict(raw)
    for _ in range(60):
        if not pending:
            break
        before = len(pending)
        for name, expr in list(pending.items()):
            try:
                ns[name] = eval(expr, {'__builtins__': {}}, ns)
                del pending[name]
            except Exception:
                pass
        if len(pending) == before:
            break
    if pending:
        raise SystemExit('Not evaluable: %s' % ', '.join(sorted(pending)))
    return ns


# ---------------------------------------------------------- Check harness

class Report:
    def __init__(self):
        self.lines = []
        self.failed = 0

    def check(self, group, what, actual, op, target, unit='mm', note=''):
        cmp = {'>=': lambda a, b: a >= b - 1e-9,
               '<=': lambda a, b: a <= b + 1e-9,
               '>':  lambda a, b: a > b + 1e-9,
               '==': lambda a, b: abs(a - b) < 1e-6}[op]
        ok = cmp(actual, target)
        if not ok:
            self.failed += 1
        self.lines.append((group, what, actual, op, target, unit, ok, note))

    def info(self, group, what, text):
        self.lines.append((group, what, text, None, None, '', True, ''))

    def emit(self):
        last = None
        for group, what, actual, op, target, unit, ok, note in self.lines:
            if group != last:
                print('\n%s' % group)
                print('-' * len(group))
                last = group
            if op is None:
                print('  ·    %-46s %s' % (what, actual))
                continue
            mark = 'ok  ' if ok else 'FAIL'
            print('  %s %-46s %9.3f %-2s %9.3f %s%s'
                  % (mark, what, actual, op, target, unit,
                     '' if ok else '   <-- ' + note if note else ''))
        print()
        if self.failed:
            print('%d check(s) failed.' % self.failed)
        else:
            print('All checks passed.')
        return 1 if self.failed else 0


def multiple_of(x, q):
    return abs(x / q - round(x / q)) < 1e-6


# ---------------------------------------------------------- Recalculation

def compute(p, bed, phone_mass, fill):
    b = Report()
    G = lambda n: p[n]
    py, pz = p['py'], p['pz']

    # --- 1. The card in its slot ------------------------------------------
    g = '1. The card in its slot'
    channel = G('card_t') + G('sticker_t') + 2 * G('clr')
    b.check(g, 'channel matches the .scad', channel, '==', G('channel'))
    b.check(g, 'card never jams (channel - card - sticker)',
            channel - G('card_t') - G('sticker_t'), '>=', 0.4,
            note='a printer running fat and the card catches')
    b.check(g, 'sticker cannot wander off the coil',
            channel - G('card_t') - G('sticker_t'), '<=', 1.2,
            note='the card leans and the sticker tilts away')
    b.check(g, 'lead-in chamfer at the mouth', G('chamfer'), '>=', 0.4)
    b.check(g, 'no finger fits the slot', channel, '<=', 6.0)
    b.info(g, 'read path', '%.1f mm: window %.1f + clearance %.1f + sticker %.1f'
           % (G('win_t') + G('clr') + G('sticker_t'), G('win_t'), G('clr'),
              G('sticker_t')))
    b.check(g, 'plate = window + channel + rear wall', G('plate_t'), '==',
            G('win_t') + channel + G('back_t'))

    # --- 2. The stop puts the sticker over the coil ------------------------
    g = '2. Insertion depth - the stop puts the sticker over the coil'
    insert = G('coil_from_top') + G('sticker_y')
    b.check(g, 'insert matches the .scad', insert, '==', G('insert'))
    b.check(g, 'sticker centre lands on the coil centre (u)',
            abs((G('phone_h') - insert + G('sticker_y'))
                - (G('phone_h') - G('coil_from_top'))), '<=', 0.001,
            note='sticker_y and coil_from_top disagree with insert')
    b.check(g, 'sticker centre lands on the coil centre (x)',
            abs((G('slot_x0') + G('clr') + G('sticker_x')) - G('coil_x')), '<=', 0.001)
    b.check(g, 'whole sticker inside the slot at the stop',
            insert - (G('sticker_y') + G('sticker_d') / 2), '>=', 2.0,
            note='the sticker\'s top edge is above the phone\'s edge')
    b.check(g, 'card stands proud enough to take back',
            G('card_h') - insert, '>=', G('card_proud_min'),
            note='a child needs a fingernail')
    b.check(g, 'card stands proud (matches .scad)', G('card_h') - insert,
            '==', G('card_proud'))
    b.check(g, 'coil area inside the phone (x, left)',
            G('coil_x') - G('coil_r'), '>=', 0.0)
    b.check(g, 'coil area inside the phone (x, right)',
            G('phone_l') - G('coil_x') - G('coil_r'), '>=', 0.0)
    b.check(g, 'coil area inside the phone (u)',
            G('coil_from_top') - G('coil_r'), '>=', -G('coil_r'))
    b.info(g, 'E4 has not run', 'coil_x %.1f / coil_from_top %.1f are [A]'
           % (G('coil_x'), G('coil_from_top')))

    # --- 3. The slot inside the plate --------------------------------------
    g = '3. The slot inside the plate'
    b.check(g, 'left slot wall inside the plate',
            G('slot_x0') - G('wall') - G('plate_x_left'), '>=', 0.0,
            note='the plate follows the slot; this cannot fail unless plate_x_left was edited')
    b.check(g, 'right slot wall inside the plate',
            G('plate_x_right') - (G('slot_x0') + G('slot_w') + G('wall')), '>=', 0.0)
    past = max(0.0, -(G('slot_x0') + G('clr')))
    b.info(g, 'card past the phone\'s left end', '%.0f mm - the plate is %.0f mm wider than the frame there'
           % (past, G('x_left') - G('plate_x_left')))
    b.check(g, 'card does not stand past the phone by more than half its width', past, '<=', G('card_w') / 2,
            note='the coil is too near the end for a centred sticker')
    b.check(g, 'stop sits above the floor slab, one wall clear',
            G('slot_u0') - (G('plate_u_at_floor') + G('wall')), '>=', 0.0)
    b.check(g, 'funnel starts at the frame\'s top rail, not below it',
            G('mouth_u0') - (G('phone_h') + G('play') + G('frame_wall')), '>=', 0.0,
            note='the funnel would open into the frame')
    b.check(g, 'funnel front reaches the plate face (a ramp to slide down)',
            G('mouth_n_front'), '==', 0.0, unit='',
            note='raise mouth_flare_n to at least win_t')
    b.check(g, 'ramp is not a step (rise over run)', G('win_t') / G('mouth_ramp_h'), '<=', 0.6, unit='',
            note='a steep ramp is a step, not a funnel')
    b.check(g, 'plate edge above the ramp is not a knife',
            2.0 * G('win_t') / G('mouth_ramp_h'), '>=', 0.6,
            note='2 mm below the top the wall is thinner than 0.6 mm')
    b.check(g, 'rear wall kept behind the funnel', G('back_t'), '>=', 0.8)
    # the camera island's relief pocket is cut into the same plate the slot
    # is in. Where the two overlap, what is left between them is the
    # thinnest wall in the part.
    cam_over_slot = (G('cam_x1') > G('slot_x0') and G('cam_x0') < G('slot_x0') + G('slot_w')
                     and G('cam_u1') > G('slot_u0'))
    left = G('win_t') - G('cam_h') - G('cam_play')
    if cam_over_slot:
        b.check(g, 'wall left between camera pocket and slot', left, '>=', 0.8,
                note='two perimeters minimum - raise win_t or cam_play')
        b.info(g, 'note', 'the camera pocket lies over the slot: the read '
                          'path there is %.1f mm, not %.1f' % (left + G('clr') + G('sticker_t'),
                                                                G('win_t') + G('clr') + G('sticker_t')))
    else:
        b.info(g, 'camera pocket', 'clear of the slot')

    # --- 4. The frame holds the phone -------------------------------------
    g = '4. The frame - the phone stays in, the screen stays visible'
    b.check(g, 'lip covers border only, no pixels', G('frame_over'), '<=',
            G('screen_inset'), note='the frame hides picture')
    b.check(g, 'lip wide enough to hold', G('frame_over'), '>=', 1.5)
    b.check(g, 'play round the phone (never jams)', G('play'), '>=', 0.2)
    b.check(g, 'play round the phone (no rattle)', G('play'), '<=', 0.6)
    b.check(g, 'face is whole layers', 1.0 if multiple_of(G('frame_face'), 0.2) else 0.0,
            '==', 1.0, unit='')
    w0, w1 = G('key_vol_up')[0] - G('key_win_margin'), G('key_power')[1] + G('key_win_margin')
    b.check(g, 'key window inside the top rail (left)', w0 - G('x_left'), '>=', G('frame_wall'))
    b.check(g, 'key window clear of the right rail', G('phone_l') - w1, '>=', G('frame_wall'))
    b.info(g, 'key window', 'x %.0f..%.0f: volume keys and power key both reachable' % (w0, w1))
    # the closed right end: two openings, three posts between and around them
    u_lo, u_hi = -G('play') - G('frame_wall'), G('phone_h') + G('play') + G('top_wall')
    usb = (G('usb_u')[0] - G('port_margin'), G('usb_u')[1] + G('port_margin'))
    spk = (G('speaker_u')[0] - G('port_margin'), G('speaker_u')[1] + G('port_margin'))
    b.check(g, 'right end: post below the USB-C opening', usb[0] - u_lo, '>=', G('frame_wall'))
    b.check(g, 'right end: post between USB-C and speaker', spk[0] - usb[1], '>=', G('frame_wall'))
    b.check(g, 'right end: post above the speaker opening', u_hi - spk[1], '>=', G('frame_wall'))
    b.check(g, 'USB-C opening takes a plug (12 mm)', usb[1] - usb[0], '>=', 12.0)
    b.info(g, 'openings', 'USB-C u %.0f..%.0f, speaker u %.0f..%.0f' % (usb + spk))
    # two screws from the front through the top rail into the plate
    solid_from = G('slot_x0') + G('slot_w') + G('mouth_flare_x') + G('wall')
    for x in G('top_screw_x'):
        b.check(g, 'top screw at x=%.0f hits solid plate, past the funnel' % x, x - solid_from, '>=', 3.0,
                note='the screw would go into the slot')
        b.check(g, 'top screw at x=%.0f clear of the key window' % x, x - w1, '>=', G('top_cb_d'))
        b.check(g, 'top screw at x=%.0f inside the frame' % x, G('phone_l') - x, '>=', G('top_cb_d'))
    b.check(g, 'top rail takes the counterbore', G('top_wall') - G('top_cb_d'), '>=', 1.6,
            note='less than two perimeters beside the head')
    bite = G('top_bite')
    b.check(g, 'top screw bites the plate (2.5 x d)', bite, '>=', 2.5 * G('screw_d'))
    b.check(g, 'top screw stays inside the plate', G('plate_t') - bite, '>=', 0.3,
            note='the tip comes out of the back')
    b.check(g, 'top screw hole clear of the funnel\'s reach (u)',
            G('mouth_u0') - (G('phone_h') + G('play') + G('top_wall') / 2 + G('screw_tap_d') / 2), '>=', 0.0)

    # --- 5. Screws --------------------------------------------------------
    g = '5. Two M2 screws from underneath'
    floor_left = G('floor_t') - G('cb_depth')
    engage = G('screw_l') - floor_left
    b.check(g, 'floor left under the screw head', floor_left, '>=', 1.0,
            note='the head pulls through')
    b.check(g, 'thread in the foot (2.5 x d for PLA)', engage, '>=', 2.5 * G('screw_d'),
            note='a longer screw, or a thinner floor')
    # the foot's top at the screw: on the plane u = -play, at y = screw_y
    screw_y = (G('y_front') + py(G('plate_u_at_floor'), 0)) / 2
    n_at = (-G('play') * G('s') - screw_y) / G('c')
    foot_top = pz(-G('play'), n_at)
    b.check(g, 'screw stays inside the foot', foot_top - (floor_left + engage), '>=', 1.0,
            note='the screw tip comes out under the phone')
    b.check(g, 'screw stands on the slab (front)',
            screw_y - G('y_front'), '>=', G('screw_tap_d'))
    b.check(g, 'screw stands under the foot (back)',
            py(G('plate_u_at_floor'), 0) - screw_y, '>=', G('screw_tap_d'))
    for x in G('screw_x'):
        b.check(g, 'screw at x=%.0f clear of the rail/returns' % x,
                min(x - (G('x_left') + G('frame_wall')), G('phone_l') - x), '>=', 4.0)
    b.check(g, 'counterbore takes the head', G('screw_head_d') + 0.4, '>=',
            G('screw_head_d'))
    b.check(g, 'clearance hole > screw', G('screw_clear_d') - G('screw_d'), '>=', 0.2)
    b.check(g, 'tap hole < screw', G('screw_d') - G('screw_tap_d'), '>=', 0.15,
            note='1.8 for M2 in PLA is the usual pilot; the thread still bites')
    b.info(g, 'shopping', 'M2 x %.0f, %d pieces: %d from below, %d from the front'
           % (G('screw_l'), len(G('screw_x')) + len(G('top_screw_x')), len(G('screw_x')), len(G('top_screw_x'))))

    # --- 6. Printing ------------------------------------------------------
    g = '6. Printing - Ender 3 V2, 0.4 mm nozzle, 0.2 mm layers, PLA'
    body_l = G('plate_x_right') - G('plate_x_left')
    body_h = pz(G('u_top'), -G('plate_t'))
    b.check(g, 'body fits the bed (length)', body_l, '<=', bed[0])
    b.check(g, 'body fits the bed (depth)', G('base_depth'), '<=', bed[1])
    b.check(g, 'body fits the bed (height)', body_h, '<=', bed[2])
    frame_d = G('u_hi') + G('play') + G('frame_wall') + (G('z0') / G('c'))
    frame_l = G('x_right') - G('x_left')
    b.check(g, 'frame fits the bed', max(frame_l, frame_d), '<=', max(bed[0], bed[1]))
    for n in ('wall', 'win_t', 'back_t', 'frame_wall', 'rib_t'):
        b.check(g, '%s is whole perimeters (0.4)' % n,
                1.0 if multiple_of(G(n), 0.4) else 0.0, '==', 1.0, unit='')
    b.check(g, 'floor is whole layers (0.2)',
            1.0 if multiple_of(G('floor_t'), 0.2) else 0.0, '==', 1.0, unit='')
    b.check(g, 'plate underside overhang (no supports)', G('tilt'), '<=', 45.0, unit='deg')
    b.check(g, 'rear slope underside overhang (no supports)', 90.0 - G('slope_deg'), '<=', 45.0,
            unit='deg', note='steeper slope_deg')
    b.check(g, 'ridge holds the funnel and a wall', G('ridge_t') - (G('plate_t') + G('mouth_flare_n')), '>=', G('wall'))
    b.check(g, 'cavity stops below the funnel', G('mouth_u0') - G('cavity_u_top'), '>=', G('wall'))
    b.info(g, 'body', '%.1f x %.1f x %.1f mm, base down' % (body_l, G('base_depth'), body_h))
    b.info(g, 'frame', '%.1f x %.1f x %.1f mm, face down' % (frame_l, frame_d, G('face_n1')))
    b.info(g, 'with a card', '%.1f mm tall' % pz(G('slot_u0') + G('card_h'), -G('win_t') - channel))

    # --- 7. Stability -------------------------------------------------------
    g = '7. Stability - what tips it'
    # masses from the geometry: PLA at 1.24 g/cm3, thin walls solid, the
    # plate and the ridge at `fill` of solid (perimeters, top/bottom, infill) [A]
    rho = 1.24e-3  # g/mm3
    L = G('x_right') - G('x_left')
    u_lo = G('plate_u_at_floor')
    plate_m = (G('u_top') - u_lo) * G('plate_t') * L * rho * fill
    ridge_m = (G('u_top') - G('cavity_u_top')) * (2 * G('wall')) * L * rho   # hollow: back and top wall
    fy, fz = G('ridge_y'), G('ridge_z')
    slope_len = math.hypot(G('slope_y') - fy, fz)
    slope_m = slope_len * G('wall') * L * rho
    front_m = (py(u_lo, 0) - G('y_front')) * G('floor_t') * L * rho
    apron_m = 0.0
    cavity_a = 0.5 * (G('slope_y') - py(u_lo, -G('plate_t'))) * pz(G('cavity_u_top'), -G('plate_t'))
    ribs_m = cavity_a * G('wall') * (2 + len(G('rib_x'))) * rho * 0.8
    Lf = G('x_right') - G('x_left')
    frame_m = (Lf * (G('u_hi') + G('play') + G('frame_wall') + 8) * G('frame_face')
               + 2 * Lf * G('frame_wall') * G('face_n1') + 10 * Lf * G('frame_wall')) * rho
    u_mid = (u_lo + G('u_top')) / 2
    parts = [
        (plate_m, py(u_mid, -G('plate_t') / 2)),
        (ridge_m, py((G('u_top') + G('cavity_u_top')) / 2, -(G('plate_t') + G('ridge_t')) / 2)),
        (slope_m, (fy + G('slope_y')) / 2),
        (front_m, (G('y_front') + py(u_lo, 0)) / 2),
        (ribs_m, (py(u_lo, -G('plate_t')) + G('slope_y')) / 2),
        (frame_m, py(G('phone_h') / 2, G('phone_t') + G('play'))),
        (phone_mass, py(G('phone_h') / 2, G('phone_t') / 2)),
    ]
    m_body = plate_m + ridge_m + slope_m + front_m + ribs_m
    m_frame, m_phone = frame_m, phone_mass
    total = sum(m for m, _ in parts)
    y_cg = sum(m * y for m, y in parts) / total
    weight = total / 1000 * 9.81
    h_card = pz(G('slot_u0') + G('card_h'), -G('win_t') - channel)
    h_phone = pz(G('phone_h'), 0)
    back_lever = (G('y_back') - y_cg) / 1000
    front_lever = (y_cg - G('y_front')) / 1000
    tip_back_card = weight * back_lever / (h_card / 1000)
    tip_back_phone = weight * back_lever / (h_phone / 1000)
    tip_front_card = weight * front_lever / (h_card / 1000)
    b.info(g, 'mass', '%.0f g in all (body %.0f, frame %.0f, phone %.0f) [A]'
           % (total, m_body, m_frame, m_phone))
    b.info(g, 'centre of mass', 'y = %.0f mm, %.0f mm in front of the back edge'
           % (y_cg, G('y_back') - y_cg))
    # Numbers, not gates: the base is 110 mm by decision (ADR 0007), which
    # accepts that a sideways shove at the card's top tips the holder. What
    # must still hold is that pushing a card IN does not.
    b.info(g, 'push at the card top, backwards, tips at', '%.1f N' % tip_back_card)
    b.info(g, 'push at the phone top, backwards, tips at', '%.1f N' % tip_back_phone)
    b.info(g, 'pull at the card top, forwards, tips at', '%.1f N' % tip_front_card)
    # a card pushed in: mostly down the plane, a fifth of it backwards
    # A card is pushed in along its own plane: the backward part of that
    # push tips, the downward part presses the base onto the table in front
    # of the back edge and rights it. The push at which the two moments meet:
    y_top = py(G('slot_u0') + G('card_h'), -G('win_t') - channel)
    tip_arm = G('s') * h_card / 1000 - G('c') * (G('y_back') - y_top) / 1000
    push_tip = weight * back_lever / tip_arm if tip_arm > 0 else float('inf')
    b.check(g, 'pushing a card in tips only above', push_tip, '>=', 5.0, unit='N',
            note='a firm push on the card tips the holder')

    return b


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n\n')[0])
    ap.add_argument('--coil', type=float, help='coil_from_top to try (E4)')
    ap.add_argument('--coil-x', type=float, help='coil_x to try (E4)')
    ap.add_argument('--window', type=float, help='win_t to try (E5)')
    ap.add_argument('--bed', type=float, nargs=3, default=[220, 220, 250],
                    metavar=('X', 'Y', 'Z'), help='print volume, mm')
    ap.add_argument('--phone-mass', type=float, default=172, help='grams [R]')
    ap.add_argument('--fill', type=float, default=0.5,
                    help='the plate\'s printed density against solid [A]')
    a = ap.parse_args()

    src = open(SCAD, encoding='utf-8').read()
    if a.coil is not None:
        src = re.sub(r'^(coil_from_top\s*=\s*)[^;]+;', r'\g<1>%r;' % a.coil, src, flags=re.M)
    if a.coil_x is not None:
        src = re.sub(r'^(coil_x\s*=\s*)[^;]+;', r'\g<1>%r;' % a.coil_x, src, flags=re.M)
    if a.window is not None:
        src = re.sub(r'^(win_t\s*=\s*)[^;]+;', r'\g<1>%r;' % a.window, src, flags=re.M)
    tmp = SCAD + '.verify-tmp'
    with open(tmp, 'w', encoding='utf-8') as f:
        f.write(src)
    try:
        p = load_parameters(tmp)
    finally:
        os.remove(tmp)

    print('zeigmal-case.scad: coil at x %.1f / %.1f below the top, window %.1f, '
          'insert %.1f, %.1f proud'
          % (p['coil_x'], p['coil_from_top'], p['win_t'], p['insert'], p['card_proud']))
    return compute(p, a.bed, a.phone_mass, a.fill).emit()


if __name__ == '__main__':
    sys.exit(main())
