#!/usr/bin/env python3
"""Validate a rendered catalog against its release package and schema contract."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REPO = "marcel322gg-png/StopSlop"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("catalog", type=Path)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--package", type=Path, help="Verify the catalog against the built package bytes")
    source.add_argument("--size", type=int, help="Previously verified package byte size for the Pages deployment job")
    parser.add_argument("--sha256", help="Previously verified package SHA-256 for the Pages deployment job")
    parser.add_argument("--version", required=True)
    args = parser.parse_args()
    schema_path = ROOT / "site/catalog/v1/schema.json"
    schema = json.loads(schema_path.read_text(encoding="utf-8"))
    required_root = set(schema["required"])
    required_entry = set(schema["properties"]["models"]["items"]["required"])
    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    if set(catalog) != required_root or catalog.get("schemaVersion") != 1:
        raise SystemExit("Catalog root does not match the v1 schema")
    if catalog.get("catalogVersion") != args.version:
        raise SystemExit("Catalog version does not match the model release")
    models = catalog.get("models")
    if not isinstance(models, list) or len(models) != 1:
        raise SystemExit("Stable catalog must contain exactly the audited E5 package")
    entry = models[0]
    if set(entry) != required_entry:
        raise SystemExit("Catalog model fields do not match the published schema")
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", args.version):
        raise SystemExit("Model version is invalid")
    if args.package:
        package_hash = hashlib.sha256(args.package.read_bytes()).hexdigest()
        package_size = args.package.stat().st_size
        with zipfile.ZipFile(args.package) as archive:
            if archive.testzip() is not None:
                raise SystemExit("Release package ZIP integrity check failed")
    else:
        if args.size is None or not args.sha256 or not re.fullmatch(r"[0-9a-f]{64}", args.sha256):
            raise SystemExit("Pages validation requires the previously audited package size and SHA-256")
        package_size = args.size
        package_hash = args.sha256
    expected_url = f"https://github.com/{REPO}/releases/download/model-v{args.version}/multilingual-e5-small.ssmodel"
    expected = {
        "id": "multilingual-e5-small",
        "packageVersion": args.version,
        "packageSchemaVersion": 1,
        "runtimeProfile": "e5-xlm-roberta-onnx-v1",
        "task": "text-embedding",
        "licenseSpdx": "MIT",
        "upstreamRepository": "Xenova/multilingual-e5-small",
        "upstreamRevision": "761b726dd34fb83930e26aab4e9ac3899aa1fa78",
        "packageSizeBytes": package_size,
        "sha256": package_hash,
        "assetName": "multilingual-e5-small.ssmodel",
        "releaseTag": f"model-v{args.version}",
        "downloadUrl": expected_url,
        "minimumAppVersion": "0.84.0",
        "minimumAndroidApi": 23,
        "compatibleAbis": ["arm64-v8a", "armeabi-v7a"],
        "publisherSignature": False,
    }
    for key, value in expected.items():
        if entry.get(key) != value:
            raise SystemExit(f"Catalog does not match verified release data: {key}")
    if not isinstance(entry.get("languages"), list) or not entry["languages"]:
        raise SystemExit("Catalog must include language evidence")
    if not entry.get("licenseUrl", "").startswith("https://"):
        raise SystemExit("Catalog license URL must use HTTPS")
    if set(entry["compatibleAbis"]) != {"arm64-v8a", "armeabi-v7a"}:
        raise SystemExit("Catalog ABI list differs from the packaged runtime")
    print(f"Catalog v1 validated against the release artifact ({package_size} bytes, sha256={package_hash})")


if __name__ == "__main__":
    main()
