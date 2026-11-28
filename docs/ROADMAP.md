# StopSlop - Master Technical Roadmap

**Revision:** 8.4
**Date:** 28 November 2026<br>
**Product:** StopSlop - local-first moderation integrated into a modified TikTok client<br>
**Primary validation device:** Samsung Galaxy A25 5G<br>
**Secondary compatibility target:** Samsung Galaxy A12, after A25 is stable

> This roadmap supersedes revision 8.2. It records the optional-model manager, GitHub Release publication pipeline, Pages catalog client/site and the model-free verified `.mpp`. The catalog and Release have not yet been published, and no model is verified runnable inside the Morphe host or on a device. Build/device statements remain separate from source review.

---

## 1. Product definition

StopSlop is one modified TikTok application with feed filtering and StopSlop settings integrated into the TikTok client. It is not a standalone companion application, browser extension, VPN, DNS filter, or AccessibilityService auto-swiper.

The active source is the existing Morphe/HushFeed-derived patch repository. Source changes are compiled into a Morphe `.mpp` bundle and applied by a compatible Morphe Manager workflow to a supported TikTok package. The `.mpp` is a patch bundle, not the final installed TikTok APK.

Keep StopSlop as the user-facing product identity. Preserve upstream licenses, copyright notices, required attribution, source references, and compatibility-sensitive identifiers.

### Product promise

The user controls both what should be hidden and what should be prioritized. StopSlop combines three complementary components:

1. **Mechanical moderation** - deterministic rules, platform markers, thresholds, exact phrases, normalized text, and known patterns. This remains available regardless of the AI setting.
2. **AI-assisted moderation** - optional on-device models that expand topics and understand supported content semantically. AI is an additional engine, not a replacement for deterministic rules.
3. **Preferred Content** - the mechanical first slice is now source-implemented and tested: positive semicolon-separated topics such as `Dota 2; science; wildlife;` score caption matches and stably prioritize eligible ordinary videos within the current fetched batch. Non-matching videos remain visible. Device behavior still requires Galaxy A25 verification.

The user can switch AI-assisted moderation off globally. In that state, negative moderation and positive preferences continue mechanically. When AI is on, only explicitly enabled and implemented AI modules may run. A missing, invalid, incompatible or deleted model must never make a filter appear AI-enabled when it is not.

**Optional AI distribution principle:** the base `.mpp`/TikTok install must not contain every model. Model weights are selected from a curated Stable catalogue and downloaded only after explicit user choice. They are stored as app-private model data inside the host TikTok application's private data area and opened by a verified runtime. They are not merged into or repacked into the APK/MPP after download. This makes the model feel integrated into the patched app while keeping the base patch small. The runtime itself must be bundled once in a supported way or provided by a verified platform runtime; StopSlop must never download executable DEX or `.so` code as a model.

**Important scope boundary:** the patched client can only prioritize videos that TikTok has already returned to the current feed batch. Preferred Content may safely move matching eligible videos earlier within that batch if the source's list semantics permit it. It cannot directly change TikTok's server-side recommendation model or guarantee that future requests contain more of a topic. Do not claim backend recommendation training or guaranteed long-term frequency changes.

---

## 2. Current reported baseline

This is a snapshot from the user's Codex reports on 26 November 2026. It is not an independent inspection of the active Git working tree. Status labels distinguish source changes and test/build evidence from device verification.

| Area | Latest reported state | Still required |
|---|---|---|
| Morphe patch source | Existing HushFeed-derived repository remains active | Reconcile current branch and working tree before the next source change |
| `.mpp` output | Rebuilt `patches/build/release/patches-0.84.0.mpp`; `verifyBundle` confirmed 156 patches and three non-empty DEX payloads. Size 26,136,171 bytes (24.925 MiB); SHA-256 `af49c659196faa76e77da57d095df5c52f60336a7e9f44c032dd47ae4e8e38f2`; unsigned. The archive is model-free; MPP target gates are missed due mainly to native runtime size. The dirty source tree caused the release-receipt gate to refuse a receipt. | Produce/inspect a patched APK only with a supported base APK/workflow; clean-source signed release and manual A25 verification remain separate |
| Ads & Sponsorships | One visible StopSlop master control; migration preserves an explicit canonical value and derives the master for legacy installs; individual legacy signal preferences are preserved; feed, insertion and card paths are gated | Manual A25 verification of fresh-install and upgrade combinations |
| Ads automated checks | `AdsFilterTest` and `DeepFeedFilterBoundaryTest` reported passing; MPP build succeeded | Device verification remains separate |
| Topic Filter - mechanical | Single-line semicolon editor; phrases with spaces; exclusions, hashtags, creator allowlist; Enter/IME action no longer saves/closes; parser still accepts legacy newline-separated values | Manual check of save/reopen and actual matching on A25 |
| Preferred Content | `SOURCE_REVIEWED`: separate enable switch and semicolon topic list; unique literal caption-match scoring; stable in-batch ordering of eligible ordinary videos in fixed structural slots; non-matches remain visible | Prior test/bundle claims are inherited reports, not repeated in this run; Galaxy A25 ordering, pagination and persistence checks remain outstanding |
| Topic Filter tests | Focused parser tests reported passing | Device verification and false-positive evaluation |
| Settings search | Search indexes registered StopSlop controls; old ad names such as `Remove feed ads`, `Hide paid partnerships`, and `Hide promotional music` resolve to the canonical StopSlop control | Manual search checks in supported languages |
| Empty ad category | Orphaned inherited `ADS` header removed | Verify on device |
| Global settings appearance | Shared StopSlop styling exists in the touched settings components; complete visual consistency across every inherited screen has not been demonstrated | Inspect real screens; do not claim repository-wide visual unification without evidence |
| Official TikTok AI-generated marker | `SOURCE_REVIEWED`: deterministic marker filter controlled by `stop_slop_ai_generated`; separate from ML | Verify exact marker coverage and device behavior; this is not AI inference |
| Minimum Likes | `SOURCE_REVIEWED`: threshold 0–1,000,000; unknown/negative statistics fail open; filter cache token includes current threshold and enabled state | Confirm threshold boundaries and actual feed behavior on A25 |
| AI runtime/model | `SOURCE_IMPLEMENTED; EXTENSION_COMPILED; MPP_BUNDLE_VERIFIED; APK/DEVICE_UNVERIFIED`: ONNX Runtime Android 1.20.0 / Extensions 0.13.0 declare API 21. ONNX session/tokenizer/inference code is reachable only from the private `:stopslop_ai` Service; the main process checks the installed `ServiceInfo.processName` and `exported` before binding and fails closed if the declaration is absent or wrong. The Morphe manifest patch is attached to Settings and present in the built MPP; asynchronous Messenger IPC carries bounded text/topics and selected model key/revision. AI stays optional and explicit; selection does not activate it, and feed callbacks never bind. The MPP contains native runtime libraries but no model/tokenizer data. This does not prove the final patched APK manifest or device execution. | On Galaxy A25 verify PackageManager process/exported values and distinct PIDs, then E5 readiness/inference. Measure memory, latency and language quality; confirm no process-boundary or runtime claim without those checks |
| AI Model Manager and catalog | `SOURCE_IMPLEMENTED; BUNDLE_VERIFIED; PAGES_NOT_LIVE; DEVICE_UNVERIFIED`: app fetches the fixed HTTPS catalog, validates supported E5 profile and canonical GitHub Release URLs, falls back to a bounded cached catalog, and verifies release package size/SHA-256 before import into private storage. GitHub workflows build/publish a versioned package and deploy Pages metadata using the least-privilege `GITHUB_TOKEN`; no signed-catalog key is used. The actual package Release and Pages deployment have not run. | Enable Pages if needed and run the deliberate model release workflow; verify public URLs; manually check network permission and catalog/download behavior on phone |
| Model candidates | One data-only E5 package was generated and audited locally: Xenova multilingual E5 Small INT8 at immutable revision `761b726dd34fb83930e26aab4e9ac3899aa1fa78`, 86,306,396 bytes, SHA-256 `7a631638566759f87960f1e90eb913bcb50052148ae7a06730886069692235ee`; MIT license. It remains ignored from Git and has no public Release URL yet. The workflow rebuild is hash-gated but has not run on GitHub. | Verify CI tokenizer regeneration and package publication; then verify installation/session behavior and per-language quality, runtime, ABI, RAM and A25 latency |
| Optional generator model | No compact multilingual generator has been proven in the target runtime | Keep separate from embedding models; do not claim embeddings generate aliases |
| TikTok Native Recommendation Bridge | Localized informational rows for native Manage topics and Filter keywords are source-implemented; tapping shows guidance rather than deep-linking. No stable in-app route was verified for TikTok 47.9.3 (versionCode 2024709030). Existing user-triggered Not interested control is unchanged; no automatic feedback is sent. | Manually confirm dialogs and guided navigation on Galaxy A25. Do not claim direct navigation or backend API access. M5 has the documented safe fallback; device check remains pending. |
| Semantic topic expansion | `NOT_IMPLEMENTED` | Requires a real local generation/embedding provider and reviewed profile persistence |
| Semantic content understanding | Caption-based semantic Topic Filter is source-implemented with asynchronous inference and cached `KEEP/BLOCK`; first evaluation fails open and same-item re-evaluation is not established. This is embedding similarity, not generation or broad video understanding. | Verify host execution/feed re-evaluation, assess false positives and calibrate UA/RU/EN; other content modalities remain unimplemented |
| OCR / ASR / subtitle analysis | No complete, verified pre-display pipeline is established | Separate feasibility gate; do not assume availability |
| AI research catalogue | `SOURCE_REVIEWED`: existing 65 candidate IDs and 52 explicit language tags remain research-only; the optional manager currently supports only the pinned E5 profile, and no model is verified runnable in the Morphe host | Add signed/hosted Stable-catalogue entries only after runtime and offline inference are proven |
| M4 Blocked Videos History | `SOURCE_REVIEWED`: bounded local JSON history, separate exact-ID restore list, final-exclusion capture, restore check before filtering, review/delete/restore/clear UI | Verify persistence races and all actions on Galaxy A25; no runtime check performed in this run |
| Documentation publication | Codex reports docs-only commit `f110e57be` containing `docs/ROADMAP.md` and `docs/research/AI_MODEL_CATALOG.md`, pushed to `origin/main` | Confirm the active local branch is on that commit or a descendant; this does not prove source cleanliness or device/runtime behaviour |

### Build and release notes

