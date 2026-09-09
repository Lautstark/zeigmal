# ADR 0006 — Content arrives in a directory over a cable, and nothing leaves

**Status:** accepted · **Date:** 2026-09-09 · **Applies to:** `AndroidManifest.xml`, `StationViewModel.directory`, docs/media-import.md

## Context

vorlaut-app imports a package three ways: a file picker, an intent filter, and a
LAN server on `POST /paket`. The last one cost it the structural guarantee that
a viewer without an INTERNET permission cannot move a licensed package off the
device, and three forty-line comments now defend the narrower claim.

## Decision

**The Kartensatz lives in the app's external files directory,
`Android/data/de.lautstark.zeigmal/files/kartensatz/`, and gets there by
cable — `adb push`, or a file manager over USB. No INTERNET permission, no
receiver, no picker in the MVP.** The app writes exactly one file there,
`cards.json`, and copies nothing anywhere. Cloud backup and device transfer are
excluded for every domain.

## Why

**That directory needs no permission and is reachable over MTP.** A parent with
a cable and a laptop can put a Kartensatz there and copy `cards.json` back out
without the app offering anything.

**Inbound to the child's own device is the sanctioned METACOM case; outbound
from the app is what §5.2 forbids.** The family memory on this is explicit: the
direction is the whole distinction. A zero-permission app has the outbound half
by construction, and that is cheaper to keep than to win back.

**The Kartensatz is large and lives once.** No staging copy, no ZIP held beside
its extraction.

## Consequences

The directory is deleted with the app. That is acceptable because the media is
prepared elsewhere and re-copyable, and because the one irreplaceable file is
the card map, whose backup is a copy over the same cable
(docs/media-import.md). A Kartensatz replaced while the app runs is picked up
on the next resume, not atomically; the stage-and-rename swap vorlaut-app's
`PackageStore` does is the answer if that ever bites.

## Not to be "fixed" later

"Receive over Wi-Fi like vorlaut-app." Only with a written argument as long as
vorlaut-app's, and only if the cable has actually been the problem.
