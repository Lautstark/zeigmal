# ADR 0010 — A shelf shows two cards, and there are several shelves

**Status:** accepted · **Date:** 2026-10-22 · **Applies to:** `case/zeigmal-cards.scad`, case/cards.md

## Context

The cards needed somewhere to live and somewhere to be seen: a box to keep
three hundred of them sorted by theme, and something that stands a day's
selection up where a child can take one herself. The box was easy. The stand
was drawn five times.

Everything that shows many cards at once runs into the same arithmetic. For a
card to be recognisable behind another it has to stand about 23 mm higher, so
twenty cards are 46 cm of height or depth — off the bed and off a child's
table. Overlapping them sideways instead keeps their full height and costs
only the strip you give up: 42 mm a card, so eight fit on a 211 mm shelf.

That was drawn, and rejected: the cards should be whole, not strips.

## Decision

**A shelf holds two cards side by side, whole, and there are as many shelves as
there are themes.** Each shelf is 185 mm long, prints in about three hours, and
clips to the next one with a tab so a row stays a row. The slots are 8 mm wide,
so each card on show has seven more behind it: a shelf is a theme of sixteen
cards with two of them visible.

**The box stays one box**, 204 mm long, four compartments of 48 cards, open at
the top, no scoops.

## Why

**Whole cards cost 88 mm each and the bed is 220 mm.** Two is not a
compromise, it is the ceiling. `cards-verify.py` fails rather than letting a
third one through, and says that sentence in the failure.

**Several small things beat one big thing here.** A three-hour print can be
tried and repeated; a sixteen-hour display cannot. A child can carry one shelf
to the table. A theme can be added without reprinting anything. And the number
of shelves is the number of themes, which is a thing the family can change
without a CAD file.

**The scoops came out of the box** because 47 mm of card stands proud of it.
They were solving a problem that the proportions had already solved.

## Consequences

A theme with more than two cards on show takes two shelves. A row of three is
54 cm of table.

A shelf on its own is light — a pull at the top of a card tips it at 0.6 N.
The foot reaches 25 mm in front of the bar to get even that far; without it the
figure was 0.3 N, the weight of a sheet of paper. Clipped into a row it is
three times better, which is the tab's real job; keeping the row tidy is the
side effect.

## Not to be "fixed" later

"Make the shelf longer so it holds four." The bed is 220 mm and a card is
88 mm of it. Print two shelves.

"Let the cards overlap a little, nobody will mind." They were drawn
overlapping, shown, and turned down. Whole cards was the requirement.
