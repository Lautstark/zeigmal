# Getting a Kartensatz onto the phone

A cable. That is the whole mechanism in v1 (ADR 0006).

## Where

```text
Android/data/de.lautstark.zeigmal/files/kartensatz/
  manifest.json
  cards.json
  videos/  audio/  symbols/
```

The app's external files directory needs no permission, is visible to a
computer over USB (MTP) and to `adb`, is excluded from every Android backup
domain, and is deleted with the app.

## How

With a laptop and `adb`:

```sh
tools/push-example.sh              # the example set
adb push my-kartensatz/. /sdcard/Android/data/de.lautstark.zeigmal/files/kartensatz/
```

Without `adb`: plug the phone in, choose *File transfer*, open
`Android/data/de.lautstark.zeigmal/files/`, drop the `kartensatz` folder in.
On the phone, leave the app and open it again; the diagnostics screen shows
the new name and counts.

Replacing a set is copying over it. There is no staging and no atomic swap;
if a half-copied set is ever opened mid-copy, the reader refuses or warns and
the next resume reads the finished one.

## Backing up the card map

`cards.json` is the one file that cannot be prepared again. After scanning:

```sh
adb pull /sdcard/Android/data/de.lautstark.zeigmal/files/kartensatz/cards.json ~/Sicherung/zeigmal-karten-$(date +%F).json
```

or copy it out in the file manager. Put the copy where the family's other
Sicherung lives. Moving to another phone is the same file pushed into the same
directory beside the same manifest — no rescan.

The app writes `cards.json` when a card is mapped on the phone; today that is a
text edit on the laptop, and the write path exists in `Cards.write` for the day
the diagnostics screen gains a "this UID is *trinken*" step.

## Not in v1

A ZIP import through a file picker (the media would exist twice), a Wi-Fi
receiver like vorlaut-app's (costs the no-network guarantee), a shelf entry on
sammlungen (a household's METACOM set cannot be published). Each is a decision
with a place to be taken, not a missing feature.
