# Architecture

The smallest thing that turns a sticker into a sign video.

```text
 sticker ──NDEF──► CardRecord ──► Provider.resolve(ref) ──► link ──► Media3 ──► screen
                                   (SIGNdigital today)
```

## Two modules

**`:core`** is plain Kotlin with no Android in it, and a Gradle task fails the
build on any `android.*` import. It holds everything that has to be exactly
right: the card record and its encoding, the station's state machine with its
repeat rounds, the presence filter that turns the reader's blinking into one
card in and one card out, the `Provider` interface and the SIGNdigital
implementation over OkHttp, and the SIGNbox 1 word list. Tested with JUnit,
coroutines-test and MockWebServer, in milliseconds.

**`:core`** also holds the two things that used to be a ViewModel:
`StationController` (reader events in through the presence filter, the
station's state out, the provider asked for the link) and `AdultModel` (login
as state, the writing mode walking the box). Both take a `CoroutineScope` and
their collaborators as constructor arguments and are tested with virtual time
against `FakeTagSource`, `FakeProvider` and an in-memory store from the
module's test fixtures.

**`:app`** is one activity, a ViewModel that only wires core pieces to the
hardware, and three screens drawn from core state. `AndroidTagSource` is the
`TagSource` over reader mode with the NDEF read and write; `Deps` is three
replaceable factories so a test can swap the hardware. Tests: the ViewModel's
wiring on the JVM with the fakes; the screens against plain state and the
whole path from a fake tag to Media3's first rendered frame as instrumented
tests on the device. The one thing no test can do is put a sticker on the
antenna.

## The state machine

```text
Idle ──CardSeen(record)──► Card(LOADING)          the ring
Card(LOADING) ──FirstFrame──► Card(PLAYING)       the video
Card(PLAYING) ──PlaybackEnded──► Card(LOADING, round+1)   again, up to MAX_ROUNDS
Card(PLAYING) ──PlaybackEnded──► Card(DONE)       rounds used up: the card's picture stays
Card(any) ──CardGone──► present=false … ──PlaybackEnded──► Idle
Card ──CardSeen(other)──► Card(other, LOADING)    at once
Idle ──CardSeen(no record)──► Unknown             the grey ring ──CardGone──► Idle
Card ──PlaybackFailed──► Card(DONE)               no network, no link: the ring stays
```

`MAX_ROUNDS` is 20 and the pause between rounds is one second, both named
constants in `Station`, to be tuned after watching a child with it.

## The provider seam

```kotlin
interface Provider {
    val id: String
    suspend fun resolve(ref: String): Media   // Media(videoUrl, cardImageUrl)
}
```

A card names a provider and a ref; the station never knows what is behind
them. `SignDigitalProvider` logs in with the local strategy, keeps the token in
the `CredentialStore` the app hands it, fetches the sign by slug and asks for
signed links, and logs in again once when a token has run out. Nothing is
written to disk: their terms allow watching through a subscription and do not
allow keeping the file. SignDict is a second class without a login; a provider
that returns only a sound is one more.

## Playback

Media3 ExoPlayer plays from the signed link. The view is on screen from the
start at alpha 0 and fades in when Media3 reports the first rendered frame;
the ring is what shows until then. Listener first, then prepare, then play:
a short clip can end before a listener attached afterwards hears about it.

## Kiosk behaviour

Landscape from the manifest. The screen stays on through `FLAG_KEEP_SCREEN_ON`
while the activity is in front. System bars are hidden with swipe-to-reveal;
screen pinning is a setting the adult turns on. No boot receiver, on purpose:
a boot receiver combined with pinning got knopfpost's build flagged by Play
Protect, and a single tap after a reboot is cheaper than finding out.

## Permissions

NFC, and INTERNET for the provider. The only outbound requests are the three
the provider's own player makes; nothing here uploads anything, and
`SignDigitalProvider` is the one place a URL is built.

## Build

vorlaut-app's toolchain, taken whole: Gradle 9.7, AGP 9.3, Kotlin 2.4, Compose
BOM 2026.08, Media3 1.11, OkHttp 5, Coil 3 for the one picture the adult mode
shows, minSdk 26, Spotless/ktlint, lint warnings as errors, SHA-pinned actions,
conventional commits gated by one script.
