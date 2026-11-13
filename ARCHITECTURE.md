# PASHAFLIXBOX architecture

## Target platforms
A shared Android application for phones/tablets and Google TV / Android TV. UI components are touch-friendly on mobile and reachable with a remote control on TV.

## Current layers
- **app:** Compose navigation, catalogue, details, settings and UI state.
- **data:** M3U network loading/parsing, playlist metadata, DataStore favourites/history/settings.
- **providers:** extensible contracts for additional authorized catalogue/source providers.
- **playback:** Media3/ExoPlayer for supported direct media, HLS and DASH streams.
- **tv:** Leanback launcher entry and focusable Compose controls; further TV-specific styling remains planned.

## Playlist model
Each M3U entry is normalized to a stable ID derived from its stream URL, title, group/category, optional artwork URL and playback URL.

## Provider contract
A provider returns normalized metadata and playback options for content it is authorized to serve. The UI must not depend on provider-specific HTML or parsing details. Future providers should return typed errors such as unavailable, authentication required, network error and unsupported media.

## Playback
- Media3/ExoPlayer is the default for direct authorized media URLs.
- HLS and DASH are included through matching Media3 modules.
- External players may be added as optional explicit integrations.
- No DRM circumvention, access-control bypass or unauthorized stream extraction.

## Persistence and security
- Persist stable media IDs for favourites and history.
- Playlist URLs are stored locally in DataStore; avoid placing private credentials in playlist URLs.
- Never commit credentials, private tokens or server passwords.
- Use secure storage for secrets when account-based server connections are implemented.
- Do not log authentication headers or private playback URLs.

## TV requirements
- D-pad navigation for every interactive screen.
- Clear visible focus states.
- No required touch gestures.
- Leanback launcher entry and TV-appropriate artwork.
- Large readable text and adequate focus targets.
