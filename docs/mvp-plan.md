# MVP plan

Ten cards from SIGNbox 1, one phone on a wall charger, one printed fit test.

## Done

- The core: card record, station that counts the loops, presence filter,
  provider seam, SIGNdigital behind it, the box's word list, the PIN and the
  settings; 40 unit tests, 9 instrumented on the managed emulator.
- The app: reader mode with NDEF read and write, the child screen with the
  mark and the ring, the corner and the PIN, the login, the writing mode with
  the box's list, the settings, screen pinning and keep-screen-on.
- Measured on the A51: E1, E2, S1 (docs/experiments.md).
- Step 1: "schmutzig" written on the phone, the SIGN video played from the
  sticker (2026-09-13).

## Order

| step | what | done when |
|---|---|---|
| 1 | Log in on the phone, write two stickers in the writing mode, play them | the first real SIGN video plays from a sticker |
| 2 | Latency: card in → first frame, five cards, five times | S1 has a number; if over 700 ms, one warm player instead of one per card |
| 3 | E3 swap, E8 hundred cycles in a mock slot | rows filled |
| 4 | E4 the antenna map, E5 read-through coupons | the sticker's spot on the card is decided and written into step 2 of the writing mode |
| 5 | Write the ten MVP cards; a day on the kitchen table | what the children do with it, written down |
| 6 | Fit test print, then the holder | docs/enclosure.md |
| 7 | Release build: vorlaut-app's signing arrangement, a signed APK on a GitHub release | the phone gets updates without a cable |

## After the MVP

The rest of the box (224 stickers, one afternoon). The battery protection
setting. A second provider.

## Not on the plan

Storing clips, a manifest, a card map, a file import, OCR, camera, symbol
search, audio generation, PDF, accounts, cloud, a second device.
