# ADR 0007 — The holder is a leaning body with the slot in it, and a frame screwed on from underneath

**Status:** accepted, amended 2026-09-16 and 2026-09-18 · **Date:** 2026-09-13 · **Applies to:** `case/`, docs/enclosure.md

## Context

The station is a bare Galaxy A51 lying landscape, a card pushed into a slot
behind its top edge, and nothing else. Three shapes were drawn to scale before
any of this was modelled: a one-piece wedge with the slot cut into its plate; a
thin tray with a separate slot tower on a rail; and the wedge with a drop-in
slot cartridge that would hold every number E4 and E5 can still change. The
cartridge was the engineer's favourite. Then two requirements arrived that the
drawings had not been made for: the design has to be *simple*, and the children
must not be able to take the phone out.

## Decision

**Two printed parts and two M2 screws.** The *body* is a block leaning 20° back,
hollow underneath, with the card slot cut straight into the top of its plate
behind the phone. The *frame* is a picture frame that overlaps the screen by
2 mm on three sides, is open at the port end except for two corner returns, and
is held by two M2 × 10 screws that come up through the body's floor slab into
the frame's foot. The volume keys get windows in the frame's top rail; the
power key gets a relief and no window.

The slot's position and depth are computed from the coil's position, not
chosen: `insert = coil_from_top + sticker_y`. E4 fills in the coil, and the
body is printed after that, not before.

## Why

**Nothing a child can undo.** A phone lying in a lip comes out. A snap-fit
comes out with a fingernail and a reason. Two screws underneath need the holder
turned over and a screwdriver, which is exactly the barrier wanted: an adult
does it in a minute, twice a year.

**The cartridge bought insurance nobody needs.** It made the slot reprintable
in half an hour after E4 or E5 moved a number. But E4 is a paper grid and half
an hour on the phone, E5 is three flat coupons, and the fit test is a slice of
the body — all of them happen before the six-hour print, so the number the
cartridge protected is known by then. What the cartridge cost was a second
clearance, a seam under the phone, and a part to explain.

**The tower was the one honest alternative and it puts a joint in the load
path.** A card is pushed down; a tower held at its foot flexes; the wedge does
not.

**M2 because they are on the shelf.** 2.5 × d of thread in PLA is 5 mm; the
foot gives 8.6. Nothing about the design wants M3.

**The power key is covered on purpose.** A child who finds it turns the screen
off and NFC with it (docs/hardware.md). The frame's rail over it is hollowed by
1 mm so the key is not pressed either.

## Consequences

A different card family later is a different body, not a different cartridge
— an evening of printing, once. The read path through the plate is the slot's
whole front wall (`win_t`), not a window in it, so E5's answer changes one
number and the plate's thickness follows. The base is 110 mm deep, and
that accepts something: a card standing 80 mm proud is a lever, PLA is light,
and `verify.py` shows that a sideways push of about 1.5 N at the card's top
tips the holder. Pushing a card *in* does not, and that is what the children
do; a base deep enough to shrug off a shove was drawn, 160 mm, and it was a
giant plate on a kids' table for a case that does not happen. If it turns out
to happen, ballast in the hollow under the slope comes before a deeper base.

## Amended, 2026-09-16, after the first body and frame were printed

Three things the first print settled differently from the text above.

**The frame is screwed at the top as well as the bottom, and is a closed
ring.** A 1.6 mm face held by two screws at the foot flexed; a 2.4 mm face,
a fourth rail at the port end with openings for USB-C and the speaker, and two
more M2 screws from the front through the top rail into the plate make it
rigid. The plate has no holes for those; the frame's counterbores are the jig
and the pilot holes are drilled by hand, once.

**The power key is exposed.** Covering it was the engineer's caution; the
person running the station wants to reach it. One window over all three keys.

**The sticker sits in the card's corner, not on its centre line.** The
centre line existed so that a card pushed in back to front reads too; that
requirement was never asked for, and it is what made the slot start 20 mm
past the phone's end and the plate 20 mm wider than the phone. The printed
body keeps that slot. A future body puts the sticker's corner over the coil
and the slot inside the phone's length.

## Amended, 2026-09-18, after the second holder was printed

The second holder was the first one that worked: the card goes in, the phone
reads it, the phone stays put. Four things it still got wrong, and one of them
is the reason this ADR needed a third pass.

**A holder has exactly one opening.** The frame's top rail stopped 8 mm below
the holder's top edge, so the plate's face stood 10.6 mm behind it — a recess
the whole width of the holder, card-shaped, right under the real slot. The
children posted cards into it, which is not a mistake on their part: it looks
like a slot. The frame's face now reaches the top edge and both parts are
rounded at that same edge. The first fix drawn was a slope on the body filling
the recess from behind; it would have been printed in mid-air, and the frame
reaching up costs nothing because the frame prints flat on its face.

**Overhangs are measured in world coordinates.** The funnel's back wall looked
steep in the plate's own frame and was 38° in the world, because the plate's
20° of tilt came off it. `verify.py` computes it properly now and
`check-stl.py` walks the exported facets, tells a bridge from a ledge, and
says where support would have to go. Support is allowed when it is planned;
today neither part needs any.

**The exported file is checked, not the preview.** Each of the three openings
in the frame's right rail cut the whole rail, and the two strips between them
were attached to nothing — two loose blocks in a file that previewed
perfectly. Only the speaker's opening wraps past the lip now, because past the
lip the face is already the screen's window.

**The power key is reachable and the sound is not the holder's problem.** The
key window is 3 mm longer at the power end. The speaker opening is tight and
wraps round the corner, the jack has an opening for an active speaker, and the
app puts 8 dB of loudness on the player.

## Not to be "fixed" later

"Make the frame a snap fit so there are no screws." The screws *are* the
feature. "Put the slot in a cartridge after all." Only when a second card
family actually arrives and the first body has actually been reprinted for it.
