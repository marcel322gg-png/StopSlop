# StopSlop - Master Technical Roadmap

**Revision:** 7.1<br>
**Date:** 26 November 2026<br>
**Product:** StopSlop - local-first moderation integrated into a modified TikTok client<br>
**Primary validation device:** Samsung Galaxy A25 5G<br>
**Secondary compatibility target:** Samsung Galaxy A12, after A25 is stable

> This roadmap supersedes revision 7.0. It records the M5B.1 catalogue research pass while preserving the previous M5B result (`BLOCKED_BY_FEASIBILITY`). The research inventory is broad enough for discovery but still lacks immutable package pins for every candidate. No model was downloaded or executed; current host JNI, model storage and inference remain unverified. Do not advance to the consumer Model Manager until the runtime and data-delivery gates pass.

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
| `.mpp` output | `patches/build/release/patches-0.84.0.mpp`; verifier reported 155 patches and three non-empty DEX payloads | Manual application/verification on the target phone; versioning is intentionally deferred |
| Ads & Sponsorships | One visible StopSlop master control; migration preserves an explicit canonical value and derives the master for legacy installs; individual legacy signal preferences are preserved; feed, insertion and card paths are gated | Manual A25 verification of fresh-install and upgrade combinations |
| Ads automated checks | `AdsFilterTest` and `DeepFeedFilterBoundaryTest` reported passing; MPP build succeeded | Device verification remains separate |
| Topic Filter - mechanical | Single-line semicolon editor; phrases with spaces; exclusions, hashtags, creator allowlist; Enter/IME action no longer saves/closes; parser still accepts legacy newline-separated values | Manual check of save/reopen and actual matching on A25 |
| Preferred Content | `PreferredContentFilter` implemented; separate enable switch and semicolon topic list; unique literal caption-match scoring; stable in-batch ordering of eligible ordinary videos; non-matches remain visible | Focused tests and MPP bundle verification passed in Codex report; manual Galaxy A25 ordering/slot/persistence checks remain outstanding |
| Topic Filter tests | Focused parser tests reported passing | Device verification and false-positive evaluation |
| Settings search | Search indexes registered StopSlop controls; old ad names such as `Remove feed ads`, `Hide paid partnerships`, and `Hide promotional music` resolve to the canonical StopSlop control | Manual search checks in supported languages |
| Empty ad category | Orphaned inherited `ADS` header removed | Verify on device |
| Global settings appearance | Shared StopSlop styling exists in the touched settings components; complete visual consistency across every inherited screen has not been demonstrated | Inspect real screens; do not claim repository-wide visual unification without evidence |
| Official TikTok AI-generated marker | Existing deterministic marker filter is controlled by `stop_slop_ai_generated` | Verify exact marker coverage and device behavior; this is not AI inference |
| Minimum Likes | StopSlop enable key and threshold preference exist in source | Confirm threshold boundaries and actual feed behavior on A25 |
| AI runtime/model | M5B result is `BLOCKED_BY_FEASIBILITY`: no verified JNI/native-library path or model/tokenizer asset delivery path exists in the current MPP; no model was downloaded or executed | Prove runtime + data-file loading in a disposable host-process harness before adding production AI controls |
| AI model catalogue | Research inventory contains 65 distinct model IDs and an explicit 52-tag language list; all entries remain research-only and exact immutable package pins are incomplete | Complete pinned artifact/licence/runtime metadata, then prove host device profile, private-storage canary, JNI path and offline inference before production selector/installer UI |
| Model candidates | Small language-specific and multilingual embedding candidates identified from public model cards; none validated inside StopSlop | Verify exact converted package size, language quality, runtime, licence, ABI, RAM and A25 latency |
| Optional generator model | No compact multilingual generator has been proven in the target runtime | Keep separate from embedding models; do not claim embeddings generate aliases |
| TikTok Native Recommendation Bridge | Localized informational rows for native Manage topics and Filter keywords are source-implemented; tapping shows guidance rather than deep-linking. No stable in-app route was verified for TikTok 47.9.3 (versionCode 2024709030). Existing user-triggered Not interested control is unchanged; no automatic feedback is sent. | Manually confirm dialogs and guided navigation on Galaxy A25. Do not claim direct navigation or backend API access. M5 has the documented safe fallback; device check remains pending. |
| Semantic topic expansion | Not implemented | First planned user-facing AI feature |
| Semantic content understanding | Not implemented | Requires a proven model, supported metadata and feed-time budget |
| OCR / ASR / subtitle analysis | No complete, verified pre-display pipeline is established | Separate feasibility gate; do not assume availability |

