# StopSlop - Master Technical Roadmap

**Revision:** 2.0
**Date:** 25 November 2026
**Product:** StopSlop - a TikTok feed-filtering product delivered as a modified TikTok client
**Primary device:** Samsung Galaxy A25 5G
**Secondary device:** Samsung Galaxy A12, after A25 is stable

---

## 1. Product definition

StopSlop is not a standalone Android companion app and is not a Firefox extension or firewall. The intended product is **one modified TikTok application** with StopSlop settings and filtering integrated into the TikTok client.

The current implementation is built from Morphe/HushFeed patch sources. The source project is edited, compiled into a Morphe `.mpp` patch bundle, then applied by Morphe Manager to a compatible original TikTok APK/BUNDLE. The `.mpp` is a patch bundle, not the final installed TikTok APK.

The user wants the installed product and all user-facing StopSlop surfaces to have their own identity. The project must retain required upstream licenses, notices, copyright and attribution. Technical ancestry must not be confused with product branding.

### Current working direction

- Continue from the **currently modified HushFeed-based source repository** that has already produced a successful `.mpp`.
- Treat that repository as the active implementation until a separately approved architecture decision says otherwise.
- Do not silently migrate to a new add-on repository, create a companion application, or split patch bundles during cleanup.
- Keep Morphe patching and its source/build infrastructure.
- Keep inherited HushFeed features that remain useful and functional; organize them below StopSlop's own controls rather than deleting them merely because they are inherited.
- Revisit a separate StopSlop add-on bundle only as a deliberate future maintenance decision. HushFeed documents a public add-on contract and Morphe supports multiple local `.mpp` sources, but that is not a reason to migrate the already-working implementation now.

## 2. Confirmed current state and project stage

This snapshot is based on the user's supplied Codex reports, screenshots and build output. It is not a fresh inspection of the local working tree. Codex must reconcile it with the actual branch before any cleanup or feature work.

### 2.1 What has been implemented

| Area | Current reported state | Evidence/status |
|---|---|---|
| Patch project | HushFeed-based Morphe patch source, built locally | User supplied successful `:patches:buildAndroid` output |
| Bundle verification | `patches-0.84.0.mpp`; `verifyBundle` reported 155 patches and three non-empty DEX payloads | Verified in supplied console screenshot |
| StopSlop settings | Settings entry/page opens after a dependency-order fix | User manually confirmed on A25 |
| Ads & sponsorships | Preference key `stop_slop_ad_sponsorship`, default `true`; FeedItemsFilter registration and marker filtering reported | Source-reported; full runtime behavior not independently verified |
| AI-generated content | Preference key `stop_slop_ai_generated`, default `false`; ContentMarkerFilters/FeedItemsFilter integration reported | Source-reported; exact signal coverage and runtime behavior need confirmation |
| Minimum Likes | `stop_slop_min_likes`, default `false`; `stop_slop_min_likes_threshold`, default `0`, range 0-1,000,000 | User confirmed settings work; filtering behavior still needs explicit device verification |
| Minimum Likes dependency | Threshold is added to the same PreferenceScreen before `setDependency()`; exact dependency targets `Settings.STOP_SLOP_MIN_LIKES.key` | Source audit passed |
| Reload TikTok | Convenience action on the StopSlop settings page | Source-reported |
| Settings order | StopSlop filters moved ahead of inherited settings; inherited sections retained below | Source audit passed |
| Localization | English lookup entries and German translations/runtime map generated | Source-reported; no full locale/device verification |
| StopSlop styling | Branded header, violet headings, gradient/inset cards and disabled-state styling | Diff-reported; visual runtime check still required |
| Recycled-row fix | `ScreenColors.java` adjusted to clear stale `TAG_SURFACE` cache for StopSlop rows | Diff-reported; must be visually checked on device |
| Branding/assets | Asset audit identified legacy HushFeed PNGs, a generated monochrome status vector and user-facing text. Replacement package/instructions were prepared but implementation is not confirmed | Not yet accepted as integrated |
| Repository cleanup | A broad cleanup was discussed, but no cleanup result has been accepted | Not started / do not assume |
| Future detectors | Topic semantics, in-video promotion detection, model-based AI/slop classification, filtered history, model manager | Not implemented as complete features |

