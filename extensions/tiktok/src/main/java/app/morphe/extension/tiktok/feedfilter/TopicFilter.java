/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.feedfilter;

import app.morphe.extension.tiktok.blockauthor.Reflect;
import app.morphe.extension.tiktok.settings.Settings;
import app.morphe.extension.shared.Utils;

import com.ss.android.ugc.aweme.feed.model.Aweme;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Deterministic caption and creator rules using only metadata present on the feed item. */
final class TopicFilter implements IFilter {
    private static volatile CompiledRules cachedRules;
    private static long nextRulesVersion;

    @Override
    public boolean getEnabled() {
        try {
            CompiledRules rules = compiled(Settings.STOP_SLOP_TOPIC_RULES.get());
            boolean semanticEnabled = Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED.get()
                    && !rules.semanticTopics().isEmpty()
                    && StopSlopModelManager.hasActiveModel(Utils.getContext());
            LocalContentInference.setEnabled(semanticEnabled);
            if (semanticEnabled) {
                LocalContentInference.updateTopicConfiguration(Long.toString(rules.version), semanticThreshold());
            }
            return (Settings.STOP_SLOP_TOPIC_FILTER.get() && !rules.blocksNothing())
                    || semanticEnabled;
        } catch (Throwable unavailableRules) {
            LocalContentInference.setEnabled(false);
            return false;
        }
    }

    @Override
    public boolean getFiltered(Aweme item) {
        try {
            CompiledRules rules = compiled(Settings.STOP_SLOP_TOPIC_RULES.get());
            boolean mechanicalEnabled = Settings.STOP_SLOP_TOPIC_FILTER.get();
            boolean semanticEnabled = Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED.get()
                    && StopSlopModelManager.hasActiveModel(Utils.getContext());
            if ((!mechanicalEnabled || rules.blocksNothing())
                    && (!semanticEnabled || rules.semanticTopics().isEmpty())) return false;
            String caption = Reflect.string(item, "getDesc", "desc");
            if (caption == null) return false;
            String foldedCaption = caption.toLowerCase(Locale.ROOT);
            if (matchesPhrase(foldedCaption, rules.allowedPhrases)
                    || matchesHashtag(caption, rules.allowedHashtags)) {
                return false;
            }

            boolean topicMatch = matchesPhrase(foldedCaption, rules.blockedPhrases)
                    || matchesHashtag(caption, rules.blockedHashtags);
            if (mechanicalEnabled && topicMatch) {
                if (rules.allowedCreators.isEmpty()) return true;
                CreatorIdentity creator = CreatorIdentity.of(item);
                // A missing handle makes the allowlist decision unknown, so keep the post.
                if (creator.normalizedHandle.isEmpty()
                        || rules.allowedCreators.contains(creator.normalizedHandle)) return false;
                return true;
            }

            if (!semanticEnabled) return false;
            if (!rules.allowedCreators.isEmpty()) {
                CreatorIdentity creator = CreatorIdentity.of(item);
                if (creator.normalizedHandle.isEmpty()
                        || rules.allowedCreators.contains(creator.normalizedHandle)) return false;
            }
            List<String> semanticTopics = rules.semanticTopics();
            if (semanticTopics.isEmpty()) return false;
            LocalContentInference.setEnabled(true);
            float threshold = semanticThreshold();
            LocalContentInference.updateTopicConfiguration(Long.toString(rules.version), threshold);
            LocalContentInference.Features features = LocalContentInference.extract(item);
            if (features == null) return false;
            LocalContentInference.FilterDecision decision = LocalContentInference.request(
                    features, semanticTopics, Long.toString(rules.version),
                    threshold, ignored -> { });
            return decision.outcome == LocalContentInference.Outcome.BLOCK;
        } catch (Throwable unavailableMetadata) {
            // Missing, unreadable or changed TikTok metadata must never hide a post.
            return false;
        }
    }

    private static float semanticThreshold() {
        try {
            float value = Float.parseFloat(Settings.STOP_SLOP_TOPIC_SEMANTIC_THRESHOLD.get());
            return value >= 0.50f && value <= 0.98f ? value : 0.84f;
        } catch (RuntimeException invalidSetting) {
            return 0.84f;
        }
    }

    /** A non-content token so a saved rules edit invalidates an already-processed feed list. */
    String cacheToken() {
        try {
            return Long.toString(compiled(Settings.STOP_SLOP_TOPIC_RULES.get()).version);
        } catch (Throwable unavailableRules) {
            return "unknown";
        }
    }

    private static CompiledRules compiled(String source) {
        String value = source == null ? "" : source;
        CompiledRules current = cachedRules;
        if (current != null && current.source.equals(value)) return current;
        synchronized (TopicFilter.class) {
            current = cachedRules;
            if (current == null || !current.source.equals(value)) {
                current = parse(value, ++nextRulesVersion);
                cachedRules = current;
            }
            return current;
        }
    }

