# ADR 0009 — The phone writes the stickers

**Status:** accepted · **Date:** 2026-09-13 · **Applies to:** `app/ui/WriteScreen.kt`, `app/nfc/AndroidTagSource.kt`, `core/SignBox.kt`

## Context

ADR 0001 said zeigmal plays and does not prepare, and pointed at a browser tool
for everything else. With the record on the sticker (ADR 0008) the one
preparation step left is writing 224 stickers, and a browser on a Mac cannot
do it: Web NFC exists only in Chrome on Android, and macOS's smart-card
service holds a USB reader before WebUSB can claim it.

## Decision

**The app has an adult mode that writes the stickers.** It walks HHO's public
SIGNbox 1 word list in the box's order, shows each card as SIGNdigital serves
it so the right one comes out of the stack, and writes the record for that
word onto the next sticker held to the phone. A sticker that already carries a
record is shown, not overwritten; overwriting is a button.

## Why

**The phone is the reader anyway.** Writing where reading happens means the
sticker is tested by the same antenna that will read it, on the spot.

**Showing the card beats showing the word.** The adult is matching a physical
card out of a stack; the picture is what they hold, the word is what they
would have to read.

**This is still not authoring.** No symbol is chosen, no word invented, no
video edited. The list is HHO's, the picture is theirs, the record is three
strings. ADR 0001 stands for everything beyond that.

## Consequences

The app knows one card box by name. A second box, or "Eigenes Wort", is a
list and a text field away and is not in the MVP. The writing mode needs the
provider's login, because the card picture comes from the provider.

## Not to be "fixed" later

"Let the writing mode search SIGNdigital for any word." That is a search box
in front of a provider's catalogue, and the line ADR 0001 drew.
