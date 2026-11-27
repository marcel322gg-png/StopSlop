# StopSlop optional AI models

## Implemented in source and release bundle

StopSlop's deterministic filters work without a model. AI defaults off, and the Semantic Topic Filter control is unavailable until a model is installed and selected. The existing ONNX E5 provider now opens model and tokenizer files from the selected app-private model directory; it no longer copies either file from APK assets. Model changes clear pending/cached semantic decisions and replace the provider session on its bounded worker.

The existing Settings page now has an **AI / Models** area with:

- an official catalog loaded from `https://marcel322gg-png.github.io/StopSlop/catalog/v1/catalog.json`;
- compatible model details including language evidence, size, version, runtime profile, license, source revision and package SHA-256;
- explicit download from the model's canonical GitHub Release URL, checked against catalog size and SHA-256 before package validation;
- cached catalog fallback for offline browsing, bounded to 64 KiB and at most 16 entries;
- the existing advanced direct HTTPS `.ssmodel` URL flow, with an unverified-host warning;
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

The app accepts only the fixed public catalog origin and canonical release URLs for catalog entries. Unknown runtime profiles, package schemas, revisions, ABI/API constraints or non-canonical download URLs are shown as unavailable; catalog metadata cannot make an arbitrary ONNX package compatible. Catalog and release metadata use HTTPS and SHA-256; SHA-256 is a corruption check, **not** a publisher signature. No private key or GitHub token is embedded in the app. The manual URL route is retained as an explicitly unverified fallback. Hugging Face is used only by CI to retrieve the pinned source files; the Android app never calls the Hub.

## Model publication and Pages deployment

`.github/workflows/publish-model-release.yml` runs only by deliberate `workflow_dispatch` or a `model-vX.Y.Z` tag. It uses the built-in `GITHUB_TOKEN`, pinned full-commit GitHub Actions, a locked Python 3.11.9 converter environment, and anonymous HTTPS downloads from the immutable Hugging Face revision. It regenerates the tokenizer graph, builds the `.ssmodel`, and fails closed unless exact encoder/tokenizer/license/notice hashes, archive paths, schema, license, size and package metadata pass. It creates a draft GitHub Release only after validation, attaches the package, SHA-256 file, release manifest and catalog JSON, then publishes it. A version collision fails instead of replacing an existing release.

The generator's pinned dependency set is new and the current worktree did not run a GitHub-hosted tokenizer rebuild; the expected tokenizer hash is enforced before publication. Thus a release is **not** claimed until the workflow successfully rebuilds the graph and publishes its assets.

`.github/workflows/deploy-model-catalog.yml` runs after the publisher workflow completes successfully and receives only the version through a five-day Actions artifact. This works with the release created using `GITHUB_TOKEN` without granting Actions write access or depending on a token-generated `release` event. It checks release metadata/assets against the catalog, validates the v1 schema contract and uses the official Pages Actions pinned by full commit SHA. A Pages setup failure can be retried by rerunning this workflow for the existing version, without recreating or overwriting the Release.

The intended Pages base is `https://marcel322gg-png.github.io/StopSlop/`; the catalog URL above is the app's fixed endpoint. A read-only GitHub Pages API check returned HTTP 404, and the public Release list has no `model-v*` Release. Thus neither URL is live yet. One-time repository setup is required: GitHub repository **Settings → Pages → Build and deployment → Source → GitHub Actions**. No PAT or Actions secret is required. After enabling it, run **Actions → Publish StopSlop E5 model release → Run workflow** with version `1.0.0`; after the release publishes, the catalog deployment runs automatically. If the Pages job failed before setup, rerun **Deploy StopSlop model catalog** for the already-published version.

The release workflow refuses an existing release/tag collision. If tokenizer regeneration does not produce the pinned graph checksum, the release job stops before uploading anything; inspect the failed Actions log before changing the lock or expected hash.

## Model-free release MPP

Built with the repository's pinned Gradle 9.9.1 and Morphe plugin 2.1.1. `verifyBundle` passed for 156 patches and all three non-empty DEX payloads. ZIP integrity passed, there are no duplicate entries, and the archive contains no model or tokenizer files. The extension patch maps only the eight pinned ONNX/ORT Extensions native libraries to standard ARM64 and ARM32 library paths. The existing patch name and Settings dependency remain stable.

`patches/build/release/patches-0.84.0.mpp` is 26,136,171 bytes (24.925 MiB), SHA-256 `af49c659196faa76e77da57d095df5c52f60336a7e9f44c032dd47ae4e8e38f2`. It is unsigned; the release receipt was refused because the source tree is dirty. ZIP integrity passed with no duplicate paths or model/tokenizer files. Size breakdown:

| Payload | Uncompressed bytes | Compressed bytes in MPP |
|---|---:|---:|
| TikTok and shared extension MPEs (DEX) | 9,001,548 | 3,787,335 |
| Eight ARM64/ARM32 native runtime libraries | 41,853,012 | 16,896,457 |
| Other patch payloads and metadata, including `classes.dex` | 13,053,682 | 5,070,811 |
| ZIP headers and directory overhead | — | 381,568 |
| **Total** | **63,908,242** | **26,136,171** |

The compressed native libraries alone exceed 15 MiB, so this build misses both the 10 MiB stretch goal and 15 MiB target while preserving the current generic ONNX/ORT Extensions runtime and both ARM ABIs. No custom operator-pruned runtime has been built or validated. API 23+ device execution remains unverified.

## Manual verification still needed

Install the MPP with Morphe Manager, then verify on the phone: no model is selected by default; deterministic filters remain active; the official catalog loads, displays the real package metadata and can download the public Release asset; offline cache works; wrong catalog hash/size, corrupted package and unsupported-profile cases fail closed; import and manual HTTPS fallback work; a valid package becomes `READY` only after session creation; switching clears stale state; active-model deletion is safe; inference works offline and no caption/topic data leaves the device. Confirm the target TikTok manifest grants INTERNET; no permission was added by this patch, and the available manifest fixture is not the exact target APK. Check app-data removal behavior, API 23 and both supported ARM ABIs. No automated tests, ADB, emulator, or device checks were run for this implementation.
