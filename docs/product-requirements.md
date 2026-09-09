# Product requirements

What the station has to do, for whom, and what it must never do. The handover
of 2026-09-09 is the source; this is the part of it that binds the code.

## Who

Two children who use around three hundred physical cards — SIGNdigital's
SIGNbox 1. Each card carries a METACOM symbol, a German word and a sign. The adults in the house prepare
content on a laptop; the children only ever touch cards.

## The interaction

```text
idle  →  card in  →  sign video at once  →  spoken word when the entry says so  →  ready
                     another card in  →  the new video replaces the old one at once
                     unknown card in  →  a quiet "I do not know this card yet"  →  ready
```

Nothing else is visible to a child: no menu, no button, no navigation bar, no
notification, no keyboard, no settings, no "add card" flow. The idle screen is
the name and one line of text; it may become a mark later.

## Must

1. Run on the Samsung Galaxy A51, landscape, on power, screen never sleeping
   while the app is in front.
2. Identify a card by its NFC sticker within a moment of insertion, with the
   phone's own antenna, through the card and the holder wall.
3. Start the right video immediately; replace it immediately when another card
   arrives; play the same card again when it is presented again.
4. Play a prepared spoken word beside the video when, and only when, the
   manifest says the video does not carry one.
5. Work fully offline. No backend, no account, no synchronisation, no network
   permission.
6. Take its whole content from one directory copied onto the phone, and let
   that directory be replaced.
7. Keep the card map as a file a person can copy off the phone and onto the
   next one, so three hundred cards are scanned once.
8. Fail quietly on an unknown card, a missing file or a broken manifest: a
   child sees a calm screen, an adult sees the reason on the diagnostics
   screen.
9. Stay in fullscreen; survive rotation; come back to the same screen after a
   reboot with one tap.

## Should

- Hide a diagnostics screen behind a gesture a child does not make by
  accident, showing NFC state, the loaded Kartensatz, its warnings, the app
  version and a log of tag events with timestamps.
- Show the video's attribution somewhere an adult can see it, because sign
  video sources are licensed at least as tightly as METACOM.
- Be pinnable with Android's screen pinning, and say honestly that pinning is
  not a lock.

## Must not

- Create, edit, search, generate or fetch content of any kind (ADR 0001).
- Inspect a video to decide whether to speak (ADR 0005).
- Offer any path that moves a Kartensatz off the phone (ADR 0006).
- Depend on the card being reported as removed. Removal is measured first
  (docs/experiments.md) and designed around second.
- Introduce a second device. No Raspberry Pi, ESP32 or external reader unless
  E1 in docs/experiments.md fails on the phone's own antenna.

## Open until measured

Whether a card resting in the slot causes repeated reads, how fast a swapped
card is seen, and whether removal is reported at all. The state machine treats
removal as informational for exactly this reason.
