#!/usr/bin/env python3
"""Fetch only the files required to rebuild the pinned E5 data package."""

from __future__ import annotations

import hashlib
import os
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "model-packages" / "source"
REVISION = "761b726dd34fb83930e26aab4e9ac3899aa1fa78"
BASE = f"https://huggingface.co/Xenova/multilingual-e5-small/resolve/{REVISION}/"
FILES = {
    "model_quantized.onnx": ("onnx/model_quantized.onnx", 125_000_000),
    "tokenizer/config.json": ("config.json", 64_000),
    "tokenizer/tokenizer.json": ("tokenizer.json", 32_000_000),
    "tokenizer/tokenizer_config.json": ("tokenizer_config.json", 128_000),
    "tokenizer/special_tokens_map.json": ("special_tokens_map.json", 64_000),
    "tokenizer/sentencepiece.bpe.model": ("sentencepiece.bpe.model", 8_000_000),
}
EXPECTED_MODEL_BYTES = 118_308_185
EXPECTED_MODEL_SHA256 = "f80102d3f2a1229f387d3c81909990d8945513e347b0eab049f7de3c6f98c193"


def download(relative_path: str, upstream_path: str, max_bytes: int) -> None:
    destination = SOURCE / relative_path
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".part")
    request = urllib.request.Request(BASE + upstream_path + "?download=true", headers={"User-Agent": "StopSlop-model-packager/1"})
    digest = hashlib.sha256()
    size = 0
    try:
        with urllib.request.urlopen(request, timeout=60) as response, temporary.open("wb") as output:
            if not response.geturl().lower().startswith("https://"):
                raise RuntimeError(f"Upstream redirect was not HTTPS for {upstream_path}")
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                size += len(chunk)
                if size > max_bytes:
                    raise RuntimeError(f"Pinned upstream file exceeded its limit: {upstream_path}")
                output.write(chunk)
                digest.update(chunk)
        if size <= 0:
            raise RuntimeError(f"Pinned upstream file was empty: {upstream_path}")
        if relative_path == "model_quantized.onnx" and (size != EXPECTED_MODEL_BYTES or digest.hexdigest() != EXPECTED_MODEL_SHA256):
            raise RuntimeError("Pinned E5 encoder size or SHA-256 changed")
        os.replace(temporary, destination)
        print(f"Fetched {upstream_path}: {size} bytes sha256={digest.hexdigest()}")
    except Exception:
        temporary.unlink(missing_ok=True)
        raise


def main() -> None:
    SOURCE.mkdir(parents=True, exist_ok=True)
    for destination, (upstream, limit) in FILES.items():
        download(destination, upstream, limit)
    for name in ("LICENSE-MIT.txt", "NOTICE.txt"):
        source = ROOT / "model-packages" / "provenance" / name
        if not source.is_file():
            raise SystemExit(f"Missing tracked model provenance file: {source}")
        (SOURCE / name).write_bytes(source.read_bytes())


if __name__ == "__main__":
    main()
