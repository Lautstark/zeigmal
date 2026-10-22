# Building the card furniture

Two things that have nothing to do with the phone: a box the cards live in,
and small shelves that put a theme where a child can see it and take it. Both
are in [`zeigmal-cards.scad`](zeigmal-cards.scad), built for the same card and
the same printer as the holder next door. ADR 0010 says why they are shaped
the way they are.

| Part | What it does | Size | Volume |
|---|---|---|---|
| **Box** | four compartments, one theme each, 48 cards per compartment | 204 × 87.8 × 73 mm | 191 cm³ |
| **Shelf** | two cards standing side by side, eight behind each | 184.8 × 69 × 18 mm | 38 cm³ |

```bash
openscad -o box.stl   -D 'part="box"'   case/zeigmal-cards.scad
openscad -o shelf.stl -D 'part="shelf"' case/zeigmal-cards.scad
python3 case/cards-verify.py
python3 case/check-stl.py --scad case/zeigmal-cards.scad box.stl shelf.stl
```

`part="row"` shows three shelves clipped together, `part="all"` the box and a
row beside it. `show_cards=false` leaves the dummy cards out.

## The box

Four compartments, open at the top, 192 cards in all. The cards stand 47 mm
proud, which is what a hand gets hold of — the first drawing had a thumb scoop
in each compartment and it was taken out again, because with that much card
standing out there is nothing for a scoop to do and it only weakens the wall.

The upright corners are rounded 5 mm, the same radius the station's top edge
has, so the two look like they belong together.

## The shelf

A flat bar with two slots, a foot, and a name plate. It prints in one piece,
on its foot, in about three hours.

**Two cards, and that is the ceiling.** The children wanted whole cards, not
overlapping ones, so a card costs its own width plus air: 88 mm. Two of them
plus the walls make a 173 mm shelf, and with the tab 185 mm. A third would be
273 mm and the bed is 220. This is the one number in the file that cannot be
turned up, and `cards-verify.py --cards 3` says so rather than leaving you to
find out in the slicer.

**The slots hold eight cards each.** One stands on show, the rest wait behind
it. So a shelf is a theme: sixteen cards, two of them visible.

**The name plate** is the bar's front face, leaning back 12° — 15 mm of flat
surface for a word or a symbol. It leans back because a vertical face would be
flat on the bed and a forward-leaning one would be an overhang; leaning back,
each layer is smaller than the one below and it needs no support.

**The tab and the socket** make a row. A shelf alone is light: a pull at the
top of a card tips it at 0.6 N, which is not much. Clipped into a row of
three, the same pull has three times the mass to lift. The foot also reaches
25 mm in front of the bar for this reason — without that it was 0.3 N, the
weight of a sheet of paper.

## Printing

The same settings as the holder, and neither part needs support.

| Setting | Box | Shelf |
|---|---|---|
| Layer height | 0.2 mm | 0.2 mm |
| Perimeters | 3 | 3 |
| Infill | 15 % | 20 % |
| Brim | 5 mm | none |
| Orientation | as it comes from the file | as it comes from the file |
| Time | ~16 h | ~3 h |

The box is the long print; the shelves are an afternoon each, which is the
point of there being several of them.

`check-stl.py` walks the exported files for loose pieces and for anything
hanging in the air shallower than 45°. The shelf's name plate at 12° is the
only slope either part has, and it leans the right way.

## Measure first

| Variable | Assumed | Check |
|---|---|---|
| `card_w` | 80 mm | measured 2026-09-14 |
| `card_h` | 120 mm | still from the box the set came in |
| `card_t` | 0.7 mm | ten cards with calipers, divided by ten |
| `pitch` | 1.0 mm | how thick a card really is in a stack — 48 per compartment and 8 per slot both follow from it |

If `pitch` is wrong the capacities are wrong and nothing else is; if `card_w`
is wrong the shelf is the wrong length and the compartments are too tight.
