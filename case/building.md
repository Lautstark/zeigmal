# Building the holder

Two printed parts, five M2 screws, an evening's printing. All the dimensions
are in [`zeigmal-case.scad`](zeigmal-case.scad); this file explains what to do
with them.

> **The second holder.** The first (git tag `holder-v1`, printed 2026-09-15)
> had a plate 20 mm wider than the phone, a frame that flexed on two screws,
> and a recess above that frame the children kept posting cards into. What it
> got right — a funnel a card finds without aiming — is unchanged. ADR 0007
> says what changed and why.

## The two parts

| Part | What it does | Outer size | Volume |
|---|---|---|---|
| **Body** | the wedge: the plate the phone lies on, the funnel and slot cut into its top, a 60° back down to the table, hollow inside with two ribs | 164.3 × 97.3 × 94.0 mm | 180 cm³ |
| **Frame** | the front, from the table to the holder's top edge: a 2.4 mm face with a 3 mm lip over the screen, rails round all four edges, openings for jack, USB-C and speaker, one window over the keys | 164.3 × 103.9 × 10.6 mm | 44 cm³ |

Plus M2 × 10, five pieces: two from underneath, three from the front.

```bash
openscad -o body.stl  -D 'part="body"'  case/zeigmal-case.scad
openscad -o frame.stl -D 'part="frame"' case/zeigmal-case.scad
python3 case/verify.py                         # the numbers
python3 case/check-stl.py body.stl frame.stl   # what OpenSCAD made of them
```

`part="assembly"` shows everything together with a dummy phone and card,
`part="exploded"` pulls the frame and the phone forward, `part="frame-placed"`
is the frame where it sits rather than where it prints.

**Run `check-stl.py` before every print.** It is there because the frame once
exported as three pieces: the openings in its right rail each cut the whole
rail, and the two strips between them hung on nothing. Nothing in the preview
showed it.

## One opening, and one only

A child reads a holder by its gaps. The first one had two: the slot, and a
10.6 mm recess the whole width of the holder where the frame's top rail ended
and the plate's face stood back. The recess is card-shaped, so cards went into
it.

So the frame's face now reaches the holder's top edge, and the two parts are
rounded at that same edge with the same radius, meeting flush across the full
width. The only gap left anywhere on the front is the mouth. The key window
keeps a 3 mm band above it rather than breaking through the top, for the same
reason: a notch in the top edge is a slot.

The mouth opens upward and only upward. It used to fall away at the front as
well, so a card could be slid down the plate's face into it — but the frame's
rail stood 10.6 mm in front of that face and nobody could reach it.

## How it holds the phone

The body's plate is flat; nothing on it locates the phone. The frame does all
of that: a closed ring of rails round the phone's four edges, and a face that
overlaps the screen by 3 mm and keeps the phone from lifting out. The A51's
black border is about 2.5 mm, so half a millimetre of picture goes under the
frame on each side. That was a decision, not an accident: 2 mm held the phone
less well.

Five M2 × 10 hold the frame to the body:

- **Two from below**, through the holes in the body's floor slab, up into the
  frame's foot. The foot has 1.8 mm holes; the screws cut their own thread.
- **Three from the front**, through the top rail into the plate: one at the
  far left beside the slot, two on the right past the funnel. The body has the
  tap holes — nothing to drill.

The top rail has one window over the volume keys and the power key, 25 to
72 mm from the camera end. All three keys are reachable; a child reaching the
power key was accepted on 2026-09-16, and the window is 3 mm longer at that
end because the rail overlapped the key by about a millimetre.

The right end is closed, with three openings straight through rail and face:
the 3.5 mm jack, the USB-C plug, and the speaker. Only the speaker's opening
wraps round the corner onto the face — a plug does not need it, and every
opening that reaches past the lip meets the screen's window behind it and
leaves the rail between two openings attached to nothing.

The microphone stays covered. The station never records.

To take the phone out: five screws, lift the frame, lift the phone.

## How it holds the card

The slot is a pocket in the plate right behind the phone: window wall, a
1.5 mm channel, rear wall. The card leans on the rear wall — gravity, the
channel leans back — and reads through the window wall, the clearance and its
own sticker, 2.9 mm from the back glass to the sticker face.

The mouth is a funnel: 93 mm wide and 5 mm front to back at the top, narrowing
to the channel over 8 mm. That 8 mm is exactly how far the holder stands above
the frame, and it is the whole reason it is taller than the phone.

