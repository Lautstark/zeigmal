# ADR 0011 — A card set is a list of words and slugs in the app

**Status:** superseded by [0012](0012-a-list-is-a-lernliste-on-the-site-and-the-phone-only-reads-it.md) · **Date:** 2026-10-31 · **Applies to:** `core/SignBox.kt`, `core/src/main/resources/*.txt`, `core/AdultModel.kt`, `app/ui/WriteScreen.kt`

## Context

The family made Halloween cards: 24 words of their own, not a box anyone
sells. ADR 0009 let the writing mode walk one list, SIGNbox 1, and said a
second one was "a list and a text field away". For Halloween the list alone
is not enough: half the words have no sign of their own on SIGNdigital and
another stands in for them (Show is Theater, Feuer is Lagerfeuer, Saures is
sauer (Geschmack)), and two cards may share one sign (verkleiden and Kostüm).
Picking those is a decision a person makes with the site open, not a rule.

## Decision

**A set is a text file in `:core`'s resources, one card per line, `Wort =
slug`.** A bare word still gets SIGNbox 1's slug rule. The writing mode shows
the sets above its list; a tap switches, and each set keeps where it stopped.
What is done is kept by the card's word, not its slug, so two cards on one
sign are two cards. SIGNbox 1 keeps its old storage keys; slugs stored there
before read as their word.

## Why

**The slug is the decision, so the file says it.** Deriving it would have
been wrong for half the words, and a lookup table on the side is the same
file with worse names.

**A set in the app, not a set on the phone.** A new set is a commit and an
install, which is how the phone gets everything else. Nothing on the phone
edits or imports lists.

## Consequences

A new set is a new file and a line in `SignBox.sets`. The slugs in it are
checked against the site once, by hand, when the set is made; the site does
rename slugs (on 2026-10-31 five of SIGNbox 1's no longer existed and were
followed, ten more are still open), so a card that stops playing is a slug to
look up again.

## Not to be "fixed" later

"Let the adult type a word and pick the sign on the phone." That is the
search ADR 0009 ruled out. The search happens on the site, by a person; the
file is what comes out of it.
