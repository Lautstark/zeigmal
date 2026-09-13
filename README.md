# zeigmal

**Karte rein. Gebärde sehen.**

A small station on the table: a Samsung Galaxy A51 lying in a printed holder,
a slot behind it, and a box of picture cards with NFC stickers on them. A child
pushes a card into the slot and the phone plays the sign video for the word on
it, again and again while the card lies there.

I am building it for my children, who have around three hundred picture cards
with a symbol, a word and a sign on each. The cards exist; the station is what
was missing. Which cards does not matter to it: a bought card set, cards printed
from METACOM or ARASAAC symbols, or a plain sticker on anything. Where the
videos come from does not matter to it either: a provider's files, an open
dictionary, or a parent in front of a camera.

zeigmal belongs to the [Lautstark](https://github.com/Lautstark) family of AAC
tools. Like its siblings it runs without a server or an account, and the
child-facing part has no menu, no button and no keyboard.

## What it does, and what it deliberately does not

| | |
|---|---|
| **does** | reads the record on a card's NFC sticker, asks the provider named there for the sign video, plays it while the card lies in the slot, and lets an adult write the stickers on the phone itself |
| **does not** | store a video, keep a map or a manifest, need a computer, show a child any text or button, or talk to anything but the one provider the sticker names |

Everything the phone needs is on the card: [docs/card-record.md](docs/card-record.md).

## Status

**Working end to end on the phone with the first stickers, before the first
real card is written.** The app reads and writes stickers, streams from
SIGNdigital with a login kept on the phone, and the child's screen is the
family's mark with a ring around it. The first measurements on the Galaxy A51
are in [docs/experiments.md](docs/experiments.md); the order of what comes
next is [docs/mvp-plan.md](docs/mvp-plan.md).

## Architecture at a glance

```text
sticker ──NDEF──► card record ──► provider (SIGNdigital) ──► signed link ──► video
```

Two Gradle modules. `:core` is plain Kotlin with no Android in it: the card
record, the station's rules, the presence filter, the provider seam. `:app` is
one activity: reader mode in, Media3 out, three screens. [docs/architecture.md](docs/architecture.md) has the rest.

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
./gradlew :core:check :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Hold the faint dot in the bottom-right corner for two seconds and enter the
PIN (the first entry sets it) for the adult mode: log in to SIGNdigital once,
then write stickers card by card through the box. The settings behind the gear
hold the number of loops, the PIN and the login.

Instrumented tests run on Gradle's managed emulator only
(`./gradlew :app:emulatorDebugAndroidTest`); the build refuses to run them on
an attached phone because they uninstall the app and its data.

## Documentation

| | |
|---|---|
| [docs/product-requirements.md](docs/product-requirements.md) | what the station must do, for whom, and what it must never do |
| [docs/architecture.md](docs/architecture.md) | the two modules, the state machine, the provider seam |
| [docs/lautstark-integration.md](docs/lautstark-integration.md) | what was inspected in the sibling repositories, what is reused, what stays apart |
| [docs/card-record.md](docs/card-record.md) | what is on a sticker, how it is read and written |
| [docs/nfc.md](docs/nfc.md) | Android reader mode, NTAG213, presence as measured on the A51 |
| [docs/hardware.md](docs/hardware.md) | the Galaxy A51, the cards, the stickers, the measurements owed |
| [docs/enclosure.md](docs/enclosure.md) | the pre-CAD checklist |
| [docs/experiments.md](docs/experiments.md) | the hardware experiments, as an acceptance table with a date column |
| [docs/mvp-plan.md](docs/mvp-plan.md) | the ordered plan to the first working station |
| [adr/](adr/README.md) | the decisions that look like oversights from outside |

## Licence

MIT. The sign videos and the card images a provider serves keep their own
terms; none of them is in this repository, and the app keeps none of them.
