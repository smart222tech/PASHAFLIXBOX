# PASHAFLIXBOX product plan

## Implemented foundation
- [x] Dedicated repository and development branch.
- [x] Kotlin, Compose and Media3 project shell.
- [x] Android phone and Leanback launcher entry points.
- [x] Search and catalogue sections for movies and series.
- [x] M3U playlist download and parsing over HTTP(S).
- [x] Playlist title, group/category and poster URL extraction.
- [x] Poster display in catalogue and detail screen.
- [x] Persistent favourites, saved playlist URL and watch history.
- [x] Automatic reload of the saved playlist at app launch.
- [x] Direct HTTPS source testing and Media3 playback.
- [x] Unit tests for M3U parsing.
- [x] GitHub Actions debug APK workflow.

## Remaining product work
- [ ] Confirm CI build for the current M3U implementation and fix any compiler/test issues.
- [ ] Add robust TV layout, focus styling, TV banner and launcher artwork.
- [ ] Improve movie/series classification and category filters.
- [ ] Add playback resume position and next-episode flow.
- [ ] Add optional JSON API provider for personal media servers.
- [ ] Add source management for multiple authorized playlists.
- [ ] Add typed network/playback error UI and retry actions.
- [ ] Test on a physical Android phone and Google TV device.
- [ ] Release signing, privacy and dependency/license review.

## Source and rights boundary
Users provide playlists and media servers they own or are authorized to access. The app does not scrape unauthorized websites or bypass DRM, authentication, paywalls or access controls.