### Build and release notes

- The last supplied MPP build succeeded and passed bundle verification.
- The artifact remained named `patches-0.84.0.mpp`. The Morphe/base version is not the StopSlop feature revision; custom versioning is deferred to the release milestone.
- The bundle was unsigned because no signing key was configured.
- A release receipt was refused because the source tree was dirty.
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

## 7. Development roadmap - one vertical slice at a time

### M0 - baseline and working-tree reconciliation

Brief read-only preflight. Inspect Git status/branch/remotes and only relevant packaging, model, storage, settings and feed paths. Preserve all staged, unstaged and untracked data. No broad repository dump or cleanup.

### M1 - close mechanical baseline on Galaxy A25

Manually validate Ads & Sponsorships, Topic Filter, Minimum Likes, official AI label, Preferred Content, settings search, Native Bridge guidance and reload action. Record exact TikTok package/build and scenarios. No versioning changes.

### M2 - close remaining settings/UI consistency

Fix only reproducible issues in touched StopSlop UI. Preserve unique inherited settings and working functionality.

### M3 - verify feed decision/ranking boundary

Verify metadata, feed routes, threads, timing and safe in-batch ranking. No ML inference.

### M4 - formalize hybrid moderation contract

Preserve `KEEP/BLOCK/UNKNOWN`, explicit exception precedence, positive-score separation, reasons and model/profile versioning. No fake AI control.

### M5 - TikTok Native Recommendation Bridge

**Reported:** `SOURCE_IMPLEMENTED`, device check pending. Guidance rows for Manage topics and Filter keywords exist; stable deep link not found for TikTok 47.9.3. Verify on A25. Never fake likes, watch time, views, follows or Not interested; no undocumented network endpoints or arbitrary server weights.

### M5B.1 - broad global model research registry

- Expand the candidate seed into a broad registry of multilingual and language-specialist models.
- Target 40+ credible candidates/families and 50+ language tags where real candidates exist; never fabricate a candidate to meet a quota.
- Record immutable source revision, task, BCP-47 languages/evidence, licence, package artifacts and total size, runtime/format, ABI and mobile readiness.
- Create `docs/research/AI_MODEL_CATALOG.md` plus a machine-readable research index if useful.
- No model download, runtime/UI install control or claim that candidate equals installable.

**Current result (26 November 2026): partial research pass.** The catalogue now covers 65 distinct model IDs across multilingual and regional candidates, and preserves a 52-tag explicitly enumerated language set from IBM Granite enhanced-retrieval evidence. Additional declared tags are not counted where the source card conflicts internally. The candidate expansion cites model cards and licences, but exact immutable commits and full weight/tokenizer/config byte manifests are still missing for many entries. Therefore M5B.1 is **not complete**, the Stable installable count remains zero, and every entry remains non-installable.

### M5B.2 - device profile and hardware compatibility scoring

- Verify host context, Android API, ABI, CPU/SoC where exposed, RAM class/free memory and storage.
- Define hard gates for API, ABI, runtime, model format, RAM and storage.
- Implement device-preference tiers 1-3 and a transparent 0-100 Hardware Fit index based on measured/calibrated data.
- A hard-incompatible model has no percentage; a model without measurements shows `Not measured`.
- Hardware Fit must never be confused with language quality/accuracy.

### M5B.3 - app-private data delivery and runtime canary

- Verify TikTok host `Context`, private `filesDir`/`cacheDir`, persistence and lifecycle.
- Test staged file download/hash/size/promotion/reopen/delete using harmless canary data.
- Prove a static runtime/JNI build-time path or verified platform runtime. Never download `.so` or DEX.
- Do not add broad storage permissions or use shared Downloads/SAF.
- No large model until the canary succeeds.

### M5B.4 - first ARM64 model comparison

- Test the smallest suitable specialist model and one broad multilingual embedding candidate.
- Re-export/quantize for ARM64 where required; an AVX2/AVX512 model is not Android mobile proof.
- Test Russian, Ukrainian, English and other declared languages on a fixed StopSlop dataset.
- Record full file/tokenizer/config size, RAM, cold start, latency, heat, battery and quality metrics.
- Embeddings and generators remain different model tasks.
- Only report `MODEL_INFERENCE_VERIFIED` after real offline inference in the patched host/faithful harness.

