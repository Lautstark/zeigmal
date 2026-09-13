# ADR 0007 — The holder is a leaning body with the slot in it, and a frame screwed on from underneath

**Status:** accepted · **Date:** 2026-09-13 · **Applies to:** `case/`, docs/enclosure.md

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
number and the plate's thickness follows. The base is 160 mm deep — two
thin aprons in front of and behind the leaning block — because a card standing
80 mm proud is a lever and PLA is light; `verify.py` says what push at the
card's top tips it. If the real thing tips, the hollow under the slope is where
ballast goes before the base grows.

## Not to be "fixed" later

"Make the frame a snap fit so there are no screws." The screws *are* the
feature. "Put the slot in a cartridge after all." Only when a second card
family actually arrives and the first body has actually been reprinted for it.
