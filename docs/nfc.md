# NFC

How the app uses the phone's NFC reader, as the code does it today
(`app/nfc/AndroidTagSource.kt`, `core/Presence.kt`, `core/AdultModel.kt`).
The sticker carries the card's record (ADR 0008) and the phone writes it
(ADR 0009); docs/card-record.md is the format. The expectations further down
were written before anything was measured and keep their experiment numbers
from docs/experiments.md.

## What the app does

Reader mode, owned by the foreground activity through `AndroidTagSource`:

- `enableReaderMode` in `onResume`, `disableReaderMode` in `onPause`
  (`MainActivity`). While the station is in front, no other app, no payment
  stack and no system "tag read" sound gets the tag first, and there is no
  intent dispatch to race.
- Flags: every technology (`NFC_A | NFC_B | NFC_F | NFC_V`) so the log can
  name what a sticker is, and `NO_PLATFORM_SOUNDS` because the video is the
  sound. The platform's NDEF check is **not** skipped: with
  `FLAG_READER_SKIP_NDEF_CHECK` a tag arrives without the `Ndef` technology,
  and then the record can be neither read nor written (seen 2026-09-13,
  "Aufkleber kann kein NDEF").
- `EXTRA_READER_PRESENCE_CHECK_DELAY = 250 ms`: how often Android polls a tag
  it is still holding.
- On discovery, in the child's mode: the UID becomes a `TagId` (lowercase hex,
  no separators) for the log and for telling stickers apart; the sticker's
  NDEF message is read for our external record `lautstark.de:zeigmal` and
  decoded into a `CardRecord`, or null when there is none. The two arrive
  together as `TagEvent.Seen`. Then `NfcAdapter.ignore(tag, 500 ms, listener)`:
  the same tag object is not reported again while it stays, and the listener
  reports `TagEvent.Gone` once the presence check has missed it for 500 ms.
- On discovery, in the writing mode (`TagMode.Write`): a sticker that already
  carries one of our records is reported as `AlreadyWritten` and left alone,
  unless the adult chose to overwrite. Otherwise `WriteStarted`, then the
  record is written with `Ndef.writeNdefMessage`, or the sticker is
  NDEF-formatted with it first when it comes without NDEF, and the outcome is
  reported (`Written` or `Failed` with a reason). Nothing is decided in the
  app: `AdultModel` in `core` decides what an outcome means.

The record is what the station plays; the UID is never a key. A sticker
without our record is a grey ring: seen, nothing behind it.

**Measured on the Galaxy A51, 2026-09-13:** Android does not hold a resting
sticker. It reports it seen, gone about 210 ms later, and seen again about 80
ms after that, three times a second for as long as the card lies there; the
presence check fails before the card has moved. So the reader's events go
through `Presence` first: a card is present from its first `seen` until it
has been unseen for a full second, a `seen` inside that window is nothing, and
another card ends the first at once. The station sees one `card in` and one
`card out` about a second after the real removal. `card out` lets the running
round finish and then returns to idle (`Station.kt`); a card that has played
all its rounds goes at once.

The writing mode sees the same blinking: a sticker that has just been written
is rediscovered about every 290 ms while it lies there. `AdultModel` treats the
same sticker with the same record within three seconds as the one just
written, not as news, and moves the box on by one word per sticker however
often the reader wrote it.

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
ferrite** — the card is paper and laminate, nothing metallic. The record is
under a hundred bytes of the 144 an NTAG213 has (docs/card-record.md). The UID
is only logged and shown to the adult while writing; a cloned batch would
still play correctly, because the record, not the UID, says which card it is.

A sticker's position on the card is standardised once E4 says where the coil
is: the aim is that sliding the card fully into the slot puts the sticker
over the centre of the reliable read area without anyone aiming.

## What could go wrong, and what the answer would be

- **No removal reported, or seconds late.** Acceptable. The UX does not need it
  (a new card replaces the old at once); the state machine already treats it
  as informational.
- **Repeated discoveries while a card rests.** Measured: they happen, three
  times a second on the A51. `Presence` holds a card for a second past its
  last `gone`, so they do not restart the video, and `AdultModel` does the
  same for a sticker just written.
- **Read distance too short through the holder wall.** Thin the wall at the
  coil (a 1 mm window in a 2.4 mm wall), or let the card rest against the
  phone's back directly. E5 decides.
- **The phone's coil is in a place the slot cannot reach.** The one outcome
  that would reopen the hardware decision. E4 is first for that reason.

## Not done, on purpose

No HCE, no foreground dispatch, no Beam, and no locking: a written sticker
stays rewritable, so a card can change its word or its provider. The UID is
not a key and no map from UIDs to cards exists any more (ADR 0003 is
superseded by ADR 0008).