### 2.2 Honest maturity assessment

The project is at **early MVP foundation / first functional slice**, not near feature completion. The user's estimate that roughly 1/10 of the intended product functionality exists is a reasonable planning approximation, not a measured completion percentage.

Current stage name:

> **Stage 1 - Stabilize the integrated StopSlop shell and verify the first metadata/marker filters.**

The immediate work is not to add all AI capabilities. It is to protect the working settings and build, verify what the first filters actually do, complete the product-facing rebrand, and perform only low-risk cleanup.

Use these implementation statuses consistently:

- `NOT_IMPLEMENTED` - no working implementation.
- `UI_ONLY` - control/label exists, but no working detector or action.
- `SOURCE_IMPLEMENTED` - relevant source logic exists.
- `HOOK_REGISTERED` - detector/filter is connected to a patch hook.
- `DEVICE_VERIFIED` - behavior was manually confirmed on the target phone.
- `BLOCKED_BY_FEASIBILITY` - required TikTok data/hook is not available or not proven.

Never describe a feature as working based only on the presence of a switch.

## 3. Feature priority and settings hierarchy

The current UI should make StopSlop the primary product, while keeping inherited working settings available in a clearly secondary area.

### P0 - current core: stabilize and verify

1. **Ads & sponsorships**
   - TikTok's official ad/promotional labels.
   - Paid partnership/commercial-content signals where the hook exposes reliable metadata.
   - Keep official platform labels distinct from inferred sponsorship.
2. **Minimum Likes**
   - Hide posts below the configured threshold.
   - Preserve the current enabled state, threshold key, default and range.
3. **Official AI-generated label**
   - Detect TikTok-provided AI content markers where exposed.
   - Do not claim generic visual AI detection.
4. **Pause / master enable**
   - Pause all StopSlop filtering without destroying individual filter settings.
5. **Reload TikTok**
   - Convenience action only; never describe it as a filter.
6. **Detector state and transparency**
   - Clearly indicate implemented, unavailable, hook-not-found or not-yet-implemented states.

**P0 acceptance:** each enabled filter has a verified data source, a registered hook, a reason for each decision, a fail-open path and a manual A25 result.

### P1 - deterministic topic filtering

- User-defined topic/block rules.
- Include/exclude phrases and hashtags.
- Creator allowlist/blocklist.
- Metadata-backed rules before ML.
- Sound/music identifiers only if the verified hook exposes them.
- Sensitivity presets only when their behavior is defined and explainable.

### P2 - in-video promotion detection

- Caption, URL, promo-code, affiliate and call-to-action patterns.
- Brand/product/service patterns, including VPN promotions.
- Separate an actual promotion from an ordinary mention or review.
- Transcript/ASR and sampled-frame OCR only after proving media access and pre-display time budget.
- First safe version may skip the entire video. Do not claim the ability to skip only an advertisement segment until timestamp detection and seeking are demonstrated.

### P3 - AI content and AI slop

Keep three concepts separate:
1. TikTok's official AI-generated label.
2. Probable synthetic/AI-generated content.
3. Low-quality, repetitive or mass-produced "AI slop".

An AI-generated video is not automatically slop. Use conservative thresholds, explainable reasons and a user override.

### P4 - history and feedback

- Local history: timestamp, creator, stable post ID/link when available, matched rule, reason and confidence.
- Feedback: allow this video, allow creator, block creator, refine topic.
- Clear history and reset controls.
- Do not download/archive full videos by default.
- Do not persist unstable fingerprints as if they were video IDs.

### P5 - model management and advanced controls

- Model availability/version/size/license/hash/integrity.
- Explicit import/download and delete controls.
- Lazy loading and resource release when idle.
- Sensitivity profiles, export/import settings and diagnostics.
- Local-first inference only unless the user explicitly approves a different privacy model.

### Settings-page order

1. StopSlop title, concise description, status and pause.
2. Advertising: Ads & sponsorships.
3. AI-generated content: official TikTok signal.
4. Feed quality: Minimum Likes and its threshold.
5. Topic filtering when implemented.
6. In-video promotions when implemented.
7. AI/slop classification when implemented.
8. Filtered history and feedback when implemented.
9. Model management and diagnostics when implemented.
10. Secondary inherited HushFeed settings, grouped by their existing function.
11. Actions, appearance, language, About and support.

