# X — Live TV & Playlist Player

A small, open source Android IPTV player. Playlists come from one JSON API, so the
channel list can be updated from your server without publishing a new APK.

[![Android CI](https://github.com/johirxofficial/x/actions/workflows/build.yml/badge.svg)](https://github.com/johirxofficial/x/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Download

Grab the newest APK from the [Releases](https://github.com/johirxofficial/x/releases)
page and install it on your phone (allow "install from unknown sources").

## Features

- **Category home screen** — categories come from a JSON API (Indian, Bangla, Sports, Others…)
- **Two playlist modes**
  - category has a `playlist_url` → app downloads the M3U file and parses it
  - category has an inline `channels` array → used directly
- **Plays almost anything** — HLS (`.m3u8`), MPEG-TS (`.ts`), MP4, MKV, WebM, DASH (`.mpd`), MP3/AAC
- **Built on Media3 / ExoPlayer** with custom `Referer` / `User-Agent` / `Cookie` header support
- **Search inside a category**, pull the latest data with one tap
- **Change the API URL inside the app** (Settings → API) — no rebuild needed
- **Dark, edge-to-edge Compose UI**, aspect ratio modes (FIT / ZOOM / FILL)

## Changing the API

The default API is
`https://raw.githubusercontent.com/johirxofficial/x/main/docs/api/categories.json`.

Open the app → **Settings (gear icon)** → paste your own JSON URL → **Save**.
The URL is stored on the device, so you can point the app at any server at any time.

It can also be baked in at build time:

```kotlin
// app/build.gradle.kts
buildConfigField(
    "String",
    "DEFAULT_API_URL",
    "\"https://your-server.com/categories.json\"",
)
```

## JSON API format

Everything is optional except `name` and channel `url`. Field names are matched
case-insensitively, and the parser also accepts `title`, `logo`, `icon`, `link`,
`stream`, `m3u8`, etc. so slightly different APIs keep working.

### Shape A — categories with a playlist URL

```json
{
  "categories": [
    { "id": "indian", "name": "Indian", "playlist_url": "https://example.com/indian.m3u" },
    { "id": "bangla", "name": "Bangla", "playlist_url": "https://example.com/bangla.m3u" }
  ]
}
```

### Shape B — channels inline

```json
{
  "categories": [
    {
      "id": "bangla",
      "name": "Bangla",
      "channels": [
        { "name": "Channel One", "url": "https://example.com/live/one.m3u8", "logo": "https://example.com/one.png" },
        {
          "name": "Channel Two",
          "url": "https://example.com/live/two.ts",
          "headers": { "Referer": "https://example.com/", "User-Agent": "Mozilla/5.0" }
        }
      ]
    }
  ]
}
```

### Shape C — a plain map of categories

```json
{
  "Indian": [ { "name": "Channel", "url": "https://example.com/a.m3u8" } ],
  "Sports": [ { "name": "Channel", "url": "https://example.com/b.ts" } ]
}
```

A bare top-level array (`[ { "name": "Indian", ... } ]`) and `{ "data": { ... } }`
wrappers are accepted too. If the API URL points directly at an M3U playlist instead of
JSON, the app shows all of its channels as a single category.

### Supported channel fields

| Field | Meaning |
| --- | --- |
| `name` / `title` | Display name |
| `url` / `link` / `stream` / `m3u8` | Stream URL (http, https, rtsp, rtmp) |
| `logo` / `icon` / `image` | Logo image |
| `group` / `group_title` | Small subtitle in the channel list |
| `headers` | Extra HTTP headers, e.g. `Referer`, `User-Agent`, `Cookie`, `Origin` |

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
are all read by the parser.

## Build from source

Requirements: JDK 17 and the Android SDK (API 35). Or just let GitHub Actions do it.

```bash
./gradlew :app:assembleDebug      # app/build/outputs/apk/debug/
./gradlew :app:assembleRelease    # app/build/outputs/apk/release/
```

CI builds both APKs on every push and publishes the release APK on every `v*` tag.

Release builds are signed with the default debug key so the APK can be sideloaded —
they are **not** Play Store ready. Add your own keystore before publishing.

## Project layout

```
app/src/main/java/com/johirxofficial/x/
├── MainActivity.kt
├── data/
│   ├── Models.kt              # Channel + Category
│   ├── PlaylistJsonParser.kt  # tolerant JSON parsing
│   ├── M3uParser.kt           # M3U / M3U8 parsing
│   ├── Repository.kt          # network + in-memory cache
│   └── Settings.kt            # API URL storage
└── ui/
    ├── HomeScreen.kt          # category grid + API settings dialog
    ├── ChannelsScreen.kt      # channel list + search
    ├── PlayerScreen.kt        # Media3 player
    └── MainViewModel.kt
```

## Disclaimer

This app is only a player. It ships with public **test streams** (Big Buck Bunny, Apple
HLS samples) and contains no copyrighted content, no channel list and no stream URLs of
its own. If you add streams, make sure you have the right to distribute them in your
country.

## License

[MIT](LICENSE) © 2026 johirxofficial
