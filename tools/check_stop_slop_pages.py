#!/usr/bin/env python3
"""Fail with the precise repository setting when GitHub Pages is not enabled."""

from __future__ import annotations

import json
import os
import urllib.error
import urllib.request


repository = os.environ.get("GITHUB_REPOSITORY", "")
token = os.environ.get("GITHUB_TOKEN", "")
if repository != "marcel322gg-png/StopSlop" or not token:
    raise SystemExit("Expected the StopSlop repository and its Actions GITHUB_TOKEN")
request = urllib.request.Request(
    f"https://api.github.com/repos/{repository}/pages",
    headers={
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {token}",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "StopSlop-pages-preflight/1",
    },
)
try:
    with urllib.request.urlopen(request, timeout=20) as response:
        page = json.loads(response.read(64 * 1024))
except urllib.error.HTTPError as error:
    if error.code == 404:
        raise SystemExit(
            "GitHub Pages is not enabled. One-time setup: Repository Settings > Pages > "
            "Build and deployment > Source > GitHub Actions, then rerun this workflow."
        ) from None
    raise SystemExit(f"Could not confirm GitHub Pages configuration (HTTP {error.code})") from None
if page.get("build_type") != "workflow":
    raise SystemExit(
        "GitHub Pages is configured to publish from a branch. One-time setup: Repository Settings > "
        "Pages > Build and deployment > Source > GitHub Actions, then rerun this workflow."
    )
print("GitHub Pages is configured for GitHub Actions deployment.")
