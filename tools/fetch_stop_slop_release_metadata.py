#!/usr/bin/env python3
"""Fetch and cross-check the small metadata assets of one published model release."""

from __future__ import annotations

import argparse
import http.client
import json
import os
import re
import urllib.parse
import urllib.request
from pathlib import Path


OWNER = "marcel322gg-png"
REPOSITORY = "StopSlop"
API = "https://api.github.com"
RELEASE_BASE = f"https://github.com/{OWNER}/{REPOSITORY}/releases/download/"
MAX_METADATA = 64 * 1024


def read_https(url: str, limit: int, token: str | None = None) -> bytes:
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "StopSlop-catalog-deployer/1"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
        headers["X-GitHub-Api-Version"] = "2022-11-28"
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request, timeout=30) as response:
        final = urllib.parse.urlsplit(response.geturl())
        allowed = {"api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com"}
        if final.scheme != "https" or final.hostname not in allowed:
            raise SystemExit("GitHub returned a non-HTTPS or unapproved asset redirect")
        data = response.read(limit + 1)
        if len(data) > limit:
            raise SystemExit("Release metadata asset exceeds its size limit")
        return data


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", args.version):
        raise SystemExit("Model release version must be semantic X.Y.Z")
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        raise SystemExit("GITHUB_TOKEN is unavailable; catalog deployment needs contents: read")
    tag = f"model-v{args.version}"
    api_url = f"{API}/repos/{OWNER}/{REPOSITORY}/releases/tags/{urllib.parse.quote(tag, safe='')}"
    try:
        release = json.loads(read_https(api_url, MAX_METADATA, token))
    except Exception as error:
        raise SystemExit(f"Could not read published model release: {error}") from None
    if release.get("tag_name") != tag or release.get("draft") or release.get("prerelease"):
        raise SystemExit("The requested model release is missing, draft, or prerelease")
    assets = {asset.get("name"): asset for asset in release.get("assets", [])}
    package_name = "multilingual-e5-small.ssmodel"
    release_name = "multilingual-e5-small.release.json"
    checksum_name = package_name + ".sha256"
    catalog_name = "multilingual-e5-small.catalog.json"
    for name in (package_name, release_name, checksum_name, catalog_name):
        if name not in assets:
            raise SystemExit(f"Published model release is missing required asset: {name}")
    expected_package_url = RELEASE_BASE + tag + "/" + package_name
    if assets[package_name].get("browser_download_url") != expected_package_url:
        raise SystemExit("Published model package URL is not the canonical project release asset")

    def fetch_asset(name: str) -> bytes:
        item = assets[name]
        if item.get("size", MAX_METADATA + 1) > MAX_METADATA:
            raise SystemExit(f"Release metadata file is too large: {name}")
        url = item.get("browser_download_url", "")
        if not url.startswith(RELEASE_BASE + tag + "/"):
            raise SystemExit(f"Release asset URL is outside the pinned version: {name}")
        return read_https(url, MAX_METADATA)

    manifest = json.loads(fetch_asset(release_name))
    catalog = json.loads(fetch_asset(catalog_name))
    checksum = fetch_asset(checksum_name).decode("ascii").strip()
    package_asset = assets[package_name]
    expected_checksum = f"{manifest.get('sha256')}  {package_name}"
    if manifest.get("releaseTag") != tag or manifest.get("packageVersion") != args.version:
        raise SystemExit("Release manifest version does not match its tag")
    if manifest.get("packageSizeBytes") != package_asset.get("size"):
        raise SystemExit("Release manifest package size differs from GitHub asset metadata")
    if checksum != expected_checksum:
        raise SystemExit("Release checksum asset differs from its release manifest")
    if catalog.get("schemaVersion") != 1 or catalog.get("catalogVersion") != args.version:
        raise SystemExit("Release catalog metadata has an unsupported schema or version")
    models = catalog.get("models")
    if not isinstance(models, list) or len(models) != 1:
        raise SystemExit("Release catalog must contain exactly one verified model")
    entry = models[0]
    expected = {
        "id": "multilingual-e5-small",
        "packageVersion": args.version,
        "packageSizeBytes": manifest["packageSizeBytes"],
        "sha256": manifest["sha256"],
        "downloadUrl": expected_package_url,
        "releaseTag": tag,
        "upstreamRevision": manifest["upstreamRevision"],
    }
    for key, value in expected.items():
        if entry.get(key) != value:
            raise SystemExit(f"Published catalog differs from release manifest at {key}")
    args.output.mkdir(parents=True, exist_ok=True)
    root = Path(__file__).resolve().parents[1] / "site"
    (args.output / "index.html").write_bytes((root / "index.html").read_bytes())
    (args.output / "catalog/v1").mkdir(parents=True, exist_ok=True)
    (args.output / "catalog/v1/schema.json").write_bytes((root / "catalog/v1/schema.json").read_bytes())
    (args.output / "catalog/v1/catalog.json").write_bytes(
        json.dumps(catalog, sort_keys=True, indent=2).encode("utf-8") + b"\n"
    )
    print(f"Prepared verified catalog for {tag}; package asset is {package_asset['size']} bytes")


if __name__ == "__main__":
    main()
