# StopSlop

**A local-first feed-filtering project built into a modified TikTok client.**

> **Project status: early MVP foundation.** StopSlop is under active development. The current codebase contains an integrated settings shell and the first feed-filtering foundations, but several important behaviors still require source-level reconciliation and manual verification on a device. This repository is not a finished consumer release.

<!--
README presentation note:
Real application screenshots, the final gallery, release artwork, and polished feature demonstrations are intentionally deferred until roadmap milestone M11. Do not add mock screenshots or imply that planned features already work.
-->

## What is StopSlop?

StopSlop is a project to give users more control over the recommendations they see in TikTok. The intended experience is a **single modified TikTok application** with StopSlop settings and feed filtering integrated into the client.

StopSlop is not intended to be a standalone companion app, browser extension, DNS filter, VPN, or accessibility-based auto-swiper. The active implementation uses the Morphe patching workflow and a source tree derived from the HushFeed project. The source is built into a Morphe `.mpp` patch bundle, which is then applied to a compatible TikTok package using a compatible Morphe Manager workflow.

The product-facing identity is StopSlop. The technical history is not being erased: upstream licenses, copyright notices, required attribution, and identifiers needed for compatibility must remain intact.

### Project links

- **Repository:** [marcel322gg-png/StopSlop](https://github.com/marcel322gg-png/StopSlop)
- **Upstream source:** [SysAdminDoc/HushFeed](https://github.com/SysAdminDoc/hushfeed)
- **Issues and bug reports:** [GitHub Issues](https://github.com/marcel322gg-png/StopSlop/issues)
- **Roadmap:** this README's [Development roadmap](#development-roadmap) section; a more detailed technical roadmap should be kept in sync with it.

There are intentionally no donation links in this README. Project-specific support and release links can be added when those channels exist.

---

## Contents

- [Project status](#project-status)
- [Goals and non-goals](#goals-and-non-goals)
- [Current implementation](#current-implementation)
- [Feature status](#feature-status)
- [How the filtering pipeline should work](#how-the-filtering-pipeline-should-work)
- [Architecture](#architecture)
- [Privacy and safety principles](#privacy-and-safety-principles)
- [Development roadmap](#development-roadmap)
- [Build from source](#build-from-source)
- [Applying a patch bundle](#applying-a-patch-bundle)
- [Manual verification](#manual-verification)
- [Repository layout](#repository-layout)
- [Development workflow](#development-workflow)
- [Compatibility and release policy](#compatibility-and-release-policy)
- [Known limitations](#known-limitations)
- [Contributing](#contributing)
- [Security reports](#security-reports)
- [Licensing and attribution](#licensing-and-attribution)
- [FAQ](#faq)

---

## Project status

**Current stage: Stage 1 - stabilize the integrated StopSlop shell and verify the first metadata/marker filters.**

The project is at an early MVP foundation stage. A settings page and initial filter controls exist, and a Morphe patch bundle has been built successfully in the development environment. That does not mean every visible switch has been proven to filter the correct posts at runtime.

The first project task is to reconcile the local source with the roadmap, verify the exact TikTok target/build compatibility, and record which filters are implemented, registered with a feed hook, and manually verified on-device.

### Evidence labels

Feature descriptions and project reports should use these labels consistently:

| Label | Meaning |
|---|---|
| `NOT_IMPLEMENTED` | No working implementation exists yet. |
| `UI_ONLY` | A control or placeholder exists, but the underlying detector/action is not implemented. |
| `SOURCE_IMPLEMENTED` | Relevant source logic exists. Runtime behavior is not implied. |
| `HOOK_REGISTERED` | The detector/filter is connected to a relevant patch hook. |
| `DEVICE_VERIFIED` | The behavior was manually confirmed on a named target device and compatible TikTok build. |
| `BLOCKED_BY_FEASIBILITY` | The required data, hook, timing, or runtime capability is unavailable or not proven. |

A preference key, settings row, patch declaration, successful compilation, or non-empty `.mpp` payload is not by itself proof that a feature works in the feed.

## Goals and non-goals

### Goals

1. Filter supported unwanted recommendations as early as the verified TikTok feed integration allows.
2. Start with deterministic, metadata-backed rules before considering machine-learning features.
3. Make every blocking decision explainable by a rule or detector.
4. Keep filtering local to the device by default.
5. Fail open when evidence is missing, unsupported, late, or unreliable.
6. Keep the feed responsive on the primary Samsung Galaxy A25 5G target, then evaluate the lower-end Samsung Galaxy A12.
7. Preserve useful inherited HushFeed functionality while making StopSlop the primary product identity.

### Non-goals

- A separate companion Android app that attempts to control TikTok from outside the client.
- AccessibilityService gestures as the core filtering mechanism.
- Root, Shizuku, VPN traffic interception, or unsupported access to TikTok private storage/process memory.
- Uploading watched videos, audio, frames, captions, or transcripts to a cloud service for classification.
- Running a large generative model on every feed item.
- Claiming generic AI detection merely because a TikTok official AI label can be read.
- Removing inherited functionality solely because it is not a StopSlop-specific feature.

---

## Current implementation

This table is a conservative snapshot based on the latest development reports. It must be reconciled against the active source before being treated as a release checklist.

| Area | Current reported state | What remains to prove |
|---|---|---|
| Morphe patch project | Existing HushFeed-based source tree is the active project. A `.mpp` build completed successfully in the development environment. | Reconcile the exact current build task, output path, versions, and source state. |
| StopSlop settings page | The settings entry/page opens on the primary Galaxy A25 after a dependency-order correction. | Recheck current branch and settings behavior after subsequent source changes. |
| Ads & sponsorships | `stop_slop_ad_sponsorship`, default `true`; ad/paid-promotion marker filtering is reported in the source and feed-filter registration. | Verify the exact metadata source, supported labels, hook timing, and correct hide/keep behavior on-device. |
| Official AI-generated marker | `stop_slop_ai_generated`, default `false`; marker-filter integration is reported. Existing inherited AI preference compatibility is retained. | Confirm which exact TikTok-provided marker is read and whether it is available before display. This is not generic AI-image/video detection. |
| Minimum Likes | `stop_slop_min_likes`, default `false`; `stop_slop_min_likes_threshold`, default `0`, range `0..1,000,000`. | Verify the likes field, comparison, boundary cases, feed hook, and actual filtering on-device. |
| Minimum Likes dependency | Threshold preference is added before dependency assignment and depends on the StopSlop enable key. | Preserve this ordering and dependency when changing the settings UI. |
| Reload TikTok | Convenience action is reported on the settings page. | Verify action behavior on the target device. It is not a feed filter. |
| Pause/master enable | An overall pause concept exists in the inherited settings foundation. | Verify that it pauses all StopSlop filters without resetting individual preferences. |
| English/German localization | English source entries and German translations/runtime map have been updated. | Verify completeness and presentation in both locales. |
| Branded settings UI | StopSlop header, section styling, and placeholder controls have been added. | Verify layout, recycled rows, accessibility, and visual consistency on-device. |
| Topic filtering | Roadmap item only. | Not implemented as a complete feature. |
| In-video promotion detection | Roadmap item only. | Not implemented as a complete feature. |
| Probabilistic AI/slop detection | Roadmap/research item only. | Not implemented as a complete feature. |
| Filter history and feedback | Roadmap item only. | Not implemented. |
| Model management | Roadmap item only. | Not implemented. |

### Current filter preferences

| Preference | Type | Default | Intended meaning |
|---|---|---:|---|
| `stop_slop_ad_sponsorship` | Boolean | `true` | Enable filtering for supported official ad/sponsorship markers. |
| `stop_slop_ai_generated` | Boolean | `false` | Enable filtering for a supported official TikTok AI-generated marker. |
| `stop_slop_min_likes` | Boolean | `false` | Enable the minimum-like-count rule. |
| `stop_slop_min_likes_threshold` | Integer | `0` | Minimum likes threshold, currently constrained to `0..1,000,000`. |

These names, types, defaults, and compatibility relationships are part of the current project state. Do not rename or migrate stored keys as part of unrelated cleanup.

---

## How the filtering pipeline should work

The target behavior is to make a decision using the most reliable information available at the verified feed hook. The conceptual pipeline is:

```text
TikTok recommendation/post object
              |
              v
      Verified Morphe hook
              |
              v
   Normalized post metadata
              |
              v
       StopSlop detectors
       - ad/sponsorship marker
       - minimum likes
       - official AI label
       - topic rules (later)
       - promotion signals (later)
       - optional local models (later)
              |
              v
    Filtering decision engine
       KEEP / BLOCK / UNKNOWN
              |
              v
   Existing feed/list integration
              |
              v
       Modified TikTok UI
```

This is a target architecture, not a claim that every stage is implemented or that the hook has already been proven to run before first display. Exact classes, method names, fields, and insertion points must be established from the active source.

### Decision contract

- **`KEEP`** - available evidence does not match an enabled blocking rule.
- **`BLOCK`** - reliable evidence satisfies an enabled rule.
- **`UNKNOWN`** - metadata is missing, unsupported, too old, too late, low-confidence, timed out, or a detector/model failed.

`UNKNOWN` must fail open: keep the post. A missing field must never be converted to a blocking value. A detector exception or unavailable model must not break feed rendering.

### Before-display requirement

The product goal is to filter a recommendation before it becomes visible whenever the verified TikTok/Morp hook makes that possible. This must be demonstrated, not assumed.

For each candidate hook, development must establish:

1. Which method receives the recommendation/post object.
2. Which fields are available at that exact point.
3. Whether an item can be removed before it reaches the adapter/player.
4. Whether a first video frame can appear before the filter runs.
5. What preload and timing constraints exist.
6. What happens when the hook is missing or the object shape changes after a TikTok update.

An accessibility node, caption text in a later UI component, or a visible ad badge does not prove that audio or frame data is available before display. Do not add OCR, ASR, frame capture, or model inference until the relevant media access and timing budget are demonstrated.

---

## Architecture

StopSlop remains integrated into the existing Morphe/HushFeed-based patch source. The repository should not be migrated into a new standalone app or add-on project as part of routine cleanup.

### Main layers

1. **Morphe patch layer** - declares and applies bytecode/resource changes to the compatible TikTok target.
2. **TikTok extension layer** - provides extension-side settings, preferences, filters, and runtime integration.
3. **StopSlop filter layer** - owns StopSlop preference semantics and filtering decisions while reusing proven inherited integration points where appropriate.
4. **Settings/UI layer** - exposes StopSlop controls first and inherited settings below them.
5. **Localization/resources layer** - keeps user-facing strings, generated localization resources, and visual assets consistent.

### Design rules

- Keep the current working `.mpp` output pipeline.
- Follow the existing package/module conventions unless a documented architecture decision changes them.
- Prefer small, localized changes over broad rewrites.
- Keep StopSlop-specific settings distinct from inherited settings when their semantics differ.
- Reuse existing hooks only after verifying their data and timing behavior.
- Keep compatibility keys when renaming a visible label would otherwise break stored preferences or patch integrations.
- Do not delete inherited features just because StopSlop does not replace them yet.

### AI and machine-learning strategy

AI/ML work is explicitly staged. The intended order is:

1. Deterministic metadata and preference-based rules.
2. A compact text classifier or embedding model for topic similarity, if metadata access and performance allow it.
3. Optional OCR/ASR only after media availability before display is proven.
4. Separate evaluation of probable synthetic-content detection and low-quality/repetitive-content detection.
5. Optional generative topic expansion only when a user edits a topic, not on every feed item.

Potential Android inference technologies such as LiteRT must be evaluated for license, binary size, Android compatibility, A25 latency, RAM, heat, battery, model delivery, and A12 fallback before dependencies or model binaries are added. LiteRT-LM or another generative runtime is not a default per-video detector.

---

## Privacy and safety principles

StopSlop is designed to be local-first.

- Do not upload watched videos, audio, frames, transcripts, captions, account data, or filter history to a remote classifier.
- Do not access TikTok private storage, private caches, or process memory through unsupported mechanisms.
- Do not use root, Shizuku, VPN interception, or hidden APIs as a shortcut to feed access.
- Use only the data and extension APIs available through the verified in-process patch integration and required local Android APIs.
- Keep diagnostic records bounded and avoid raw caption/transcript logging by default.
- Store only stable identifiers/fingerprints that have a demonstrated purpose.
- Provide clear reset/clear behavior for any future local history.
- On missing metadata, timeout, exception, model absence, or unsupported build, use `UNKNOWN` and keep the post.

These are project design requirements. They do not imply that every current component has already been audited or that the project has received an independent security review.

---

## Development roadmap

The roadmap is milestone-based. Complete and review one milestone at a time. Do not implement later features by adding placeholder switches that imply functionality.

### M0 - current-state reconciliation

**Goal:** align the written plan with the active source tree and identify the exact current baseline.

Inspect only the relevant repository instructions, current Git status/branch, build output configuration, compatibility declaration, StopSlop preference/settings files, and feed-filter implementations/registrations.

**Deliverables:**
- active repository and architecture;
- exact TikTok package, version, and versionCode where declared;
- exact `.mpp` build task and output path;
- implementation/hook/device-verification status for each current filter;
- list of inherited features still registered;
- contradictions between source and roadmap.

**Restrictions:** read-only. No broad repository dump, tests, builds, Gradle, ADB, or emulator work.

### M1 - lightweight legacy cleanup

**Goal:** remove only accidental clutter while preserving the working Morphe/TikTok patch project.

First produce a short candidate list grouped into:
- definitely safe to remove;
- likely obsolete but needing proof;
- preserve.

Search references, reflection/naming conventions, Gradle generation, scripts, CI, release tasks, and external workflows before recommending removal. Do not delete anything until the candidate list is reviewed and approved.

README/gallery screenshots and marketing artwork are handled in the final M11 presentation pass; this README is an early project overview and can evolve as the roadmap evolves.

### M2 - stabilize current P0 filters

Work one filter at a time:

1. Verify the official ad/sponsorship data fields and hook.
2. Verify the likes field, threshold comparison, and feed hook.
3. Verify the official TikTok AI marker and its exact scope.
4. Verify pause/master-enable and the reload action.
5. Add concise decision reasons/status diagnostics without raw-content logging.

**Acceptance criteria:** each enabled filter has a verified data source, registered hook, reason for its decision, fail-open path, and manual A25 result.

### M3 - verify feed timing and decision boundary

Trace the exact feed hook and establish whether a post can be filtered before the first visible frame. Document method signatures, fields, ordering, and timing. Do not claim pre-display filtering without evidence.

### M4 - metadata topic filtering

Add user-defined topic rules using only proven metadata. Planned rule types include phrases, hashtags, exclusions, and creator allowlists/blocklists. Keep the first implementation deterministic and explainable. Add ML only in a separately approved step.

### M5 - metadata-based in-video promotion detection

Evaluate URLs, promo codes, calls to action, affiliate patterns, brands, products, and caption signals. Avoid treating ordinary reviews or discussions as advertisements. The initial safe behavior may hide an entire video. Do not claim segment-only skipping until timestamp detection and seeking are demonstrated.

### M6 - multimodal feasibility and local inference

Research frame/audio availability, pre-display timing, Android runtime compatibility, model licenses, model size, delivery, and A25 resource cost. Do not add OCR, ASR, or model binaries speculatively.

### M7 - official AI label and AI/slop analysis

First stabilize the official TikTok-provided AI-generated label if exposed by the verified hook. Evaluate probable synthetic-content detection and low-quality/repetitive AI-slop detection as separate concepts. An AI-generated video is not automatically slop. Use conservative confidence thresholds, clear reasons, and user overrides.

### M8 - local filtered history and feedback

Add bounded local history with a timestamp, stable post ID/link when available, creator, matched rule, reason, and confidence. Add feedback such as allow this post, allow creator, block creator, or refine topic. Do not archive complete videos by default.

### M9 - model management and diagnostics

Only after a real model is selected, add version, hash, license, size/integrity checks, explicit install/import/delete controls, lazy loading, resource release, failure fallback, and compact diagnostics.

### M10 - A25 performance, then A12 compatibility

Measure feed latency, detector latency, memory, CPU/accelerator use, heat, and battery on the Galaxy A25 5G. Then evaluate a Galaxy A12 profile. Provide a deterministic/rule-only fallback when a model is too slow or unavailable.

### M11 - final product identity, screenshots, README gallery, and release presentation

This is the **final presentation pass**, after runtime behavior and the settings UI stabilize.

- Finish remaining user-facing StopSlop branding.
- Audit and replace remaining obsolete visual assets where appropriate.
- Capture real screenshots from the actual app/device and supported build.
- Replace this early README presentation with final screenshots, gallery, polished feature descriptions, and current compatibility information.
- Update release notes and user documentation together.
- Preserve upstream/legal attribution and source notices.

Do not add fake screenshots, fabricated metrics, or claims about unimplemented features.

### M12 - release and version maintenance

Verify exact TikTok package/version/versionCode, signer/hash, and Morphe/HushFeed compatibility. Use only a target supported by the source. Do not force fingerprints to match. Keep a known-working `.mpp`, a secure signing-key backup outside Git, and a rollback artifact. After TikTok updates, re-audit hook fingerprints and manually validate on A25 before A12.

---

## Build from source

> The commands below describe the reported current workflow. The exact target versions and compatibility matrix remain subject to M0 reconciliation.

### Requirements

- Windows with PowerShell, or another environment supported by the repository's Gradle wrapper.
- A compatible JDK as required by the checked-in Gradle wrapper and Android Gradle Plugin configuration.
- Android build tooling and any dependencies declared by the repository.
- Access to required upstream package registries when dependencies are not already cached.
- A compatible Morphe Manager workflow and a legally obtained TikTok package/build supported by the patch source.

Do not assume that the JDK, Gradle, Android Gradle Plugin, or TikTok target mentioned in an old log is still the authoritative version. Inspect the wrapper and project build files first.

### Clone the repository

```powershell
git clone https://github.com/marcel322gg-png/StopSlop.git
cd StopSlop
```

### Check the working tree

```powershell
git status --short
git branch --show-current
```

Do not overwrite or discard local changes when switching branches or updating dependencies.

### Build the Morphe patch bundle

The last reported successful task was:

```powershell
.\gradlew.bat :patches:buildAndroid
```

The previously reported output was:

```text
patches\build\release\patches-0.84.0.mpp
```

That filename is a historical example from the current development snapshot, not a promise that every future build will use the same version. Confirm the actual output path and build metadata after building.

A `.mpp` file is a Morphe patch bundle. **It is not the final TikTok APK and is not installed as a standalone Android app.**

### Build troubleshooting

- Start with the exact Gradle error, not a speculative dependency upgrade.
- Check the repository wrapper files and configured JDK before changing toolchain versions.
- Do not commit local credentials, package registry tokens, keystores, signing keys, or generated APKs.
- Use a user-local Gradle properties file or an approved secret store for credentials.
- Do not paste tokens or signing secrets into issue reports, terminal transcripts, or Codex prompts.
- Preserve the dependency verification metadata and existing version constraints unless there is a demonstrated compatibility issue.

---

## Applying a patch bundle

The `.mpp` bundle must be applied using a compatible Morphe Manager workflow to a TikTok package/build supported by the current patches.

1. Read the current compatibility declaration and release notes.
2. Obtain the correct TikTok package/build through a source you are entitled to use.
3. Use a Morphe Manager version compatible with the patch bundle and target build.
4. Apply the `.mpp` following the Manager's documented workflow.
5. Install the resulting patched client using the supported workflow for the device.
6. Keep a known-working build and a rollback path before changing TikTok versions.
7. Verify the StopSlop settings page and each filter independently on the target device.

Exact package names, TikTok versions, Manager versions, fingerprints, and install steps are intentionally not hard-coded here until M0 establishes the current compatibility contract. A successful patch build alone does not establish installation or runtime compatibility.

---

## Manual verification

Manual verification is the primary acceptance method for the current project stage. Do not report a filter as `DEVICE_VERIFIED` until it has been checked on a named device with the exact tested TikTok build.

### Primary target

| Device | Role | Status |
|---|---|---|
| Samsung Galaxy A25 5G | Primary development and behavior-verification target | Settings page was reported to open. Filter behavior still needs per-filter confirmation. |
| Samsung Galaxy A12 | Lower-end compatibility target | Deferred until A25 behavior and performance are stable. |

### Suggested verification record

For every test, record:

- date and device model;
- Android version;
- TikTok package name and exact version/versionCode;
- Morphe Manager version;
- StopSlop commit and `.mpp` build identity;
- preference state and configured threshold;
- test case and expected behavior;
- observed behavior;
- whether the item was hidden before first display;
- logs or screenshots only when they are necessary and contain no secrets or sensitive account data.

### Initial manual test cases

| Area | Test | Expected result |
|---|---|---|
| Settings | Open StopSlop settings from the patched TikTok client. | Page opens without crash or blank state. |
| Ads | Compare a clearly labeled supported ad with an ordinary post. | Only supported ad markers trigger the enabled rule. |
| Sponsorship | Compare a supported paid-partnership marker with an ordinary product mention. | The configured marker is handled; ordinary mentions are not assumed to be sponsored. |
| AI label | Compare a TikTok-labeled AI post with an unlabeled post. | Only the supported official marker is used by this filter. |
| Minimum Likes | Test values below, equal to, and above the threshold. | Boundary comparison matches the documented rule. |
| Fail-open | Test missing metadata or an unsupported post shape. | The post is kept; the feed remains usable. |
| Pause | Pause StopSlop while individual filter preferences are enabled. | Filters stop acting without losing their individual preferences. |
| Reload | Trigger the convenience action. | TikTok restarts/reloads as documented; no filter semantics are implied. |

These are test proposals, not reported results.

---

## Repository layout

The repository evolves from the existing Morphe/HushFeed-based source. The following is a high-level guide; exact paths should be confirmed against the current branch.

| Path | Purpose |
|---|---|
| `patches/` | Morphe patch source, bytecode/resource integrations, and patch build configuration. |
| `patches/src/main/resources/` | Patch resources and settings-default metadata. |
| `patches-list.json` | Patch declarations and metadata consumed by the patch workflow. |
| `extensions/tiktok/` | TikTok extension-side runtime code, settings, preferences, and localization sources. |
| `extensions/tiktok/src/main/l10n/` | Localization source tables. |
| `assets/` | Project visual assets and documentation-related artwork. |
| `gradle/` and Gradle wrapper files | Build tooling and dependency verification/configuration. |
| `PROJECT_CONTEXT.md` | Short, active repository context for development tooling. |
| `AGENTS.md` | Project-specific instructions for coding agents, if present. |

Generated build outputs, local SDK settings, tokens, and signing material must not be committed unless a specific release process explicitly requires a public artifact.

---

## Development workflow

### One milestone at a time

- Keep each Codex task scoped to one milestone or one narrowly defined source change.
- Inspect only the files needed for the task.
- Read root `AGENTS.md` and `PROJECT_CONTEXT.md` when changing implementation.
- Preserve all existing user changes and useful inherited features.
- Report exact files changed and the evidence for behavior claims.
- Do not run automated tests, Gradle, builds, ADB, or emulators unless explicitly requested. Build only when a `.mpp` artifact is needed for manual device verification or compilation was specifically requested.
- Do not run broad repository audits repeatedly or dump whole source files into the prompt.
- Do not use subagents unless explicitly approved.

### Git safety

Before work:

```powershell
git status --short
git branch --show-current
git log -1 --oneline
```

Before committing:

```powershell
git diff --check
git diff --cached --check
git status --short
```

Use a focused branch for each milestone. Keep `main` as a known-good history. Never use destructive reset/clean commands on an unreviewed working tree. Do not force-push rewritten history unless the maintainer explicitly asks for it and the consequences are understood.

### Suggested commit style

Use concise conventional commit subjects, for example:

- `docs: establish StopSlop project overview`
- `fix: preserve fail-open behavior for missing metadata`
- `feat: add metadata-based topic rules`
- `refactor: isolate StopSlop filter preferences`

A commit message must not claim manual verification that has not occurred.

---

## Compatibility and release policy

Compatibility is tied to the exact TikTok target and patch fingerprints, not merely to an Android version or a broad app family name.

Before declaring a release compatible, record:

- TikTok package name;
- version name and versionCode;
- APK/BUNDLE source identity, signer, and hash where appropriate;
- Morphe Manager version;
- StopSlop source commit;
- `.mpp` bundle identity/hash;
- device and Android version used for manual verification;
- known limitations and rollback path.

Do not claim support for an untested TikTok build. Do not silently update fingerprints to force a patch to apply. If a hook cannot be resolved, the affected functionality must be reported as unavailable rather than pretending it is active.

---

## Known limitations

- The current stage is early MVP foundation, not a feature-complete feed filtering product.
- The exact pre-display hook and its available metadata still require evidence-based reconciliation.
- Current filter controls need per-filter runtime verification.
- TikTok's official AI label is distinct from general synthetic-media detection.
- Topic similarity, in-video ad segmentation, AI-slop classification, local history, feedback, and model management are future milestones.
- The performance budget and model feasibility on Galaxy A25/A12 are not yet established.
- The final screenshot gallery, actual device captures, polished release visuals, and final README presentation are postponed to M11.
- TikTok updates can change class names, method fingerprints, feed models, or hook timing and may require compatibility work.

---

## Contributing

The project is still establishing its architecture and verification baseline. Before proposing a substantial change:

1. Read this README and the relevant project instructions.
2. Check the current roadmap milestone and open issues.
3. Keep the change narrow and explain which behavior it affects.
4. Preserve GPL/copyright/upstream notices and existing preference compatibility.
5. Include source evidence and manual test details where applicable.
6. Do not submit secrets, signing files, private TikTok packages, personal account data, or raw private logs.

For now, bug reports and feature proposals should use [GitHub Issues](https://github.com/marcel322gg-png/StopSlop/issues). A dedicated `CONTRIBUTING.md`, issue templates, and broader community policies can be added during repository setup if and when they are needed.

### Useful bug report information

- StopSlop commit or release;
- TikTok package/version/versionCode;
- Morphe Manager version;
- device and Android version;
- exact steps to reproduce;
- expected vs. actual result;
- whether the issue occurs with the relevant filter enabled or paused;
- sanitized logs only when necessary.

Never attach tokens, cookies, signing keys, private APKs, or unredacted account identifiers.

---

## Security reports

Do not publish exploitable vulnerabilities, leaked credentials, tokens, or private user data in a public issue. Use GitHub's private vulnerability reporting if it is enabled for this repository. If private reporting is not enabled, contact the maintainer through a private channel before disclosing details publicly.

A `SECURITY.md` policy should be added as part of the repository-health setup once a contact/reporting path is established.

---

## Licensing and attribution

StopSlop is built from a source tree derived from HushFeed and uses the Morphe patching ecosystem. The project must retain the applicable upstream licenses, copyright notices, third-party notices, and required attribution.

The current patch metadata identifies the patch set as **GNU General Public License v3.0, with additional GPL section 7 requirements**. Read the repository's `LICENSE` and all applicable source/asset notices before copying, modifying, redistributing, or relicensing any component. Do not assume that every image, font, icon, or external asset has the same license as the patch source.

- Upstream project: [HushFeed](https://github.com/SysAdminDoc/hushfeed)
- Product/project fork: [StopSlop](https://github.com/marcel322gg-png/StopSlop)

StopSlop branding does not remove upstream authorship or legal obligations. Keep technical identifiers and compatibility names where changing them would break settings, patch behavior, or attribution.

TikTok and related marks belong to their respective owners. StopSlop is an independent community project and is not affiliated with, endorsed by, or sponsored by TikTok or ByteDance.

---

## FAQ

### Is StopSlop a separate Android application?

No. The intended product is a modified TikTok client with StopSlop integrated into it.

### What is an `.mpp` file?

It is a Morphe patch bundle. It is an input to the patching workflow, not a standalone APK and not the final installed TikTok application.

### Does every visible switch already work?

No. A setting can exist before its detector is fully implemented or verified. See the [feature status](#feature-status) table and evidence labels.

### Does the AI-generated switch detect every AI video?

No. The current goal is to use a reliable TikTok-provided AI marker if the verified feed hook exposes one. Generic AI-generated content detection is a separate future research/implementation task.

### Does StopSlop already detect VPN advertisements inside a video?

No. In-video promotion detection is a roadmap item. Caption/URL/promo-code patterns and multimodal timestamp-level detection require separate feasibility and implementation work.

### Will StopSlop upload videos to an AI service?

The design requirement is local-first processing. Uploading watched media or user data to a cloud classifier is outside the current project scope.

### Why are there no screenshots here?

Real screenshots and the final visual gallery are intentionally postponed until M11, when the UI and runtime behavior are stable enough to document accurately.

### Which TikTok versions are supported?

Only versions explicitly confirmed by the current source compatibility declarations and manual release verification should be considered supported. The compatibility matrix is being reconciled in M0.

### Where can I report an issue?

Use [GitHub Issues](https://github.com/marcel322gg-png/StopSlop/issues). Include build/device details and sanitized reproduction steps; do not post secrets or private account data.

---

## Project maturity note

This README is an intentionally detailed **development-stage project overview**, not a promise that all described roadmap capabilities exist today. Planned capabilities are labeled as planned, and current functionality is described conservatively. The final screenshots, visual gallery, polished feature walkthroughs, release-specific compatibility table, and end-user installation guide will be completed in M11 after the implementation has been verified.
