#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Recalculation for zeigmal-cards.scad — the box and the theme shelves.

The same job `verify.py` does for the holder, and it borrows that file's
parser and report: read the dimensions out of the .scad and work the
consequences out again, rather than trusting a preview.

What it is really here for: a shelf's length and what a child sees are the
same number. 88 mm of shelf buys one card on show, and the bed is 220 mm
long, so two is the most a shelf can carry. Every attempt to squeeze in a
third shows up here as a failed check rather than as a part that will not
slice.

    python3 case/cards-verify.py
    python3 case/cards-verify.py --cards 3      # try a third card on a shelf
    python3 case/cards-verify.py --bed 300 300 400

Exit code 0 = everything fine, 1 = at least one check failed.
"""

import argparse
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from verify import Report, load_parameters, multiple_of  # noqa: E402

SCAD = os.path.join(HERE, 'zeigmal-cards.scad')


def compute(p, bed, phone_free=True):
    b = Report()
    G = lambda n: p[n]

    # --- 1. A card in a compartment ---------------------------------------
    g = '1. The box - a card in its compartment'
    air = G('box_inner_w') - G('card_w')
    b.check(g, 'air beside a card, both sides together', air, '>=', 2.0,
            note='the card binds on the walls')
    b.check(g, 'and not so much that it leans over', air, '<=', 6.0)
    b.check(g, 'a compartment holds a theme', G('box_comp') / G('pitch'), '>=', 20.0,
            unit='cards', note='too few cards for a theme to be worth a compartment')
    b.check(g, 'the cards stand proud enough to grab',
            G('card_h') - (G('box_h') + G('floor_t')), '>=', 40.0,
            note='a child cannot get hold of them; this is what the scoops were for')
    b.check(g, 'and not so proud that they fall over',
            G('card_h') - (G('box_h') + G('floor_t')), '<=', 70.0)
    b.info(g, 'the box', '%d compartments of %d cards = %d in all'
           % (G('box_comps'), G('box_comp') / G('pitch'), G('box_cards')))

    # --- 2. The box as a printed thing ------------------------------------
    g = '2. The box - printed'
    b.check(g, 'inner rounding leaves a wall', G('corner_r') - G('wall'), '>=', 1.2,
            note='the corner is thinner than the wall it is part of')
    b.check(g, 'divider is a whole number of perimeters',
            1.0 if multiple_of(G('wall'), 0.4) else 0.0, '==', 1.0, unit='')
    b.check(g, 'floor is a whole number of layers',
            1.0 if multiple_of(G('floor_t'), 0.2) else 0.0, '==', 1.0, unit='')
    b.check(g, 'fits the bed (length)', G('box_l'), '<=', bed[0])
    b.check(g, 'fits the bed (depth)', G('box_w'), '<=', bed[1])
    b.check(g, 'fits the bed (height)', G('box_h') + G('floor_t'), '<=', bed[2])
    b.info(g, 'the box', '%.1f x %.1f x %.1f mm' % (G('box_l'), G('box_w'),
                                                    G('box_h') + G('floor_t')))

    # --- 3. The shelf shows whole cards -----------------------------------
    g = '3. The shelf - whole cards, side by side'
    b.check(g, 'no card hides any part of another',
            G('sh_pitch') - G('card_w'), '>=', 4.0,
            note='they overlap; the children asked for whole cards')
    b.check(g, 'and they are not strangers either',
            G('sh_pitch') - G('card_w'), '<=', 20.0,
            note='a gap that wide wastes the bed a card could have used')
    b.check(g, 'the shelf fits the bed', G('sh_l') + G('tab_w'), '<=', bed[0],
            note='88 mm per card is the price of not overlapping: at %d cards '
                 'the shelf is %.0f mm' % (G('sh_n'), G('sh_l') + G('tab_w')))
    b.info(g, 'on show', '%d cards, %.0f mm each, on a %.1f mm shelf'
           % (G('sh_n'), G('card_w'), G('sh_l')))
    b.info(g, 'in the slots', '%d cards each, %d in the shelf'
           % (G('sh_cards_per_slot'), G('sh_n') * G('sh_cards_per_slot')))

    # --- 4. The slot holds the card ---------------------------------------
    g = '4. The shelf - the slot'
    b.check(g, 'the slot takes the card and some spares',
            G('sh_slot') / G('pitch'), '>=', 3.0, unit='cards')
    b.check(g, 'deep enough to hold a card upright', G('sh_depth'), '>=', 10.0,
            note='a 120 mm card in a shallower slot flops about')
    b.check(g, 'and it stops short of the foot',
            G('sh_bar_h') - G('sh_depth') - G('floor_t'), '>=', 1.0,
            note='the slot breaks through into the foot and the card drops out')
    b.check(g, 'wall left in front of the slot, at its thinnest',
            G('sh_front_wall'), '>=', 2.0,
            note='the name plate leans back into the slot')
    b.check(g, 'wall left behind the slot',
            G('sh_bar_d') - G('sh_slot_y') - G('sh_slot'), '>=', G('wall'))
    b.check(g, 'the card leans back, so it stays put', G('sh_lean'), '>=', 5.0,
            unit='deg', note='upright, it falls out forwards')
    b.check(g, 'but not so far that the row grows a tail', G('sh_lean'), '<=', 25.0,
            unit='deg')

    # --- 5. The name plate, and printing ----------------------------------
    g = '5. The shelf - printed'
    b.check(g, 'the name plate leans back, so it is not an overhang',
            G('plate_deg'), '>', 0.0, unit='deg')
    b.check(g, 'and it is a plate, not a chamfer', G('sh_plate_h'), '>=', 14.0,
            note='too little to write a theme on')
    b.info(g, 'name plate', '%.1f mm tall, leaning back %.0f degrees'
           % (G('sh_plate_h'), G('plate_deg')))
    b.check(g, 'nothing else overhangs', 90.0 - G('plate_deg'), '>=', 45.0, unit='deg')
    b.check(g, 'the window leaves a rim', G('window_rim'), '>=', G('wall'))
    b.check(g, 'and the window stays behind the bar',
            G('sh_base') - G('sh_bar_y') - G('sh_bar_d') - 2 * G('window_rim'), '>=', 4.0,
            note='nothing left of the foot to lighten; drop the window')
    b.check(g, 'the window clears the socket',
            G('window_rim'), '>=', G('tab_clr') + 1.0)

    # --- 6. A row of shelves ----------------------------------------------
    g = '6. A row of shelves'
    b.check(g, 'the tab fits across the foot', G('tab_d'), '<=', G('sh_base') - 2 * G('wall'))
    b.check(g, 'the tab is long enough to hold', G('tab_w'), '>=', 8.0,
            note='a shorter tongue pulls out when a card is taken')
    b.check(g, 'the socket is a slip fit', G('tab_clr'), '>=', 0.2)
    b.check(g, 'and not a rattle', G('tab_clr'), '<=', 0.5)
    b.check(g, 'the tab is flush with the foot', G('floor_t'), '>=', 2.0,
            note='a tab thinner than this snaps')
    b.info(g, 'three shelves', '%d cards across %.0f mm of table'
           % (3 * G('sh_n'), 3 * G('sh_l') + 2 * G('tab_w')))

    # --- 7. Stability ------------------------------------------------------
    g = '7. Stability - a child pulling a card'
    rho = 1.24e-3            # PLA, g/mm3
    paper = 1.0e-3           # the laminated card, g/mm3 [A]
    # the shelf's own section, by the shoelace on the profile in the .scad
    pts = [(0, 0), (G('sh_base'), 0), (G('sh_base'), G('floor_t')),
           (G('sh_bar_y') + G('sh_bar_d'), G('floor_t')),
           (G('sh_bar_y') + G('sh_bar_d'), G('sh_bar_h')),
           (G('sh_bar_y') + G('sh_plate_back'), G('sh_bar_h')),
           (G('sh_bar_y'), G('floor_t')), (0, G('floor_t'))]
    area = abs(sum(pts[i][0] * pts[(i + 1) % len(pts)][1]
                   - pts[(i + 1) % len(pts)][0] * pts[i][1]
                   for i in range(len(pts)))) / 2
    m_shelf = area * G('sh_l') * rho * 0.75        # [A] the slots and the window
    m_cards = G('sh_n') * G('sh_cards_per_slot') * G('card_w') * G('card_h') * G('card_t') * paper
    # where the mass sits, measured back from the shelf's front edge
    y_shelf = G('sh_base') / 2
    lean = math.radians(G('sh_lean'))
    y_cards = (G('sh_bar_y') + G('sh_slot_y') + G('sh_slot') / 2
               + G('card_h') / 2 * math.sin(lean))
    total = m_shelf + m_cards
    y_cg = (m_shelf * y_shelf + m_cards * y_cards) / total
    weight = total / 1000 * 9.81
    h_card = G('sh_bar_h') + G('card_h') * math.cos(lean)
    b.info(g, 'mass', '%.0f g: shelf %.0f, a full load of cards %.0f [A]'
           % (total, m_shelf, m_cards))
    b.check(g, 'the loaded shelf does not tip backwards on its own',
            G('sh_base') - y_cg, '>=', 8.0,
            note='the cards lean further back than the foot reaches')
    # Both lengths in mm, so they cancel: F = W * y_cg / h.
    tip_fwd = weight * y_cg / h_card
    b.check(g, 'a pull at the top of a card tips it only above', tip_fwd, '>=', 0.5,
            unit='N', note='a child taking a card drags the shelf over')
    b.info(g, 'tipping', 'forwards at %.1f N, with the mass %.0f mm back of the front edge'
           % (tip_fwd, y_cg))
    return b


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n\n')[0])
    ap.add_argument('--cards', type=int, help='cards on show per shelf (sh_n)')
    ap.add_argument('--compartments', type=int, help='compartments in the box')
    ap.add_argument('--bed', type=float, nargs=3, default=[220, 220, 250],
                    metavar=('X', 'Y', 'Z'), help='print volume, mm')
    a = ap.parse_args()

    src = open(SCAD, encoding='utf-8').read()
    if a.cards is not None:
        src = re.sub(r'^(sh_n\s*=\s*)[^;]+;', r'\g<1>%d;' % a.cards, src, flags=re.M)
    if a.compartments is not None:
        src = re.sub(r'^(box_comps\s*=\s*)[^;]+;', r'\g<1>%d;' % a.compartments,
                     src, flags=re.M)
    tmp = SCAD + '.verify-tmp'
    with open(tmp, 'w', encoding='utf-8') as f:
        f.write(src)
    try:
        p = load_parameters(tmp)
    finally:
        os.remove(tmp)

    print('zeigmal-cards.scad: box %d x %d cards, shelf %d on show of %d'
          % (p['box_comps'], p['box_comp'] / p['pitch'], p['sh_n'],
             p['sh_n'] * p['sh_cards_per_slot']))
    return compute(p, a.bed).emit()


if __name__ == '__main__':
    sys.exit(main())
