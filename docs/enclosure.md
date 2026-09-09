# Enclosure

Nothing here is a drawing. This is the list that has to be ticked before any
CAD, in the order the measurements depend on each other, plus what the family's
two existing printed cases already settle.

## The shape, in words

```text
             ┌─────────┐
             │ METACOM │   the upper part of the card stays visible
             │  SYMBOL │
             └────┬────┘
                  │  vertical slot, behind the phone's back edge
          ┌───────┴───────┐
         ╱   Galaxy A51    ╲   landscape, tilted back ~15–25° [K]
        │    sign video     │
        └──────────────────┘   base wide enough not to tip when a card is pushed in
```

## Checklist, in dependency order

**A. The phone.** Width, height, depth with calipers, with the case it will
actually wear (or without — decide first). Where the camera island stands
proud. Where the USB-C port and the speaker are in the chosen landscape
orientation.

**B. The coil (E4).** Move one sticker over the back of the phone with the
diagnostics screen open and mark the area where `seen` appears every time. The
centre of that area is the one coordinate the slot is designed around. Record
it as an offset from the phone's top edge and from its long centre line.

**C. Material (E5).** Print three flat coupons, 1 / 2 / 3 mm, PLA and PETG.
Card + sticker + coupon between the phone and the sticker; note the largest
thickness that still reads every time at the coil centre, and how far off
centre it still reads through each. That decides the wall at the coil: a
uniform 2.4 mm wall, or a 1 mm window in it.

**D. The card.** Width, height, thickness of a SIGNbox 1 card (the 25 × 25 mm
velcro cards in `card-case.scad` are a different family). Then:

- slot width = card width + 2 × 0.3 mm per side, 0.8 mm lead-in chamfer — the
  family's numbers for a moving part (`card-case.scad`, the talker's key caps);
  loose enough never to jam, tight enough that the sticker cannot wander off
  the coil
- insertion depth = whatever puts the sticker over the coil centre with the
  card resting on a positive stop, and leaves the symbol visible above the
  phone; wochenwerk asks for 45–60 mm standing proud so a child can take the
  card back without a fingernail
- whether the card leans against the phone-facing wall (shortest read
  distance) or is guided by rails
- the sticker's position on the card follows from this and is then
  standardised for all cards

**E. Charging.** Which long side the port ends up on; cable bend radius; a
right-angle USB-C cable is likely; the exit is at the back or the side, never
where a child pulls. Whether the holder can charge with the cable never
unplugged.

**F. Stability and safety.** Tilt angle, base depth, rubber feet, tip
resistance when a card is pushed in from the front; no exposed electronics; the
speaker and the microphone free; no gap a finger fits.

**G. Printing.** FDM, 0.4 mm nozzle, 0.2 mm layers, 3 perimeters, walls a
multiple of 0.4 mm (2.4 mm outer, as both existing cases), PLA or PETG, no
ABS near a small child, no supports if the slot prints upright.

## What the existing cases already settle

- `~/Code/card-case/card-case.scad`: `wall = 2.0`, `shell = floor = 2.4`,
  `clr = 0.3` per side marked "the one to tune", `chamfer = 0.8`, a
  `part = "test"` render of one slot, and an `echo()` of the derived numbers.
- `vorlaut-diy-talker/case`: every dimension tagged `[M] [R] [A] [K] [G]`,
  a `verify.py` that recomputes from the `.scad`, a `check-stl.py` that probes
  the exported geometry, and a `building.md` with a "measure first" table of
  remaining assumptions. That is the shape zeigmal's `case/` takes when it
  exists.

## Print a fit test before a holder

One slot and one phone cradle, nothing else. `card-case` and the talker case
both did this; it is the cheapest print in the project.
