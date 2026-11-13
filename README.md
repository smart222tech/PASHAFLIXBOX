# PASHAFLIXBOX

Native Android online-cinema client for Android phones, Google TV and Android TV.

## Current implementation
- Cinemeta-backed movie and series catalogue, metadata, posters and title search.
- Movie and series detail pages, series episode list, favourites and watch history.
- Generic Stremio add-on integration: add-on manifest is configured once; available stream URLs are requested per selected title/episode and played with Media3.
- Android TV Leanback launcher support and Compose focusable cards.
- GitHub Actions debug APK build.

## Configure playback sources
The public metadata catalogue is loaded automatically; no playlist URL is required for browsing or searching.
To obtain playable streams, save a compatible Stremio add-on's **configured** manifest URL once in Settings. AIOStreams' generic installation manifest reports that configuration is required; use its personal configured manifest URL. The app does not claim that a metadata listing is itself a playable stream.

## Build
GitHub Actions workflow: **Build PASHAFLIXBOX APK**.
- JDK 17
- Android SDK 36
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Kotlin 2.2.21
- Media3 1.11.1

## Legal and security boundaries
Integrate only content sources the user owns or is authorized to access. Do not bypass DRM, authentication, paywalls or access controls. The app only plays direct HTTP(S) stream URLs returned by a compatible add-on; torrent/infoHash-only sources are not supported by the built-in player.
