# ADR 0001 — zeigmal plays, and does not prepare

**Status:** accepted · **Date:** 2026-09-09 · **Applies to:** the whole repository

## Context

The obvious app for three hundred cards is one that also makes them: point the
camera at a card, recognise the symbol, find the video, write the sticker. Every
piece of that exists as a request in the handover, and every piece of it also
exists, or is planned, in a sibling: symbol search in bildquelle, printed cards
in bildhaft, spoken words in mitreden and stimmquelle, a Wortschatz across them.

## Decision

**zeigmal consumes a prepared Kartensatz and never produces one.** No OCR, no
camera, no symbol search, no audio generation, no TTS configuration, no PDF, no
video acquisition, no bulk authoring. The one thing the phone writes is the card
map, because the phone is where a sticker's UID is first seen.

## Why

**A second symbol model, audio pipeline or card format would be the mistake the
family has already paid for twice.** bildquelle and stimmquelle exist because
three products each wrote the same rules separately and none could learn from
the others.

**A player can be tiny and stay tiny.** Two modules, one activity, no network.
Everything on the "does not" list would bring a permission, a dependency or a UI
with it.

**A child never sees an editor by accident.** There is nothing to fall into.

## Consequences

Preparing a Kartensatz needs a tool that does not exist yet
(docs/lautstark-integration.md says where it belongs). Until it does, the
manifest is written by hand, which is fine for the ten cards the MVP needs.

## Not to be "fixed" later

"Just add a scan-and-map screen so an adult can assign cards on the phone." That
is the one authoring step the phone is genuinely the right place for, and it is
the diagnostics screen showing a UID plus a text editor — not a card editor.
Anything that shows a symbol picker on the phone has crossed the line.
