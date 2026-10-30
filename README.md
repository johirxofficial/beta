# beta — watch live

A fast, open source IPTV player for **Android, Android TV and Windows**. Every category,
notice and channel comes from a single JSON API, so the whole catalog — and even the
update link — can change on your server without a new build.

[![CI](https://github.com/johirxofficial/x/actions/workflows/build.yml/badge.svg)](https://github.com/johirxofficial/x/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Version 5.0.0** · developer **johirxofficial** · Kotlin + Jetpack Compose + Media3 · Electron for Windows

## Download

| Platform | File |
| --- | --- |
| Android phone / tablet / TV | `beta-v5.0.0.apk` from [Releases](https://github.com/johirxofficial/x/releases) |
| Windows 10 / 11 (64-bit) | `beta-5.0.0-portable.exe` from the same release |

The APK is also published as **`beta-latest.apk`**, which always points at the newest
release — that is the URL the in-app updater uses. `X-latest.apk` is kept as a
compatibility copy for v4 installs that still poll the old URL. On Windows just run the
portable `.exe`; there is no installer.

## What's new in v5

- **New name and logo** — **beta** with the tagline *watch live*. The launcher icon
  (legacy, round and adaptive), TV banner, in-app branding and Windows exe all use the
  new beta wordmark.
- **Rebuilt player controls** — a custom Compose overlay replaces the stock controller.
  It auto-hides after 4 seconds of playback, returns on any tap or key press, and works
  with touch, mouse, D-pad and keyboard.
- **Channel list inside the player** — press **Channels**, **Menu**, **C** or **L** (or
  the remote's channel up/down keys to zap). Pick any channel without leaving playback.
- **Rotation works** — the phone/tablet player requests full-sensor orientation, and the
  overlay has a Landscape/Portrait toggle. TV layouts keep a fixed orientation.
- **App layout settings** — choose **System / Dark / Light** theme and a **Home grid**
  size (Auto, 2–6 columns). The Windows app has the same grid setting. Both are saved
  between launches.
- **Cleaner player** — no LIVE, VOD or `720p` badges over the video; quality tags are also
  stripped from the player title.
- **English-only UI** — every screen, dialog, player message, notice and JSON default is
  English. The build ships only the `en` resource configuration.
- **Home page cleanup** — the old info/hero card under the notice capsule is gone; the
  page starts with favorites, recent channels and the category grid.
- **Bug fixes and polish** — smoother category loading, English error dialogs, TV focus
  rings preserved, and a shorter path from a failed stream to another channel.

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
- Adaptive 2–6 column home grid, dark/light/system theme, focus rings for remotes

## Player controls

### Android phone / tablet

| Action | How |
| --- | --- |
| Show or hide controls | Tap the video; controls hide themselves after 4 seconds while playing |
| Play / pause | On-screen button, Space, or the media play/pause key |
| Seek ±10 seconds | `-10s` / `+10s`, or D-pad left/right while the controls are hidden |
| Channel list | **Channels** button, **Menu**, **C** or **L** |
| Previous / next channel | Remote channel up/down keys |
| Rotate | **Landscape / Portrait** button, or rotate the device |
| Mute | **Mute / Sound** |
| Aspect ratio | **Fit → Zoom → Fill** |
| Picture-in-picture | **PiP** (Android 8.0+, phones/tablets) |
| Favorite | **Save / Saved** in the top bar |
| Back | **Back** button or system Back (closes the channel list first) |

### Android TV / Google TV

The same overlay is fully D-pad driven: focus always has a white ring, the channel list
opens with **Menu**, and playback keeps the leanback layout. Rotation and PiP buttons are
hidden on TV.

### Windows desktop

Click the video to show the overlay. It hides after 3.5 seconds while playing. Press
**C**, **L** or **Esc** to open the channel list, or use the **Channels** button.

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
URL, theme, grid, playback toggles), favorites and watch history. The Windows app stores
the API URL, grid size and favorites in `localStorage`.

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
  "app": "beta",
  "developer": "johirxofficial",
  "version": 5,
  "stats": { "categories": 54, "channels": 16778 },
  "notices": [
    "beta 5.0.0 — watch live from multiple servers.",
    "New: player controls, channel list shortcut and rotation."
  ],
  "update": {
    "latest_version": "5.0.0",
    "version_code": 5,
    "apk_url": "https://github.com/johirxofficial/x/releases/latest/download/beta-latest.apk",
    "desktop_url": "https://github.com/johirxofficial/x/releases/latest",
    "message": "Redesigned player controls, layout settings and the new beta logo.",
    "size_mb": 9.4
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
| `update.latest_version` | `string` | Version shown in Settings, e.g. `"5.0.0"` |
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
npm run dist     # release/beta-5.0.0-portable.exe
```

CI runs the tests, builds both APKs and the Windows exe on every push; pushing a `v*` tag
publishes `beta-vX.Y.Z.apk`, `beta-latest.apk`, the compatibility `X-latest.apk`, and
`beta-<version>-portable.exe`. Release builds are signed with the default debug key so they
can be sideloaded — add your own keystore before publishing on a store.

## Project layout

```
app/src/main/java/com/johirxofficial/x/
├── MainActivity.kt            # theme-aware activity, edge-to-edge system bars
├── BetaApp.kt                 # Coil image loader
├── data/
│   ├── Models.kt              # Channel, Category, Catalog, AppUpdate
│   ├── PlaylistJsonParser.kt  # tolerant JSON parsing + notices/update
│   ├── M3uParser.kt           # M3U / M3U8 / Xtream parsing
│   ├── Repository.kt          # network only — no cache
│   ├── Settings.kt            # API URL, theme, grid, playback options
│   ├── UpdateManager.kt       # APK download + package installer
│   └── UserDataStore.kt       # favorites + history
└── ui/
    ├── AppRoot.kt             # navigation + player channel list
    ├── HomeScreen.kt          # header, ticker, favorites, category grid
    ├── ChannelsScreen.kt      # search, groups, channel list
    ├── PlayerScreen.kt        # custom controls, channel overlay, rotation, PiP
    ├── SettingsSheet.kt       # layout, playback, update, API URL, about
    └── theme/                 # dark + light BetaTheme

desktop/                      # Electron app for Windows
├── main.js                   # window + CORS-free HTTP bridge
├── preload.js                # isolated IPC API
├── renderer/                 # index.html, styles.css, app.js
└── assets/                   # beta icon.png + icon.ico
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
