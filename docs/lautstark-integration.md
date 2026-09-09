# Lautstark integration

What the sibling repositories hold, read against their source on 2026-09-09,
and what zeigmal takes from each. The rule the family applies to shared code —
a thing moves into a package when a *second* product demonstrably needs it,
not before — is applied here too: zeigmal reuses conventions, identifiers and
ideas, and extracts no code.

## The inspection, in one table

| repository | what it is | what zeigmal does with it |
|---|---|---|
| **vorlaut-app** | native Android viewer for `.obz` board packages; Kotlin, Compose, a plain-JVM parser module | **copied as the build and the shape**: Gradle setup, version catalog, Spotless, CI, commit gate, backup-exclusion rules, dark theme, the parser-as-JVM-module idea with the `forbidAndroidImports` task, the nothing-throws `Parsed` result. Not copied: the `.obz` format (ADR 0002), the LAN receiver (ADR 0006), the design-token port (no shared-look screen yet) |
| **vorlaut-editor `exchange/SPEC.md`** | the `.obz` package spec | **referenced** for its discipline: format/version header, required `redistributable`, relative paths only, strict-set/lenient-entry, "the fixture is normative". Its §5.2 licensing rule is inherited as a decision |
| **knopfpost** (SteffiPeTaffy, not the org) | a child-facing Android tablet appliance with Media3 video | **patterns adopted**, no code: the ExoPlayer listener-prepare-play order, `texture_view`, `STATE_ENDED` → flag → effect, `FLAG_KEEP_SCREEN_ON` in `onCreate`, the pure state-machine file, the debug-gated `startLockTask`, the long-press-for-adults gesture, and its warnings (no boot receiver with pinning, no wake-lock-to-wake-screen, three Samsung battery settings that defeat everything) |
| **stimmquelle** | the family's voice: piper voices catalogue, loudness contract, `keyFor` fingerprint, MP3 encoder; browser and Node, nothing for Android | **identifiers reused**: an entry's audio carries `voice` (`backend:model`), `text` and `key`, so whether a file still matches its word is checkable without the bytes. The audio *files* are produced outside zeigmal by whatever calls stimmquelle. `de_DE-kerstin-low` is flagged `rushesFragments` — single words come out as mush — so the set's default voice should be `de_DE-thorsten-medium` |
| **mitreden** | sentence → audio workshop; exports a flat ZIP of `<slug>.mp3` and an Anybook `.abs` | **slug rule reused** (`Trinken` → `trinken`, `ä→ae`, max 40 chars); its ZIP is the right file shape and has no manifest, so a Kartensatz carries the facts mitreden's filenames drop. mitreden does not become the preparation tool: its unit is a sentence in a Sammlung and it has no headless mode |
| **bildquelle** | ARASAAC + METACOM symbol search; browser-only, closed `ProviderId` | **identifiers reused**: a METACOM symbol is referenced by *name* (the stem `idForName` resolves), an ARASAAC one by number, never a path and never pixels. The METACOM rule — references may be stored, bytes may not leave — is inherited |
| **bildhaft** | sentences → symbol strips, card sheets, the Wortschatz | **nothing reused now**; its `concept` (lowercased token) is what the entry id lines up with, and its card-sheet geometry is where printed card faces would come from |
| **wochenwerk** | a calendar-driven wall board; already models NFC cards (`Card { nfc, symbol: {source,id,label}, speech }`) | **the nearest precedent, adopted**: several tags per card, hex-digits-only comparison, tag map kept with the reader not the record, presence measured before designed around, the 100-cycle physical gate, a failure-state list, the board's exemption from the shared look |
| **druckwerk** (archived) | printable material; a *targets* seam of code carrier + audio bundle, with "QR/NFC" named as a future target | **referenced**: a Kartensatz is what a druckwerk NFC target would emit — printed card faces plus a map — and the seam's rule that an export file never holds provider pixels binds here too |
| **design** | tokens, components, conventions | **not a dependency**. The player screen is a black surface and claims wochenwerk's exemption. A settings screen, if one ever exists, takes `products/zeigmal.json` and a Kotlin token port headed like vorlaut-app's |
| **sicherung**, **werkzeuge**, **sammlungen** | folder backup (Chromium desktop only), small browser helpers, the public shelf | **not applicable**: no browser, and a household's 300 METACOM cards cannot be a shelf entry (the shelf refuses the `metacom` string in any file, on purpose) |
| **card-case** (`~/Code/card-case`, not a repository) | an OpenSCAD tray for 25 × 25 mm velcro cards | **clearance and wall numbers reused** in docs/enclosure.md; the cards themselves are a different family from the sign cards |

## Duplicated concepts, named

- **A card.** wochenwerk has `Card`, bildhaft has `wordcard`, druckwerk had
  `kartensatz`, zeigmal has `MediaEntry` + `CardMapping`. Four dialects of the
  family's one noun; the Wortschatz proposal in `lautstark.github.io/docs` is
  where they would meet, and a zeigmal entry is a one-part *Wort* by its
  derivation, with a video as a third rendering beside picture and sound. Not
  unified now: the Wortschatz has one reader and adopting it would be adopting
  a guess.
- **A spoken word.** mitreden and stimmquelle produce it; vorlaut-app and
  zeigmal play it. The join is the fingerprint, not a file.
- **A symbol reference.** bildquelle defines it; wochenwerk and zeigmal store
  it by name/number.

## Where preparation belongs

Not on the phone (ADR 0001). The plan is the family's own split, the one
vorlaut-editor and vorlaut-app already have: **a browser tool on GitHub Pages
prepares, the app plays.**

The browser tool, not yet started, would hold a Kartensatz in the browser the
way its siblings hold a Sammlung, and do the four things a Kartensatz needs:

1. **Entries** — a word list typed or pasted, ids by mitreden's slug rule,
   symbols by name or number through bildquelle where a printed card face is
   wanted.
2. **Videos** — a file per entry, from wherever the person has them, or
   **recorded in the browser**: the tool shows the word, the camera records a
   few seconds, the parent signs and speaks. That is the source with no licence
   and the child's own people in the picture.
3. **Spoken words** for entries whose video is silent, through stimmquelle's
   chain, the same voice as mitreden and vorlaut, with the fingerprint facts
   written into the manifest.
4. **Export** — the Kartensatz as a directory or ZIP to copy onto the phone,
   and the standing Sicherung for the card map.

What the browser tool does not do is read stickers. A desktop browser cannot
reach a USB NFC reader on macOS (PC/SC claims the device before WebUSB can),
and a phone is the reader anyway. So **mapping happens on the phone, in the
adult mode**: the app walks the entries, says which card to hold to the back
next, records the UID, and writes `cards.json`. That is the one authoring step
ADR 0001 leaves to the phone, and it shows a word, never a symbol picker.

Until the tool exists, `example/` shows the manifest and a person writes it by
hand for the ten MVP cards.

## Rules inherited, not restated

- METACOM per person: inbound to the child's device is sanctioned, outbound
  from the app is not (`exchange/SPEC.md` §5.2, the family memory on it).
- No server, no accounts, anywhere in the toolchain (vorlaut-diy-talker ADR
  0002, which states itself as family-wide).
- Trunk-based, no pull requests; conventional commits; English code, German
  UI; a generated file carries only what its inputs determine.
