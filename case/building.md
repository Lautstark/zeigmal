# Building the holder

Two printed parts, two M2 screws, an evening's printing. All the dimensions
are in [`zeigmal-case.scad`](zeigmal-case.scad); this file explains what to do
with them.

> **Not built yet.** The phone has not been measured and the coil has not
> been found. Everything below is calculated, and the numbers that decide the
> slot are assumptions until E4 and E5 in `docs/experiments.md` have run. Do
> the [measure first](#measure-first) section before the body is printed.

## The two parts

| Part | What it does | Outer size |
|---|---|---|
| **Body** | the block that leans 20° back: a front apron, the plate with the card slot in its top, a 50° rear slope, a rear apron; hollow under the slope with two ribs | 164.3 × 110 × 82.4 mm |
| **Frame** | the picture frame over the phone, open at the port end; lip 2 mm over the screen, rails round three edges, a foot the screws bite into | 164.3 × 79 × 9.8 mm |

Plus M2 × 10, two pieces, from underneath.

```bash
openscad -o body.stl  -D 'part="body"'  case/zeigmal-case.scad
openscad -o frame.stl -D 'part="frame"' case/zeigmal-case.scad
```

`part="assembly"` shows everything together with a dummy phone and card,
`part="exploded"` pulls the frame and the phone forward, `part="printbed"`
lays both parts out as they print.

## How it holds the phone

The body's plate is flat; nothing on it locates the phone. The frame does all
of that: its left rail, top rail and foot are the pocket's three walls, the two
corner returns at the port end are the fourth, and the 2 mm lip over the screen
keeps the phone from lifting out. The frame is pulled onto the body by two
screws from below, so from the front there is nothing to find. To take the
phone out: turn the holder over, two screws, lift the frame, lift the phone.

The top rail has a window over each volume key, so an adult can still change
the volume, and over the power key it is only hollowed by 1 mm: the key is
neither pressed by the rail nor reachable through it. A pressed power key
would switch NFC off with the screen (`docs/hardware.md`).

The port end is open. USB-C, speaker, microphone and jack all sit on that edge
and nothing of the holder is in front of them; the returns cover the 5 mm at
each corner only.

## How it holds the card

The slot is a pocket in the plate right behind the phone: window wall, a
1.5 mm channel, rear wall. The card leans on the rear wall (gravity, the
channel leans back) and reads through the window wall, the clearance and its
own sticker — 2.9 mm from the back glass to the sticker face with the numbers
as they stand.

The stop is not chosen. It is where a sticker 30 mm above the card's bottom
edge lands on the coil: `insert = coil_from_top + sticker_y`. With the coil
15 mm below the phone's top edge that is 45 mm in and 75 mm standing proud. A deeper coil takes more of the card in; `python3 case/verify.py --coil
38` shows what.

The mouth is a funnel: the plate rises 15 mm above the frame's top rail, and
over that height the slot opens to 93 mm wide and 9 mm front to back. At the
front the last 6 mm fall away to the plate's face, so a child can put the
card flat against the wall above the phone, slide it down, and the funnel
finds the slot. That is the gesture; the funnel is what makes it work.

The sticker goes on the width centre line of every card, so a card pushed in
back to front still reads. Upside down it does not, and the upside-down
symbol is the child's own cue.

## Printing

| Setting | Value | Why |
|---|---|---|
| Printer | Ender 3 V2, 220 × 220 × 250 | the body is 164 × 110, flat on the bed |
| Nozzle | 0.4 mm | every wall is a whole multiple of it |
| Layer height | 0.2 mm | the floor slab and the frame's face are whole multiples |
| Perimeters | 3 | 2.4 mm walls are then solid |
| Bottom / top | 5 / 5 layers | the floor slab carries the screw heads |
| Infill | 20 % grid | the plate is 6.3 mm and mostly infill; more gains nothing |
| Material | PLA | the only filament there is; no ABS near a small child |
| Supports | **off**, both parts | see orientation |
| Brim | 5 mm on the body | 164 mm of flat floor tends to lift |

### Orientation on the bed

**Body: on its base**, as modelled. The plate's underside is then a 70° wall
and the rear slope's underside a 50° one; neither needs support (45° is the
limit, `verify.py` checks both). The slot opens upward, the camera relief
pocket is a recess in a steep wall, the screw counterbores are holes in the
first layers. The hollow under the slope is open, so there is nothing to
bridge. The two aprons are 3 mm slabs and print in minutes; they are there
for stability, not strength.

**Frame: face down**, which is `part="printbed"`'s orientation and the
mirror of how it sits on the body. The face is then the first 8 layers and
gets the bed's finish; the rails and the foot stand up from it. The tap holes
run at 70° through the foot and print as slanted holes, which is fine at
1.6 mm.

### What can go wrong

- **The card catches at the mouth.** The 0.8 mm chamfer is the lead-in; if
  the printer rounds it off, `chamfer = 1.2`.
- **The card rattles.** `clr` is 0.3 per side — the same number as the
  card-case tray. Do not tune the printer here; change `clr` and reprint the
  fit test.
- **The frame does not go on.** `play` is 0.3 round the phone. The phone's
  real length decides this; measure it (below) before blaming the number.
- **The screw strips.** 1.6 mm tap hole for M2 in PLA is standard; drive by
  hand, not by drill, and stop when it seats.

## Tolerances

All clearances are named variables in section 3 of the `.scad`.

| Where | Variable | Value | Meant for |
|---|---|---|---|
| The card in the slot | `clr` | 0.30 mm per side | never jams, sticker stays on the coil — **the one to tune** |
| The phone in the frame | `play` | 0.30 mm per side | never jams, no rattle |
| The camera island in its pocket | `cam_play` | 0.30 mm | what is left of the wall under the pocket is the thinnest place in the part |
| Frame lip over the screen | `frame_over` | 2.00 mm | inside the phone's own black border |
| Volume window | `key_win_margin` | 2.00 mm each side | a fingertip finds the keys, and a guessed position still fits |

If the printer generally prints fat, do **not** fiddle here but calibrate the
extrusion multiplier. These numbers are design dimensions, not printer
corrections.

## Screws

Two **M2 × 10 pan head**, from underneath. The head sinks 1.6 mm into the
floor slab, 1.4 mm of floor stays under it, and 8.6 mm of thread go into the
frame's foot — 2.5 × d is the rule for PLA, 5 mm, so there is margin. The
tap hole is 1.6 mm; the screw cuts its own thread.

Why screws and not a snap fit: the point of the frame is that a child cannot
open it, and a snap fit is a thing a child opens. Why from underneath: nothing
to see, nothing to pick at, and the holder has to be turned over — with the
phone in it — to reach them, which nobody does by accident.

## Assembly

1. Check the body's slot with a card before anything else: it should slide to
   the stop and out again with no catch. If not, this is the fit test's job
   (below), not the body's.
2. Lay the phone on the plate, port to the right, camera island into its
   pocket. Volume keys up.
3. Put the frame over it: left rail against the phone's left end, foot under
   its bottom edge. The corner returns go round the right corners.
4. Turn the whole thing over, holding the frame on. Two M2 × 10 into the
   holes in the floor slab, by hand, until they seat.
5. Turn it back. Plug the cable in when needed; it is not routed.

## Measure first

These numbers are assumptions. If they are wrong, the design changes — in part
considerably. Every one is a variable in section 1 or 2 of the `.scad`, and
`verify.py` recalculates everything from them.

### Where the coil is (E4)

**The two numbers the slot is computed from.** `coil_x` from the phone's
left end, `coil_from_top` below the top edge, both in landscape with the port
to the right. The coil is in the corner by the camera, so the slot sits in
that corner and the card stands about 20 mm past the phone's left end; the
plate follows the card there and is that much wider than the frame. What is
still missing is the *centre* of the area that reads reliably, and a 25 mm
sticker forgives about 10 mm of error, not more. Tape a 10 mm grid to the back, touch a sticker to every square
with the diagnostics screen open, mark the squares that read 5 of 5, take the
centre. Half an hour, no printing.

```
python3 case/verify.py --coil 38 --coil-x 50
```

tries a result before it is written into the file. Enter the numbers, and the
slot's position, its depth and the card's proud height follow.

If the coil is more than about 28 mm below the top edge the card goes in
deeper than 48 mm and less than 72 mm stand proud — still fine. If it is more
than 45 mm down, the card goes in more than halfway and the one-third rule is
gone; say so in the ADR before printing.

### Through what it reads (E5)

`win_t`, the slot's front wall. Three coupons, `part="coupons"`, 1 / 2 /
3 mm PLA. The largest thickness that reads 10 of 10 at the coil centre is
the answer; 2.4 is assumed. `--window 1.0` previews a thinner wall. Under
the camera pocket the wall is `win_t - cam_h - cam_play` and `verify.py`
refuses less than 0.8 mm there.

### The phone

| Variable | Assumed | Check |
|---|---|---|
| `phone_l`, `phone_h`, `phone_t` | 158.9 × 73.6 × 7.9 | calipers, bare phone; the frame's pocket is these plus 0.3 |
| `screen_inset` | 2.5 mm | the black border round the picture; the lip covers 2.0 of it |
| `cam_x0..cam_u1`, `cam_h` | 8–30 × 45.6–67.6, 1.2 proud | the camera island in landscape; its pocket in the plate |
| `key_vol_up`, `key_vol_dn`, `key_power` | 28–38, 40–48, 56–66 from the left end | one window over both volume keys (26–50), a relief over the power key (53–69); the first corner's rail reached 2 mm over the volume key, hence 28 |
| `key_proud` | 0.5 mm | the power relief must be deeper than this |
| `jack_u`, `usb_u`, `speaker_u` | 8–14, 30–40, 55–68 from the bottom | the corner returns must not cover the jack or the speaker |

### The card

| Variable | Assumed | Check |
|---|---|---|
| `card_w`, `card_h` | 80 × 120 | the box says 8 × 12 cm; measure three cards |
| `card_t` | 0.7 mm | with laminate; a stack of ten with calipers, divided by ten |
| `sticker_t` | 0.2 mm | a stuck sticker over the card, minus the card |

### Stability

`verify.py` estimates the sideways push at the card's top that tips the
holder, from masses it works out of the geometry (the plate at half density,
the walls solid, the phone at 172 g). It is a light push — about 1.5 N
backwards, under 1 N forwards — because PLA is light and a card standing
80 mm proud is a lever. The base stays 110 mm deep anyway (ADR 0007): the
children push cards in, which does not tip it, and do not shove them. If that
turns out wrong, the hollow under the slope is where ballast goes — a strip
of steel taped in, coins in a bag — and `base_depth` is one number. Four
self-adhesive rubber feet at the corners; they stop sliding, not tipping.

## The fit test

```bash
openscad -o fit-test.stl     -D 'part="fit-test"'     case/zeigmal-case.scad
openscad -o frame-corner.stl -D 'part="frame-corner"' case/zeigmal-case.scad
```

The fit test is the slot and the plate around it, cut off level 40 mm above
the table so it stands on the cut at the real tilt; the foot and the screw are
the frame corner's job. For both test parts two walls and 15 % infill are
enough; the body's three walls are for the phone's weight, not for a test. The frame corner is the
left 40 mm of the frame, full height, with its screw hole in the foot and
the end of the top rail, already lying face down; twenty minutes. Together
they try every clearance in the design:

1. A card into the slot, twenty times. It slides to the stop and out again
   with two fingers, does not catch at the mouth, does not rattle in the
   channel. If it catches: `clr`. If the mouth is sharp: `chamfer`.
2. The phone's bottom-left corner into the frame corner: it drops in, the lip
   reaches over the screen's border and no further.
3. An M2 into the corner's hole, by hand: it bites and seats.
4. Look at the slot's window wall against the light: it is solid, no gaps
   between perimeters. If it is not, three perimeters are not printing solid
   and the wall wants to be 2.8 (7 × 0.4).

Then the body and the frame.
