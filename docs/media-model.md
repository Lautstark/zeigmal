# The Kartensatz

What the player reads. One directory:

```text
kartensatz/
  manifest.json      the entries and their media          (zeigmal.kartensatz, v1)
  cards.json         which tag means which entry           (zeigmal.karten, v1)
  videos/            sign videos, H.264/AAC in MP4
  audio/             prepared spoken words, MP3
  symbols/           optional pictures for the idle or diagnostics screen
```

Why a format of its own and not the `.obz`: ADR 0002. Why the map is a file of
its own: ADR 0003.

## manifest.json

```json
{
  "format": "zeigmal.kartensatz",
  "version": 1,
  "name": "Erste Karten",
  "modified": "2026-09-09",
  "redistributable": false,
  "voice": "piper:de_DE-thorsten-medium",
  "entries": [
    {
      "id": "trinken",
      "label": "Trinken",
      "video": {
        "file": "videos/trinken.mp4",
        "provider": "signdigital",
        "licence": "subscription, private use",
        "attribution": "SIGNdigital",
        "url": "https://…"
      },
      "speech": "external",
      "audio": {
        "file": "audio/trinken.mp3",
        "text": "Trinken",
        "voice": "piper:de_DE-thorsten-medium",
        "key": "3f9c1a7b2e4d"
      },
      "symbol": { "metacom": "Trinken", "arasaac": 6061 }
    }
  ]
}
```

| field | | |
|---|---|---|
| `format`, `version` | required | a higher major refuses the file; unknown fields are ignored, so a later minor may add some |
| `name`, `modified` | name required | shown on the diagnostics screen |
| `redistributable` | required | `false` for anything holding METACOM names or licensed video. Stored so the constraint outlives the import; the app has no export path, and this is what a future one must read |
| `voice` | optional | the set's default voice, informational |
| `entries[].id` | required | lowercase slug, `[a-z0-9][a-z0-9-]{0,63}`. The join key the rest of the family calls `concept`; mitreden's slug rule (`Trinken` → `trinken`, `ä` → `ae`) produces it |
| `entries[].label` | required | the household's word as printed and spoken — kept apart from any source's own name for the picture |
| `entries[].video` | required | a relative path, bare or as `{ file, provider, licence, attribution, url }`. Never absolute, never `..` |
| `entries[].speech` | required | `video` (the video says the word), `external` (play `audio` too), `none` (silent on purpose). Never inferred — ADR 0005 |
| `entries[].audio` | when `external` | `{ file, text, voice, key }`. `voice` is a stimmquelle id in `backend:model` form; `key` is stimmquelle's `keyFor(text, voice, options)`; together they say whether the file still matches its word without the bytes |
| `entries[].symbol` | optional | `{ metacom, arasaac, file }`: a METACOM file *name* (the stem, no path, no pixels — what bildquelle's `idForName` resolves), an ARASAAC number, an optional picture inside the set |

What the reader does with a bad entry: drops it with a warning
(`ENTRY_INVALID`, `ENTRY_DUPLICATE`), or keeps it with less (`external`
without audio plays silent, `ENTRY_WITHOUT_AUDIO`). An entry whose video file
is not on disk is dropped (`MEDIA_MISSING`), so the card is *unknown* rather
than a card that starts nothing. An audio file missing only costs the word.

## cards.json

```json
{
  "format": "zeigmal.karten",
  "version": 1,
  "cards": [
    { "tag": "04:a7:91:b2:c3:d4:80", "entry": "trinken" },
    { "tag": "04a791b2c3d481", "entry": "trinken", "note": "zweite Karte" }
  ]
}
```

A tag is its UID as hex digits; separators and case do not matter, and only
the digits compare. Several rows may name one entry: a card laminated twice, a
sticker replaced. One tag naming two entries keeps the first and warns
(`CARD_DUPLICATE`); a tag naming an entry the manifest does not have is
dropped and warned about (`CARD_UNKNOWN_ENTRY`).

This is the one file the phone writes, and the one worth backing up on its
own: the media can be prepared again, three hundred scans cannot.

## Video

The cards are SIGNdigital's SIGNbox 1, and SIGNdigital's subscription carries
a sign video for each of their words, so the natural first source is that one
and the natural entry id is the card's word slugged. Whether the subscription's
terms allow a local copy on the child's own device is a question for their
terms and for SIGNdigital, not for this repository: no scraping, no download
automation, nothing provider-specific in the player. `provider: "signdigital"`
in the manifest is a label for the attribution, nothing more.

Local files, H.264 in MP4 with AAC or no audio, landscape. Media3 plays these
without transcoding; knopfpost never found a case that needed FFmpeg. A video
is a relative path with provenance beside it. There is no remote video in v1 —
no INTERNET permission, ADR 0006 — and the `url` field is where it came from,
not where to fetch it. Whether a source's terms permit a local copy is a fact
about the source and is settled before the file enters a Kartensatz; the
player neither knows nor cares where a file came from.

## Audio

MP3, mono, as mitreden stores it (44.1 kHz, 192 kbit/s) or as a preparation
tool chooses; the `voice`/`text`/`key` triple is what makes the file
accountable. The same voice as the family's other tools is the point.

## What is not modelled

A card's position, a category, a sentence, a board. The Wortschatz proposal in
`lautstark.github.io/docs/wortschatz.md` is where a zeigmal entry would become
a one-part *Wort* with a video as a third rendering; the identifiers above are
chosen so that day needs no translation table.
