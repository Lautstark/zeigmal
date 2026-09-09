# Experiments

What has to be true, written down before the run rather than remembered after
it. Results belong in the date column as they come in; a row that was checked
once and remembered is a row nobody can audit later. `⬜ not tested` stays
until it is.

The instrument for every NFC row is the app's diagnostics screen (long-press
the black screen): every `seen` and `gone` with a millisecond timestamp, the
tag's technologies, and the loaded Kartensatz. `adb logcat` is the second
instrument.

## NFC on the Galaxy A51

| id | what has to be true | how you would know / why it matters | 2026-__-__ |
|---|---|---|---|
| E1 | A bare NTAG213 held to the back is seen once, with its 7-byte UID and `NfcA` in the technology list | one `seen` line per touch; UID matches the vendor's if printed; no duplicate UIDs across the batch of stickers | ⬜ not tested |
| E2 | A card left in place produces no second `seen`; lifting it produces `gone` within about a second | the log after 60 s of resting shows one `seen`; the `gone` timestamp minus the lift moment; repeat 10× | ⬜ not tested |
| E3 | Swapping A for B gives `seen B` within 500 ms of B arriving, with or without a `gone A` before it | timestamps; try fast (under 300 ms) and slow swaps; try B arriving while A is still half in | ⬜ not tested |
| E4 | The reliable read area is found and drawn | a 10 mm grid on a paper taped to the back; mark every cell where 5 of 5 touches read; the centre and the radius go into docs/hardware.md as [M] | ⬜ not tested |
| E5 | The sticker still reads through card + laminate + 1, 2, 3 mm PLA and PETG at the coil centre | 10 of 10 at each thickness; note the thickness at which it first fails and how far off centre each thickness still reads | ⬜ not tested |
| E6 | (optional) an NDEF-formatted sticker and a blank one behave the same with `SKIP_NDEF_CHECK` | same log for both; this row exists so ADR 0003 is a measured decision | ⬜ not tested |
| E7 | NFC is dead while the screen is off or locked, and alive again when the app comes back | expected and documented, not a bug; confirms the "no lock screen" setup rule | ⬜ not tested |
| E8 | 100 insert/remove cycles of one card in a mock slot give 100 `seen` | wochenwerk's physical gate before enclosure work; count the misses and where the card sat when they happened | ⬜ not tested |

## The station

| id | what has to be true | how you would know | 2026-__-__ |
|---|---|---|---|
| S1 | A known card starts its video within 300 ms of `seen` | the `→ playing` line follows `seen` at once and the first frame is visible; measure with a phone camera at 240 fps if it feels slow | ⬜ not tested |
| S2 | `speech: external` plays the word with the video; `video` does not add one; `none` is silent | the three example entries, by ear | ⬜ not tested |
| S3 | Ten swaps in a row never leave a black screen or a frozen last frame | the end of each video reaches `→ idle`; a swap mid-video shows the new video's first frame, no flash of the old one | ⬜ not tested |
| S4 | The screen never sleeps with the app in front, over an hour on power | it is still lit | ⬜ not tested |
| S5 | System bars stay hidden; a swipe shows them and they hide again | by eye | ⬜ not tested |
| S6 | With screen pinning on, Home and Recents do nothing; Back+Overview held still leaves | by hand; write down the exact One UI wording of the pin step | ⬜ not tested |
| S7 | After a reboot one tap brings the station back and NFC works | by hand | ⬜ not tested |
| S8 | An unknown card shows the neutral line and the next known card plays | by hand | ⬜ not tested |
| S9 | A replaced `kartensatz/` directory is in use after leaving and reopening the app | diagnostics shows the new name and counts | ⬜ not tested |
| S10 | `cards.json` copied off the phone and onto a second install maps the same cards | the second install plays them without a rescan | ⬜ not tested |
| S11 | (branch `claude/stream-latency`) a streamed clip reaches its first frame within a second of the tap, and a local file within 300 ms | the diagnostics "S11" panel: tap Stream / Datei five times each on the home Wi-Fi, note lookup, link and first-frame ms; add E1's NFC latency for card-to-picture. From the Mac the network part alone measured 100–170 ms for the link and 240–470 ms for the whole 550 KB clip | ⬜ not tested |

## Material coupons

Three flat pieces, 40 × 40 mm, at 1.0 / 2.0 / 3.0 mm, PLA and PETG — six
prints, no supports, no fit involved. Print these before anything shaped.

## What is deliberately not measured yet

Video source quality, voice quality, the enclosure's tilt. None of them
changes a decision until E4 and E5 have answered whether the phone's own coil
reaches a card in a slot at all.