- The 28 November 2026 clean-source release build and bundle verification succeeded; exact artifact details and hash are recorded in the AI process isolation update below. This supersedes the 27 November model-free MPP hash.
- The artifact remained named `patches-0.84.0.mpp`. The Morphe/base version is not the StopSlop feature revision; custom versioning is deferred to the release milestone.
- The bundle was unsigned because no signing key was configured.
- The 27 November build had no clean-source receipt because that source tree was dirty; the 28 November artifact records `Hushfeed-Source-Clean: true` at commit `429cd6ac61ab824902fdf382c862573ec666e909`.
- The latest M5B report could not compile focused tests because the existing `CustomLauncherIconTest.kt:232` references unresolved `HUSHFEED_STATUS_ICON`; this was not caused by an AI change. Track/fix it separately before relying on the full test suite.
- No configured emulator AVD/system image was available. Do not infer that an emulator test happened.
- Do not delete or overwrite the user's existing dirty changes, untracked brand ZIP, local-only prompts, or other unrelated files.

### Evidence labels

Use these labels consistently:

- `NOT_IMPLEMENTED` - no implementation exists.
- `UI_ONLY` - a control exists without a working behavior; do not present it as a completed feature.
- `SOURCE_IMPLEMENTED` - source logic exists.
- `TESTED` - relevant automated tests passed.
- `BUNDLE_VERIFIED` - the `.mpp` build and bundle checks passed.
- `DEVICE_VERIFIED` - manually tested on a named device and compatible TikTok build.
- `BLOCKED_BY_FEASIBILITY` - required data, hook, runtime, packaging or timing is not proven.

A preference key, settings row, model file, successful compilation, or non-empty DEX payload is not proof of correct runtime moderation.

### Runtime compatibility and packaging pass - 27 November 2026

This is a source-level assessment of the Android runtime options against this repository's API 23
floor and Morphe extension packaging. The TikTok extension is registered as a DEX `.mpe`; Morphe's
`extendWith` merges those classes into the patched APK. The registered StopSlop raw-resource patch
adds only eight pinned ARM native runtime libraries under standard APK `lib/<abi>/` paths. Optional
model and tokenizer data are imported/downloaded separately and staged into TikTok's app-private
files after profile, size and hash checks. The rebuilt `.mpp` is model-free and bundle-verified;
patched APK entries and Morphe host loading remain unverified.

| Candidate | Android / ABI evidence | Format and packaging | Assessment |
|---|---|---|---|
| LiteRT bundled (current 2.4.0) | Current Android package floor is API 24. The previous 2.2.0 line documented API 23 for its CPU Interpreter; ABI/package details must be pinned to the selected AAR. | TFLite/LiteRT models; tokenizer/model metadata remain model-specific. Runtime ships native `.so` files in Maven AARs. | Current API excludes API 23; older 2.2 CPU Interpreter is an API 23 candidate but is legacy and native libraries have no proven MPP delivery route. |
| LiteRT via Google Play services (17.0.0) | API 24+ with Google Play Store/services; not present on all compatible hosts. | Java API delegates execution to Play services; model still needs a supported local file/data path. | Cannot be mandatory. API 23 needs another runtime, and access from the injected extension process plus model storage has not been proven. |
| MediaPipe Tasks Text | Current Android setup requires API 28. Text Classifier/Text Embedder run on 32-bit ARM CPU per platform notes; ARM64 also supported. | `.task`/`.tflite`; task API handles tokenizer/preprocessing only for compatible model packages. Android Maven dependencies include native runtime libraries. | Does not meet API 23 and shares the unproven native packaging boundary. Not selected. |
| ONNX Runtime Android 1.20.0 + Extensions 0.13.0 | The pinned AAR manifests declare API 21 and contain ARM64, ARM32, x86 and x86_64 libraries. API 23 behavior and Morphe loading remain unverified. | Real quantized E5 ONNX model with an XLM-R tokenizer graph using ORT Extensions. Patch resources ship ARM64 and ARM32 only. | Selected for the source vertical slice because its inspected AAR floor covers API 23; source selection is not an Android compatibility claim. |

