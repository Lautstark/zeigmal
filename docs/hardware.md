# Hardware

One phone, some stickers, a printed holder. Every number here carries a tag in
the style of the talker's case file:

```text
[M]  measured, by whom and when
[R]  researched (manufacturer or datasheet figure)
[A]  assumption — unverified, measure on the real part
[K]  design decision — freely choosable
```

## The phone: Samsung Galaxy A51 (SM-A515F)

| | value | tag |
|---|---|---|
| body | 158.9 × 73.6 × 7.9 mm, 172 g | [R] verify with calipers, with and without case |
| screen | 6.5" AMOLED, 1080 × 2400, 20:9 | [R] |
| NFC | yes on the European model; Android reader mode from API 19 | [R] |
| Android | shipped with 10, last update 13 (One UI 5.1) | [R] check *Settings → About phone* |
| USB | USB-C, on the bottom edge in portrait — one long side in landscape | [R] which side faces which way is [K] |
| headphone jack | 3.5 mm, bottom edge | [R] a speaker option if the built-in one is too quiet in the holder |
| speaker | single bottom-firing | [R] the holder must not cover it |
| NFC coil position | upper half of the back, roughly behind the camera island | [A] **E4 measures it** — the one number the enclosure cannot do without |
| reliable read area | a circle a few centimetres across around the coil centre | [A] E4 |
| read distance through 1 / 2 / 3 mm PLA | | [A] E5 |
| case | with or without | [K] decide before E4, since a case changes every distance |

## The cards

| | value | tag |
|---|---|---|
| the first card set | a bought box of 224 laminated portrait cards, 8 × 12 cm nominal, a symbol, a word and a sign drawing on each | [R] the product page; width, height and thickness are [A] **to measure**. The slot is designed for these first; any other card gets its own row here |
| the 25 × 25 mm cards in `~/Code/card-case` | 25 × 25 mm, 1.5 mm thick with a 10 mm velcro coin (designed at 1.6 mm pitch) | [M] Stefanie, 2026-08 — a different card family; only the wall and clearance numbers carry over |
| wochenwerk's NFC cards | 85 × 120 mm laminated, sticker at a fixed marked position | [K] from `wochenwerk/docs/hardware.md`; a different card, the same slot idea |
| sticker | NTAG213, 25 mm round | [K] docs/nfc.md |
| sticker position on the card | | [K] after E4: the slot's stop puts it over the coil |
| insertion depth | | [K] after E4 and the card height: the symbol stays visible above the phone |

## The stand

Landscape, tilted back a little, on power, the card slot vertical behind the
phone's back edge. Everything dimensional is in docs/enclosure.md's checklist
and is [A] until the card and the coil are measured.

## Setting the phone up, once

The list of things no app can do for itself, in the order to do them. Samsung
wording is One UI 5's and may differ by a word.

1. **NFC on.** *Einstellungen → Verbindungen → NFC und kontaktlose Zahlungen*
   — on. Reader mode does not care about payment routing.
2. **No lock screen.** *Sperrbildschirm → Sperrbildschirmtyp → Keine.* NFC
   is off while the phone is locked, and a station has nobody to unlock it.
3. **Screen timeout is irrelevant** while the app is in front
   (`FLAG_KEEP_SCREEN_ON`), but set it long anyway so the phone does not go
   dark between a reboot and the first tap.
4. **Battery.** *Apps → Zeigmal → Akku → Uneingeschränkt*, and turn off
   *Apps in den Ruhezustand versetzen* for it. knopfpost found three separate
   One UI layers that stop a foreground appliance and the third one silently
   defeated the other two.
5. **Notifications.** Do Not Disturb, scheduled always, or the station will
   one day show a system update prompt over a video.
6. **Screen pinning.** *Sicherheit und Datenschutz → Weitere
   Sicherheitseinstellungen → Apps anheften* — on. Then open Zeigmal, open
   recents, pin it. This is *pinned*, not locked: Back+Overview held together
   still leaves. Say so honestly, as vorlaut-app does.
7. **Auto-rotate** may stay on; the manifest fixes landscape.
8. **Sound.** Media volume up; the holder must leave the speaker free.
9. After a reboot: one tap on the icon. There is no boot receiver, on purpose
   (docs/architecture.md).

## What the station must never rely on

The card being reported removed; the screen waking itself; a network. Each is
either unmeasured or absent by decision.
