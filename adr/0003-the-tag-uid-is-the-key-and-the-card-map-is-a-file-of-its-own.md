# ADR 0003 — The tag UID is the key, and the card map is a file of its own

**Status:** accepted · **Date:** 2026-09-09 · **Applies to:** `Cards.kt`, `TagId`, docs/nfc.md

## Context

An NFC sticker can carry data. Writing the entry id (`trinken`) into an NDEF
record on each sticker would make every card self-describing: no map, nothing
to back up, a re-stickered card just gets written again. The alternative is to
read only the factory UID and keep a map from UID to entry.

## Decision

**The key is the UID, as lowercase hex digits with the separators stripped, and
the map lives in `cards.json` beside the manifest, not inside it.** Several
rows may name the same entry. Writing NDEF to stickers is not done; reading it
is skipped (`FLAG_READER_SKIP_NDEF_CHECK`).

## Why

**Reading a UID costs nothing and needs no writer.** Three hundred stickers are
three hundred taps against the phone either way; writing NDEF would add a second
tool and a second failure mode per card.

**A map is what gets backed up, and it is tiny.** Three hundred lines of JSON
that another phone can read in, without rescanning. That is MVP success
criterion 12.

**The map is a fact about one household's stickers; the manifest is a fact
about the content.** A sticker replaced, a card laminated twice, a set given to
a second family: all of those change `cards.json` and none of them change the
manifest. wochenwerk arrived at the same split (`docs/data-model.md`: the tag
mapping "belongs with the reader") and the same rule that only the hex digits
compare.

**Genuine NTAG213 UIDs are factory-set and read-only.** The UID is as permanent
as the sticker.

## Consequences

The UID is not an application identifier, and the map is the only thing that
makes it one. Lose `cards.json` and the cards are unknown until rescanned. Hence
docs/media-import.md's backup rule.

Cheap stickers from unknown sources may have cloned or writable UIDs; docs/nfc.md
says to buy NXP-genuine NTAG213 and to check that two stickers never share a UID
when the batch arrives.

## Not to be "fixed" later

"Write the entry id into the tag so the map is unnecessary." It would make the
map *redundant*, not unnecessary: a tag that says `trinken` still needs the
manifest to say which video that is, and a family that lends cards to another
family still wants the map. If NDEF is ever written, it is a second key beside
the UID, and E6 in docs/experiments.md is the experiment that would justify it.
