# NFC

The main technical risk, written down before it is measured. What is known
comes from Android's documentation and the family's earlier reader work
(wochenwerk, with a desktop reader); what is not known is in
docs/experiments.md with a blank result column.

## What the app does

Reader mode, owned by the foreground activity (`NfcReader.kt`):

- `enableReaderMode` on resume, `disableReaderMode` on pause. While the station
  is in front, no other app, no payment stack and no system "tag read" sound
  gets the tag first, and there is no intent dispatch to race.
- Flags: every technology (`NFC_A | NFC_B | NFC_F | NFC_V`) so the diagnostics
  screen can name what a sticker is; `SKIP_NDEF_CHECK` because the UID is the
  key and reading NDEF costs a round trip per card; `NO_PLATFORM_SOUNDS`
  because the video is the sound.
- `EXTRA_READER_PRESENCE_CHECK_DELAY = 250 ms`: how often Android polls a tag
  it is still holding.
- On discovery: UID → `TagId` (lowercase hex, no separators) → `CardSeen`.
  Then `NfcAdapter.ignore(tag, 500 ms, listener)`: the same tag is not
  reported again while it stays in the field, and the listener fires when the
  presence check has missed it for 500 ms → `CardGone`. A single missed poll
  must never arrive as a removal; wochenwerk's rule, kept.

`CardSeen` is what the product is built on. `CardGone` is logged and, today,
changes nothing about a running video (ADR 0003, `Station.kt`).

## What is expected, and has to be checked

| | expectation | to verify |
|---|---|---|
| tag technology | NTAG213 is ISO 14443-3A: Android reports `NfcA`, `MifareUltralight`, and `Ndef` if formatted | E1 |
| UID | 7 bytes, factory-set, read-only on NXP-genuine NTAG213; unique per sticker | E1, check the batch for duplicates |
| detection on insertion | one `onTagDiscovered` per arrival, typically under 200 ms once the tag is over the coil | E1, E3 |
| card resting in place | no repeated callbacks after `ignore()`; presence polling every 250 ms | E2 |
| removal | `OnTagRemovedListener` within roughly `presence delay + debounce` — half a second to a second | E2 |
| the same card again | a new `onTagDiscovered` once removed and re-presented; `ignore` is per tag object | E2 |
| swapping cards quickly | the second card is discovered as soon as the first leaves the field; two tags in the field at once may be neither | E3 |
| read through material | NTAG213 on a 25 mm coil reads through a laminated card and 1–3 mm of PLA at a few millimetres of air; distance falls off fast with offset from the coil centre | E4, E5 |
| screen state | NFC only works with the screen on and the device unlocked; the app keeps the screen on, and the phone must have no lock screen | E7 |
| Samsung specifics | One UI has an NFC toggle and a "contactless payments" setting; reader mode ignores payment routing but the toggle must be on | setup checklist in docs/hardware.md |

## Stickers

Buy **NTAG213, NXP-genuine, 25 mm round, paper or PET face, no on-metal
ferrite** — the card is paper and laminate, nothing metallic. 144 bytes of user
memory are irrelevant; only the UID is used (ADR 0003). Check when the batch
arrives that the diagnostics log shows a different UID for every sticker;
cloned batches exist and would make two cards one word.

A sticker's position on the card is standardised once E4 says where the coil
is: the aim is that sliding the card fully into the slot puts the sticker
over the centre of the reliable read area without anyone aiming.

## What could go wrong, and what the answer would be

- **No removal reported, or seconds late.** Acceptable. The UX does not need it
  (a new card replaces the old at once); the state machine already treats it
  as informational.
- **Repeated discoveries while a card rests.** Would restart the video. The
  fix is in software: ignore a `CardSeen` for the tag currently playing within
  a short window. One line, one test.
- **Read distance too short through the holder wall.** Thin the wall at the
  coil (a 1 mm window in a 2.4 mm wall), or let the card rest against the
  phone's back directly. E5 decides.
- **The phone's coil is in a place the slot cannot reach.** The one outcome
  that would reopen the hardware decision. E4 is first for that reason.

## Not done, on purpose

No NDEF written to stickers (ADR 0003), no HCE, no foreground dispatch, no
Beam. If E6 ever shows a benefit in a self-describing tag, it becomes a second
key beside the UID, not a replacement.
