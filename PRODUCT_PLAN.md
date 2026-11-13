# PASHAFLIXBOX product plan

## Phase 0 — Foundation
- [x] Dedicated repository and development branch.
- [x] Kotlin, Compose and Media3 project shell.
- [x] New application ID and launcher identity.
- [x] Search, demo catalogue and persistent favourites.
- [x] Direct authorized HTTPS playback entry point.
- [x] GitHub Actions debug APK workflow.
- [ ] Confirm first CI build and fix compiler/build issues.

## Phase 1 — Core data
- [ ] Normalized Movie, Series, Season and Episode models.
- [ ] Repository abstraction around the catalogue.
- [ ] Persistent watch history and resume position.
- [ ] Loading, empty and typed error states.

## Phase 2 — Authorized providers
- [ ] Personal-server provider interface.
- [ ] JSON/API provider implementation.
- [ ] M3U playlist support for user-owned or authorized playlists.
- [ ] Provider settings, connection testing and source management.
- [ ] Metadata and artwork mapping.

## Phase 3 — Playback
- [ ] Playback controller and lifecycle handling.
- [ ] HLS/DASH source selection.
- [ ] Audio and subtitle selection.
- [ ] Resume playback and playback history.
- [ ] Graceful error and retry handling.

## Phase 4 — Phone and TV experience
- [ ] Responsive phone/tablet layouts.
- [ ] TV home screen and D-pad navigation.
- [ ] Explicit focus styling and focus-order tests.
- [ ] TV launcher banner and app artwork.
- [ ] Remote-control playback behavior.

## Phase 5 — Quality and release
- [ ] Unit and UI tests.
- [ ] CI verification for debug and release variants.
- [ ] Install and test on Android phone and Google TV.
- [ ] Privacy, security and third-party license review.
- [ ] Release signing and versioning.
