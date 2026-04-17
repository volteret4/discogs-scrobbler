# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug                    # Build debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease                  # Build release APK (minified)
./gradlew installDebug                     # Build + install on connected device/emulator
./gradlew test                             # Unit tests
./gradlew connectedAndroidTest             # Instrumented tests (requires device/emulator)
./gradlew testDebugUnitTest --tests "com.pollo.scrobbler.FooTest"  # Single test
./gradlew lint                             # Lint
```

Build config: Min SDK 24, target SDK 35, JVM 17. Permissions: `CAMERA`, `INTERNET`.

API keys are read from `local.properties` (gitignored) and injected as `BuildConfig` fields:
```properties
sdk.dir=/home/huan/Android/Sdk
discogs.consumer.key=...
discogs.consumer.secret=...
lastfm.api.key=...
lastfm.api.secret=...
```

## Architecture

**Clean Architecture + MVVM**, Hilt DI. Domain layer has zero dependency on data or UI.

```
domain/       → model/, repository/ (interfaces), usecase/
data/         → remote/ (Retrofit services, DTOs, OAuthInterceptor, mappers)
              → local/  (Room: ScrobblerDatabase, DAOs, entities)
              → repository/ (implementations of domain interfaces)
di/           → NetworkModule, DatabaseModule, RepositoryModule (Hilt modules)
ui/           → theme/, navigation/, screens/{auth,collection,scanner,search,detail,settings}/
```

Each screen package has exactly one `*Screen.kt` (Compose) and one `*ViewModel.kt` (HiltViewModel + StateFlow).

## Key Files

| File | Purpose |
|---|---|
| `data/remote/interceptor/OAuthInterceptor.kt` | HMAC-SHA1 signing for Discogs OAuth 1.0a |
| `data/remote/interceptor/EncryptedOAuthTokenProvider.kt` | Token storage in `EncryptedSharedPreferences` |
| `data/repository/DiscogsRepositoryImpl.kt` | Handles OAuth handshake (request token → user auth → access token) via direct OkHttp calls; API calls via Retrofit |
| `data/repository/LastFmRepositoryImpl.kt` | Last.fm MD5-signed API calls and session management |
| `data/remote/mapper/ReleaseMappings.kt` | DTO → domain model conversions; parses Discogs "Artist - Title" format |
| `ui/navigation/AppNavigation.kt` | NavHost + bottom nav wiring |
| `MainActivity.kt` | Deep link handling for OAuth callbacks (`com.pollo.scrobbler://oauth-callback`, `://lastfm-callback`) |

## External APIs

**Discogs** `https://api.discogs.com` — OAuth 1.0a, 60 req/min.
- Search: `GET /database/search?barcode={code}` or `?q={query}`
- Detail: `GET /releases/{id}` (includes tracklist)
- Collection: `GET /users/{username}/collection/folders/0/releases`
- OAuth token exchange uses direct OkHttp (not Retrofit) because the response is form-encoded, not JSON

**Last.fm** `https://ws.audioscrobbler.com/2.0/` — API key + session key.
- Scrobbling is per-track: `track.scrobble` (not album-level)
- API signature = MD5 of sorted params concatenated + api_secret appended
- Album tracklist is batch-scrobbled with timestamps spaced 3.5 min apart

## Important Notes

- `OAuthInterceptor` is used only for authenticated Discogs API calls. The OAuth handshake itself (request token, access token exchange) uses a separate `rawOkHttp` client without the interceptor, with temporary credentials.
- Tracklist is stored in Room as a JSON blob (`tracklistJson` column) to avoid a separate table; deserialized on read via Gson.
- Discogs search results use "Artist - Title" format in the `title` field — `parseDiscogsTitle()` in the mapper splits this.
- `CollectionWithRelease` is a Room `@Relation` join — Room requires a `@Transaction` query for it.