Do not display fake functional switches for future features. A future feature can be documented in the roadmap; it should only appear in the UI when there is a real implementation or a clearly labeled, approved disabled preview.

### Inherited settings policy

Keep useful inherited HushFeed sections while StopSlop is early. The supplied audit lists Feed filter, Feed tabs, Feed screen, Playback, Screen time, Comments, Inbox, Downloads, Share sheet, Privacy, App and Region, plus pause/theme/language/status/diagnostics/backup/About/support. Preserve these unless a later feature inventory proves a specific item is unused, broken, redundant or explicitly unwanted. Move secondary settings below StopSlop; do not delete broad categories during light cleanup.

## 4. Runtime architecture

Use the architecture already present in the Morphe/HushFeed patch project. Do not create a second app architecture.

Conceptual flow:

```text
TikTok feed/post object
        |
        v
Verified Morphe patch hook
        |
        v
VideoSnapshot / normalized post metadata
        |
        v
StopSlop detector pipeline
  - official ad/sponsorship markers
  - likes threshold
  - official AI label
  - topic rules
  - promotion signals
  - optional local models later
        |
        v
Filtering Decision Engine
  KEEP / BLOCK / UNKNOWN
        |
        v
Existing feed/list integration
        |
        v
Patched TikTok UI
```

The exact class/module names must follow the current source. The diagram is conceptual, not an instruction to introduce all these classes immediately.

### 4.1 Decision contract

- `KEEP`: available evidence does not trigger an enabled rule.
- `BLOCK`: reliable evidence meets an enabled rule.
- `UNKNOWN`: missing metadata, unsupported signal, low confidence, timeout, model unavailable or exception.

`UNKNOWN` must fail open: retain the video. Missing values must never become an automatic block. Preserve stable TikTok IDs where available and include ruleset/model version in cache keys.

### 4.2 Critical feasibility gate: before-display filtering

The product goal is to filter an unwanted recommendation before it becomes visible. Do not equate that with an accessibility swipe after display.

For the actual TikTok target, trace:
1. Where a recommendation/post object arrives.
2. Which patch hook sees it.
3. Which fields are available at that exact point.
4. Whether an item can be removed before the adapter/player receives it.
5. Whether the first video frame can appear before the hook executes.
6. Actual timing constraints and preloaded data availability.

Captions/hashtags do not prove that audio or frame bytes are available. Accessibility metadata does not expose the media buffer. Do not add OCR/ASR/MediaProjection or assume a preload buffer without evidence.

### 4.3 Local-first privacy and resource limits

- No cloud inference or upload of watched videos, audio, frames, transcripts or account data.
- Do not inspect TikTok private cache/storage/process memory through unsupported methods.
- No root, Shizuku, VPN interception or hidden APIs.
- Use only data exposed by the verified in-process patch hook and required local APIs.
- Bound caches and diagnostics. Do not log raw captions/transcripts by default.
- Keep the feed responsive. On timeout/crash/model unavailability, return `UNKNOWN`.

## 5. AI/ML strategy - staged, no giant LLM per video

Runtime AI is code and model files in the patched TikTok app. Codex itself does not provide runtime AI.

Recommended progression:
1. Deterministic metadata/rule checks.
2. Compact text embedding/classifier for topic similarity.
3. Optional OCR/ASR only if actual pre-display media access is proven.
4. Separate AI/slop classifier only after a suitable dataset, evaluation and runtime budget exist.
5. Optional generative topic expansion only when the user edits a topic; cache the result and let the user review it.

Evaluate Google AI Edge LiteRT for compact inference only after the first metadata-only version is verified. Consider LiteRT-LM only for occasional generative actions, not every feed item. Do not add dependencies or download model weights before a proof-of-concept confirms:
- data availability at the verified hook;
- license and source;
- Android/runtime compatibility;
- model size and delivery mechanism outside the `.mpp`;
- A25 latency, RAM, heat and battery impact;
- fail-open fallback.

A12 is a later constraint. Provide metadata/rule-only fallback when a model is too slow or unavailable.

