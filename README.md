# zeigmal

**Karte rein. Gebärde sehen.**

A small station on the table: a Samsung Galaxy A51 lying in a printed holder,
a slot behind it, and a box of picture cards. A child pushes a card into the
slot and the phone plays the sign for the word on it. Where the video does not
say the word, the phone says it, in the same voice the family's other Lautstark
tools use.

I am building it for my children, who have around three hundred cards with a
METACOM symbol, a word and a sign on each. The cards exist; the station is what
was missing.

zeigmal belongs to the [Lautstark](https://github.com/Lautstark) family of AAC
tools. Like its siblings it runs without a server or an account, and the
child-facing part has no menu, no button and no keyboard.

## What it does, and what it deliberately does not

| | |
|---|---|
| **does** | reads the NFC sticker on a card, looks the tag up, plays the sign video, plays a prepared spoken word when the manifest says so, and waits for the next card |
| **does not** | make cards, search symbols, generate audio, fetch videos, edit anything, talk to a network, or show a child any user interface |

Card preparation happens elsewhere in the family and arrives here as a
**Kartensatz**: a directory holding a manifest, a card map and the media. What
that directory looks like is [docs/media-model.md](docs/media-model.md); how it
gets onto the phone is [docs/media-import.md](docs/media-import.md); which
sibling makes which part of it is
[docs/lautstark-integration.md](docs/lautstark-integration.md).

## Status

**Skeleton, before the first hardware run.** The app builds, its reader is
tested on the JVM, and the phone side is wired: reader mode, one video player,
one diagnostics screen behind a long press. Nothing has been held against the
Galaxy A51 yet, and the whole design rests on two things only a real card can
answer: where the phone's antenna is, and what Android says when a card stays,
goes, or is swapped. [docs/experiments.md](docs/experiments.md) is the list;
[docs/mvp-plan.md](docs/mvp-plan.md) is the order.

## Architecture at a glance

```text
NFC tag UID  →  cards.json  →  manifest.json entry  →  video (+ audio when speech = external)
```

Two Gradle modules. `:cardset` reads the Kartensatz and is plain Kotlin with no
Android in it, so the part that decides which video a card starts is tested in
milliseconds. `:app` is one activity: reader mode in, Media3 out, a pure state
machine in between. [docs/architecture.md](docs/architecture.md) has the rest.

## Hardware

A Samsung Galaxy A51, landscape, slightly tilted, on power, inside an
FDM-printed holder with a vertical card slot behind it. Nothing else — no
external reader, no microcontroller, unless the phone's own NFC turns out not to
reach through the card. [docs/hardware.md](docs/hardware.md) lists what is
known and what has to be measured; [docs/enclosure.md](docs/enclosure.md) is
the checklist that has to be ticked before any CAD.

## Building

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :cardset:check :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
tools/make-example-media.sh && tools/push-example.sh
```

The example Kartensatz under `example/` is colour bars and a tone, not signs;
it exists so the mechanics can be tried before any licensed video is in hand.
Open the app, long-press the black screen, hold a sticker to the back of the
phone, and the diagnostics screen shows the UID to write into `cards.json`.

## Documentation

| | |
|---|---|
| [docs/product-requirements.md](docs/product-requirements.md) | what the station must do, for whom, and what it must never do |
| [docs/architecture.md](docs/architecture.md) | the two modules, the state machine, the players |
| [docs/lautstark-integration.md](docs/lautstark-integration.md) | what was inspected in the sibling repositories, what is reused, what stays apart |
| [docs/media-model.md](docs/media-model.md) | the Kartensatz: manifest, card map, identifiers |
| [docs/media-import.md](docs/media-import.md) | getting a Kartensatz onto the phone, backing the card map up |
| [docs/nfc.md](docs/nfc.md) | Android reader mode, NTAG213, presence, what is known and what is not |
| [docs/hardware.md](docs/hardware.md) | the Galaxy A51, the cards, the stickers, the measurements owed |
| [docs/enclosure.md](docs/enclosure.md) | the pre-CAD checklist |
| [docs/experiments.md](docs/experiments.md) | the hardware experiments, as an acceptance table with a date column |
| [docs/mvp-plan.md](docs/mvp-plan.md) | the ordered plan to the first working station |
| [adr/](adr/README.md) | the decisions that look like oversights from outside |

## Licence

MIT. The sign videos, the METACOM symbols and the voice models a Kartensatz
holds keep their own terms; none of them is in this repository and none is
shipped with the app.