    static CompiledRules parse(String source, long version) {
        Set<String> blockedPhrases = new LinkedHashSet<>();
        Set<String> blockedHashtags = new LinkedHashSet<>();
        Set<String> allowedPhrases = new LinkedHashSet<>();
        Set<String> allowedHashtags = new LinkedHashSet<>();
        Set<String> allowedCreators = new LinkedHashSet<>();

        if (source.length() > FeedRuleLimits.MAX_UTF8_BYTES
                || FeedRuleLimits.captionViolation(source) != null) {
            return new CompiledRules(source, version, blockedPhrases, blockedHashtags,
                    allowedPhrases, allowedHashtags, allowedCreators);
        }

        for (String raw : source.split("[;\\r\\n]", -1)) {
            String rule = raw.trim();
            if (rule.isEmpty()) continue;
            if (rule.charAt(0) == '@') {
                String handle = rule.substring(1).trim();
                if (validUsername(handle)) allowedCreators.add(AdvancedFeedRules.normalizedCreator(handle));
                continue;
            }

            boolean exception = rule.charAt(0) == '!';
            if (exception) rule = rule.substring(1).trim();
            if (rule.isEmpty() || rule.charAt(0) == '@' || rule.charAt(0) == '!') continue;

            if (rule.charAt(0) == '#') {
                String hashtag = hashtagRule(rule.substring(1));
                if (hashtag == null) continue;
                (exception ? allowedHashtags : blockedHashtags).add(hashtag);
            } else {
                String phrase = rule.toLowerCase(Locale.ROOT);
                (exception ? allowedPhrases : blockedPhrases).add(phrase);
            }
        }
        return new CompiledRules(source, version, blockedPhrases, blockedHashtags,
                allowedPhrases, allowedHashtags, allowedCreators);
    }

    private static boolean validUsername(String value) {
        if (value.isEmpty()) return false;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (!Character.isLetterOrDigit(codePoint) && codePoint != '.' && codePoint != '_') return false;
            offset += Character.charCount(codePoint);
        }
        return true;
    }

    private static String hashtagRule(String value) {
        if (value.isEmpty()) return null;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (!isHashtagCharacter(codePoint)) return null;
            offset += Character.charCount(codePoint);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static boolean matchesPhrase(String caption, Set<String> phrases) {
        for (String phrase : phrases) if (caption.contains(phrase)) return true;
        return false;
    }

    private static boolean matchesHashtag(String caption, Set<String> hashtags) {
        if (hashtags.isEmpty()) return false;
        for (int marker = caption.indexOf('#'); marker >= 0; marker = caption.indexOf('#', marker + 1)) {
            if (marker > 0 && isHashtagCharacter(caption.codePointBefore(marker))) continue;
            int end = marker + 1;
            while (end < caption.length()) {
                int codePoint = caption.codePointAt(end);
                if (!isHashtagCharacter(codePoint)) break;
                end += Character.charCount(codePoint);
            }
            if (end > marker + 1 && hashtags.contains(caption.substring(marker + 1, end).toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHashtagCharacter(int codePoint) {
        int type = Character.getType(codePoint);
        return Character.isLetterOrDigit(codePoint) || codePoint == '_'
                || type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK;
    }

    static final class CompiledRules {
        final String source;
        final long version;
        final Set<String> blockedPhrases;
        final Set<String> blockedHashtags;
        final Set<String> allowedPhrases;
        final Set<String> allowedHashtags;
        final Set<String> allowedCreators;

        CompiledRules(String source, long version, Set<String> blockedPhrases, Set<String> blockedHashtags,
                Set<String> allowedPhrases, Set<String> allowedHashtags, Set<String> allowedCreators) {
            this.source = source;
            this.version = version;
            this.blockedPhrases = Collections.unmodifiableSet(blockedPhrases);
            this.blockedHashtags = Collections.unmodifiableSet(blockedHashtags);
            this.allowedPhrases = Collections.unmodifiableSet(allowedPhrases);
            this.allowedHashtags = Collections.unmodifiableSet(allowedHashtags);
            this.allowedCreators = Collections.unmodifiableSet(allowedCreators);
        }

        boolean blocksNothing() {
            return blockedPhrases.isEmpty() && blockedHashtags.isEmpty();
        }

        /** Bounded positive terms for semantic matching; exclusions and creator rules never become topics. */
        List<String> semanticTopics() {
            List<String> result = new ArrayList<>(16);
            Set<String> seen = new LinkedHashSet<>();
            for (String raw : source.split("[;\\r\\n]", -1)) {
                String rule = raw.trim();
                if (rule.isEmpty() || rule.charAt(0) == '@' || rule.charAt(0) == '!') continue;
                String topic = rule.charAt(0) == '#' ? rule.substring(1).trim() : rule;
                String canonical = topic.toLowerCase(Locale.ROOT);
                if (topic.isEmpty() || seen.contains(canonical)) continue;
                if (rule.charAt(0) == '#') {
                    if (!blockedHashtags.contains(canonical)) continue;
                } else if (!blockedPhrases.contains(canonical)) {
                    continue;
                }
                seen.add(canonical);
                result.add(topic);
                if (result.size() == 16) break;
            }
            return Collections.unmodifiableList(result);
        }
    }
}