## 6. Roadmap: one accepted milestone at a time

### M0 - current-state reconciliation (read-only; next project task)

Goal: make the roadmap match the active local repository, not an old plan.

Inspect only:
- `git status --short`, current branch and repository root.
- root `AGENTS.md` and `PROJECT_CONTEXT.md` if present.
- current build task/output definition and target compatibility declaration.
- `Settings.java`, `StopSlopPlaceholders.java`, `TikTokPreferenceFragment.java`.
- `FeedItemsFilter` and exact detector implementations/registrations.
- StopSlop UI styling entry points and `ScreenColors` recycled-view cache fix.
- localization table inputs and generated runtime map references.

Deliver:
- exact active repository and source architecture;
- exact supported TikTok package/version/versionCode if locally declared;
- exact `.mpp` build task and output;
- status matrix for each current StopSlop feature;
- list of inherited features still registered;
- risks or contradictions between local code and this roadmap.

Read-only. No whole-repository dump, no recursive asset audit, no edits, no tests/build/Gradle/ADB. Do not repeat work already proved in supplied Codex reports unless a local reference has changed.

### M1 - light legacy cleanup (candidate-only first, implementation after review)

This is intentionally **not** a broad rewrite. The objective is to remove accidental old-project clutter while preserving useful inherited functionality and unfinished feature foundations.

First produce a short candidate list grouped as:
- definitely safe (temporary logs, editor/OS debris, accidental duplicate files not used by any workflow);
- likely obsolete but needs proof (old experiments, abandoned scripts, redundant resources);
- preserve (patches, feed hooks, preference infrastructure, build/release inputs, localization, tests/fixtures, inherited user features, legal notices, code potentially needed by planned StopSlop features).

Do not delete an item solely because it is not currently displayed or because the matching StopSlop feature is not implemented yet. Search references, reflection/naming conventions, Gradle resource generation, scripts, CI, release tasks and external workflow usage before recommending removal.

Only after the user accepts the candidate list may Codex delete the narrowly approved items. Do not modify README/gallery/marketing images during this milestone; those belong to the final documentation/branding stage.

### M2 - stabilize current P0 filters

One filter at a time:
1. Confirm the official ad/sponsorship data fields and hook.
2. Confirm Minimum Likes field, threshold comparison and feed hook.
3. Confirm the official TikTok AI marker field and exact scope.
4. Verify pause/master-enable and reload action.
5. Add concise reason/status diagnostics without raw-content logging.

For each filter report `SOURCE_IMPLEMENTED`, `HOOK_REGISTERED` or `DEVICE_VERIFIED`. Build `.mpp` only when needed for manual A25 verification. The user performs manual device verification; do not run tests by default.

### M3 - verify feed timing and decision boundary

Read-only hook trace first, then an implementation proposal. Establish whether filtering occurs before first display. Document fields, method signatures, timing and unsupported evidence. Do not promise pre-display exclusion without proof.

### M4 - metadata topic filtering

Implement user topic rules using only proven metadata. Support phrases, hashtags, exclusions and creator allowlists. Deterministic, explainable and fail-open. No ML dependency at this stage.

### M5 - metadata-based promotion detection

Combine URL, promo code, CTA, brand/product and caption signals. Avoid blocking ordinary reviews/discussions. Each signal returns reason/confidence. Entire-video skipping only in first version.

### M6 - multimodal feasibility and local inference

Research separately before implementation. Prove frame/audio availability, latency and runtime support. Evaluate LiteRT and candidate model licenses, model size and A25 performance. Do not add OCR, ASR or model binaries speculatively.

### M7 - official AI label and AI/slop analysis

First implement official TikTok label if exposed. Later evaluate probabilistic synthetic-content and low-quality/repetition detectors separately. Add user correction and confidence thresholds.

### M8 - local filtered history and feedback

Add bounded local history and user feedback. Never archive full videos by default. Ensure safe clear/export/import and allowlist precedence.

### M9 - model management and diagnostics

Only after a real model is chosen: version/hash/license/size checks, explicit import/download, delete, lazy loading, crash/timeout fallback and compact diagnostics.

### M10 - A25 performance, then A12 compatibility

