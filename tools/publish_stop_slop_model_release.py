#!/usr/bin/env python3
"""Publish a verified model package as a draft-then-published GitHub Release."""

from __future__ import annotations

import argparse
import http.client
import json
import os
import re
import urllib.parse
from pathlib import Path


OWNER = "marcel322gg-png"
REPOSITORY = "StopSlop"
API_HOST = "api.github.com"
UPLOAD_HOST = "uploads.github.com"


def api(method: str, path: str, token: str, payload: dict | None = None) -> tuple[int, dict]:
    connection = http.client.HTTPSConnection(API_HOST, timeout=30)
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {token}",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "StopSlop-model-release/1",
    }
    if body is not None:
        headers["Content-Type"] = "application/json"
    connection.request(method, path, body=body, headers=headers)
    response = connection.getresponse()
    raw = response.read(128 * 1024)
    status = response.status
    connection.close()
    try:
        data = json.loads(raw) if raw else {}
    except json.JSONDecodeError:
        data = {"message": "GitHub API returned a non-JSON response"}
    return status, data


def upload_asset(upload_url: str, name: str, source: Path, token: str) -> None:
    parsed = urllib.parse.urlsplit(upload_url)
    if parsed.scheme != "https" or parsed.hostname != UPLOAD_HOST or parsed.username or parsed.password:
        raise SystemExit("GitHub returned an unexpected release asset host; refusing upload")
    path = parsed.path + ("?" + parsed.query if parsed.query else "")
    separator = "&" if "?" in path else "?"
    path += separator + "name=" + urllib.parse.quote(name, safe="")
    size = source.stat().st_size
    connection = http.client.HTTPSConnection(UPLOAD_HOST, timeout=120)
    connection.putrequest("POST", path)
    connection.putheader("Authorization", f"Bearer {token}")
    connection.putheader("Accept", "application/vnd.github+json")
    connection.putheader("Content-Type", "application/octet-stream")
    connection.putheader("Content-Length", str(size))
    connection.putheader("User-Agent", "StopSlop-model-release/1")
    connection.endheaders()
    with source.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            connection.send(chunk)
    response = connection.getresponse()
    result = response.read(64 * 1024)
    status = response.status
    connection.close()
    if status != 201:
        raise SystemExit(f"Release asset upload failed for {name} (HTTP {status}): {result[:2000].decode('utf-8', 'replace')}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--package", required=True, type=Path)
    parser.add_argument("--metadata-dir", required=True, type=Path)
    parser.add_argument("--allow-tag-from-push", action="store_true")
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", args.version):
        raise SystemExit("Release version must be semantic X.Y.Z")
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        raise SystemExit("GITHUB_TOKEN is unavailable; workflow needs contents: write")
    tag = f"model-v{args.version}"
    release_path = args.metadata_dir / "multilingual-e5-small.release.json"
    checksum_path = args.metadata_dir / "multilingual-e5-small.ssmodel.sha256"
    catalog_path = args.metadata_dir / "catalog.json"
    if not args.package.is_file() or not release_path.is_file() or not checksum_path.is_file() or not catalog_path.is_file():
        raise SystemExit("Verified package or release metadata is missing")
    manifest = json.loads(release_path.read_text(encoding="utf-8"))
    if manifest.get("releaseTag") != tag or manifest.get("packageVersion") != args.version:
        raise SystemExit("Release metadata does not match the requested immutable version")

    encoded_tag = urllib.parse.quote(tag, safe="")
    status, existing = api("GET", f"/repos/{OWNER}/{REPOSITORY}/releases/tags/{encoded_tag}", token)
    if status == 200:
        raise SystemExit(f"Release {tag} already exists; refusing to overwrite or replace it")
    if status != 404:
        raise SystemExit(f"Could not check release collision safely (HTTP {status})")
    status, tag_ref = api("GET", f"/repos/{OWNER}/{REPOSITORY}/git/ref/tags/{encoded_tag}", token)
    if status == 200 and not args.allow_tag_from_push:
        raise SystemExit(f"Tag {tag} already exists; refusing to attach a new package to an existing tag")
    if status not in (200, 404):
        raise SystemExit(f"Could not check tag collision safely (HTTP {status})")

    release_request = {
        "tag_name": tag,
        "target_commitish": "main",
        "name": f"StopSlop E5 model {args.version}",
        "body": (
            "Verified data-only E5 package. See attached release manifest and SHA-256 file.\n\n"
            f"Upstream: Xenova/multilingual-e5-small at {manifest['upstreamRevision']}\n"
            "Profile: e5-xlm-roberta-onnx-v1; license: MIT.\n"
            "SHA-256 is a corruption check, not a publisher signature."
        ),
        "draft": True,
        "prerelease": False,
        "make_latest": "false",
    }
    status, created = api("POST", f"/repos/{OWNER}/{REPOSITORY}/releases", token, release_request)
    if status != 201:
        raise SystemExit(f"Could not create draft model release (HTTP {status}): {created.get('message', 'unknown error')}")
    upload_url = created.get("upload_url", "").split("{", 1)[0]
    if not upload_url:
        raise SystemExit("GitHub release response omitted the upload URL")
    upload_asset(upload_url, args.package.name, args.package, token)
    upload_asset(upload_url, release_path.name, release_path, token)
    upload_asset(upload_url, checksum_path.name, checksum_path, token)
    upload_asset(upload_url, "multilingual-e5-small.catalog.json", catalog_path, token)
    status, published = api("PATCH", f"/repos/{OWNER}/{REPOSITORY}/releases/{created['id']}", token, {"draft": False})
    if status != 200:
        raise SystemExit(f"Assets uploaded but GitHub could not publish the release (HTTP {status})")
    print(f"Published {published.get('html_url', tag)}")
    print("Release asset names and package hashes were validated before upload.")


if __name__ == "__main__":
    main()
