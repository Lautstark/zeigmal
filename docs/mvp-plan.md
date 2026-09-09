# MVP plan

Five to ten cards, one phone, one printed fit test. The order is the order the
answers depend on each other; the hardware rows come first because a "no" at E4
or E5 changes everything after them, and nothing before.

## Done in this skeleton

- Repository, build, CI, commit gate, formatting; ADRs 0001–0006; these docs.
- `:cardset`: manifest and card-map readers with tests; directory loader that
  drops entries whose video is missing and downgrades entries whose audio is.
- `:app`: reader mode with removal listener, the state machine with tests,
  Media3 playback of video + external word, idle and unknown screens, the
  diagnostics screen with a tag log.
- `example/`: a hand-written Kartensatz with generated placeholder media and
  two scripts (`make-example-media.sh`, `push-example.sh`).

## Order

| step | what | done when |
|---|---|---|
| 1 | **Install and see a tag.** Build, `adb install`, push the example, open diagnostics, hold a sticker to the back | E1 has a result |
| 2 | **Map three cards.** Copy three UIDs from the log into `example/kartensatz/cards.json`, push again | S2, S8 pass with the colour-bar videos |
| 3 | **Presence.** Rest, lift, swap | E2, E3, E7 have results; `Station.next` gets whatever E2 justifies (probably nothing) |
| 4 | **Find the coil.** Grid on the back, mark the area | E4 recorded as [M] in docs/hardware.md |
| 5 | **Coupons.** Six flat prints, read-through table | E5 recorded; the wall at the coil decided |
| 6 | **Measure the cards and the phone.** Calipers, with the case decision made | docs/hardware.md has no [A] left in the phone and card tables |
| 7 | **Fit test.** One slot and one cradle, printed; 100 cycles | E8 has a result; slot width, depth and sticker position are fixed and written down |
| 8 | **Real media for five to ten cards.** Sign videos from the chosen source under its terms, spoken words from stimmquelle's chain (mitreden today, a script beside stimmquelle later), a manifest by hand | S1–S3 pass with real files; the attribution is visible on the diagnostics screen |
| 9 | **Appliance behaviour.** The phone set up per docs/hardware.md; pinning; an hour on power; a reboot | S4–S7 pass |
| 10 | **Backup and migration.** `cards.json` off the phone, onto a clean install | S10 passes; the backup rule in docs/media-import.md is confirmed by doing it |
| 11 | **A release build.** Copy vorlaut-app's signing arrangement (keystore in secrets, certificate fingerprint in `gradle.properties`, tag-driven version, `snapshot` and `release` jobs, a bare `zeigmal.apk` link) | a signed APK on a GitHub release installs over the debug one's replacement |

That is the MVP. Success criteria 1–12 from the handover map onto rows E1, E4,
E5, S1–S4, S7, S10 and step 8.

## After the MVP, in this order

1. The preparation tool — where docs/lautstark-integration.md says, not here.
2. The full library: ~300 stickers, ~300 scans, one `cards.json`.
3. Enclosure CAD in `case/`, in the talker case's style (tagged dimensions,
   `verify.py`, a "measure first" table), only after step 7.
4. Adult mode beyond diagnostics, if anything is actually missing: a PIN in
   front of the diagnostics screen, a "reload" button. Not a content editor.
5. `products/zeigmal.json` in Lautstark/design and a token port, if a screen
   ever needs the shared look.
6. A mark for the idle screen and the launcher icon.

## Not on the plan

OCR, camera recognition, symbol search, audio generation, TTS settings, PDF,
video acquisition, bulk authoring, accounts, cloud, a second device.