The stop is not chosen. It is where a sticker 30 mm above the card's bottom
edge lands on the coil: `insert = coil_from_top + sticker_y`. With the coil
12.5 mm below the phone's top edge that is 42.5 mm in and 77.5 mm standing
proud.

The sticker goes in the card's lower-left corner seen from the front, on the
line `sticker_x` sets. The centre line was the first guess and it is what made
holder-v1's plate stand 20 mm past the phone.

## Printing

| Setting | Value | Why |
|---|---|---|
| Printer | Ender 3 V2, 220 × 220 × 250 | the body is 164 × 97, flat on the bed |
| Nozzle | 0.4 mm | every wall is a whole multiple of it |
| Layer height | 0.2 mm | the floor slab and the frame's face are whole multiples |
| Temperature | 210 °C, bed 60 | 200 gave layers that came apart |
| Speed | 40–50 mm/s | same reason |
| Fan | off for the first layers, then 80 % | same reason |
| Perimeters | body 2, frame 3 | the body is mostly wall already; the frame is handled |
| Infill | body 15 %, frame 20 % | grid |
| Bottom / top | 4 / 4 layers | the floor slab carries the screw heads |
| Material | PLA | no ABS near a small child |
| Supports | **off**, both parts | see below |
| Brim | 5 mm on the body | 164 mm of flat floor tends to lift |

Roughly 14 to 16 hours for the body, 1.5 for the frame.

### Orientation on the bed

Both parts come out of the file in the orientation they print in. **Body** on
its base; **frame** face down.

### Supports: none, and why that is checked

`check-stl.py` walks every downward-facing facet of the exported file, asks
whether there is really air under it, and — when there is — how far that air
reaches sideways before it meets a wall. A gap narrower than 25 mm is a bridge
and the printer manages it; anything wider is a ledge and the check fails.

Today neither part has a ledge. The three overhangs that exist are:

| Where | Angle from horizontal |
|---|---|
| the plate's underside | 70° |
| the back of the wedge | 60° |
| the funnel's back wall | 46° |

The last one is the tight one, and it is why `mouth_flare_n` is 3.5 mm rather
than 5: the funnel's back wall leans back as it rises, and once the funnel
came down to 8 mm tall, 5 mm of flare was a 38° overhang. `verify.py` computes
that angle in world coordinates — in the plate's own frame the plate's 20°
flatter it by exactly that much.

Inside the body there is one flat ceiling, where the hollow ends: a 15 mm
bridge between the plate's back and the back wall. It cannot be supported (it
is inside) and does not need to be.

### What can go wrong

- **The card catches at the mouth.** The 0.8 mm chamfer is the lead-in; if
  the printer rounds it off, `chamfer = 1.2`.
- **The card rattles.** `clr` is 0.3 per side. Do not tune the printer here;
  change `clr` and reprint the fit test.
- **The frame does not go on.** `play` is 0.3 round the phone. Measure the
  phone before blaming the number.
- **The screw strips.** 1.8 mm tap hole for M2 in PLA; drive by hand, not by
  drill, and stop when it seats.

## Tolerances

All clearances are named variables in section 3 of the `.scad`.

| Where | Variable | Value | Meant for |
|---|---|---|---|
| The card in the slot | `clr` | 0.30 mm per side | never jams, sticker stays on the coil — **the one to tune** |
| The phone in the frame | `play` | 0.30 mm per side | never jams, no rattle. It is also the gap left above the frame: less than half a card, so nothing can be posted into it |
| The camera island in its pocket | `cam_play` | 0.30 mm | what is left of the wall under the pocket is the thinnest place in the part |
| Frame lip over the screen | `frame_over` | 3.00 mm | holds the phone; costs 0.5 mm of picture per side |
| Key window | `key_win_left` / `key_win_right` | 3.00 / 6.00 mm | a fingertip finds the keys, and a guessed position still fits |
| Jack, USB-C, speaker | `ports` | 3.0 / 4.0 / 1.5 mm | a plug goes in without aiming; the speaker is tight because it needs no plug |

If the printer generally prints fat, do **not** fiddle here but calibrate the
extrusion multiplier. These numbers are design dimensions, not printer
corrections.

## Assembly

