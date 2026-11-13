# PASHAFLIXBOX

Native Android app foundation for phones, Google TV and Android TV.

## Product direction
One application with a shared catalogue and playback layer:
- Android phones and tablets
- Google TV / Android TV with D-pad navigation
- Movies, series, search, favourites and playback history
- Media3 playback for supported direct media, HLS and DASH URLs
- Extensible provider interfaces for personal servers and other sources the user is authorized to access

## Current foundation
The working Compose/Media3 foundation from the DeutschStream prototype has been migrated into this clean repository with the new package identity `com.smart222tech.pashaflixbox`.

The app currently includes a demo catalogue, search, persistent favourites, a direct HTTPS source dialog and Media3 playback. Demo titles are placeholders, not real catalogue data. Provider contracts are foundations only; no third-party scraping, DRM circumvention, or unauthorized stream access is included.

## Build
Open this repository in Android Studio or run the GitHub Actions workflow **Build PASHAFLIXBOX APK**. The debug APK is uploaded as a workflow artifact.

- JDK 17
- Android SDK 36
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Kotlin 2.2.21
- AndroidX Media3 1.11.1

## Planned milestones
1. Stable phone/TV shell and CI build.
2. Persistent catalogue, watch history and favourites.
3. Configurable personal-server providers (JSON/API and M3U where authorized).
4. Metadata, artwork, seasons/episodes and source selection.
5. TV-specific focus states, playback controls and launcher artwork.
6. Device testing and release signing.

## Legal and security boundaries
Only integrate sources that the user owns or is explicitly authorized to access. Do not bypass DRM, authentication, paywalls, or access controls. Credentials and tokens must be stored securely and never committed to Git.
