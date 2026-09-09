# ADR 0005 — Who speaks is written in the manifest, never read off the video

**Status:** accepted · **Date:** 2026-09-09 · **Applies to:** `MediaEntry.speech`, `SignVideo.kt`

## Context

Some sign videos speak the word, some are silent, and some carry a track that
is not the word — room noise, a different phrasing, a different voice. The
player could inspect the file and add a spoken word when it finds no audio
track.

## Decision

**Every entry carries `speech: video | external | none`, the reader refuses an
entry without it, and the player never inspects the file.** With `external`,
the prepared audio starts with the video. With `none`, nothing is said, on
purpose. An `external` entry whose audio is missing plays silently and is
warned about; the reader downgrades it to `none` rather than guessing.

## Why

**An audio track is not a spoken word.** The technical fact and the child's
experience are different questions, and only a person who watched the video can
answer the second one.

**The same voice, everywhere.** A prepared file from stimmquelle's chain is the
voice the child hears from mitreden and vorlaut. Android TTS would be a fourth
voice; it stays an emergency fallback at most and is not in the MVP.

**A default that is silently applied is a decision nobody made.** Requiring the
field makes the preparation say what it knows.

## Consequences

Hand-written manifests need one more field per entry. The reader has a test
that a missing `speech` drops the entry with `ENTRY_INVALID`.

## Not to be "fixed" later

"Default `speech` to `video` when the file has an audio track." See the first
reason.
