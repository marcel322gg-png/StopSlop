#!/usr/bin/env python3
"""Create the optional data-only StopSlop .ssmodel package from pinned local inputs."""

import argparse
import hashlib
import json
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "model-packages" / "source"
REVISION = "761b726dd34fb83930e26aab4e9ac3899aa1fa78"
FILES = (
    ("model_quantized.onnx", "model/model.onnx",
     118_308_185, "f80102d3f2a1229f387d3c81909990d8945513e347b0eab049f7de3c6f98c193"),
    ("tokenizer.onnx", "tokenizer/tokenizer.onnx",
     5_069_559, "d74a9c6240a3a337fabba6acf450707658041036b490b83a219ff77e844a1d45"),
    ("LICENSE-MIT.txt", "LICENSE.txt",
     1_096, "ea599e07ac6f4c2386fb47cb3a3c399e5f1d2a327b59bcaac72d82adf163a797"),
    ("NOTICE.txt", "NOTICE.txt",
     1_312, "d38daa1a7502e0841736c5ef5a942ad07142e4239fcde688d4152512004cfd06"),
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def zip_info(name: str, method: int) -> zipfile.ZipInfo:
    info = zipfile.ZipInfo(name, date_time=(2020, 1, 1, 0, 0, 0))
    info.compress_type = method
    info.external_attr = 0o100644 << 16
    return info


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", nargs="?", type=Path,
                        default=ROOT / "artifacts" / "model-packages" / "multilingual-e5-small.ssmodel")
    args = parser.parse_args()

    manifest_files = []
    for source_name, package_path, expected_size, expected_hash in FILES:
        source = SOURCE / source_name
        if not source.is_file():
            raise SystemExit(f"Missing optional model input: {source}")
        actual_size = source.stat().st_size
        actual_hash = sha256(source)
        if actual_size != expected_size or actual_hash != expected_hash:
            raise SystemExit(f"Pinned model input mismatch: {source_name} ({actual_size}, {actual_hash})")
        source_url = (f"https://huggingface.co/Xenova/multilingual-e5-small/resolve/{REVISION}/onnx/model_quantized.onnx"
                      if source_name == "model_quantized.onnx" else "")
        entry = {"path": package_path, "sizeBytes": actual_size, "sha256": actual_hash}
        if source_url:
            entry["sourceUrl"] = source_url
        manifest_files.append(entry)

    manifest = {
        "schemaVersion": 1,
        "modelId": "multilingual-e5-small",
        "modelVersion": REVISION,
        "displayName": "Multilingual E5 Small (INT8)",
        "publisher": "Xenova / intfloat",
        "engineProfile": "e5-xlm-roberta-onnx-v1",
        "runtimeVersion": "onnxruntime-android 1.20.0; extensions 0.13.0",
        "task": "text-embedding",
        "languages": "100+ language model-card claim; StopSlop quality not independently measured",
        "quantization": "INT8",
        "format": "ONNX",
        "sourceRepository": "Xenova/multilingual-e5-small",
        "sourceRevision": REVISION,
        "licenseSpdx": "MIT",
        "licenseUrl": f"https://huggingface.co/Xenova/multilingual-e5-small/tree/{REVISION}",
        "attribution": "intfloat/multilingual-e5-small; ONNX export by Xenova",
        "tokenizerType": "XLM-Roberta tokenizer via ONNX Runtime Extensions",
        "tokenizerSource": "Generated from the XLM-R tokenizer material at the pinned E5 revision using tools/generate_stop_slop_tokenizer_onnx.py",
        "queryPrefix": "query: ",
        "passagePrefix": "passage: ",
        "maximumSequenceLength": 512,
        "pooling": "mean",
        "l2Normalize": True,
        "embeddingDimensions": 384,
        "tensorContract": {
            "tokenizerInput": {"name": "inputs", "dtype": "string", "shape": [1]},
            "tokenizerOutput": {"name": "tokens", "dtype": "int64", "shape": [1, "sequence"]},
            "modelInputs": [
                {"name": "input_ids", "dtype": "int64"},
                {"name": "attention_mask", "dtype": "int64"},
                {"name": "token_type_ids", "dtype": "int64"},
            ],
            "modelOutput": {"name": "last_hidden_state", "dtype": "float32", "shape": [1, "sequence", 384]},
        },
        "minimumAndroidApi": 23,
        "compatibleAbis": ["arm64-v8a", "armeabi-v7a"],
        "memoryEstimateBytes": 805_306_368,
        "memoryEstimateLabel": "Unmeasured planning estimate (768 MiB)",
        "maximumPackageBytes": 160 * 1024 * 1024,
        "files": manifest_files,
    }
    encoded_manifest = (json.dumps(manifest, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".part")
    try:
        with zipfile.ZipFile(temporary, "w", allowZip64=True) as package:
            package.writestr(zip_info("manifest.json", zipfile.ZIP_DEFLATED), encoded_manifest,
                             compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
            for source_name, package_path, _, _ in FILES:
                package.write(SOURCE / source_name, package_path,
                              compress_type=zipfile.ZIP_DEFLATED, compresslevel=1)
        temporary.replace(args.output)
    except Exception:
        temporary.unlink(missing_ok=True)
        raise
    print(f"Created {args.output} ({args.output.stat().st_size} bytes)")
    print(f"SHA-256 {sha256(args.output)}")


if __name__ == "__main__":
    main()
