# beta — watch live

A fast, open source IPTV player for **Android, Android TV and Windows**. Every category,
notice and channel comes from a single JSON API, so the whole catalog — and even the
update link — can change on your server without a new build.

[![CI](https://github.com/johirxofficial/beta/actions/workflows/build.yml/badge.svg)](https://github.com/johirxofficial/beta/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Version 5.2.3 (final update)** · developer **johirxofficial** · Kotlin + Jetpack Compose + Media3 · Electron for Windows

## Download

| Platform | File |
| --- | --- |
| Android phone / tablet / TV | `beta-v5.2.3.apk` from [Releases](https://github.com/johirxofficial/beta/releases) |
| Windows 10 / 11 (64-bit) | `beta-5.2.3-portable.exe` from the same release |

The APK is also published as **`beta-latest.apk`**, which always points at the newest
release — that is the URL the in-app updater uses. On Windows just run the portable `.exe`;
there is no installer.

> **Clean install required for v5.0.0 users:** the project moved from the `x` name to
> **beta**, including the Android application id (`com.johirxofficial.beta`). Android
> treats that as a different app, so uninstall the old **X** build once, then install
> **beta 5.2.3**. The old and new apps cannot be updated in place. Favorites and history
> saved by the old app are not migrated.

## What's new in 5.2.3

- **Previous / Next channel buttons** replace the old `-10s` / `+10s` seek controls.
  Channel up/down, D-pad left/right, and the desktop arrow keys zap through the current
  list with wrap-around.
- **Any playlist URL works.** The app detects M3U, M3U8, JSON or a plain URL list from the
  response **content**, not the file extension. `.m3u`, `.m3u8`, `.php`, `.txt`,
  extension-less endpoints and query-string routes are all supported. Relative stream URLs
  are resolved against the playlist URL.
- **Cleaner v2 JSON catalog.** Categories now describe their content with a single
  `source` object: an inline channel list or a playlist URL. Old field names remain accepted
  for backward compatibility.
- **List and grid layouts.** Choose Grid or List for home categories, and List or Grid for
  channels. Home grid supports Auto or 2–6 columns; channel grid supports 2–4 columns.
  Both the Android app and Windows app remember the choice.
- **Exact Dalvik user agent.** API, playlist, update and stream requests use:
  `Dalvik/2.1.0 (Linux; U; Android 12; SM-A217F Build/SP1A.210812.016) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Mobile Safari/537.36`.
  The Windows app applies the same agent to its Electron session.
- **Project renamed to beta everywhere** — repository, Android package/namespace, prefs,
  desktop IPC bridge, local storage keys and download names.

## Features

- Category catalog loaded from one JSON API — no cache, always fresh
- Two source modes per category:
  - `source.type = "playlist"` → downloaded and parsed on demand
  - `source.type = "inline"` → channels played straight from the API response
- Plays HLS (`.m3u8`), MPEG-TS (`.ts`), MP4, MKV, WebM, DASH (`.mpd`), MP3/AAC on Android
  (Media3/ExoPlayer) and HLS/MPEG-TS/MP4 on Windows (hls.js / mpegts.js)
- Custom `Referer`, `User-Agent`, `Cookie`, `Origin` headers per channel
- Instant search, group filter, favorites, recently watched
- Notice marquee + remote-driven app update from the same JSON
- API URL editable inside the app, plus a public demo catalog preset
- List/grid layouts, dark/light/system theme, focus rings for remotes

## Player controls

### Android phone / tablet

| Action | How |
| --- | --- |
| Show or hide controls | Tap the video; controls hide themselves after 4 seconds while playing |
| Play / pause | On-screen button, Space, or the media play/pause key |
| Previous / next channel | **Previous** / **Next** buttons, remote channel up/down, or D-pad left/right while the controls are hidden |
| Channel list | **Channels** button, **Menu**, **C** or **L** |
| Rotate | **Landscape / Portrait** button, or rotate the device |
| Mute | **Mute / Sound** |
| Aspect ratio | **Fit → Zoom → Fill** |
| Picture-in-picture | **PiP** (Android 8.0+, phones/tablets) |
| Favorite | **Save / Saved** in the top bar |
| Back | **Back** button or system Back (closes the channel list first) |

### Android TV / Google TV

The same overlay is fully D-pad driven: focus always has a white ring, the channel list
opens with **Menu**, and the channel up/down keys change channels. Rotation and PiP
buttons are hidden on TV.

### Windows desktop

Click the video to show the overlay. It hides after 3.5 seconds while playing. Use the
**Previous** / **Next** buttons, the arrow keys, **C**, **L** or **Esc** to open the channel
list, or use the **Channels** button.

## Supported devices

| Device | Support |
| --- | --- |
| Phones (Android 5.0+) | ✅ |
| Tablets & foldables | ✅ 2–6 columns |
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
instead of stale data. The only things kept between launches are your own settings (API
URL, theme, layouts, playback toggles), favorites and watch history. The Windows app stores
the API URL, layouts and favorites in `localStorage`.

Channel logos and category flags are loaded with the platform image loader, which keeps its
normal image cache. That is image loading, not API data.

## Changing the API

The default API is `http://10.31.253.82/live/api/data.json` (a LAN server), so on a fresh
install the app expects that server to be reachable. A public demo catalog lives in this
repo:

```
https://raw.githubusercontent.com/johirxofficial/beta/main/docs/api/categories.json
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

## JSON API format (v2)

The canonical catalog uses a small, organized shape. `source` is the only place that
describes where channels come from.

```json
{
  "schema": "beta.catalog.v2",
  "app": "beta",
  "app_info": {
    "name": "beta",
    "tagline": "watch live",
    "developer": "johirxofficial",
    "version": "5.2.3",
    "version_code": 523,
    "homepage": "https://github.com/johirxofficial/beta"
  },
  "updated": "2026-10-30 18:00",
  "source": "iptv-org public playlists + local Xtream panel",
  "stats": { "categories": 2, "channels": 851 },
  "notices": [
    "beta 5.2.3 — previous/next channel controls and a cleaner catalog API."
  ],
  "update": {
    "latest_version": "5.2.3",
    "version_code": 523,
    "apk_url": "https://github.com/johirxofficial/beta/releases/latest/download/beta-latest.apk",
    "desktop_url": "https://github.com/johirxofficial/beta/releases/latest",
    "message": "Final update with layout choices and content-detected playlists.",
    "size_mb": 9.5
  },
  "categories": [
    {
      "id": "indian",
      "name": "Indian",
      "type": "country",
      "logo": "https://flagcdn.com/w80/in.png",
      "count": 849,
      "source": {
        "type": "playlist",
        "format": "auto",
        "url": "https://iptv-org.github.io/iptv/countries/in.m3u"
      }
    },
    {
      "id": "featured",
      "name": "Featured",
      "type": "mixed",
      "count": 2,
      "source": {
        "type": "inline",
        "channels": [
          {
            "name": "Channel One",
            "url": "https://example.com/live/one.m3u8",
            "logo": "https://example.com/one.png",
            "group": "Entertainment"
          }
        ]
      }
    }
  ]
}
```

### Playlist URLs

`source.url` may point at **any** endpoint: `.m3u`, `.m3u8`, `.php`, `.txt`, an Xtream
`get.php?...` route, or a URL with no extension. The app fetches the response and detects:

1. M3U/M3U8 content (`#EXTM3U` / `#EXTINF`)
2. JSON channels (`{"channels":[...]}`, a bare array, or a nested `source.channels`)
3. A plain list of stream URLs, one per line

Relative stream URLs in an M3U playlist are resolved against the playlist URL. A PHP
endpoint that changes its output format keeps working without an app rebuild.

### Backward compatibility

The parser also accepts the old v5 shapes, including top-level `playlist_url`,
`channels_count` and inline `channels`. A bare top-level array, a
`{ "data": { "categories": [...] } }` wrapper, and an API URL that returns an M3U playlist
directly are still supported.

### Notices and app update

| Key | Type | Meaning |
| --- | --- | --- |
| `notices` | `string[]` | Lines shown in the capsule marquee under the header. Also accepts objects with `text` / `message` |
| `update` | `object` | App update metadata |
| `update.latest_version` | `string` | Version shown in Settings, e.g. `"5.2.3"` |
| `update.version_code` | `int` | Android `versionCode` of the new APK — update is offered when it is higher |
| `update.apk_url` | `string` | Direct APK download used by **Update now** |
| `update.desktop_url` | `string` | Page opened by the Windows app for its own update |
| `update.message` | `string` | Release notes shown in the update section |
| `update.size_mb` | `number` | Optional size hint |

The updater downloads with OkHttp into `cacheDir/updates`, then hands the file to the
system installer through a `FileProvider`. The app never installs anything silently:
Android shows its normal confirmation dialog. `REQUEST_INSTALL_PACKAGES` is declared so
the installer can be opened.

### Category and channel fields

| Field | Meaning |
| --- | --- |
| `id` / `slug` | Stable id (falls back to a slug of `name`) |
| `name` / `title` | Display name |
| `logo` / `icon` | Category or channel image |
| `type` | Free-form category label, e.g. `country`, `genre`, `mixed` |
| `count` / `channels_count` | Number shown before the playlist is opened |
| `source.type` | `inline` or `playlist` |
| `source.url` | Playlist endpoint (any extension) |
| `source.channels` | Inline channel array |
| Channel `url` / `link` / `stream` / `m3u8` | Stream URL (http, https, rtsp, rtmp) |
| Channel `group` / `group_title` | Group header + subtitle |
| Channel `headers` | Extra HTTP headers: `Referer`, `User-Agent`, `Cookie`, `Origin` |

### M3U playlists

```m3u
#EXTM3U
#EXTINF:-1 tvg-id="one" tvg-logo="https://example.com/one.png" group-title="News",Channel One
#EXTVLCOPT:http-referrer=https://example.com/
https://example.com/live/one.m3u8
#EXTINF:-1,Channel Two
https://example.com/live/two.ts
```

`tvg-logo`, `tvg-name`, `group-title`, `#EXTGRP`, `#EXTVLCOPT` (referrer / user agent /
cookie), `#KODIPROP` stream headers and both quoted/unquoted attributes are read. Duplicate
stream URLs are skipped, and M3U, Xtream `m3u_plus`, and plain URL lists are supported.

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
npm run dist     # release/beta-5.2.3-portable.exe
```

CI runs the tests, builds both APKs and the Windows exe on every push; pushing a `v*` tag
publishes `beta-vX.Y.Z.apk`, `beta-latest.apk` and `beta-<version>-portable.exe`. Release
builds are signed with the default debug key so they can be sideloaded — add your own
keystore before publishing on a store.

## Project layout

```
app/src/main/java/com/johirxofficial/beta/
├── MainActivity.kt            # theme-aware activity, edge-to-edge system bars
├── BetaApp.kt                 # Coil image loader
├── data/
│   ├── Models.kt              # Channel, Category, Catalog, AppUpdate
│   ├── PlaylistJsonParser.kt  # v2 source parsing + notices/update
│   ├── M3uParser.kt           # M3U / M3U8 / Xtream parsing
│   ├── Repository.kt          # network only, content-detected playlists
│   ├── Settings.kt            # API URL, theme, layouts, playback options
│   ├── UpdateManager.kt       # APK download + package installer
│   └── UserDataStore.kt       # favorites + history
└── ui/
    ├── AppRoot.kt             # navigation + player channel list
    ├── HomeScreen.kt          # header, ticker, favorites, category grid/list
    ├── ChannelsScreen.kt      # search, groups, channel list/grid
    ├── PlayerScreen.kt        # custom controls, previous/next, rotation, PiP
    ├── SettingsSheet.kt       # layouts, playback, update, API URL, about
    └── theme/                 # dark + light BetaTheme

desktop/                      # Electron app for Windows
├── main.js                   # window, Dalvik user agent + CORS-free HTTP bridge
├── preload.js                # isolated betaapi IPC bridge
├── renderer/                 # index.html, styles.css, app.js
└── assets/                   # beta icon.png + icon.ico
```

`docs/api/categories.json` is the public demo catalog used by the **Public** preset. It is
generated from public [iptv-org](https://github.com/iptv-org/iptv) playlists and contains
no private server credentials. `tools/migrate_catalog_v2.py` converts an older catalog to
the v2 layout.

## Disclaimer

This app is only a player and ships no content of its own. The demo catalog links to
publicly available playlists; stream availability and geo-restrictions are controlled by
their providers. If you add streams, make sure you have the right to distribute them in
your country.

## License

MIT — see [LICENSE](LICENSE).
