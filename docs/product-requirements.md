# Product requirements

What the station has to do, for whom, and what it must never do.

## Who

Two children, two and three years old, who cannot read yet and use a box of
picture cards, today HHO's SIGNbox 1, with a symbol, a word and a sign drawing
on each. An adult sets the station up once and writes the stickers once.

## The interaction

```text
idle: black, the mark
card in  →  a ring around the mark, at once
         →  the sign video, full screen, with its sound, when its first frame is there
         →  again while the card stays, up to ten times, a second of ring between
card out →  the video finishes, then the mark
blank sticker  →  a grey ring, once
```

Nothing else is visible to a child: no word, no button, no menu, no bar, no
notification. Everything that happens, happens around the mark; the mark
itself never changes.

## Must

1. Run on the Samsung Galaxy A51, landscape, on a wall charger, screen never
   sleeping while the app is in front.
2. Read the card's sticker with the phone's own antenna, through the card and
   the holder wall, and play the right video within about a second.
3. Play the video from the provider the sticker names, with the login kept on
   the phone; store no clip.
4. Treat a resting card as one card, and a removed card as gone about a second
   after it left.
5. Let an adult write the stickers on the phone itself, card by card through
   the box, with the card shown so the right one comes out of the stack.
6. Fail quietly: no network, no link, an unknown sticker, all end in the ring,
   never in a message a child has to dismiss.
7. Stay in fullscreen; come back after a reboot with one tap.

## Must not

- Store a provider's clip on the phone.
- Show a child text, a button, or a menu.
- Need a computer, a file, or a map to play a written card.
- Keep a login anywhere but the app's private storage on the phone.

## Later, not now

A second provider (SignDict, or a sound without a sign); the family's own
spoken word through stimmquelle for silent clips; a proper enclosure; a browser
tool for anything that is not NFC.