Measure feed latency, classifier latency, memory, CPU/accelerator use, heat and battery on A25. Then establish an A12 profile and rule-only fallback.

### M11 - final product identity, README and gallery

Only after runtime and UI stabilize:
- complete remaining user-facing StopSlop branding;
- replace remaining obsolete visual assets using a Codex-generated asset inventory;
- regenerate real screenshots from the actual app/device;
- update README, hero, gallery, screenshots, release descriptions and user documentation together;
- preserve upstream/legal attribution and source notices.

Do not create fake screenshots or claim a future feature is implemented.

### M12 - release and version maintenance

Verify exact TikTok package/version/versionCode/signer/hash and HushFeed/Morphe compatibility. Use the exact clean TikTok artifact supported by the source. Do not force version fingerprints to match. Preserve a known-working `.mpp`, signing-key backup (outside Git) and rollback artifact. After a new TikTok release, re-audit hook fingerprints and manually validate on A25 before A12.

## 7. Lightweight cleanup rules

The project is early-stage: remove only clear old clutter, not useful infrastructure.

Safe candidates may include:
- temporary local logs/dumps accidentally tracked;
- editor/OS artifacts;
- exact duplicate files proven unused;
- abandoned one-off outputs that are not build inputs or release artifacts.

Do not remove:
- inherited HushFeed settings/features merely because StopSlop has not replaced them;
- patch declarations, fingerprints, resources, hook code or helper abstractions that support existing or planned features;
- localization source files or generated files still consumed by the build;
- scripts, CI, wrapper files, Gradle configuration or release inputs without dependency proof;
- tests/fixtures/audit data solely to reduce repository size;
- legal notices, license files, copyright or attribution;
- StopSlop keys, defaults, detector registrations or the Minimum Likes dependency fix.

Avoid speculative modernization, dependency upgrades, package renames, large refactors and format-only churn.

README, README hero/header, screenshot gallery and marketing assets are explicitly deferred until M11.

## 8. Project workflow and Codex token budget

### Permanent project instructions

Keep root `AGENTS.md` short and current. It should contain only:
- active architecture (Morphe/HushFeed patch source, `.mpp`, one patched TikTok app);
- preserve user changes and inherited functionality;
- inspect only relevant files;
- minimal diffs and no unrelated refactors;
- no tests/build/Gradle/ADB unless explicitly requested;
- local-first/privacy and `UNKNOWN` fail-open rule;
- short evidence-based final reports.

Do not retain obsolete AndroidProject Phase 0 rules such as "no APK patching" or "no AI ever" in the active repository instructions. The old AndroidProject may remain a reference, but it is not the runtime product.

Keep detailed technical decisions in this roadmap or small topic-specific docs. A small task prompt should reference only the relevant section rather than asking Codex to reread the whole roadmap.

### Prompt format

Every Codex prompt should state only:
1. task and target milestone;
2. relevant files/section;
3. scope and non-goals;
4. token-saving read boundaries;
5. whether edits are allowed;
6. execution restrictions;
7. concise acceptance/report format.

Default:
- Agent + Medium for small localized implementation.
- Ask/read-only + High for architecture, hook feasibility, compatibility changes, ML/runtime research and complex crashes.
- Do not use subagents unless explicitly approved.
- Do not run automated tests or builds unless requested. Build only when a `.mpp` is needed for manual validation.
- No full-repository scans, full-file dumps or long logs unless there is a demonstrated need.
- Batch related searches; open narrow line ranges; do not reread unchanged files.
- One milestone per prompt; stop after that milestone.

OpenAI's Codex guidance recommends concise, focused skills and project instructions, progressive disclosure and avoiding repeated context. Follow that guidance; do not make Codex re-read this entire roadmap for every small task.

## 9. Current next step

Do **not** run cleanup now. Do **not** generate a broad cleanup prompt yet.

First accept this roadmap as the new project plan. When the user is ready, the next task should be a very small, read-only M0 reconciliation using only the files listed in M0. After M0, prepare a short M1 candidate-only cleanup prompt. Codex should not delete files until the candidate list has been reviewed and approved.

The immediate goal is to spend the remaining Codex budget on the right work, not on a large speculative cleanup.
