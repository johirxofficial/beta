# X live — IPTV & playlist player

A fast, open source Android IPTV player. Every category and channel comes from a single
JSON API, so the whole catalog can change on your server without a new APK.

[![Android CI](https://github.com/johirxofficial/x/actions/workflows/build.yml/badge.svg)](https://github.com/johirxofficial/x/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Version 3.0.0** · developer **johirxofficial** · Kotlin + Jetpack Compose + Media3

## Download

Grab the newest APK from the [Releases](https://github.com/johirxofficial/x/releases)
page and install it (allow "install from unknown sources").

## What's new in v3

- **No cache at all.** Categories and channels are fetched from the API every single time.
  Nothing is stored on disk, nothing is served from memory — you always see the current
  server data. (Only channel/flag images use the image loader's normal image cache.)
- **Much richer API data** — the catalog now ships 54 categories, ~16,778 channel entries,
  country flags, per-category `channels_count`, `type` labels and 100+ channels embedded
  directly inside the JSON.
- **Redesigned UI** — hero header, gradient category cards with flags, favorites row,
  "continue watching" row, search + group chips + sticky group headers in the channel list.
- **Made for every screen** — phone, tablet, foldable, Android TV (D-pad focus states,
  leanback launcher, banner) and desktop/ChromeOS windows (2–6 responsive columns).
- **Better player** — immersive full screen, tap-to-toggle controls, FIT / ZOOM / FILL,
  picture-in-picture (Android 8+), retry on error, LIVE/VOD and quality pills.
- **Favorites & recents** — star any channel, the home screen keeps your list.

## Features

- Category grid loaded from one JSON API
- Two playlist modes per category:
  - `playlist_url` → the M3U/M3U8 file is downloaded and parsed on demand
  - inline `channels` array → played straight from the API response
- Plays HLS (`.m3u8`), MPEG-TS (`.ts`), MP4, MKV, WebM, DASH (`.mpd`), MP3/AAC
- Media3 / ExoPlayer with custom `Referer`, `User-Agent`, `Cookie`, `Origin` headers
- Instant search, group filter, favorites, recently watched
- API URL editable inside the app (Settings → JSON API), plus a public demo catalog preset
- Dark edge-to-edge Compose UI, adaptive layout from 2 to 6 columns

## Supported devices

| Device | Support |
| --- | --- |
| Phones (Android 5.0+) | ✅ |
| Tablets & foldables | ✅ 3–5 columns |
| Android TV / Google TV | ✅ leanback launcher + D-pad focus + banner |
| ChromeOS / desktop windows | ✅ resizable, 2–6 columns |
| Picture-in-picture | ✅ Android 8.0+ |

## No cache — by design

There is **no cache system** in this app:

- no OkHttp disk cache, no HTTP cache (requests send `Cache-Control: no-store`)
- no saved JSON snapshot, no bundled fallback catalog
- no in-memory category/playlist cache — reopening a category downloads it again

If the server is unreachable the app shows an error with a Retry button instead of old
data. The only thing kept between launches is your own data: API URL, settings,
favorites and watch history.

Channel logos and category flags are loaded with Coil, which keeps its standard image
cache. That is image loading, not API data.

## Changing the API

The default API is `http://10.31.253.82/live/api/data.json` (a LAN server), so on a fresh
install the app expects that server to be reachable. A public demo catalog lives in this
repo:

```
https://raw.githubusercontent.com/johirxofficial/x/main/docs/api/categories.json
```

Open **Settings (gear icon)** → paste your JSON URL → **Save**. You can also hit
**My server** or **Public** to fill the field, or **Reset to default**.

The default can be baked in at build time:

```kotlin
// app/build.gradle.kts
buildConfigField(
    "String",
    "DEFAULT_API_URL",
    "\"https://your-server.com/categories.json\"",
)
```

## JSON API format

Everything is optional except `name` and channel `url`. Keys are matched
case-insensitively, and aliases like `title`, `logo`, `icon`, `link`, `stream`, `m3u8`
are accepted.

### Shape A — categories with a playlist URL

```json
{
  "app": "X",
  "developer": "johirxofficial",
  "stats": { "categories": 54, "channels": 16778 },
  "categories": [
    {
      "id": "indian",
      "name": "Indian",
      "type": "country",
      "logo": "https://flagcdn.com/w80/in.png",
      "channels_count": 849,
      "playlist_url": "https://iptv-org.github.io/iptv/countries/in.m3u"
    }
  ]
}
```

### Shape B — channels inline in the same JSON

```json
{
  "categories": [
    {
      "id": "featured",
      "name": "Featured",
      "type": "mixed",
      "channels_count": 2,
      "channels": [
        { "name": "Channel One", "url": "https://example.com/live/one.m3u8",
          "logo": "https://example.com/one.png", "group": "Entertainment" },
        { "name": "Channel Two", "url": "https://example.com/live/two.ts",
          "headers": { "Referer": "https://example.com/", "User-Agent": "Mozilla/5.0" } }
      ]
    }
  ]
}
```

### Shape C — map of category name to channels

```json
{
  "Indian": [ { "name": "Channel", "url": "https://example.com/a.m3u8" } ],
  "Sports": [ { "name": "Channel", "url": "https://example.com/b.ts" } ]
}
```

A bare top-level array and `{ "data": { "categories": [...] } }` wrappers work too. If
the API URL points directly at an M3U playlist, all of its channels are shown as one
category.

### Category fields

| Field | Meaning |
| --- | --- |
| `id` / `slug` | Stable id (falls back to a slug of `name`) |
| `name` / `title` | Display name |
| `logo` / `icon` | Category image (country flag, poster…) |
| `type` | Free-form label shown on the card, e.g. `country`, `genre`, `mixed` |
| `channels_count` / `count` | Number shown before the playlist is opened |
| `playlist_url` / `m3u` / `url` | M3U or channel-JSON endpoint |
| `channels` | Inline channel array |

### Channel fields

| Field | Meaning |
| --- | --- |
| `name` / `title` | Display name |
| `url` / `link` / `stream` / `m3u8` | Stream URL (http, https, rtsp, rtmp) |
| `logo` / `icon` / `image` | Logo image |
| `group` / `group_title` | Group header + subtitle |
| `headers` | Extra HTTP headers: `Referer`, `User-Agent`, `Cookie`, `Origin` |

### M3U playlists

```m3u
#EXTM3U
#EXTINF:-1 tvg-id="one" tvg-logo="https://example.com/one.png" group-title="News",Channel One
#EXTVLCOPT:http-referrer=https://example.com/
https://example.com/live/one.m3u8
#EXTINF:-1,Channel Two
https://example.com/live/two.ts
```

`tvg-logo`, `tvg-name`, `group-title` and `#EXTVLCOPT` (referrer / user agent / cookie)
are all read. Duplicate stream URLs are skipped, and both M3U and Xtream `m3u_plus`
playlists are supported.

## Build from source

Requirements: JDK 17 and Android SDK API 35 — or just let GitHub Actions do it.

```bash
./gradlew :app:testDebugUnitTest     # parser tests
./gradlew :app:assembleDebug         # app/build/outputs/apk/debug/
./gradlew :app:assembleRelease       # app/build/outputs/apk/release/
```

CI runs the tests and builds both APKs on every push; pushing a `v*` tag publishes the
release APK. Release builds are signed with the default debug key so they can be
sideloaded — add your own keystore before publishing on a store.

## Project layout

```
app/src/main/java/com/johirxofficial/x/
├── MainActivity.kt
├── XApp.kt                    # Coil image loader
├── data/
│   ├── Models.kt              # Channel + Category
│   ├── PlaylistJsonParser.kt  # tolerant JSON parsing
│   ├── M3uParser.kt           # M3U / M3U8 / Xtream parsing
│   ├── Repository.kt          # network only — no cache
│   ├── Settings.kt            # API URL + playback options
│   └── UserDataStore.kt       # favorites + history
└── ui/
    ├── AppRoot.kt             # navigation
    ├── HomeScreen.kt          # hero, favorites, category grid
    ├── ChannelsScreen.kt      # search, groups, channel list
    ├── PlayerScreen.kt        # Media3 player, PiP, aspect modes
    ├── SettingsSheet.kt       # API URL, toggles, about
    └── components/            # shared composables
```

`docs/api/categories.json` is the public demo catalog used by the **Public** preset. It is
generated from public [iptv-org](https://github.com/iptv-org/iptv) playlists and contains
no private server credentials.

## Disclaimer

This app is only a player and ships no content of its own. The demo catalog links to
publicly available playlists; stream availability and geo-restrictions are controlled by
their providers. If you add streams, make sure you have the right to distribute them in
your country.

## License

[MIT](LICENSE) © 2026 johirxofficial
