# StopSlop project context

StopSlop is a Morphe patch bundle for TikTok, not a standalone Android app. Morphe applies the bundle to TikTok; its `Settings` patch hooks the host settings activity and loads the injected TikTok preference fragment.

## Implemented features

- StopSlop feed filters hide TikTok ad and promotional markers, recognized AI-generation signals, and posts below a user-selected like count. They are registered in the feed filter path and controlled by `stop_slop_ad_sponsorship`, `stop_slop_ai_generated`, `stop_slop_min_likes`, and `stop_slop_min_likes_threshold`.
- The Minimum Likes switch defaults off, its threshold defaults to zero, and both preferences remain in the same settings hierarchy. The threshold is added before its dependency on `stop_slop_min_likes` is resolved.
- The StopSlop settings page can reopen TikTok's task. The inherited settings also expose feed filters and navigation, interface controls, playback, screen time, comments, downloads, sharing, inbox, privacy, region, app behavior, diagnostics, and backup/restore. Patch-dependent pages appear only when their support is present. About, support, pause, theme, language, search, and update controls remain available through the settings UI.

## Compatibility and attribution

Persisted setting keys, types, defaults, and preference storage belong to the existing extension settings system; retain them when changing UI. The project retains GPL-3.0 notices and attribution to its upstream Morphe/ReVanced-derived patch infrastructure in `LICENSE`, `NOTICE`, and source headers.

## Not implemented

- Local OCR or audio detection of sponsored integrations inside ordinary videos.
- User-defined topic filtering.
- Broad model-based AI detection or a separate repetitive/low-quality AI detector. The existing AI filter is limited to the signals implemented in `ContentMarkerFilters`.

## Brand screenshot follow-up

- TODO after the next successful device installation: recapture the 25 README settings screenshots from the real rebranded UI; see `StopSlop_Brand_Replacement_Kit.zip` → `manifest/README_SCREENSHOT_RECAPTURE.md` for exact paths, variants, and dimensions.
