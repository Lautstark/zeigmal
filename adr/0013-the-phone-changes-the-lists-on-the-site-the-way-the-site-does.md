# ADR 0013 — The phone changes the lists on the site, the way the site does

**Status:** accepted · **Date:** 2026-11-14 · **Amends:** [0012](0012-a-list-is-a-lernliste-on-the-site-and-the-phone-only-reads-it.md) (the phone no longer only reads) and [0009](0009-the-phone-writes-the-stickers.md) (the phone may search) · **Applies to:** `core/SignDigitalProvider.kt`, `core/AdultModel.kt`, `app/ui/ListsScreen.kt`, `app/ui/WriteScreen.kt`

## Context

ADR 0012 made every list a Lernliste on sign-digital.de and kept the phone
to reading them. Two things moved since. The family wants to write a single
card without making a list for it first, which is a search on the phone; and
they want zeigmal and the Gebärden app (`Lautstark/signdigital-player`) to do
about the same with lists: load, change, make, delete, all kept on the site.

## Decision

**zeigmal searches the site and changes Lernlisten there.** On the phone:
search a sign and write it; add a sign to a list, take one out; make a list;
rename one; delete one. The order of a list stays the site's business.

**Every change is made the way the site makes it, and checked.** The site
takes a list back whole (measured 2026-11-08), so a change reads the list
fresh right before, applies only what the adult did to the sign ids read
then, sends the whole document, and reads it once more: if the site's list is
not what was meant, the phone says so and keeps nothing. A list is deleted by
deleting its folder entry, as the site does, and is checked gone. Deleting
asks first, plainly, because the list is then gone from the site and from the
Gebärden app as well.

**The two apps behave alike.** The requests and the read-before, check-after
rule are the same in zeigmal and in signdigital-player, and are measured once,
on a throwaway list, not by each app on its own.

**Search and write is a stack of one.** A hit opens the writing mode on that
card alone. After one sticker the reader stops writing until another card is
chosen, or the adult asks for the same card again: a quick hand never puts
the last word on the next card.

## Why

**One place for lists, two ways to reach it.** The site stays the place the
lists are; the phone and the iPhone are two hands on it. Nothing is copied
between the apps.

**Read-before, check-after is what makes whole-document writes safe.** It
does not make two edits at the same moment safe, and nothing on the site
offers that; it makes an edit on the phone after an edit at the computer
safe, which is the case that happens.

## Consequences

sign-digital.de keeps one login per account: when zeigmal logs in again it
logs the account out elsewhere, the Gebärden app and the browser included.
That was a nuisance while the phone only read; with two apps writing it has
to be solved before both run on the family's devices.

A card's word is still the site's name for the sign; a Lernliste has no word
of its own per entry.

## Not to be "fixed" later

"Let the phone keep its own copy of a list and sync it later." Then there are
two lists and a merge. The phone keeps the last load to show without a
network, and changes nothing without the site.