1. Check the body's slot with a card before anything else: it should slide to
   the stop and out again with no catch.
2. Lay the phone on the plate, port to the right, camera island into its
   pocket. Volume keys up.
3. Put the frame over it: the ring goes round all four edges, the openings at
   the port end.
4. Turn the whole thing over, holding the frame on. Two M2 × 10 into the holes
   in the floor slab, by hand, until they seat.
5. Turn it back. Three M2 × 10 from the front through the top rail.
6. Plug the cable in through the opening on the right when needed.

## The sound

The A51's single bottom-firing speaker is quiet, and the clips are streamed,
so nothing can be normalised (ADR 0008). Three things help, in this order:

1. Media volume up, and Dolby Atmos on in the phone's sound settings.
2. The app puts a loudness effect on the player, 8 dB
   (`GAIN_MILLIBELS` in `app/.../ui/Loudness.kt`). Past about 1200 this
   speaker buzzes.
3. A small active speaker on the headphone jack. That is the only way to get
   properly loud, and the frame has an opening for it.

## Measure first

These numbers are assumptions. If they are wrong, the design changes — in part
considerably. Every one is a variable in section 1 or 2 of the `.scad`, and
`verify.py` recalculates everything from them.

### Where the coil is

`coil_x` from the phone's left end, `coil_from_top` below the top edge, both
in landscape with the port to the right. Stefanie put the coil in the corner
by the camera by eye, and the printed holder read a sticker in the card's
corner through 2.4 mm of PLA on the first try, which settles the material
question with it — no coupons needed.

`coil_from_top` came down to 12.5 on 2026-09-18 because the printed slot took
the card 2.5 mm deeper than it needed to. What is still unmeasured is the
*centre* of the area that reads reliably; a 25 mm sticker forgives about
10 mm, not more.

```
python3 case/verify.py --coil 15 --coil-x 24
```

tries a correction before it is written into the file.

### The phone

| Variable | Assumed | Check |
|---|---|---|
| `phone_l`, `phone_h`, `phone_t` | 158.9 × 73.6 × 7.9 | calipers, bare phone; the frame's pocket is these plus 0.3 |
| `screen_inset` | 2.5 mm | the black border round the picture; the lip covers 3.0 of it |
| `cam_x0..cam_u1`, `cam_h` | 8–30 × 45.6–67.6, 1.2 proud | the camera island in landscape; its pocket in the plate |
| `key_vol_up`, `key_vol_dn`, `key_power` | 28–38, 40–48, 56–66 from the left end | one window over all three (25–72) |
| `key_proud` | 0.5 mm | how far a key stands out of the edge |
| `jack_u`, `usb_u`, `speaker_u` | 8–14, 30–40, 55–68 from the bottom | the three openings in the right rail |

### The card

| Variable | Assumed | Check |
|---|---|---|
| `card_w`, `card_h` | 80 × 120 | 80 measured 2026-09-14; height still from the box |
| `card_t` | 0.7 mm | a stack of ten with calipers, divided by ten |
| `sticker_x`, `sticker_y` | 12.5, 30 | where the stickers actually go on the cards |

### Stability

`verify.py` estimates the sideways push at the card's top that tips the
holder, from masses it works out of the geometry. It is a light push, about
2 N backwards, because PLA is light and a card standing 77 mm proud is a
lever. The base stays 110 mm deep anyway (ADR 0007): the children push cards
in, which does not tip it, and do not shove them. If that turns out wrong,
the hollow under the back is where ballast goes.

## The fit test

```bash
openscad -o fit-test.stl     -D 'part="fit-test"'     case/zeigmal-case.scad
openscad -o frame-corner.stl -D 'part="frame-corner"' case/zeigmal-case.scad
```

The fit test is the slot and the left end of the body, cut off level 40 mm
above the table so it stands at the real tilt; about an hour at 2 walls and
15 % infill. The frame corner is the left 40 mm of the frame with its screw
hole, already lying face down; twenty minutes. Together they try every
clearance in the design:

1. A card into the slot, twenty times. It slides to the stop and out again
   with two fingers, does not catch at the mouth, does not rattle.
2. The phone's bottom-left corner into the frame corner: it drops in, the lip
   reaches over the screen's border and no further.
3. An M2 into the corner's hole, by hand: it bites and seats.
4. Look at the slot's window wall against the light: it is solid, no gaps
   between perimeters.
