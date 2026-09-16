# ADR 0008 — The card holds the record, and the phone streams the video

**Status:** accepted · **Date:** 2026-09-13 · **Applies to:** `core/Model.kt` (`CardRecord`, `Provider`), `core/SignDigitalProvider.kt`, `app/nfc/AndroidTagSource.kt`

## Context

The first design (ADRs 0002, 0003, 0006) had the phone hold a directory of
videos and a map from sticker UIDs to entries, filled by a tool elsewhere and
copied over a cable. It was built, it ran, and the first afternoon with the
phone showed what it cost: a manifest to write by hand, a map to back up, a
folder to keep in sync, and the videos themselves, whose terms do not allow a
copy on the device without the provider's consent.

## Decision

**Each sticker carries what the phone needs — a provider, a ref and the word —
as one NDEF record of our own type. The phone reads it, asks the provider for a
link when the card comes in, plays the clip from that link, and stores
nothing.** The provider's login lives in the app's private preferences on the
phone. There is no manifest, no map, no directory.

## Why

**The card is the record because the card is the object.** It is what the
child holds and what gets lost, lent and replaced. A card that carries its own
meaning plays on any phone with the app; a map is one more thing that has to
be where the card is.

**Streaming is what the subscription allows.** Watching through it is ordinary
use; keeping the file needs consent that has not been given. Half a second of
ring before the video is the price, and the ring is designed for it.

**A phone on a kitchen table has Wi-Fi.** The appliance promise "works with
the router off" is given up knowingly; when a link cannot be fetched the card
rests on the ring and nothing is broken.

**One seam keeps it extensible.** A card names a provider and a ref; the
station never knows more. A second source is a second class.

## Consequences

An INTERNET permission, and with it the end of the structural "nothing can
leave" argument; what stands instead is narrower and checkable: the provider
class is the one place a URL is built, and nothing uploads. Two logins with
the same account, once on the phone and once wherever a browser tool ever
runs. A provider that changes its API stops the station until the app is
updated.

## Not to be "fixed" later

"Cache the clips so it works offline." That is storing, in different clothes,
and it is the provider's consent to ask for, not a code change.
