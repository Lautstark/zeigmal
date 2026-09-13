# The card record

Everything the phone needs to play a card is on the card's own NFC sticker. No
map on the phone, no manifest, no folder to copy: a written sticker plays on
any phone with the app and a login.

## What is on the sticker

One NDEF record of the external type `lautstark.de:zeigmal`, whose payload is
a few lines of UTF-8:

```text
zeigmal/1
provider=signdigital
ref=trinken
label=trinken
```

| field | |
|---|---|
| first line | the format and its version; anything else is not ours and the sticker counts as blank |
| `provider` | who serves the video. `signdigital` today; a second provider is a second class behind the same interface |
| `ref` | what the provider calls this sign. For SIGNdigital it is their slug: lowercase, umlauts to plain letters, ß to ss, the rest hyphens (`draussen`, `abend-s`) |
| `label` | the word, for the log and for an adult reading the sticker with any NFC app. The child never sees it |

Under a hundred bytes; an NTAG213 has 144. Unknown lines are ignored, so a
later version may add a field (`voice=1` is the likely first) without breaking
stickers already written. The code is `CardRecord` in `core`, with its tests.

## Reading

The reader is in reader mode while the app is in front. Every discovered tag is
read for the record and reported as seen, with or without one. A sticker
without a record is the grey ring: seen, nothing behind it.

The Galaxy A51 reports a resting card seen, gone 210 ms later and seen again
80 ms after that, three times a second; `Presence` in `core` holds a card for
a second past the last reader `gone`, so the station sees one card in and one
card out. A read that fails on a blink is simply retried on the next one.

## Writing

The adult mode walks the SIGNbox 1 list and writes the record for the current
word onto the next sticker held to the phone. A sticker that already carries a
record is reported instead of overwritten, and overwriting is a button. A
blank NTAG213 is NDEF-formatted from the factory; one that is not is formatted
first. The record is not locked, so a sticker can be rewritten when a card
changes its word or its provider.

## What is not on the sticker

Nothing licensed, nothing personal, no login. The provider's login lives in
the app's private preferences on the phone and is typed there once.
