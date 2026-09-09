# ADR 0004 — A native Android app, in Kotlin with Compose and Media3

**Status:** accepted · **Date:** 2026-09-09 · **Applies to:** `:app`, `gradle/libs.versions.toml`

## Context

Every other Lautstark product is a browser page, including wochenwerk's wall
device, and the family's shared code is TypeScript. A PWA in Chrome on the
phone would inherit the shared look and the shared packages. The family also
has one native Android app, vorlaut-app, and a Kotlin/Compose/Gradle setup that
works.

## Decision

**Native Android. Kotlin, Jetpack Compose, Media3 ExoPlayer for both video and
the spoken word, kotlinx-serialization for the manifest, no framework beyond
that.** The build is vorlaut-app's: Gradle 9.7, AGP 9.3, Kotlin 2.4, minSdk 26,
Spotless with ktlint, lint warnings as errors, SHA-pinned actions. The manifest
reader is a plain JVM module with a task that fails the build on any
`android.*` import.

## Why

**Web NFC is Chrome-only, gated on a user gesture, and gives no removal.** An
appliance that has to come up after a power cut with nobody in the room cannot
be built on a permission prompt; wochenwerk's hardware notes reached the same
conclusion for WebSerial and moved the reader out of the page.

**Reader mode is the only way to own the NFC hardware.** With it, no other app,
no payment stack and no system sound gets the tag first. A browser cannot ask
for it.

**Fullscreen, keep-awake, orientation and screen pinning are Android facts.**
Each is one line natively and a workaround in a browser.

**Media3 says when a two-second file ended.** Both siblings that used
`MediaPlayer` worked around its completion callback; knopfpost's Media3
ordering (listener, then prepare, then play) is copied verbatim because the
failure it prevents — a station stuck on the last frame — is exactly the one a
child cannot report.

**What is lost is the shared look, and the player screen does not need it.** A
black surface with a video on it claims the same exemption wochenwerk's board
does. If a settings screen ever grows, it takes a `products/zeigmal.json` in
Lautstark/design and ports the tokens the way vorlaut-app does.

## Consequences

No shared TypeScript can be used; the identifiers in docs/media-model.md are the
only thing zeigmal and its siblings share. The APK is sideloaded, never on a
store; a release pipeline with a signing key is a later step (docs/mvp-plan.md).

## Not to be "fixed" later

"Add Hilt / a navigation graph / a repository layer." One activity, one
ViewModel, one state machine. The moment a second screen needs navigation, it
is a sign the app has grown a feature ADR 0001 forbids.
