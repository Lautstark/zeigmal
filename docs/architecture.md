# Architecture

The smallest thing that turns a sticker into a video.

```text
                 :app                                   :cardset (plain JVM)
 ┌──────────────────────────────────────┐    ┌──────────────────────────────────┐
 │ MainActivity                         │    │ KartensatzDirectory.load(dir)    │
 │   reader mode on/off with resume     │    │   Manifest.parse → CardSet       │
 │   keep screen on, hide bars          │    │   Cards.parse    → [CardMapping] │
 │                                      │    │   Loaded.Ready.resolve(TagId)    │
 │ NfcReader ── TagEvent.Seen/Gone ──►  │    └──────────────────────────────────┘
 │ StationViewModel                     │
 │   Station.next(state, event)  ◄──────┼── pure state machine, tested on the JVM
 │   UiState (station, log, loaded)     │
 │                                      │
 │ ZeigmalApp (Compose)                 │
 │   Idle · Unknown · SignVideo         │
 │   DiagnosticsScreen behind long press│
 └──────────────────────────────────────┘
```

## Two modules

**`:cardset`** reads the Kartensatz. It is Kotlin with no Android in it, and a
Gradle task fails the build on any `android.*` import, so the part that decides
which video a card starts is tested in milliseconds with no emulator. Nothing in
it throws at the caller: a file is `Accepted` with warnings or `Rejected` with a
code, because the caller is a screen. Strict about the set (a wrong header
refuses the file), lenient about entries (a bad entry costs one card).

**`:app`** is one activity and one ViewModel. `NfcReader` owns reader mode;
`Station` is the state machine; `SignVideo` is a Media3 `PlayerView` and, when
the entry says `external`, a second ExoPlayer for the word; `DiagnosticsScreen`
is the instrument for the hardware experiments.

## The state machine

```text
Idle ──CardSeen(known)──► Card(entry, tag, run, LOADING)   the card face is on screen
Card(LOADING) ──FirstFrame──► Card(PLAYING)                  the video fades in over it
Card(PLAYING) ──PlaybackEnded──► Card(ENDED)                 the card face again
Card(ENDED) ──CardGone──► Idle
Card(any) ──CardGone──► Card(present = false) … ──PlaybackEnded──► Idle
Card ──CardSeen(other)──► Card(other, tag, 1, LOADING)   at once
Card ──CardSeen(same)──► Card(same, tag, run+1, LOADING)  the video restarts
Idle ──CardSeen(unknown)──► Unknown(tag) ──CardGone(tag)──► Idle
```

The card is on screen the instant the tag is seen; the video is invisible
until Media3 reports its first rendered frame, then fades in over 150 ms, and
the spoken word starts on that same frame. A removed card never stops a running
video (removal reporting is what docs/experiments.md E2 measures); it only
decides whether the card face or the idle screen follows the video.

## Playback

Media3 ExoPlayer, one instance per video, built in `remember(file, run)`,
prepared and started inside the `DisposableEffect` *after* the listener is
attached, released on dispose. That order is the difference between reliable
end detection for a two-second local file and a station stuck on its last
frame. The `PlayerView` is inflated from the one XML layout because
`surface_type="texture_view"` and `use_controller="false"` exist only as XML
attributes; the texture view is what removed black-frame flicker in knopfpost.

The spoken word is a second ExoPlayer started with the video. No audio focus
handling yet: the station is the only thing making a sound. A fade on
interrupt (vorlaut-app's `Speech.kt` does 42 ms) is worth adding once a swap
audibly clicks.

## Where the content lives

`getExternalFilesDir(null)/kartensatz/` — reachable by cable without a
permission, deleted with the app, excluded from every backup domain. ADR 0006.

## Kiosk behaviour

Landscape from the manifest (the A51 is a phone; Android honours it). The
screen stays on through `FLAG_KEEP_SCREEN_ON` while the activity is in front,
which makes the device's own timeout irrelevant. System bars are hidden with
swipe-to-reveal; screen pinning is a setting the adult turns on
(docs/hardware.md). No boot receiver: knopfpost's release build was flagged by
Play Protect for a boot receiver combined with pinning, and a single tap after a
reboot is the price of not finding out whether the A51 does the same.

## What is deliberately absent

A dependency-injection framework, a navigation graph, a database, a repository
layer, a settings store, network code, TTS. Each would be an answer to a
question ADR 0001 says this repository does not ask.

## Build

vorlaut-app's toolchain, taken whole: Gradle 9.7, AGP 9.3, Kotlin 2.4, Compose
BOM 2026.08, Media3 1.11, minSdk 26, Spotless/ktlint, lint warnings as errors,
CI actions pinned by SHA, conventional commits gated by one script. Release
signing with a certificate-fingerprint check is a later step and will be a copy
of vorlaut-app's, not a new design.
