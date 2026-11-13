# PASHAFLIXBOX architecture

## Layers
- **UI:** Jetpack Compose catalogue, search, detail, episode and source selection screens for phones and Android TV.
- **Metadata:** Cinemeta-compatible Stremio catalogue and metadata API. Browsing/search works independently from video-source configuration.
- **Providers:** configured Stremio add-on manifest; add-ons expose streams through the standard `/stream/{type}/{id}.json` resource.
- **Playback:** Media3 handles direct HTTP(S), HLS and DASH stream URLs returned by providers.
- **Persistence:** DataStore favourites/history; SharedPreferences stores the optional add-on manifest URL.

## API flow
1. Fetch Cinemeta manifest and discover movie/series catalogue IDs.
2. Query `/catalog/{type}/{catalogId}.json?search=...`.
3. Load `/meta/{type}/{id}.json` for synopsis, artwork and series episodes.
4. Query configured add-on `/stream/{type}/{id}.json` only after the user opens a title.
5. Offer direct HTTP(S) URLs to Media3. Torrent-only and external-only results are not passed to the player.

## AIOStreams
A generic AIOStreams install manifest can report `configurationRequired`. The app detects this and explains that a configured personal manifest URL is needed. The app does not pretend the bare manifest can return streams.

## Safety
No DRM circumvention, authentication bypass, paywall bypass, or unauthorized stream extraction. Provider access remains subject to the add-on's own configuration and rights.
