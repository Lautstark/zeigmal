# ADR 0012 — A list is a Lernliste on the site, and the phone only reads it

**Status:** accepted · **Date:** 2026-11-08 · **Supersedes:** [0011](0011-a-card-set-is-a-list-of-words-and-slugs-in-the-app.md) · **Applies to:** `core/Lernliste.kt`, `core/CardSet.kt`, `core/SignDigitalProvider.kt`, `core/AdultModel.kt`, `core/WriteLog.kt`, `app/ui/ListsScreen.kt`

## Context

ADR 0011 made a set a text file in the app: a new list was a commit and an
install. The family wants more lists of their own, made without a developer.
SIGNdigital already has lists: Lernlisten, made on sign-digital.de with the
site's own search, kept with the account zeigmal already logs in to. The
writing mode also kept ticks and a place per set, which stopped making sense
once a sign can sit in several lists and the family has some cards twice.

## Decision

**Every list is a SIGNdigital Lernliste, and the phone only reads them.** The
adult mode opens on the account's Lernlisten, newest first (`GET /api/lists`
with the login's own user id, the JWT's `sub`). A tap walks that list from
its first card. Lists are made, filled, ordered and deleted on the site; the
app never writes to the account. The last good load is kept on the phone, so
the overview stands without a network.

**A list is only a grouping.** Nothing is kept per list: no ticks, no place.
Instead every sticker written goes into an append-only **write log** on the
phone (`schreibprotokoll.jsonl`: when, slug, word, sign id, list, sticker
id). Beside each card the writing mode shows how often its sign has gone on a
sticker, from any list; a sign written twice counts twice.

The two sets that were in the app, SIGNbox 1 and Halloween, were moved into
Lernlisten of the same names on 2026-11-08 and their files deleted. The old
ticks on the family's phone became log entries without a time.

## Why

**The site has the search, the phone has the antenna.** Picking a sign for a
word is done with the site open (ADR 0011 said so); now the result of that
picking lives where it was made instead of being copied into a file.

**Read-only keeps the account safe.** The site's write path sends whole list
documents back; a phone that only reads cannot break a list.

**A log is cheaper than state.** Appending a line costs nothing, answers
"what, when, where and how often" later, and never decides anything for the
adult: the writing mode does not skip or resume by it.

## Consequences

A new list is a Lernliste on sign-digital.de, and "↻ Neu laden" on the phone.
Lernlisten have no label per entry, so a card's word is the site's name for
the sign; where the printed card says otherwise, the sticker says the site's
word. A slug the site renames is found by the site's own list, not by a file
here.

The site keeps one login at a time: when zeigmal logs in again it may log
the same account out elsewhere.

## Not to be "fixed" later

"Let the phone add a sign to a list." That is writing to the account, and the
site does it better. "Keep where the adult stopped in each list." The stack in
the adult's hand is where they stopped.
