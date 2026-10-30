#!/usr/bin/env python3
"""Convert a beta catalog to the clean v2 source layout.

Usage:
    python tools/migrate_catalog_v2.py docs/api/categories.json [more.json ...]

The converter is intentionally idempotent. Categories may use either the old
``playlist_url`` / inline ``channels`` shape or the new ``source`` object.
"""

from __future__ import annotations

import json
import sys
from collections import OrderedDict
from pathlib import Path

VERSION = "5.2.3"
VERSION_CODE = 523
REPO = "https://github.com/johirxofficial/beta"

NOTICES = [
    "beta 5.2.3 — final update: previous/next channel controls and a cleaner catalog API.",
    "New: choose List or Grid for categories and channels, including 2–4 channel columns.",
    "Playlist URLs can be .m3u, .m3u8, .php, .txt or any endpoint — the response is detected automatically.",
    "The app uses the Dalvik Android 12 / SM-A217F user agent for API, playlist and stream requests.",
    "No cache — every reload fetches fresh data from the API.",
]

MESSAGE = (
    "beta 5.2.3 — previous/next channel buttons, content-detected playlists, "
    "list/grid layouts and a cleaner v2 catalog."
)


def as_int(value, default=0):
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def convert_category(raw: dict) -> OrderedDict:
    item = OrderedDict()
    item["id"] = raw.get("id") or raw.get("slug") or "category"
    item["name"] = raw.get("name") or raw.get("title") or item["id"].title()
    if raw.get("type"):
        item["type"] = raw["type"]
    if raw.get("logo"):
        item["logo"] = raw["logo"]

    channels = raw.get("channels")
    if not isinstance(channels, list):
        channels = []

    source = raw.get("source")
    source = source if isinstance(source, dict) else {}

    playlist = (
        raw.get("playlist_url")
        or raw.get("playlist")
        or raw.get("m3u")
        or raw.get("m3u_url")
        or raw.get("m3u8")
        or raw.get("url")
        or raw.get("link")
        or raw.get("file")
        or source.get("url")
        or source.get("playlist_url")
        or source.get("m3u")
        or source.get("m3u_url")
        or source.get("m3u8")
        or source.get("endpoint")
    )

    if channels:
        item["count"] = as_int(raw.get("channels_count") or raw.get("count"), len(channels))
        item["source"] = OrderedDict(
            [("type", "inline"), ("channels", channels)]
        )
    elif playlist:
        item["count"] = as_int(raw.get("channels_count") or raw.get("count"), 0)
        item["source"] = OrderedDict(
            [("type", "playlist"), ("format", "auto"), ("url", str(playlist).strip())]
        )
    else:
        # Keep an empty category visible rather than silently dropping it.
        item["count"] = as_int(raw.get("channels_count") or raw.get("count"), 0)
        item["source"] = OrderedDict([("type", "inline"), ("channels", [])])

    return item


def convert_document(data: dict, stats_defaults: dict) -> OrderedDict:
    categories = [convert_category(c) for c in data.get("categories", []) if isinstance(c, dict)]

    stats = data.get("stats") if isinstance(data.get("stats"), dict) else {}
    stats_out = OrderedDict()
    stats_out["categories"] = len(categories)
    stats_out["channels"] = as_int(
        stats.get("channels"),
        sum(as_int(category.get("count")) for category in categories),
    )
    if "featured" in stats:
        stats_out["featured"] = as_int(stats.get("featured"))
    elif stats_defaults.get("featured"):
        stats_out["featured"] = stats_defaults["featured"]
    if "countries" in stats:
        stats_out["countries"] = as_int(stats.get("countries"))

    document = OrderedDict()
    document["schema"] = "beta.catalog.v2"
    document["app"] = "beta"
    document["app_info"] = OrderedDict(
        [
            ("name", "beta"),
            ("tagline", "watch live"),
            ("developer", "johirxofficial"),
            ("version", VERSION),
            ("version_code", VERSION_CODE),
            ("homepage", REPO),
        ]
    )
    document["updated"] = "2026-10-30 18:00"
    document["source"] = data.get("source") or "iptv-org public playlists + local Xtream panel"
    document["stats"] = stats_out
    document["notices"] = NOTICES
    document["update"] = OrderedDict(
        [
            ("latest_version", VERSION),
            ("version_code", VERSION_CODE),
            ("apk_url", f"{REPO}/releases/latest/download/beta-latest.apk"),
            ("desktop_url", f"{REPO}/releases/latest"),
            ("message", MESSAGE),
            ("size_mb", 9.5),
        ]
    )
    document["categories"] = categories
    return document


def main(paths: list[str]) -> int:
    if not paths:
        print("usage: migrate_catalog_v2.py <catalog.json> [...]", file=sys.stderr)
        return 2

    for raw_path in paths:
        path = Path(raw_path)
        data = json.loads(path.read_text(encoding="utf-8"))
        converted = convert_document(data, {})
        path.write_text(
            json.dumps(converted, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"converted {path} ({len(converted['categories'])} categories)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
