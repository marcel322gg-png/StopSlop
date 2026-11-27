# StopSlop optional AI models

## Implemented in source and release bundle

StopSlop's deterministic filters work without a model. AI defaults off, and the Semantic Topic Filter control is unavailable until a model is installed and selected. The existing ONNX E5 provider now opens model and tokenizer files from the selected app-private model directory; it no longer copies either file from APK assets. Model changes clear pending/cached semantic decisions and replace the provider session on its bounded worker.

The existing Settings page now has an **AI / Models** area with:

- the supported multilingual E5 Small profile and its pinned Xenova revision;
- explicit download of a `.ssmodel` from a user-supplied HTTPS URL, with an unverified-host warning;
- local `.ssmodel` import through Android's `ACTION_OPEN_DOCUMENT` picker;
- installed model details, active selection/switching, storage totals, and deletion;
- progress, cancellation, validation errors, and status text.

Downloads are streamed to temporary storage, limited to 160 MiB, redirected only over HTTPS, and then installed into TikTok's private `files/stopSlop/models` directory. The installer rejects unexpected paths and profile/task mismatches, enforces exact E5 tensor/preprocessing metadata, checks file lengths and SHA-256 values, and promotes verified content by same-filesystem rename. It allows at most two installed models and 512 MiB total. Only ONNX data, tokenizer data, license, notice, and JSON metadata are accepted; no code or native libraries are loaded from model packages.

`READY` is emitted only after the selected files pass hash checks and ONNX Runtime successfully creates the tokenizer and embedding sessions. The feed path remains asynchronous, bounded, and fail-open. A cache miss keeps the item visible until a later natural filter evaluation; same-item reevaluation is not verified.

## First model package

The generated package is a data-only `.ssmodel` ZIP with exactly:

```text
manifest.json
model/model.onnx
tokenizer/tokenizer.onnx
LICENSE.txt
NOTICE.txt
```

The pinned entry is `Xenova/multilingual-e5-small` revision `761b726dd34fb83930e26aab4e9ac3899aa1fa78`, INT8 ONNX, MIT. The ONNX encoder is 118,308,185 bytes with SHA-256 `f80102d3f2a1229f387d3c81909990d8945513e347b0eab049f7de3c6f98c193`; the matching XLM-R tokenizer graph is 5,069,559 bytes with SHA-256 `d74a9c6240a3a337fabba6acf450707658041036b490b83a219ff77e844a1d45`. The tokenizer graph was generated from the pinned tokenizer material by `tools/generate_stop_slop_tokenizer_onnx.py`. The model card's multilingual language count is a publisher claim; StopSlop language quality is not measured. [Pinned model revision](https://huggingface.co/Xenova/multilingual-e5-small/tree/761b726dd34fb83930e26aab4e9ac3899aa1fa78).

`tools/package_stop_slop_e5_model.py` verifies those source files and creates `artifacts/model-packages/multilingual-e5-small.ssmodel`. The audited package is 86,306,396 bytes with SHA-256 `7a631638566759f87960f1e90eb913bcb50052148ae7a06730886069692235ee`. It contains no DEX, JAR, `.so`, or script entries. The package artifact and model inputs are ignored by Git.

The package is generated locally; this repository has no hosted `.ssmodel` download URL or signed catalog. **Browse models** shows the pinned profile, while **Download** requires the user to enter a direct HTTPS URL for a hosted package. The host is not authenticated. Exact pinned E5 file hashes are app-pinned; other packages are accepted only for the same E5 profile and are clearly identified as unverified. This does not support arbitrary Hugging Face models or other tokenizer/runtime formats.

## Model-free release MPP

Built with the repository's pinned Gradle 9.9.1 and Morphe plugin 2.1.1. `verifyBundle` passed for 156 patches and all three non-empty DEX payloads. ZIP integrity passed, there are no duplicate entries, and the archive contains no model or tokenizer files. The extension patch maps only the eight pinned ONNX/ORT Extensions native libraries to standard ARM64 and ARM32 library paths. The existing patch name and Settings dependency remain stable.

`patches/build/release/patches-0.84.0.mpp` is 26,132,162 bytes (24.922 MiB), SHA-256 `88c6893551528d0a7cae81bc97e4b11301844f47f78a983be76daa8e5dea82b2`. It is unsigned; the release receipt was refused because the source tree is dirty. ZIP integrity passed with no duplicate paths or model/tokenizer payloads (one empty `stop_slop/models/` directory entry remains). Size breakdown:

| Payload | Uncompressed bytes | Compressed bytes in MPP |
|---|---:|---:|
| TikTok and shared extension MPEs (DEX) | 8,985,544 | 3,783,326 |
| Eight ARM64/ARM32 native runtime libraries | 41,853,012 | 16,896,457 |
| Other patch payloads and metadata | 13,053,682 | 5,070,811 |
| ZIP headers and directory overhead | — | 381,568 |
| **Total** | **63,892,238** | **26,132,162** |

The compressed native libraries alone exceed 15 MiB, so this build misses both the 10 MiB stretch goal and 15 MiB target while preserving the current generic ONNX/ORT Extensions runtime and both ARM ABIs. No custom operator-pruned runtime has been built or validated. API 23+ device execution remains unverified.

## Manual verification still needed

Install the MPP with Morphe Manager, then verify on the phone: no model is selected by default; deterministic filters remain active; model import and a hosted HTTPS download show progress and cancel cleanly; invalid/hash-mismatched packages are rejected; a valid package becomes `READY` only after session creation; switching clears stale state; active-model deletion requires switching or disabling AI; inference works offline and no caption/topic data leaves the device. Check both normal use and app-data removal behavior. No automated tests, ADB, emulator, or device checks were run for this implementation.
