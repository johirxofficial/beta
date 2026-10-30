# X live — IPTV & playlist player

A fast, open source IPTV player for **Android, Android TV and Windows**. Every category,
notice and channel comes from a single JSON API, so the whole catalog — and even the
update link — can change on your server without a new build.

[![CI](https://github.com/johirxofficial/x/actions/workflows/build.yml/badge.svg)](https://github.com/johirxofficial/x/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Version 4.0.0** · developer **johirxofficial** · Kotlin + Jetpack Compose + Media3 · Electron for Windows

## Download

| Platform | File |
| --- | --- |
| Android phone / tablet / TV | `X-v4.0.0.apk` from [Releases](https://github.com/johirxofficial/x/releases) |
| Windows 10 / 11 (64-bit) | `X-live-4.0.0-portable.exe` from the same release |

The APK is also published as **`X-latest.apk`**, which always points at the newest release —
that is the URL the in-app updater uses. On Windows just run the portable `.exe`; there is
no installer.

## What's new in v4

- **Clean header** — only the app name **X** with the tagline *“Watch live from multiple
  servers.”* The logo box is gone from the top bar.
- **Notice ticker** — a capsule-shaped marquee under the header shows every string from
  the JSON `notices` array, scrolling continuously.
- **In-app update** — a new **Settings → App update** section reads the `update` block from
  the JSON, shows the latest version and notes, downloads the new APK with a progress bar
  and opens the Android package installer. If the installer is unavailable it falls back
  to the browser.
- **Friendly failure popup** — if the main source does not answer, a dialog offers
  **Reload**, **App update** and **Close** instead of a dead screen.
- **Windows app** — a native-feeling Electron client with the same catalog, ticker,
  categories, search, favorites and HLS/MPEG-TS playback.
- **TV-first navigation** — every card, chip, row and button has a visible D-pad focus
  state, the settings sheet scrolls with the remote, and the player keeps the leanback
  controls.
- **One logo everywhere** — launcher icon (legacy + adaptive + round), TV banner, in-app
  hero/About and the Windows exe all use the same asset.

## Features

- Category grid loaded from one JSON API — no cache, always fresh
- Two playlist modes per category:
  - `playlist_url` → the M3U/M3U8 file is downloaded and parsed on demand
  - inline `channels` array → played straight from the API response
- Plays HLS (`.m3u8`), MPEG-TS (`.ts`), MP4, MKV, WebM, DASH (`.mpd`), MP3/AAC on Android
  (Media3/ExoPlayer) and HLS/MPEG-TS/MP4 on Windows (hls.js / mpegts.js)
- Custom `Referer`, `User-Agent`, `Cookie`, `Origin` headers per channel
- Instant search, group filter, favorites, recently watched
- Notice marquee + remote-driven app update from the same JSON
- API URL editable inside the app, plus a public demo catalog preset
- Dark edge-to-edge Compose UI, adaptive 2–6 columns, focus rings for remotes

## Supported devices

| Device | Support |
| --- | --- |
| Phones (Android 5.0+) | ✅ |
| Tablets & foldables | ✅ 3–5 columns |
| Android TV / Google TV | ✅ leanback launcher + D-pad focus + banner |
| Chromebooks / Android desktop windows | ✅ resizable, 2–6 columns |
| Windows 10 / 11 (x64) | ✅ portable `.exe` |
| Picture-in-picture | ✅ Android 8.0+ |

## No cache — by design

There is **no cache system** in the Android app:

- no OkHttp disk cache, no HTTP cache (requests send `Cache-Control: no-store`)
- no saved JSON snapshot, no bundled fallback catalog
- no in-memory category/playlist cache — reopening a category downloads it again

If the server is unreachable the app shows the **Reload / App update / Close** dialog
instead of stale data. The only things kept between launches are your own settings, API
URL, favorites and watch history. The Windows app stores just the API URL and favorites
in `localStorage`.

Channel logos and category flags are loaded with the platform image loader, which keeps its
normal image cache. That is image loading, not API data.

## Changing the API

The default API is `http://10.31.253.82/live/api/data.json` (a LAN server), so on a fresh
install the app expects that server to be reachable. A public demo catalog lives in this
repo:

```
https://raw.githubusercontent.com/johirxofficial/x/main/docs/api/categories.json
```

Open **Settings (gear icon)** → paste your JSON URL → **Save**. You can also press
**My server** or **Public** to fill the field, or **Reset to default**. The Windows app has
the same field under its settings panel.

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
  "version": 4,
  "stats": { "categories": 54, "channels": 16778 },
  "notices": [
    "Welcome to X live — Watch live from multiple servers.",
    "New version available in Settings → App update."
  ],
  "update": {
    "latest_version": "4.0.0",
    "version_code": 4,
    "apk_url": "https://github.com/johirxofficial/x/releases/latest/download/X-latest.apk",
    "desktop_url": "https://github.com/johirxofficial/x/releases/latest",
    "message": "Faster loading, notice ticker, TV remote support and the Windows app.",
    "size_mb": 9.3
  },
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

### Notices and app update

| Key | Type | Meaning |
| --- | --- | --- |
| `notices` | `string[]` | Lines shown in the capsule marquee under the header. Also accepts objects with `text` / `message` |
| `update` | `object` | App update metadata (see below) |
| `update.latest_version` | `string` | Version shown in Settings, e.g. `"4.0.0"` |
| `update.version_code` | `int` | Android `versionCode` of the new APK — update is offered when it is higher |
| `update.apk_url` | `string` | Direct APK download used by **Update now** |
| `update.desktop_url` | `string` | Page opened by the Windows app for its own update |
| `update.message` | `string` | Release notes shown in the update section |
| `update.size_mb` | `number` | Optional size hint |

The updater downloads with OkHttp into `cacheDir/updates`, then hands the file to the
system installer through a `FileProvider`. The app never installs anything silently:
Android shows its normal confirmation dialog. `REQUEST_INSTALL_PACKAGES` is declared so
the installer can be opened.

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

### Android

Requirements: JDK 17 and Android SDK API 35 — or just let GitHub Actions do it.

```bash
./gradlew :app:testDebugUnitTest     # parser tests
./gradlew :app:assembleDebug         # app/build/outputs/apk/debug/
./gradlew :app:assembleRelease       # app/build/outputs/apk/release/
```

### Windows desktop app

Requirements: Node.js 20+.

```bash
cd desktop
npm install
npm start        # run the app
npm run dist     # release/X-live-4.0.0-portable.exe
```

CI runs the tests, builds both APKs and the Windows exe on every push; pushing a `v*` tag
publishes the release APK (as `X-vX.Y.Z.apk` **and** `X-latest.apk`) together with the
portable exe. Release builds are signed with the default debug key so they can be
sideloaded — add your own keystore before publishing on a store.

## Project layout

```
app/src/main/java/com/johirxofficial/x/
├── MainActivity.kt
├── XApp.kt                    # Coil image loader
├── data/
│   ├── Models.kt              # Channel, Category, Catalog, AppUpdate
│   ├── PlaylistJsonParser.kt  # tolerant JSON parsing + notices/update
│   ├── M3uParser.kt           # M3U / M3U8 / Xtream parsing
│   ├── Repository.kt          # network only — no cache
│   ├── UpdateManager.kt       # APK download + package installer
│   ├── Settings.kt            # API URL + playback options
│   └── UserDataStore.kt       # favorites + history
└── ui/
    ├── AppRoot.kt             # navigation
    ├── HomeScreen.kt          # header, notice ticker, hero, category grid
    ├── ChannelsScreen.kt      # search, groups, channel list
    ├── PlayerScreen.kt        # Media3 player, PiP, aspect modes
    ├── SettingsSheet.kt       # App update, API URL, toggles, about
    └── components/            # shared composables

desktop/                      # Electron app for Windows
├── main.js                   # window + CORS-free HTTP bridge
├── preload.js                # isolated IPC API
├── renderer/                 # index.html, styles.css, app.js
└── assets/                   # same logo as the Android app
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
