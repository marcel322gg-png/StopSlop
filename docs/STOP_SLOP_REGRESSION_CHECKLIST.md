# StopSlop regression checklist

Use this checklist when changing feed filters, settings, or model management.

## Ads & Sponsorship

- `stop_slop_ad_sponsorship` defaults to `true`; the StopSlop toggle persists this key.
- `remove_ads` remains an independent legacy control. Either enabled setting activates the shared TikTok ad metadata detector.
- `hide_paid_partnership` and `hide_promotional_music` remain independently controlled legacy filters.
- Lemon8 and drama ad request hooks remain enabled when either `remove_ads` or StopSlop Ads & Sponsorship is enabled.
- StopSlop disclosure matching may use explicit TikTok labels/metadata and bounded caption, hashtag, sticker, and commerce evidence. Missing metadata fails open.

## AI-generated markers

- `stop_slop_ai_generated` defaults to `false`; `hide_ai_generated` is retained for compatibility.
- The StopSlop toggle reads both saved values and writes both keys.
- The marker filter is registered in the shared feed-filter list. It only matches explicit TikTok AI metadata/effect markers or recognized AI hashtags; it does not classify pixels, audio, or language.
- AI-marker detection is separate from E5 semantic topic matching.

## Deterministic and semantic topic filters

- Deterministic topic/creator rules use `stop_slop_topic_filter` and `stop_slop_topic_rules`; unknown text or creator metadata fails open.
- Semantic rules require the separate semantic toggle, saved semantic topics, a selected compatible model, and a ready local inference provider. Inference is local and fail-open.
- Preferred Content remains a For You ordering rule and does not replace blocking rules.

## Runtime and diagnostics

- The feed patch must keep both response and model-read filtering call sites active; verify `FeedFilterPatch` injection and `FeedItemsFilter.CONTENT_FILTERS` registration.
- Read each detector's enabled state and decision independently. A detector exception or linkage mismatch must be logged with bounded, content-free diagnostics and must not skip other detectors.
- Preserve all preference keys/defaults and legacy behavior across MPP updates; migrations must not overwrite an already stored StopSlop value.
- Model state must distinguish installed, selected, initializing, ready, and unavailable. `Ready` requires successful ONNX session initialization. Switching must reuse installed files; removal must transition selection safely first.

## Release audit

- Build and inspect the release MPP; confirm expected runtime resources, no model weights/tokenizer assets in the model-free package, and no duplicate archive entries.
- Record the artifact size and SHA-256. Compilation alone is not device verification.