### M5B.5 - optional generator model comparison

Test compact multilingual generators only if reviewed alias generation remains a requirement. Measure valid structured output, hallucinations, per-language quality, package size, licence, runtime, RAM and latency. Require user review. Never run generation per feed item.

### M6 - signed Stable catalogue and optional Model Manager

Only after at least one real model/runtime path is proven. Ship broad searchable catalogue metadata, signed Stable manifests, dynamic languages, pinned exclusive Any language, quality 1-3, device preference 1-3, Hardware Fit %, model cards, Available/Installed/Updates, one-tap install, activation, switching, Stable update, rollback and deletion. Research-only candidates are visible only as `Not installable`; no fake Install buttons.

### M7 - global AI switch and per-feature AI options

Global AI defaults OFF. OFF guarantees zero model inference and preserves all mechanical filters. ON enables only implemented per-feature AI modules with an available compatible model. Keep TikTok's official AI label filter separate.

### M8 - Topic Filter semantic expansion/matching

Use a verified model to generate/review `BLOCK` topic profiles and/or semantically compare supported metadata. Direct aliases are separated from broad concepts. Generate only when the user adds/edits/refreshes a topic. Test all advertised languages.

### M9 - Preferred Content AI

Reuse model services with separate `PREFER` profiles. Accepted aliases/matches add only local in-batch score. They never hide nonmatches, override BLOCK, reorder protected slots or alter server requests.

### M10 - semantic content understanding

Add optional caption/description/hashtag semantic matching with measured feed-time budget, cache and off-UI inference. UNKNOWN means no block/no boost.

### M11 - in-video promotion detection

Mechanical caption/URL/promo-code/affiliate/CTA checks first. Add semantic classification only when model/runtime/input are proven. No segment skipping without timestamp/seek proof.

### M12 - official AI label, synthetic content and AI-slop

Keep platform label, probable synthetic detection and repetitive/low-quality AI-slop separate. Require evaluation, conservative thresholds, reason codes and overrides.

### M13 - multimodal feasibility

Subtitles/transcript/OCR/ASR require individual proof of allowed source, timing, privacy and performance.

### M14 - local history and feedback

Add bounded local decisions/reasons and feedback. Do not archive full videos by default.

### M15 - performance and device compatibility

Measure A25 then A12. Validate Hardware Fit against real device classes, memory, latency, thermal, battery and storage.

### M16 - final branding, documentation and release/versioning

After UI/runtime stabilizes, finish screenshots, README/privacy/feature matrix and independent StopSlop release numbering. Preserve Morphe compatibility metadata and rollback artifacts.

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

**Next: finish M5B.1 artifact pinning, then proceed to M5B.2.** Continue from the previous `BLOCKED_BY_FEASIBILITY` result without repeating the same general runtime audit.

1. Pin immutable revisions and complete file manifests (sizes, hashes, tokenizer/config companions and licences) for the research candidates selected for the first comparison.
2. Keep embeddings, classifiers, rerankers, translation models and generators distinct; include multilingual and regional specialists without implying equal quality.
3. Flag x86-only exports as not ARM-ready and keep all current candidates research-only.
4. Verify the device profile and host-private data canary, then prove a static native/runtime path and one ARM64 model offline.
5. Only after offline inference passes may the project begin M6's signed Stable catalogue and user-facing selector/installer.

Do not add a production model-install screen until the actual host package, runtime and data-file lifecycle work. Do not fetch executable code. The base TikTok/MPP stays small; model data are downloaded only after explicit selection and are never merged into the APK/MPP.

The final user flow is: choose languages (dynamic searchable list, pinned exclusive `🌍 Any language`), choose quality 1-3, choose device resource preference 1-3, browse the broad catalogue with hardware-fit score/evidence, install a selected Stable model, and later switch/update/delete it. `Not measured` and `Not installable` are valid honest catalogue states.

Mechanical Topic Filter, Preferred Content, Ads & Sponsorships and all non-AI features must remain functional without any model installed or when AI is disabled/unavailable.

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

An embedding model is not a generator. A text model is not video/audio understanding. Broad pretrained language coverage is not the same as per-language StopSlop benchmark proof. Parameter count is not exact download size, and Hardware Fit percentage is not model accuracy.
