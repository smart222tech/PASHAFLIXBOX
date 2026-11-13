# PASHAFLIXBOX architecture

## Target platforms
A shared Android application for phones/tablets and Google TV / Android TV. UI components must be touch-friendly on mobile and fully usable with a remote control on TV.

## Main layers
- **app:** Compose navigation, screens, UI state and ViewModels.
- **domain:** normalized media models, repository contracts and provider contracts.
- **data:** catalogue repositories, DataStore persistence and source-provider implementations.
- **playback:** Media3 playback, source validation, track selection and playback state.
- **tv:** TV-specific layouts, D-pad focus order, focus visuals and launcher metadata.

## Media model
The production model should support:
- stable provider and media IDs
- media type: movie, series, season or episode
- title, overview, release year, runtime and artwork
- season/episode hierarchy
- one or more authorized playback options
- audio/subtitle languages, MIME type and quality metadata

## Provider contract
A provider returns normalized metadata and playback options for content it is authorized to serve. The UI must not depend on provider-specific HTML or parsing details. Providers must return typed errors such as unavailable, authentication required, network error and unsupported media.

## Playback
- Media3/ExoPlayer is the default for direct authorized media URLs.
- HLS and DASH are supported through matching Media3 modules.
- External players are optional integrations and must be invoked explicitly.
- No DRM circumvention, access-control bypass or unauthorized stream extraction.

## Persistence and security
- Persist stable media IDs for favourites and history.
- Never commit credentials, private tokens or server passwords.
- Use secure storage for secrets when account-based server connections are implemented.
- Do not log authentication headers or private playback URLs.

## TV requirements
- D-pad navigation for every interactive screen.
- Clear visible focus states.
- No required touch gestures.
- Leanback launcher entry and TV-appropriate artwork.
- Large readable text and adequate focus targets.
