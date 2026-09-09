# ADR 0002 — A Kartensatz is its own format, not an .obz

**Status:** accepted · **Date:** 2026-09-09 · **Applies to:** `:cardset`, docs/media-model.md

## Context

The family already has a package format that a viewer on an Android device
reads: `exchange/SPEC.md` in vorlaut-editor, the `.obz` that vorlaut-app opens.
Reusing it would mean one importer, one licensing flag, one set of fixtures.

## Decision

**A Kartensatz is a directory with `manifest.json`, `cards.json` and media,
in a format of its own** (`zeigmal.kartensatz` / `zeigmal.karten`, version 1).
It borrows the spec's discipline — a format/version header, a required
`redistributable`, relative paths that may not leave the directory, a reader
that is strict about the set and lenient about entries — and none of its fields.

## Why

**The .obz has no video, no card, and no external trigger, and says so.** Its
media lists are closed (PNG/JPEG, Ogg Opus), its activation model is a pointer on
a grid, and §4 declares every unknown `ext_lautstark_*` field ignored. A video
in an .obz is `image_undecodable`; a `card_uid` on a button is silently dropped
by the only viewer. The spec's own 1.5.0 changelog names stretching a shared
field to one product's need as the category error its two namespaces exist to
prevent.

**mitreden's ZIP has the right file names and no manifest.** `trinken.mp3` is
the right shape for an audio file, and the slug rule is worth copying, but a
filename is a lossy key and zeigmal has a real manifest to put the facts in.

**A directory, not a ZIP, because the media is large and the phone is the
store.** A ZIP would be held twice, and a directory can be inspected and
patched with a file manager over a cable.

## Consequences

Nothing in the family reads a Kartensatz but zeigmal. The preparation tool, when
it exists, writes one; the identifiers inside it (docs/media-model.md) are the
family's, so a later Wortschatz or druckwerk target lines up without a
translation table.

## Not to be "fixed" later

"Fold it into the exchange spec as `ext_zeigmal_*`." Only if vorlaut-app gains a
reason to open a Kartensatz. Two viewers reading one file is the test; one
viewer reading two files is not a problem.
