#!/usr/bin/env python3
"""Render the only supported public catalog entry from verified release metadata."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


REPOSITORY = "marcel322gg-png/StopSlop"
REVISION = "761b726dd34fb83930e26aab4e9ac3899aa1fa78"
MODEL_ID = "multilingual-e5-small"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--size", required=True, type=int)
    parser.add_argument("--sha256", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", args.version):
        raise SystemExit("Catalog version must be semantic X.Y.Z")
    if args.size <= 0 or args.size > 160 * 1024 * 1024 or not re.fullmatch(r"[0-9a-f]{64}", args.sha256):
        raise SystemExit("Release size or SHA-256 is invalid")
    asset = f"{MODEL_ID}.ssmodel"
    tag = f"model-v{args.version}"
    entry = {
        "id": MODEL_ID,
        "name": "Multilingual E5 Small (INT8)",
        "description": "Xenova multilingual text-embedding model for semantic topic matching. The model card claims 100+ languages; StopSlop language quality is not independently measured.",
        "packageVersion": args.version,
        "packageSchemaVersion": 1,
        "runtimeProfile": "e5-xlm-roberta-onnx-v1",
        "task": "text-embedding",
        "languages": ["100+ languages (publisher claim; quality unmeasured)"],
        "quantization": "INT8",
        "format": "ONNX",
        "publisher": "Xenova / intfloat",
        "licenseSpdx": "MIT",
        "licenseUrl": f"https://huggingface.co/Xenova/multilingual-e5-small/tree/{REVISION}",
        "upstreamRepository": "Xenova/multilingual-e5-small",
        "upstreamRevision": REVISION,
        "packageSizeBytes": args.size,
        "sha256": args.sha256,
        "assetName": asset,
        "releaseTag": tag,
        "downloadUrl": f"https://github.com/{REPOSITORY}/releases/download/{tag}/{asset}",
        "minimumAppVersion": "0.84.0",
        "minimumAndroidApi": 23,
        "compatibleAbis": ["arm64-v8a", "armeabi-v7a"],
        "runtimeVersion": "ONNX Runtime Android 1.20.0; ORT Extensions 0.13.0",
        "memoryEstimateBytes": 805306368,
        "memoryEstimateLabel": "Unmeasured planning estimate (768 MiB)",
        "publisherSignature": False,
    }
    catalog = {"schemaVersion": 1, "catalogVersion": args.version, "models": [entry]}
    if entry["downloadUrl"] != f"https://github.com/{REPOSITORY}/releases/download/{tag}/{asset}":
        raise SystemExit("Generated download URL failed its canonical source check")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(catalog, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print(f"Rendered {args.output} for {tag}")


if __name__ == "__main__":
    main()
