#!/usr/bin/env python3
"""Audit a data-only .ssmodel and emit release metadata without trusting its manifest."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path


REVISION = "761b726dd34fb83930e26aab4e9ac3899aa1fa78"
FILES = {
    "model/model.onnx": (118_308_185, "f80102d3f2a1229f387d3c81909990d8945513e347b0eab049f7de3c6f98c193"),
    "tokenizer/tokenizer.onnx": (5_069_559, "d74a9c6240a3a337fabba6acf450707658041036b490b83a219ff77e844a1d45"),
    "LICENSE.txt": (1_096, "ea599e07ac6f4c2386fb47cb3a3c399e5f1d2a327b59bcaac72d82adf163a797"),
    "NOTICE.txt": (1_312, "d38daa1a7502e0841736c5ef5a942ad07142e4239fcde688d4152512004cfd06"),
}
PACKAGE_ENTRIES = {"manifest.json", *FILES}


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("package", type=Path)
    parser.add_argument("--version", required=True, help="Release version without the model-v prefix")
    parser.add_argument("--output", type=Path, required=True, help="Directory for checksum and release JSON")
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", args.version):
        raise SystemExit("Model release version must be semantic X.Y.Z")
    if not args.package.is_file() or args.package.stat().st_size > 160 * 1024 * 1024:
        raise SystemExit("Model package is missing or exceeds the 160 MiB cap")

    with zipfile.ZipFile(args.package) as archive:
        infos = [info for info in archive.infolist() if not info.is_dir()]
        names = [info.filename for info in infos]
        if len(names) != len(set(names)) or set(names) != PACKAGE_ENTRIES:
            raise SystemExit("Package entries are duplicated or differ from the supported E5 package profile")
        for info in infos:
            if info.filename.startswith("/") or "\\" in info.filename or any(part in ("", ".", "..") for part in info.filename.split("/")):
                raise SystemExit(f"Unsafe package path: {info.filename}")
            mode = info.external_attr >> 16
            if mode and (mode & 0o170000) == 0o120000:
                raise SystemExit(f"Symlink package entry is not allowed: {info.filename}")
        bad = archive.testzip()
        if bad is not None:
            raise SystemExit(f"Package CRC failed for {bad}")
        manifest_bytes = archive.read("manifest.json")
        if len(manifest_bytes) > 64 * 1024:
            raise SystemExit("Model manifest exceeds the supported limit")
        manifest = json.loads(manifest_bytes)
        required = {
            "schemaVersion": 1,
            "modelId": "multilingual-e5-small",
            "engineProfile": "e5-xlm-roberta-onnx-v1",
            "task": "text-embedding",
            "sourceRepository": "Xenova/multilingual-e5-small",
            "sourceRevision": REVISION,
            "licenseSpdx": "MIT",
            "minimumAndroidApi": 23,
        }
        for key, value in required.items():
            if manifest.get(key) != value:
                raise SystemExit(f"Model package metadata mismatch at {key}")
        listed = manifest.get("files")
        if not isinstance(listed, list) or {entry.get("path") for entry in listed} != set(FILES):
            raise SystemExit("Model manifest file list is unsupported")
        for path, (expected_size, expected_hash) in FILES.items():
            data = archive.read(path)
            matching = [item for item in listed if item.get("path") == path]
            if len(matching) != 1 or len(data) != expected_size or digest(data) != expected_hash:
                raise SystemExit(f"Pinned model package file failed verification: {path}")
            if matching[0].get("sizeBytes") != expected_size or matching[0].get("sha256") != expected_hash:
                raise SystemExit(f"Manifest digest or size mismatch: {path}")
        if b"MIT License" not in archive.read("LICENSE.txt"):
            raise SystemExit("Package does not contain its MIT license text")

    package_size = args.package.stat().st_size
    package_hash = digest(args.package.read_bytes())
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / "multilingual-e5-small.ssmodel.sha256").write_text(
        f"{package_hash}  multilingual-e5-small.ssmodel\n", encoding="ascii"
    )
    release = {
        "releaseTag": f"model-v{args.version}",
        "modelId": "multilingual-e5-small",
        "packageVersion": args.version,
        "packageSchemaVersion": 1,
        "packageFormat": "StopSlop .ssmodel data-only ZIP",
        "packageSizeBytes": package_size,
        "sha256": package_hash,
        "runtimeProfile": "e5-xlm-roberta-onnx-v1",
        "minimumStopSlopVersion": "0.84.0",
        "minimumAndroidApi": 23,
        "compatibleAbis": ["arm64-v8a", "armeabi-v7a"],
        "licenseSpdx": "MIT",
        "licenseUrl": f"https://huggingface.co/Xenova/multilingual-e5-small/tree/{REVISION}",
        "upstreamRepository": "Xenova/multilingual-e5-small",
        "upstreamRevision": REVISION,
        "files": [
            {"path": path, "sizeBytes": size, "sha256": file_hash}
            for path, (size, file_hash) in FILES.items()
        ],
    }
    (args.output / "multilingual-e5-small.release.json").write_text(
        json.dumps(release, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    print(f"packageSizeBytes={package_size}")
    print(f"sha256={package_hash}")


if __name__ == "__main__":
    main()