Primary references: [LiteRT Android version/API table](https://developers.google.com/edge/litert/android), [LiteRT 2.3 Android API change](https://developers.google.com/edge/litert/releases/litert_oct), [Google Play services LiteRT setup](https://developers.google.com/edge/litert/android/play_services), [MediaPipe Android setup](https://developers.google.com/edge/mediapipe/solutions/setup_android), [MediaPipe Text Classifier](https://developers.google.com/edge/mediapipe/solutions/text/text_classifier/android), [ONNX Runtime Mobile](https://onnxruntime.ai/docs/get-started/with-mobile.html), [ONNX Runtime Android compatibility](https://onnxruntime.ai/docs/reference/compatibility.html), and [TensorFlow Lite Android API/ABI requirements](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/g3doc/android/development.md).

**Model decision:** multilingual E5 Small is selected for this caption-semantic embedding slice,
not as a generator, AI-generated-text detector or generic slop classifier. Its ONNX model,
tokenizer graph, immutable source revisions, sizes, SHA-256 values and MIT provenance are recorded
in `docs/AI_SEMANTIC_TOPIC_FILTER.md`. A local desktop CPU artifact check ran inference; no
Android-host or language-quality result is claimed.

**Source status:** the registered patch, real provider, async cached feed decision, off-by-default
settings and asset installer are source-implemented. First evaluation returns KEEP; a later feed
hook invocation can consume a completed cache result, but TikTok's natural same-item re-evaluation
has not been demonstrated. Android-host inference and patched-APK/device behavior remain
unverified, and mechanical
Topic Filter, marker-based TikTok AI labels, Ads & Sponsorships, Minimum Likes, Preferred Content,
and M4 remain independent of provider availability. No tests/builds or device operations were run.

---

## 3. Moderation architecture: two modes, two engines, positive prioritization

StopSlop must support both mechanical and AI-assisted algorithms without duplicating UI controls or creating contradictory decisions.

```text
TikTok post / Aweme
       |
       v
Verified Morphe feed hook
       |
       v
Normalized metadata snapshot
       |
       +-------------------------------+-----------------------------+
       |                               |                             |
       v                               v                             v
Mechanical moderation           AI engine (optional)         Preferred Content
- official labels                - topic expansion            - positive topic list
- ad/sponsorship markers          - semantic text model        - match/relevance score
- like thresholds                 - promotion model            - stable local boost
- literal block rules             - future AI/slop model       - never blocks by itself
       |                               |                             |
       +---------------+---------------+-----------------------------+
                       v
         Moderation decision + preference score
              KEEP / BLOCK / UNKNOWN
                       |
                       v
          Existing feed-list integration
```

The diagram is a target architecture. Do not add a new parallel feed hook or rewrite the feed path simply to match the drawing. Reuse the verified existing path and adapt its actual interfaces.

### 3.1 Mechanical engine

Mechanical moderation remains the baseline and must work with AI disabled or unavailable. It decides whether content is blocked or kept. Preferred Content is a separate positive-ranking stage and must not be mixed into a block decision. Examples:

- TikTok-provided ad, paid partnership, commission, promotional music and supported commerce markers.
- Minimum/maximum likes and other explicit numeric limits.
- Official TikTok AI-generated label, where the exact marker is available.
- Topic phrases, hashtags, exclusions and creator rules.
- URL, promo-code, affiliate, CTA and other explicitly defined patterns.
- Deterministic Unicode/case/whitespace normalization and carefully bounded spelling normalization.

Mechanical rules must be explainable and have focused tests. Do not silently replace deterministic user rules with model guesses.

### 3.2 Positive preference engine - Preferred Content

Preferred Content is a positive-priority feature, not a hide filter. The user enters a list such as `Dota 2; science; wildlife;` using the same convenient semicolon-separated editing pattern as Topic Filter.

Required behavior:

- Match configured positive topics against verified caption/description and hashtag metadata; add creator or sound signals only if their meaning and availability are proven and their use is explicit.
- Produce a deterministic relevance/boost score from unique matching preferred topics. Cap the score so repeated occurrences of one term cannot dominate.
- If the actual feed hook safely permits it, perform a **stable in-batch prioritization**: rank eligible ordinary videos with higher scores earlier within the currently returned batch; preserve original order for equal scores.
- Do not fetch more posts, modify server requests, fabricate items, cross pagination boundaries, alter cursors, or claim to train TikTok's recommendation backend.
- Do not move ads, pinned/structural entries, LIVE/story/special cards, or other non-eligible item types unless the source proves they can be safely ranked. Preserve their structural slots.
- A non-match receives no boost; it remains visible. Preferred Content must never block a post.
- A `BLOCK` decision from an enabled moderation rule takes precedence over any positive score. Existing explicit exclusions/allow rules keep their current documented scope.
- If safe reordering cannot be proven for a feed route, do not present an inert preference switch as working. Report the exact blocker and keep the feature disabled until a safe supported path is established.

The initial supported claim should be narrow: **prioritize matching videos within the current fetched feed batch**. It does not guarantee that TikTok will send more such videos in future batches.

### 3.3 AI engine

AI is optional and runs locally on the device. It may provide:

- **Semantic Expansion:** expand a user-entered negative topic or positive preferred topic into reviewed aliases, translations, transliterations, common misspellings, related phrases and hashtags. Profiles must retain whether they are `BLOCK` or `PREFER` intent.
- **Semantic Content Understanding:** compare available post text/metadata with a saved topic profile or classify a supported category.
- **Promotion understanding:** distinguish an actual promotion from an ordinary product/brand mention, only when verified input text is available.
- **AI/slop classification:** estimate probable synthetic content or repetitive/low-quality content as separate labels, not one combined assumption.
- **Future multimodal understanding:** use subtitles, OCR or ASR only if a lawful, supported and timely input path is demonstrated.

Do not run a generative LLM on every feed item. Generative topic expansion happens when a user creates, edits or explicitly refreshes a topic. Per-item understanding, if viable, should use a much smaller classifier/embedding model and a bounded feed-time budget.

### 3.4 Global and per-feature controls

The intended settings behavior is:

- One global **AI-assisted moderation** switch, default `OFF`.
- `OFF` means mechanical algorithms only. It must not disable existing mechanical filters or change their saved settings. Mechanical Preferred Content scoring remains available if its own feature switch is enabled.
- `ON` permits AI modules to run only when their specific feature option is enabled and a compatible local model is ready.
- Add per-feature AI options only for implemented AI functionality: initially Topic Filter semantic expansion and Preferred Content semantic expansion. Both default `OFF`. Do not create placeholder switches for future work.
- Preferred Content has its own enable switch and semicolon-separated positive topic field. It is not dependent on AI.
- If the model is missing, invalid, incompatible, busy, timed out or fails to load, the AI path is unavailable and mechanical filtering continues.
- Keep the official TikTok AI-generated-label toggle separate. It reads platform metadata and is not the same thing as enabling local AI inference.
- Do not add an "AI-only" mode that bypasses deterministic rules. The intended enabled mode is mechanical plus optional AI assistance.

Suggested key names for investigation, not permission to duplicate or rename existing preferences:

- `stop_slop_ai_moderation_enabled` - global AI-assisted moderation switch, default `false`.
- `stop_slop_topic_ai_expansion_enabled` - AI expansion for unwanted Topic Filter topics, default `false` until real inference is available.
- Candidate Preferred Content keys, subject to registry inspection: `stop_slop_preferred_content_enabled` and `stop_slop_preferred_content_rules`.
- Candidate AI key: `stop_slop_preferred_content_ai_expansion_enabled`, default `false` until its local model path is real.
- Treat these as names to investigate, not permission to create duplicate keys. Follow the repository's actual preference/defaults/owners conventions.

Codex must inspect the actual preference registry before adding keys. Preserve all existing keys, values, defaults and migrations.

### 3.5 TikTok Native Recommendation Bridge (platform-side controls)

StopSlop should help users reach and configure TikTok's own supported recommendation controls instead of pretending it can submit arbitrary backend ranking weights. TikTok's Help Center currently documents these mechanisms:

- **Filter keywords:** users can filter words, their variations and hashtags in For You, Following, Friends, LIVE and STEM feeds. TikTok also documents an optional AI feature that filters similar keywords, including synonyms and variations. This is a TikTok-native capability, not StopSlop's local model.
- **Manage topics:** users can move sliders for TikTok's predefined content categories to see more or less of those categories in the For You feed. TikTok says changes may take time to affect personalization. This is not an arbitrary custom-topic weight API.
- **Like / Not interested / Refresh feed:** TikTok documents these as ways users can influence future recommendations. Likes can help TikTok suggest similar content; Not interested can lead to less similar content; Refresh feed resets the For You recommendation experience before the user retrains it through real interactions.
- **Behavioral signals:** TikTok says recommendation systems consider user interactions (including watching fully or skipping), content information (such as sounds and hashtags), and user information. It states that interaction signals, including watch time, are generally weighted more heavily for most For You users.

**Product boundary:** these controls are genuine platform-side preferences and feedback, but public Help Center documentation does not describe a consumer API for assigning arbitrary numeric weights to user-defined topics or injecting custom positive/negative recommendation events. Do not claim such an API exists.

**Bridge implementation policy:**

1. Inspect the exact supported TikTok target and determine whether StopSlop can safely open the native Manage Topics and Filter keywords screens through stable in-app navigation.
2. If proven, add small StopSlop shortcuts to those native screens, with localized explanations of what each control changes. Preserve the user's existing TikTok settings.
3. For negative topics, explain that users can add selected words to TikTok Filter keywords and enable its own AI similar-keyword option. Do not silently sync or overwrite the user's native keyword list.
4. For positive broad categories, provide a shortcut to Manage Topics. Explain that TikTok exposes predefined categories, not arbitrary custom topic weights.
5. Positive custom topics continue to receive StopSlop's local Preferred Content boost. Positive server-side signals must come from genuine user actions such as voluntarily liking, following or watching content - never automated or simulated behavior.
6. A Not interested action may be surfaced only through an explicit user-initiated action on a specific post and only if the existing TikTok UI action can be safely reused. Never auto-submit Not interested for every locally hidden post.
7. Do not reverse-engineer or call undocumented network endpoints, replay authenticated requests, spoof events, alter signatures, or automate likes, follows, comments, watch time or views.
8. If a stable native navigation path is unavailable, do not add a broken shortcut. Report the exact blocker and provide concise user-guided navigation instead.

Official references (checked 26 November 2026):
- TikTok, [How TikTok recommends content](https://support.tiktok.com/en/using-tiktok/exploring-videos/how-tiktok-recommends-content) - signals and user controls including Like, Not interested, Refresh feed, Manage topics and AI similar-keyword filtering.
- TikTok, [Manage topics](https://support.tiktok.com/en/account-and-privacy/account-privacy-settings/manage-topics) - predefined topic sliders for For You.

### 3.6 Decision and precedence contract

All detectors return structured evidence, not an unqualified boolean. The existing source interfaces may be adapted rather than replaced wholesale.

- `KEEP` - reliable evidence does not meet an enabled blocking rule.
- `BLOCK` - a deterministic rule matches, or an enabled AI classifier meets an evaluated high-confidence threshold.
- `UNKNOWN` - data is missing, the model is unavailable, input is unsupported, inference exceeds its budget, confidence is insufficient, or an exception occurs.

Rules:

1. `UNKNOWN` always fails open. It never blocks a post.
2. A deterministic user-authored block rule may block when it matches verified input.
3. Explicit user allow/exclusion rules take precedence over AI-generated broad related terms, unless a separately documented safety invariant requires otherwise.
4. AI confidence thresholds must come from a labelled evaluation set, not an arbitrary hard-coded number.
5. AI must not broaden a topic silently. The user can review and remove generated candidates.
6. Preserve a reason code and engine/model version for a block decision. Do not log raw feed text by default.
7. A model exception, deadline miss or stale cache must not break feed rendering.
8. Preferred Content produces a separate non-negative ranking score; it never changes `KEEP` to `BLOCK`.
9. A valid `BLOCK` decision always defeats a Preferred Content boost. A preference match must never resurrect content hidden by an unrelated active moderation rule.
10. Stable ranking is restricted to eligible items in the same already-fetched batch and must preserve ties and structural entries.

---

### 3.7 Pre-display, preload and media-input boundary

The existing feed-list metadata hook is not proof of a general TikTok media-prefetch or subtitle hook. Distinguish post caption/hashtags exposed on the feed object from actual subtitle tracks, ASR text, burned-in text and decoded video frames. Only source-proven in-process inputs may be considered.

- Never run model inference in a synchronous feed getter or block the UI/player thread.
- A future analyser may inspect an already-preloaded upcoming item only after the source, timing and permission boundary are proven. Use a bounded, low-priority cancellable queue and a result cache keyed by stable video ID plus ruleset/model revision.
- No TikTok private cache/storage/process-memory reads, root, Shizuku, VPN, hidden APIs, Accessibility scraping, MediaProjection or screen capture.
- Measure the lead time before display and the effect on queueing, memory, CPU, heat and playback. Do not promise zero lag without measured pre-display margin.
- Segment skipping requires both reliable media timestamps and a verified player seek/skip action. Otherwise support only whole-video decisions based on available evidence.

### 3.8 Hybrid cloud-signature and on-device analysis

Use a compact, versioned, signed advertising-signature database for known public campaign patterns, with optional local AI for context and semantic understanding. The network distributes public rule data; it does not classify a user's current video.

A signature may contain campaign/advertiser aliases, official domains, CTA/promo-code patterns, language/region, validity dates, confidence, provenance and optional legally verified creative/audio fingerprints. A brand or product mention alone must never be sufficient to block a video. Combine multiple contextual signals and preserve user exceptions.

Use a fixed project-controlled HTTPS origin. A small manifest/index and JSON shards may be served from GitHub Pages; larger static data packages may use GitHub Releases or an approved CDN. Use ETag/If-None-Match, detached signature verification with a pinned public key, SHA-256, exact sizes, schema/version/expiry validation and rollback protection. Update only in background or on explicit user action, never on the feed path. Keep a last-known-good local snapshot for offline use. Download data only; no scripts, DEX, JAR, `.so` or executable rules.

Official references: [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases), [Android app-specific files](https://developer.android.com/training/data-storage/app-specific), [WorkManager constraints](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).

### 3.9 Blocked Videos History and local correction

Add a `Blocked Videos` page to StopSlop settings. The page title is `Blocked Videos`; the prompt is `Were these videos blocked correctly?`. The list is vertical, newest first, and contains only videos actually excluded by the final decision pipeline.

- User-selectable retention: 5, 10, 20, 50 or 100 entries; default 20. The maximum is 100. Lowering the limit prunes oldest records immediately.
- Every row shows available bounded display metadata, creator if available, blocked time, concise reason and a real `Open` action backed by a verified TikTok in-process route.
- Green thumbs-up `Correct` records that the user confirms the block. Red thumbs-down `Restore` creates a local exception for that exact stable video ID only. It must not allow the creator, topic, hashtag, sound or similar videos.
- A Restore decision is not proof that the video was reinserted into the current feed. State exactly what happens; do not claim immediate reinsertion unless a safe operation is verified.
- Deduplicate by stable video ID. Store a small reason code, timestamp, source and optional bounded display excerpt. Never persist the full Aweme object or video/audio/frame/transcript content.
- User corrections remain local. They do not send TikTok likes/dislikes, network feedback, telemetry or automatic model training.
- Clear History must not silently remove per-video Restore exceptions; make the effect explicit or expose separate controls.
- If a stable ID or open route cannot be verified, report the blocker rather than presenting a fake clickable row.


## 4. Topic Filter: three levels

### Level 1 - Literal Matching (implemented mechanical foundation)

Current reported functionality:

- case-insensitive literal phrase matching against verified metadata;
- semicolon-separated rules, for example `dota; politics; ukraine;`;
- multiword phrases such as `dota 2` and `counter strike`;
- hashtag rules such as `#dota`;
- exclusions such as `!politics` and `!#dota`;
- creator allowlist such as `@username` where the stable handle is available;
- compatibility parsing of legacy newline-separated rules.

This is mechanical text matching. It does not infer translations, synonyms, slang or meaning.

### Level 2 - Semantic Expansion (first AI feature)

The user enters one topic, for example `Россия`, either in the unwanted Topic Filter list or the positive Preferred Content list. When global AI and the matching per-feature AI expansion option are enabled, a local model creates a bounded topic profile tagged with its intent (`BLOCK` or `PREFER`).

A profile may contain:

- canonical topic and detected/input language;
- direct aliases and alternate spellings;
- translations and transliterations;
- common misspellings and mixed-script variants;
- common abbreviations and slang;
- high-relevance hashtags;
- related entities/phrases, clearly separated from direct aliases;
- explicit exclusions/disambiguation hints;
- confidence/relevance and provenance per candidate;
- profile schema and model version/hash.

Examples of the distinction:

- `Россия` -> `Russia`, `Rossiya`, `РФ`, and common spelling variants are direct/near-direct candidates.
- `Kremlin`, `Russian politics`, `war`, and similar terms may be related concepts, but must be shown in a separate review group. They must not silently become automatic blocks just because they are associated with the topic.
- `Dota 2` may produce `Dota`, `DOTA2`, `Defense of the Ancients`, and relevant hashtags, while unrelated gaming terms require separate approval.

The user can accept/reject candidates before they become active rules. The system may offer a clearly described "accept high-confidence direct variants" action only after the quality of those candidates has been evaluated.

**Critical performance rule:** generation runs only when a topic is created, edited, or manually refreshed. Store the resulting profile locally. The feed hot path uses cached data; it never invokes a generative LLM to expand a topic for each post.

### Level 3 - Content Understanding (later AI feature)

The system classifies meaning rather than relying only on literal token overlap. Initial order:

1. semantic comparison of verified caption/description and hashtag metadata against the saved topic profile;
2. optional creator/sound metadata signals where their exact meaning and availability are proven;
3. subtitle/transcript text only if TikTok exposes it through an allowed data path at a usable time;
4. on-screen text OCR only after a supported, privacy-safe pre-display input path is demonstrated;
5. speech/audio ASR only after a supported, privacy-safe pre-display input path is demonstrated.

Do not claim that the app understands a video from audio, frames or subtitles unless that data actually reaches the moderation pipeline. No private TikTok storage/cache/process-memory access, root, Shizuku, VPN interception, unsupported hidden APIs or AccessibilityService scraping.

### 4.4 Preferred Content - positive topic preferences

Preferred Content complements Topic Filter. Topic Filter says what to hide; Preferred Content says what to prioritize. They must use separate persisted lists and separate decision semantics.

**Mechanical input:** a single-line, semicolon-separated list such as:

```text
Dota 2; science; wildlife; programming;
```

Rules:
- Trim whitespace, ignore empty/trailing entries, preserve multiword phrases and normalize case/Unicode using the shared proven parser utilities where possible.
- Hashtags may be supported as explicit tokens if the verified metadata path supports exact hashtag matching. Do not silently interpret `!` as a block or exception syntax inside the positive list.
- Matching positive terms produce a separate preference score. A post that does not match stays visible with score zero.
- Apply the score only to eligible ordinary videos within the current fetched feed batch, using stable order for ties and preserving structural/special item positions.
- Never reorder across pages/batches, modify pagination metadata or claim to influence TikTok's server-side recommender.
- Existing hide rules and explicit block decisions take precedence. Preferred Content is never a bypass or allowlist.

**AI expansion:** when global AI and Preferred Content AI expansion are enabled and a compatible local model is ready, generate reviewed direct aliases, translations, transliterations, spelling variants, slang and hashtags when the user adds/edits/refreshes a preferred topic. Store accepted expansions locally. Broader related concepts require explicit approval. The generator must never run per video.

**Honest product wording:** the first version can promise to prioritize matching videos already present in a fetched batch. It cannot promise that TikTok's remote recommender will deliver more matching videos in future requests. If safe in-batch ranking is not supported by the actual list semantics, report the blocker rather than shipping a nonfunctional switch.

---


## 5. Global AI Model Catalogue and Model Manager

### 5.1 Product goal

StopSlop remains fully useful without AI. The base `.mpp` contains the mechanical filters and only a verified, shared runtime bridge if the runtime can be delivered safely. It does not contain every model. Model weights are downloaded only after the user chooses a model from the in-app catalogue.

The catalogue is intended for a worldwide user base. It must not be limited to a tiny hand-picked list of four or five models. The project maintains a broad research registry of multilingual and language-specialist models, and a separate installable Stable catalogue containing packages that have passed StopSlop's licence, conversion, Android runtime, security, language-quality and device tests.

These are deliberately different inventories:

- **Research registry:** broad model discovery across public model hubs, benchmark indexes and language-specific research. It may contain hundreds of candidates over time. Entries can be incomplete, experimental, incompatible, too large, or not legally redistributable. They are not directly installable.
- **Stable catalogue:** curated signed manifests of exact immutable packages that have passed the required gates. Only this catalogue can provide an Install button to ordinary users.
- **Installed models:** exact verified package versions stored in the host TikTok app's private data area and registered with a supported runtime.

The catalogue itself should be updated independently of the base APK/MPP through a signed metadata manifest. Adding a newly verified model should not require repacking StopSlop unless its runtime or app code changes. Model weights are data files; they are not injected into DEX or merged into the APK/MPP after download. They are logically integrated by registering the selected files with the already verified runtime.

### 5.2 User selection wizard

Use three questions, then show the full catalogue and recommendations. The user's answers are preferences; actual device capabilities and measured model data remain the authority.

#### Step 1 - languages to moderate

- Provide a searchable, scrollable, multi-select list generated dynamically from the union of BCP-47 language tags in the catalogue.
- Pin `🌍 Any language` at the very top. Selecting it is exclusive. It means the broadest language coverage for which the catalogue has acceptable evidence, not a promise to support every language on Earth.
- Display native language names and optional locale/flag emoji. Flags are visual hints for representative locales, not a claim that a language belongs to one country. Where a language spans many countries or lacks a natural flag, use a globe/script icon. Store canonical BCP-47 language/locale tags, not flag values.
- Include all catalogue languages, not a hard-coded ten-language list. Support multiple languages and mixed-script inputs.
- Show evidence levels separately: `declared`, `independently tested`, `StopSlop benchmarked`. A model's broad pretraining corpus is not equivalent to measured support for each language.
- If one model covers all selected languages at the requested evidence/quality level, recommend it. Otherwise show a compatible multi-model pack with the combined download and installed size. Do not force multiple models if one verified multilingual model is sufficient.

#### Step 2 - quality preference (1-3)

The user chooses the trade-off, but the numbers must be based on StopSlop task/language evaluations, not parameter count or model marketing:

1. **Compact** - smallest package that meets the minimum StopSlop acceptance threshold for every selected language and task.
2. **Balanced** - a measured balance between language quality, download/storage size, peak RAM and latency.
3. **Highest verified quality** - the best StopSlop benchmark result that still satisfies the user's device safety budget.

For each model, record task-appropriate results: retrieval/semantic match precision, recall, F1, false-positive/false-negative rates, ranking metrics or generator evaluation as appropriate. Do not collapse different tasks and languages into one universal accuracy number. If a language has not been tested, show `Not benchmarked`; never fabricate a score or quality tier. A model can have different quality tiers for different languages.

#### Step 3 - device resource preference (1-3)

1. **Saver** - smallest RAM/CPU/battery footprint; intended for entry-level and older phones.
2. **Balanced** - normal default for mid-range devices.
3. **Performance** - allows higher memory, latency and model-size budgets on capable devices.

Default to `Auto-detect`. Collect only publicly available, permission-safe facts from the host context: Android API, ABI, memory class / available RAM signals, total/free app-private storage, CPU/SoC identity where exposed, and supported runtime/backends. Thermal status may be used only through public Android APIs where supported. The user preference is a trade-off, not a substitute for checking the actual device.

### 5.3 Hardware compatibility score (separate from language quality)

Each model card should show a **Hardware compatibility: NN%** score for the current phone only when the score has a defensible evidence basis. This is a fit index, not probability, accuracy, or a guarantee.

First apply hard compatibility gates:

- Android API floor;
- host ABI and native runtime availability;
- model format / operator support;
- required accelerator availability if the artifact requires it;
- sufficient persistent storage and temporary staging space;
- peak RAM below the safe device budget;
- licence and package integrity accepted.

If a hard gate fails, show `Not compatible` with the exact reason. Do not display a percentage for an impossible configuration.

For compatible entries, use a transparent score derived from measured or calibrated device-class data. Initial proposed weighting (must be validated and documented before production use):

- peak-RAM headroom: 30%;
- steady-state inference latency versus the StopSlop task budget: 30%;
- cold-load/startup latency: 15%;
- thermal/stability behaviour under repeated use: 15%;
- free-storage and safe update-staging headroom: 10%.

Each subscore must be based on actual measurements or a clearly identified device-class estimate. If data are estimated from the same SoC/device class, label it `Estimated`; if measured on the exact phone/build, label it `Verified on this device`. Display the test-device class and benchmark date. If there is not enough evidence, show `Not measured`, not a guessed 90%.

The **device performance preference 1-3** influences recommendations within the safe compatibility limits; it must never override a hard gate or force an unsafe model.

### 5.4 Broad global catalogue - discovery policy

Do not stop after four or five models. Maintain a continuously expandable research registry. Use public model-hub metadata, official model cards, official source repositories and benchmark indexes such as MTEB. Search both multilingual families and language-specialist models. Discover candidates by BCP-47 language, script, task, licence, size, model architecture, supported runtime and mobile conversion status.

The discovery process should aim for broad coverage across at least 50 language tags where public candidates exist, and should continually search for specialist models for languages that are weakly represented by multilingual models. Do not invent a model for a language simply to fill a quota. If no credible compact candidate is found, use the best verified multilingual option and disclose weaker/untested coverage.

For every research-registry entry, capture:

- immutable upstream model ID/revision and source owner;
- task: embedding, classifier, reranker, generator, language detector or package;
- parameter count (not confused with download size);
- exact downloadable file sizes, total package size including tokenizer/config/auxiliary files, and expected installed size;
- quantization/format/runtime and whether the artifact is actually ARM64/mobile-compatible;
- supported BCP-47 languages, language evidence and known weak spots;
- licence/SPDX ID, redistribution and commercial-use conditions;
- model-card/benchmark source and last-checked date;
- current maturity: `RESEARCH_ONLY`, `CONVERSION_REQUIRED`, `RUNTIME_BLOCKED`, `BENCHMARK_PENDING`, `STABLE_CANDIDATE`, or `STABLE_INSTALLABLE`.

Never mistake an ONNX artifact optimized for AVX2/AVX512 x86 CPUs for a mobile ARM64 artifact. Never present a repository's total size or model parameter count as the actual one-tap package download size. A small parameter count can still produce a large file due to precision, vocabulary/tokenizer and auxiliary assets.

### 5.5 Seed research registry - broad initial candidate map

This is a discovery seed, not an approved install list. Exact artifacts, language support, complete package sizes and licences must be re-verified at an immutable revision before any Stable release. Candidate families are documented in the companion file `docs/research/AI_MODEL_CATALOG.md` (or the agreed tracked research location). The list intentionally includes multilingual, regional and task-specific options:

**Broad multilingual candidates:**

- IBM `granite-embedding-97m-multilingual-r2` - 97M; general-purpose pretraining coverage reported for 200+ languages, with enhanced retrieval/cross-lingual support explicitly documented for 52 languages; Apache-2.0. Very promising small multilingual candidate, but available quantized ONNX variants must be checked for ARM64 compatibility and complete tokenizer package size.
- IBM `granite-embedding-311m-multilingual-r2` - higher-capacity companion; same 52 enhanced language set and broader pretraining claim; Apache-2.0. Candidate for Quality 3 where device budget permits.
- IBM `granite-embedding-107m-multilingual` - 12-language candidate.
- `intfloat/multilingual-e5-small` - 118M, about 99 language tags in the MTEB registry, MIT; one quantized ONNX file found is AVX512-specific and is not an Android ARM package.
- `intfloat/multilingual-e5-base` - larger multilingual E5 candidate; verify actual package and device cost.
- `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` - 117.7M, 53 languages, Apache-2.0; original model files are large and need mobile conversion/quantization.
- `sentence-transformers/paraphrase-multilingual-mpnet-base-v2` - 278M, 53 languages, Apache-2.0; higher-resource candidate.
- `sentence-transformers/distiluse-base-multilingual-cased-v2` - multilingual family candidate; exact current artifact and per-language score need verification.
- `sentence-transformers/LaBSE` - 109 languages, Apache-2.0; broad coverage/high-resource candidate, not a presumed low-size model.
- `minishlab/potion-multilingual-128M` - static multilingual embeddings, 101 languages, MIT; lower runtime class but not equivalent to a contextual transformer. MTEB reports 489 MB memory for the benchmarked setup; verify actual model file and Android package.
- `Horizon-Labs/multilingual-embedding-small` - 141M, around 90 languages reported, Apache-2.0; published quantized ONNX artifact about 269 MB. Verify Android ARM64 execution and full package.
- Google `EmbeddingGemma 2 Text 270M` - 270M, over 100 languages, Apache-2.0; Google publishes a text-only LiteRT-LM bundle. Runtime and Morphe integration remain separate gates.
- `BAAI/bge-m3` - large multilingual embedding candidate (100+ languages), not a low-end default; verify runtime, licence and full quantized package.
- `facebook/SONAR` - broad 200+ language research candidate; high uncertainty for compact mobile packaging and actual StopSlop text task.

**Small / language-specialist candidates:**

- English: `sentence-transformers/all-MiniLM-L6-v2`, `BAAI/bge-small-en-v1.5`, `jinaai/jina-embeddings-v2-small-en`, `intfloat/e5-small-v2`, `minishlab/blink-8M`, `minishlab/M2V_base_output`. Some have low parameter counts or small benchmark memory footprints; exact Android package sizes and target-task performance must be measured.
- Russian / Russian-English: `cointegrated/rubert-tiny`, `cointegrated/rubert-tiny2`, `sergeyzh/rubert-tiny-sts-v2`, `sergeyzh/rubert-mini-frida`, `deepvk/USER2-small`, `deepvk/USER2-base`, `sergeyzh/BERTA`. Not all are equally small; preserve separate language/quality evidence.
- Japanese: `cl-nagoya/ruri-v3-30m`, `cl-nagoya/ruri-v3-70m`; small parameter counts do not guarantee small final package (one 30M file set includes a 147 MB weights file plus tokenizer assets).
- Korean: `dragonkue/multilingual-e5-small-ko`, `dragonkue/multilingual-e5-small-ko-v2`, `NGA-KR/ko-embed-nli`, `snunlp/KR-SBERT-V40K-klueNLI-augSTS`, `BM-K/KoSimCSE-roberta-multitask`. Licence, exact artifact and ARM package gates apply individually.
- Chinese: `moka-ai/m3e-small`, `shibing624/text2vec-base-chinese`; the latter has a large published weight file, so it is not automatically a Compact option.
- Arabic: `ALJIACHI/Mizan-Embeddings-0.1B`, plus multilingual Granite/E5/SONAR candidates. Verify exact language and dialect coverage.
- Vietnamese: `bkai-foundation-models/vietnamese-bi-encoder`, `AITeamVN/Vietnamese_Embedding_v2`, plus multilingual Granite candidates. Verify task, exact size and licence.
- Nordic languages: `NbAiLab/nb-bert-base`, plus multilingual candidates. Verify sentence-embedding conversion and licence.
- Other regional/language-specific search tracks must include Indic languages (Hindi, Bengali, Tamil, Telugu, Marathi, Gujarati, Kannada, Malayalam, Punjabi, Urdu), Persian, Turkish, Thai, Indonesian, Malay, Swahili, Balkan/Central European languages and scripts with low-resource coverage. Do not imply all have an installable candidate until verified.

**Generative topic expansion candidates - separate from embeddings:**

- `HuggingFaceTB/SmolLM2-135M-Instruct` - very small, Apache-2.0, but English-oriented; candidate for English-only generator experiments, not a global-language model.
- `Qwen/Qwen3-0.6B` - multilingual generative candidate; materially larger than the smallest embeddings. Verify mobile quantization, tokenizer/runtime, licence and tested Russian/Ukrainian/English output.
- `Qwen/Qwen2.5-0.5B-Instruct` - separate multilingual generator candidate; verify exact languages, package size and mobile runtime.
- `google/gemma-3-270m-it` - compact generator candidate; Gemma terms, not automatically Apache-2.0. Language quality and complete Android artifact must be evaluated.
- `meta-llama/Llama-3.2-1B-Instruct` - higher-resource generator reference, with its own community licence and supported-language constraints.

Do not confuse embedding candidates with generators. An embedding model can compare topic/caption meaning; it cannot directly produce a validated list of generated synonyms. Semantic Expansion may initially use a generator only when available, or may use embeddings plus a separately curated reviewed alias source; do not label the latter as text generation.

### 5.6 Stable versus research catalogue

The normal app catalogue must be broad but honest:

- `Stable / Installable`: exact signed packages that pass every gate. These get Install/Update/Activate controls.
- `Compatible, but not benchmarked`: may be listed as unavailable for recommendation or installation until quality tests pass.
- `Research / not installable`: development-only registry. Do not show an active Install button.
- `Not compatible with this phone`: show why and, if relevant, show a compatible alternative.

The consumer UI may include a filter to show `All models`, but non-Stable candidates must be explicitly labelled `Not available yet` and cannot be installed. Never let a huge public model hub turn into an arbitrary code/model execution surface.

### 5.7 Runtime, host package and model delivery

The previous M5B POC was `BLOCKED_BY_FEASIBILITY`: current MPP verification showed DEX payloads, but no verified native `.so` or model/tokenizer delivery path. The reported extension `minSdk` was 23, Java 11. Do not assume an external Android runtime can be loaded just because it supports standalone apps.

Compare only real options against current source:

- LiteRT / LiteRT-LM bundled at build time;
- LiteRT through Google Play services, including GMS/API-floor/runtime-context/model-format constraints;
- ONNX Runtime Mobile, and ONNX Runtime GenAI only for a tested generative model;
- ExecuTorch Android;
- other maintained runtimes only with strong reason.

Prove the actual TikTok host `Context`, extension classpath, host package, Android API, ABI, JNI load path, merged manifest and model-file access. No assumption of a separate extension sandbox or custom classloader unless observed.

Native runtime code must be shipped with the signed base patch through a supported, verified build-time packaging route or provided by a verified platform service. **Never download DEX/JAR/`.so` or executable code from the model catalogue.** Model weights/tokenizer/config/vocabulary are data only. Use only safe, supported model formats and a static runtime with allow-listed operators; no Python pickle, remote code, dynamic custom ops or custom executable plugins.

### 5.8 Hardware profiling and compatibility index

Collect only public, permission-safe host details. Determine:

- Android API / build; ABI; SoC/CPU identity if exposed;
- memory class, current free RAM where permitted and peak memory during measured inference;
- CPU thread/thermal state via public Android APIs where supported;
- free app-private disk space and staging requirements;
- available GPU/NPU/runtime backend only after capability verification.

For each stable model, publish performance profiles by exact test device or clearly defined hardware class. Score the candidate using the transparent weighted index in §5.3. Never call this percentage model accuracy. Do not generate a fabricated percentage if no measured/validated profile exists. A model can show `Hardware fit: not measured` until tests exist.

### 5.9 Install, cache, activate, update and delete lifecycle

The model is **not physically merged into APK/MPP**. It is integrated in the app's runtime registry and stored under the host TikTok app's private data only after the real host context is verified.

Recommended path layout:

```text
<verified TikTok host Context.filesDir>/stopSlop/
  models/<model-id>/<version>/       # installed, verified model data
  registry/                          # active model and package metadata

<verified TikTok host Context.cacheDir>/stopSlop/
  downloads/<model-id>/<version>/    # .part, resume and staging files
  update-staging/                    # prepared update before atomic promotion
```

`filesDir` is persistent app-private storage; `cacheDir` is temporary and may be purged. App-specific internal storage requires no broad file permission. App-data clear/uninstall of TikTok removes these files; document that lifecycle. Do not use Downloads, SAF, shared storage or file pickers for the normal one-tap path.

Install lifecycle:
1. User selects languages, quality preference 1-3 and device-resource preference 1-3.
2. StopSlop detects hardware and returns candidates, package size, language evidence, benchmark results, licence and hardware fit.
3. User explicitly selects a Stable package and sees exact download size, installed size, network/storage requirements and permissions.
4. Download over HTTPS to staging; support cancel/retry/resume safely.
5. Verify signed catalogue manifest, immutable URL, exact byte lengths and SHA-256 for every artifact. Reject mismatches.
6. Check available disk for staging + installed package + safe update rollback margin before download and before promotion.
7. Atomically promote verified files to `filesDir` and register metadata.
8. Runtime loads the selected package, tokenizer and config, runs self-test, then marks the model Ready.
9. Load lazily only when selected AI feature is invoked; unload when idle/pressure occurs. Do not initialize on every feed item.

Installed model UI:
- `Installed`, `Available`, `Updates`, `All models` tabs or equivalent;
- show model/task/languages/version/size/licence/runtime/hardware fit;
- Activate/Switch only to compatible task/model combos;
- Update only on Stable channel and only after explicit consent;
- Delete with confirmation after unloading; preserve all user-authored rules/topics and mechanical settings.

Update lifecycle:
- Keep only Stable updates for normal users; no beta/nightly channel by default.
- Show in-app update indicator; do not require `POST_NOTIFICATIONS` for initial version.
- Stage new version while old remains usable; verify manifest/signature/hash/size/licence; run a load/self-test; atomically change active pointer; rollback automatically if startup/inference fails; delete old files only after success and user storage is safe.
- Major model/profile semantic changes require user confirmation; do not silently change accepted expansion profiles.

### 5.10 Catalogue and installer security

- The app downloads only a signed, versioned JSON catalogue manifest from a fixed project-controlled HTTPS origin or approved static CDN.
- Pin the verification public key in the shipped app. Choose a signature algorithm and implementation actually supported by the minimum Android API. If a pure-Java verifier is needed, include it at build time; never fetch verifier code.
- Each model artifact has immutable upstream revision, fixed URL, expected byte length, SHA-256, SPDX licence, owner/attribution and redistribution evidence.
- Restrict redirect hosts and allowed URL schemes; reject plain HTTP and arbitrary user URLs.
- Download model data only: allow-listed `.onnx`, `.tflite`, `.litertlm`, `.safetensors`, `.gguf` or a project-defined safe tensor package only if the selected runtime demonstrably supports it. Do not assume all formats are safe merely by extension; parse and validate headers and model operators.
- No DEX, JAR, `.so`, ELF, scripts, Python pickle, `.pt`/`.pth` with pickle, `remote_code`, dynamic classes or custom executable operators from the network.
- Apply bounded file count, file-size and decompression limits. Avoid archive path traversal, symlinks, zip bombs, unexpected nested archives and model external-data path escapes.
- Validate model tensor names/shapes/config/tokenizer against the signed manifest before runtime load.
- Runtime must not fetch more dependencies or telemetry. After model installation, inference works offline.
- Check the existing merged TikTok manifest for `INTERNET`; do not add permissions blindly. Catalogue/model download is explicitly user-initiated and must not send topic/video content. No broad storage permission is required.
- No telemetry, no cloud inference and no upload of posts, captions, audio, frames, creator data, embeddings or moderation history.

### 5.11 Model catalogue references and research policy

Maintain exact URLs and immutable revisions in `docs/research/AI_MODEL_CATALOG.md`. Recheck all data before any Stable package publication. Suggested discovery sources include:

- Hugging Face model cards / Hub API: <https://huggingface.co/docs/hub/api>;
- MTEB available model index: <https://docs.mteb.org/overview/available_models/text/>;
- IBM Granite multilingual R2: <https://huggingface.co/ibm-granite/granite-embedding-97m-multilingual-r2>;
- multilingual E5: <https://huggingface.co/intfloat/multilingual-e5-small>;
- EmbeddingGemma 2: <https://ai.google.dev/gemma/docs/embeddinggemma>;
- Sentence Transformers model index: <https://www.sbert.net/docs/sentence_transformer/pretrained_models.html>.

The catalogue is expected to expand over time. No claim that every language has a tiny specialist model is allowed; where such a model does not exist or is not mobile-ready, show the best validated multilingual candidate and clearly identify evidence gaps.

---

### 5.12 Shared semantic model versus separate models

On the first attempt to enable a real AI feature, ask whether the user prefers `One shared model` or `Separate models`.

- `One shared model` is the recommended default when one compatible semantic model supports the required task. Reuse its text representations across eligible ad/topic/preference features instead of downloading duplicate weights.
- `Separate models` permits dedicated ad and topic/preference models where benchmarks demonstrate a meaningful quality or performance benefit. Higher storage cost must be shown; load models on demand rather than requiring all of them in RAM simultaneously.
- This setting concerns semantic models only. OCR, ASR and video-frame analysers may still be specialist modules. A text embedding model is not automatically a video/audio understanding model.
- Let users change the selection later and show the actual download/installed-size and compatibility consequences. Never show an unavailable configuration as selectable.
- Do not expose the choice until at least one compatible model can actually install and run offline in the verified host process or a faithful harness.

### 5.13 Third-party model disclosure and licence acceptance

Before installing/enabling each third-party model, show the model author, upstream model card, exact licence/terms, version/revision, intended tasks, language evidence, full package size, runtime and local-processing statement. Make the upstream model-card and licence links clickable. Store the accepted model ID, revision and licence version locally. A single generic StopSlop agreement is not acceptance of every future model's separate licence. Review redistribution and commercial-use terms before listing a package as Stable/installable. Do not expose model controls before runtime feasibility passes.

## 6. Privacy, reliability and performance invariants

- Keep moderation and inference on-device. Only model catalogue metadata and explicitly selected model artifact downloads may use the network.
- Never upload posts, captions, audio, frames, transcripts, creator/account data, embeddings or moderation history.
- Do not access TikTok private storage, cache or process memory through unsupported means. Access only StopSlop's own app-private directory via the verified host `Context`.
- No root, Shizuku, VPN interception, hidden APIs or AccessibilityService scraping for moderation input.
- No broad storage permissions, SAF or shared-downloads folder for normal model installation.
- No remote executable code or dynamically downloaded native libraries.
- Bound model count, cache, staging and installed size. Check available storage before download and update; avoid two full copies if the model package supports verified streaming/atomic install without reducing integrity.
- Load at most the models required for the active AI task and within a measured RAM budget; support unload and memory-pressure fallback.
- Do not run text generation on every feed item. Topic expansion is on user action; feed-time inference is a separate compact model/task with a measured deadline.
- AI failure, invalid artifact, low confidence, timeout or missing input fails open; mechanical filters continue.
- Cache keys include language, topic intent (`BLOCK`/`PREFER`), profile schema, accepted terms, model ID/revision and relevant settings.
- Keep diagnostics free of raw captions/transcripts by default.
- Stable catalogue signature and artifact hash must be verified before any model file is opened.
- A model's language/quality claims must be supported by model-card evidence or StopSlop tests; model size/parameter count is not an accuracy proxy.

---

- Blocked Videos History is local-only and bounded to the selected 5-100 records; user correction labels and per-video exceptions are never uploaded.
- Cloud ad-signature updates contain only project-published static rules. No video/audio/frame/caption/transcript/topic/embedding/creator/history data is sent upstream.
- Initial optional AI package budget: target 150-200 MB; absolute target cap 300 MB including runtime, tokenizer and selected modules. Report exact full size and do not force an unsuitable package to fit the budget.
- A shared semantic model may serve multiple text features only when its actual model task and benchmark evidence support them; OCR/ASR/video inputs require separate verified modules or capabilities.

## 7. Development roadmap - reordered by dependencies and user value

Execute one narrow vertical slice at a time. A later milestone must not be marked complete merely because its UI exists. Keep manual device validation distinct from source/build evidence.

### M0 - baseline and working-tree reconciliation

Read-only preflight. Confirm branch, origin, current commit (including documentation commit `f110e57be` if present), `git status --short`, staged/unstaged/untracked changes and prompt ignore rules. Preserve every existing change. Do not reset, clean, stash, overwrite, or publish unrelated work.

### M1 - close the existing mechanical baseline on Galaxy A25

Manually validate Ads & Sponsorships, Topic Filter, Minimum Likes, official TikTok AI label, Preferred Content, settings search, Native Recommendation Bridge guidance and reload action. Record exact TikTok package/build, device/build, scenarios and outcomes. This is a manual owner/device gate; Codex must not claim `DEVICE_VERIFIED` from source inspection.

### M2 - close reproducible settings and Native Bridge issues

Fix only confirmed StopSlop settings defects. Verify the current English base strings, search aliases, screen navigation and Native Bridge guidance. Do not redesign unrelated inherited HushFeed screens or introduce placeholder AI switches. Retain user settings and legacy migrations.

### M3 - canonical feed decision event and stable identity

Trace the existing feed hook and final filtering point. Define or adapt one small immutable event containing only: stable video/Aweme ID, decision (`BLOCK`), reason code, detector/source, timestamp, and optional bounded display metadata needed by the history UI. Preserve `KEEP/BLOCK/UNKNOWN`, allowlist precedence and `UNKNOWN -> KEEP`.

- Emit an event only when a video is actually excluded by the final pipeline, not for candidates, duplicate detector hits or temporary failures.
- Deduplicate by stable video ID; do not fabricate an ID from mutable caption/creator text.
- Do not persist the full Aweme object, video/audio bytes, frame images, transcript or raw diagnostics.
- If there is no stable ID on a route, report the gap; do not save a history row that cannot be identified or opened.
- Keep the event emission out of UI/player-critical work and ensure it cannot throw into the feed path.

### M4 - Blocked Videos History and local feedback

Implement the new user-facing history page using the existing StopSlop settings/navigation and local persistence conventions. Detailed acceptance criteria are in `prompts/active/M4_Blocked_Videos_History.md`.

- Add a `Blocked Videos` entry and page with the English-first heading `Were these videos blocked correctly?`.
- Show the latest blocked items vertically, one per row/card, with a supported action to open the original video.
- Let users select 5, 10, 20, 50 or 100 retained records; default 20. Prune oldest entries deterministically when the limit is lowered.
- Provide a green thumbs-up `Correct` action and a red thumbs-down `Restore` action. `Restore` creates a local per-video allow exception; it must not silently allow the creator, topic or all similar posts.
- Keep user feedback local. Do not send events to TikTok or any server. Do not train or update a model automatically from a single tap.
- Deduplicate repeated blocks by ID and retain a bounded status (`PENDING`, `CONFIRMED_CORRECT`, `RESTORED`).
- Do not save the videos themselves. Keep only the minimum metadata required to identify and explain the decision.
- Do not claim `Open` or immediate reinsertion works until the exact internal route has been verified for the supported TikTok build. If it is unavailable, report the blocker rather than inventing a deep link.

Source-review update (26 November 2026): the current implementation records only final exclusions with a stable ID, reason and exclusion timestamp; persistence uses a bounded single-writer queue, and Restore exceptions are read from an asynchronously hydrated memory snapshot so feed filtering does not parse preferences. Queue saturation drops a history capture without changing feed behavior. No reliable username is currently captured, so the UI uses Copy ID instead of constructing an unverified URL. Device behavior remains unverified.

### M5 - preload, cache, subtitle and pre-display input feasibility POC

Trace the exact supported TikTok target's in-process media-prefetch, player, caption/subtitle and metadata flow. This is a feasibility POC, not a general AI implementation.

- Distinguish post caption/hashtags already present in the Aweme metadata from actual subtitle tracks, ASR text and burned-in video text. Do not conflate them.
- Establish whether upcoming video metadata, subtitle text or decoded frames are exposed through a verified in-process hook before the video is displayed.
- Never inspect TikTok private cache/storage/process memory through unsupported mechanisms; no root, Shizuku, VPN, hidden APIs, Accessibility scraping, MediaProjection or screen capture.
- Never add OCR/ASR/inference to a synchronous feed getter such as `getItems()` or block the UI/player thread.
- Only if an early input route is proven, propose a bounded low-priority background queue, cache keyed by stable video ID plus detector/model/ruleset version, cancellation and memory-pressure behaviour.
- Measure lead time from input availability to display, queue delay, processing latency, dropped work, memory and playback impact. No zero-lag or pre-display claim without measurements.
- `SKIP_SEGMENT(start,end)` is not supported until a real timestamp source and safe player seek/skip action are both proven. Otherwise the supported result remains whole-video `BLOCK` or `KEEP/UNKNOWN`.
- If no early input is exposed, mark the relevant multimodal detector `BLOCKED_BY_FEASIBILITY` and leave it disabled.

### M6 - signed static advertising-signature database and updater

Design and implement the data-only signature update path independently of AI inference.

- Use a small fixed HTTPS project-controlled manifest and versioned JSON rule shards. GitHub Pages can host the small index/manifests; GitHub Releases or a controlled CDN can host larger static data packages. Verify current hosting limits before release.
- Use ETag/If-None-Match, version/expiry checks, pinned public key, signature verification, SHA-256, exact byte lengths, bounded schema and rollback protection.
- Update on an appropriate background schedule or explicit user action, never synchronously in the feed path. Respect user network/battery constraints; verify WorkManager support in the host extension before depending on it.
- The rule schema may include campaign/advertiser aliases, domains, CTA/promo patterns, language/region, validity window, confidence, provenance and optional legally verified fingerprints.
- Brand/product mentions alone are never an ad verdict. Combine contextual signals and support local exceptions/allowlists. Avoid unbounded user-supplied regex and catastrophic backtracking.
- No video, audio, caption, transcript, embeddings, user topics, creator identity or moderation history is uploaded. The server only distributes public/static rule data.
- Keep the previous last-known-good database for offline fallback and failed updates. Never download executable code or model runtime from this channel.

### M7 - verify host-private storage and runtime/JNI canary

Continue from the reported `BLOCKED_BY_FEASIBILITY` M5B result. The source now contains an optional model manager and pinned ORT/ORTX packaging path, but neither proves the extension host Context/package name, app-private `filesDir`/`cacheDir`, persistence/lifecycle nor that native libraries load in Morphe. Verify those canaries under the actual host classloader/ABI/API constraints.

- The current MPP contains runtime code/libraries only; model weights and tokenizer data are external `.ssmodel` package data. Do not mark M7 complete until the host-private storage and runtime canaries pass.
- Import uses SAF and private storage; no broad storage permission, shared Downloads, root or Shizuku is used.
- Never download DEX/JAR/`.so` or executable code at runtime.
- Preserve the API 23 compatibility constraint unless an explicit product decision and full dependency analysis authorize changing it.
- If the path fails in a host build or device, report the exact failing gate and keep semantic inference fail-open/unavailable.

Build/artifact update (27 November 2026, catalog-integrated model-free rebuild): Gradle Wrapper 9.9.1 compiled the release extension; `:patches:buildAndroid` and `:patches:verifyBundle` succeeded for 156 patches and all three non-empty DEX payloads. `patches/build/release/patches-0.84.0.mpp` is 26,136,171 bytes (24.925 MiB), SHA-256 `af49c659196faa76e77da57d095df5c52f60336a7e9f44c032dd47ae4e8e38f2`; ZIP integrity passed with 1,656 entries and no duplicate paths. No `.ssmodel`, ONNX model weights or tokenizer graph files are present. Payload breakdown: extension MPEs 9,001,548 raw / 3,787,335 compressed bytes; eight native `.so` files 41,853,012 / 16,896,457; other payloads and metadata (including `classes.dex`) 13,053,682 / 5,070,811; ZIP directory/header overhead 381,568 bytes. Native compressed size alone exceeds both 10 MiB and 15 MiB gates. ARM64 native is 24,622,576 / 8,843,504 bytes; ARM32 native is 17,230,436 / 8,052,953 bytes. API 23+, Morphe host loading, private storage and device inference remain unverified; no APK was produced. M7 and M9 remain open.

Final APK integration gate (27 November 2026): the existing patches/build/release/patches-0.84.0.mpp remains 109,819,836 bytes and its SHA-256 matches bundle.sha256 (f53e229176456d50c6c5fd2844adb399773f636815c875b463d20a2dc70e6964). No base APK was found in repository files, selected build output directories, or the checked Downloads location; no Morphe Desktop CLI/app was discoverable through PATH, registered apps, or the checked install/workspace locations. Therefore no final APK was generated or inspected; APK destinations, signature, ABI selection, Morphe host loading, and device inference remain unverified. Patch metadata identifies the supported TikTok target as com.zhiliaoapp.musically 47.9.3 (versionCode 2024709030). Manual next step: in Morphe Manager use Sources > + > Local to add this MPP, select that supported TikTok target, enable the Settings patch (which depends on StopSlop local semantic model assets), then apply/export and retain Morphe's persistent signing key. This follows documented Manager terminology, not a UI session observed here. The local-source workflow is documented at https://github.com/MorpheApp/morphe-manager/blob/main/docs/patch-sources.md.

Runtime decision (27 November 2026): source uses ONNX Runtime Android 1.20.0 and ORT Extensions Android 0.13.0 because their inspected AAR manifests declare API 21 and provide ARM64/ARM32 libraries. The MPP contains runtime code/libraries only; the pinned model/tokenizer are in a separate local `.ssmodel` artifact. Desktop artifact execution does not verify Android compatibility or host execution. Play-services LiteRT remains an alternative; its documented API 24 floor does not cover API 23. No custom operator-pruned runtime was built or validated, and the current model-free MPP misses the size gates.

GitHub model delivery implementation (27 November 2026): source now includes the pinned E5 catalog client, HTTPS/size/SHA validation, offline catalog cache, versioned Release publisher, release asset/checksum metadata, and official GitHub Pages deployment workflows. The local `.ssmodel` validator and generated catalog were checked against the current ignored package (86,306,396 bytes; SHA-256 `7a631638566759f87960f1e90eb913bcb50052148ae7a06730886069692235ee`). The Actions workflow rebuilds tokenizer ONNX from pinned public tokenizer files with a version-locked converter, but that GitHub-hosted rebuild has not run; release creation is blocked safely unless the generated tokenizer and all package hashes match. A successful publisher run passes only its version through a short-lived Actions artifact to the Pages workflow; this avoids relying on an event created with `GITHUB_TOKEN` to start another workflow. A read-only GitHub Pages API check returned HTTP 404 and the public Release list has no `model-v*` release, so the expected Pages/catalog URLs are not live yet. One-time setup is **Repository Settings → Pages → Build and deployment → Source → GitHub Actions**; then run the deliberate model release workflow with version `1.0.0`. No PAT or Actions secret is required. The tokenizer regeneration and actual public deployment remain pending.

The MPP hash/size above supersedes the earlier 109,819,836-byte model-bearing bundle reported in the Final APK integration gate. That earlier APK-gate search found no compatible TikTok base APK or Morphe CLI, so no final APK was produced; this rebuild did not repeat that search or claim APK inspection.

### AI process isolation and stability gate - 28 November 2026

Build/artifact update from committed source `429cd6ac61ab824902fdf382c862573ec666e909`: Gradle Wrapper 9.9.1 compiled the release extension and `:patches:buildAndroid` plus `:patches:verifyBundle` succeeded (156 patches; three non-empty DEX payloads). The clean-source MPP `patches-0.84.0.mpp` is 26,154,078 bytes (24.942 MiB), SHA-256 `42ebd6386e9ca8c77945f1db0a71127f18b09ede6ec62157bd2d9ca17d95e616`. ZIP inspection found 1,656 entries, no duplicate paths, eight native libraries, and no `.onnx` or `.ssmodel` files. Payload: extension MPEs 9,032,068 raw / 3,801,290 compressed bytes; native libraries 41,853,012 / 16,896,457; other entries 13,062,975 / 5,074,907; ZIP overhead 381,424. Native ARM64 is 24,622,576 / 8,843,504; ARM32 is 17,230,436 / 8,052,953.

The MPP contains the compiled `StopSlopAiServiceManifest` patch class with the service class name, `android:process`, `:stopslop_ai`, and `android:exported` values; the TikTok extension DEX contains `StopSlopAiService`, the IPC protocol, and ORT references. `SettingsPatch` depends on the manifest patch. Source checks in the host process require `ServiceInfo.processName == <package>:stopslop_ai` and `exported == false` before binding; the service independently checks its process name before model verification and session creation. Therefore a missing or malformed installed declaration results in `AI unavailable: isolated process is not supported by this patch target` and no in-process inference fallback. The MPP is not a patched APK and does not prove Morphe applied the declaration; PackageManager process name, separate PID, JNI loading, model initialization and inference remain unverified on Galaxy A25. No model files or preferences were changed. A first committed build surfaced a literal escaped newline at EOF; it was fixed in commit `429cd6a` and the successful clean-source bundle was rebuilt from that commit.

### M8 - device profile and global research registry maintenance

The latest supplied Codex report records 65 candidate IDs and 52 explicit language tags; all remain research-only. Preserve that inventory and extend it only with traceable evidence.

- Keep research candidates separate from installable Stable packages.
- Record immutable revisions, exact model/tokenizer/config files and total package sizes, SPDX/licence terms, language evidence, task type, runtime, minimum API, ARM64 status and known limitations.
- Dynamic BCP-47 language selection must be generated from actual catalogue evidence. `Any language` means broadest verified coverage, not every human language.
- Distinguish model-card-declared languages, independent benchmark support and StopSlop-tested quality.
- Device preference tiers are `Saver`, `Balanced`, `Performance`; a transparent Hardware Fit score is shown only when the hard gates and actual measurements support it. Otherwise show `Not measured` or the exact incompatibility.
- Keep the initial optional AI package target at 150-200 MB and a 300 MB absolute target cap including runtime/tokenizer/selected modules. This is a product budget, not permission to falsify sizes or force an unsuitable model. If no compatible package fits, say so.

### M9 - first ARM64 model comparison and offline inference

Only after M7 succeeds. The first multilingual embedding package and CPU provider now exist in source, and a local desktop artifact check confirmed model execution; M9 is not passed. Verify offline inference in the patched host or a demonstrably faithful harness, then measure Russian, Ukrainian, English and every other advertised language separately. Record full package size, cold start, peak RAM, latency, thermal behaviour, battery and task-specific quality. `MODEL_INFERENCE_VERIFIED` requires these runtime checks; desktop artifact execution alone is insufficient.

### M10 - signed Stable catalogue and secure Model Manager

Only after at least one candidate passes M9. Provide dynamic language selection, model task/size/licence cards, exact download and installed size, `Installed/Available/Updates/Research only/Not compatible` states, explicit install/activate/switch/update/delete, atomic promotion, rollback and offline inference. Research-only candidates never receive active Install buttons.

### M11 - first-AI-use wizard, model-sharing mode and third-party disclosures

On the first attempt to enable a real AI feature, ask:

- `One shared model` - recommended default. One compatible semantic model is reused across supported text-semantic features when its embedding/classification task actually fits.
- `Separate models` - dedicated compatible models for ad and topic/preference tasks where evidence shows a benefit. Higher storage and possibly memory cost; models may be loaded on demand rather than simultaneously.

This choice concerns the semantic model architecture only. OCR, ASR and video-frame components may still be separate specialist modules; a text embedding model is not automatically a video/audio model. Let the user change this choice later, with a clear impact summary. Do not expose the wizard until at least one real model is installable and runnable.

Before installing/enabling each third-party model, show its author, model card, exact licence/terms, source, package size, supported languages/tasks and local-processing statement. Make model-card and licence links clickable. Record the accepted model ID, revision and licence version locally. Do not treat one blanket StopSlop agreement as acceptance of every future third-party licence. Use only models with reviewed redistribution/use terms.

### M12 - global AI switch and implemented per-feature controls

Global AI defaults OFF. OFF guarantees zero model inference while preserving every mechanical filter. Only add per-feature AI controls for features backed by an installed compatible model. Missing model, invalid package, timeout, unsupported language or runtime failure disables that AI path and falls back to mechanics.

### M13 - Topic Filter semantic expansion

Use a verified model for reviewed, bounded `BLOCK` topic expansion and/or semantic text matching. The user explicitly reviews and accepts aliases. Generate only when a topic is added, edited or refreshed; never generate on every feed item.

### M14 - Preferred Content semantic expansion

Reuse the same shared semantic service when selected, but maintain a separate `PREFER` intent profile. Semantic matches may add only a bounded local in-batch score. They never hide nonmatches, override a valid BLOCK, move structural slots or alter server requests.

### M15 - semantic content and promotion understanding

Add compact, measured inference over verified metadata/text inputs only. Brand mention alone is not advertising. Combine official sponsorship evidence, CTA, promo code, destination URL and corroborating text conservatively. Keep `UNKNOWN -> KEEP` and user exceptions.

### M16 - OCR, ASR and subtitle analysis

Only proceed when M5 proves a supported input source and enough pre-display lead time. Add OCR/ASR specialist modules only when their languages, complete package size and A25 cost are independently measured. Do not read TikTok private cache or capture the rendered screen.

### M17 - in-video ad segments and safe skip

Use segment classification only if M5/M16 provide reliable timestamps and the actual player has a supported, tested seek/skip action. Otherwise retain whole-video classification only. Never stall playback waiting for AI.

### M18 - synthetic content and AI-slop

Keep TikTok's official AI-generated label separate from synthetic-content estimation and repetitive/low-quality content classification. Require independent datasets, conservative thresholds, explainable reasons and local correction controls.

### M19 - performance and device compatibility

Benchmark on Galaxy A25 before Galaxy A12. Measure p50/p95 queue and inference latency, UI/player jank, cold start, peak RAM, storage, thermal throttling, battery and queue cancellation. Remove any path that causes playback regression. Validate offline operation and fail-open behaviour.

### M20 - final branding, documentation and release/versioning

After functionality and UI stabilise, finish English-first strings, translations, screenshots captured from the actual device, privacy/feature matrix, independent StopSlop versioning and release notes. Preserve Morphe compatibility metadata, GPLv3 notices, upstream attribution and rollback artefacts.

---

## 8. Engineering workflow

- Work in the existing HushFeed/Morphe repository. Do not create a standalone companion app.
- Check `git status --short` before editing and preserve staged, unstaged and untracked user data.
- Read only relevant files and call sites; no broad audits or full-file dumps without a reason.
- Do not delete inherited HushFeed features merely to simplify the UI.
- Preserve GPLv3, notices, attribution and compatibility-sensitive identifiers.
- Separate commits per validated vertical slice. Never bundle prompts, model binaries, signing keys or unrelated staged changes into a docs/source commit.
- Build/tests/Gradle/ADB/emulator are permitted when the task prompt asks for them. Never wipe user data or destructive Git state.
- `prompts/` and root `AGENTS.md` remain local-only.
- Codex reasoning level is task-dependent: Light for tiny edits, Medium for ordinary implementation, High for runtime/security/ML architecture. Do not select High by default.

---

## 9. Current next step

**Immediate next gate: publish the first verified package and deploy the catalog.** Enable Pages with the one-time setting above, then run the versioned release workflow and confirm both public assets. After that, manually apply the rebuilt MPP with Morphe Manager and verify deterministic filters without a model, catalog download/cache, SAF import, integrity rejection, model session startup, switching/removal, offline inference and API 23+ compatibility. First feed evaluation remains fail-open; do not claim same-item re-evaluation without observing the actual route. The catalog is HTTPS + SHA-256, not digitally signed.

Keep the manual M1 Galaxy A25 verification as a separate owner/device gate. The Codex report for documentation commit `f110e57be` does not prove device behaviour, model runtime, model installation or a clean source worktree.

After the M7/M9 gates pass, add hosted/signed catalog delivery and broader AI roadmap work; do not mark the current import/direct-package manager or any model device-verified before the host/runtime path is verified.

Latest reported global research catalogue status: 65 candidate IDs and 52 explicit language tags; all remain research-only. No model is currently verified installable or runnable offline inside the Morphe extension. Preserve this honest state until real host/runtime inference is demonstrated.

## 10. Definition of done for AI moderation and Model Manager

- A real offline model/runtime is loaded in the target host process or a faithful harness with the same classloader/native packaging.
- The base APK/MPP does not contain optional downloaded model weights and remains useful without AI.
- The global research registry is broad, evidence-based and continuously extensible; Stable installables are signed, curated and immutable.
- Dynamic BCP-47 language selector, exclusive `🌍 Any language`, quality preference 1-3 and device resource preference 1-3 work.
- Every installable model has full package sizes, task, licence, language evidence and device compatibility state. Hardware Fit % is transparent and independent from language quality.
- App-private install/update/delete works without broad storage permission; staged downloads are verified and installed atomically.
- Interrupted/corrupt downloads, bad signatures/hashes, low storage, incompatible API/ABI, invalid model/tokenizer and runtime failure fail safely.
- No executable DEX/JAR/`.so` is fetched from a catalogue; inference works offline after model installation.
- AI OFF causes zero model inference and leaves mechanical filtering intact.
- Negative `BLOCK` and positive `PREFER` profiles remain separate and user-editable.
- Russian, Ukrainian and other advertised languages are measured per task.
- No raw post/media/account content is uploaded.
- Focused tests, MPP verification and manual A25 validation are recorded; A12 fallback is documented before broad claims.

- Blocked Videos History stores only actual final BLOCK events, uses stable IDs and a verified open route, keeps at most the selected 5-100 records, and applies Correct/Restore locally with per-video-only exceptions.
- Shared/separate semantic model choice appears only after a model/runtime path is truly installable; each third-party model has a clickable model-card/licence disclosure and recorded version-specific acceptance.
- Static ad-signature updates are signed, versioned, size-bounded and offline-safe; no user media or moderation data is uploaded.

An embedding model is not a generator. A text model is not video/audio understanding. Broad pretrained language coverage is not the same as per-language StopSlop benchmark proof. Parameter count is not exact download size, and Hardware Fit percentage is not model accuracy.

### AI stability and activation pass - 28 November 2026

`SOURCE_IMPLEMENTED; MPP_BUNDLE_VERIFIED; DEVICE_UNVERIFIED`:

- Removed selected-package manifest/file validation from the `TopicFilter` feed callback. Optional semantic failures are now isolated after deterministic phrase/hashtag/allowlist decisions, so an AI initialization or inference error fails open only for semantics. Selection changes no longer initialize a provider while semantic filtering is off.
- Preserved the saved semantic preference if its model is currently unavailable; the disabled control is displayed off without rewriting the user's stored value. The model manager now persists the selected package revision beside the existing ID/version key and migrates a valid legacy selection once without changing the selection.
- `READY` now follows a real tokenizer + ONNX embedding probe and output-shape/finite-value validation. E5 topic vectors use `query: ` and caption vectors use `passage: `. Initialization and inference catch Java/native-linkage and low-memory errors, release partial resources, expose sanitized failure categories, and provide an explicit retry from the selected-model actions.
- Runtime remains CPU-only and single-threaded per session, batch size 1, one worker with a 32-job bound, 2,048-character caption bound, 512-token cap, 16 topic bound, 32-entry topic-vector cache and 128-entry result/request caches. Peak RAM and Galaxy A25 latency are **unmeasured**; the catalog's 768 MiB value remains explicitly labelled an unmeasured planning estimate. A native SIGSEGV/process abort remains outside Java recovery.
- Ads/Sponsorship and TikTok AI-marker filters remain registered as deterministic detectors independent of E5. Ads/Sponsorship and legacy Remove feed ads default on; both StopSlop and inherited AI-marker switches default off. No device preference dump or crash stack was available, so the reported ad/AI overrun and TikTok crashes do not have a confirmed device-side cause. Unlabelled synthetic media and TikTok markers not exposed in supported metadata remain undetectable by these filters.
- Catalog expansion remains **E5-only when a verified public release/catalog entry is available**; the Pages catalog URL was inaccessible during this run, and the latest deployment record still says the Release/Pages workflow has not run. MiniLM is excluded until its BERT tokenizer/profile, 128-token behavior, ONNX package, hashes and adapter are verified. DistilUSE is excluded because its documented 768-to-512 Dense/Tanh head is not implemented by the 384-dimensional E5 adapter. No candidate was added or labelled compatible based only on model-card metadata.
- Release artifact `patches-0.84.0.mpp`: 26,138,893 bytes; SHA-256 `2a5bbe90a91668a81a2e9423883f4dba354de59c7ce60b0dda9a40879d0d5b1c`. `verifyBundle` found 156 patches and three non-empty DEX payloads. Archive inspection found eight runtime `.so` files (ARM64 + ARM32) and no ONNX weights, tokenizer assets or `.ssmodel`. The source tree was dirty, so the release receipt was refused. This does not verify Morphe host loading, Android inference, crash resolution, or device compatibility.

Next manual gate: retain TikTok data and the existing E5 package; explicitly select E5 under Installed models, enable semantic filtering only after the provider reports ready, then exercise each mechanical filter with semantics off/on and record Logcat crash traces, cold-start/peak memory and feed behavior on Galaxy A25. API 23–25 and ARM32 coverage remain unverified.
