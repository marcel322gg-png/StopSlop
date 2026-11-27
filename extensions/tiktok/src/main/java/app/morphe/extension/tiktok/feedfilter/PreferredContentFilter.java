/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.feedfilter;

import app.morphe.extension.tiktok.settings.Settings;
import com.ss.android.ugc.aweme.feed.model.Aweme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Positive, metadata-only priority ordering for ordinary videos in one For You response batch. */
final class PreferredContentFilter implements IFilter {
    private static volatile CompiledTopics cachedTopics;

    @Override
    public boolean getEnabled() {
        return Settings.STOP_SLOP_PREFERRED_CONTENT.get();
    }

    /** This preference never blocks. Ranking is applied after the blocking/range decisions. */
    @Override
    public boolean getFiltered(Aweme item) {
        return false;
    }

    String cacheToken() {
        return Long.toString(compiled(Settings.STOP_SLOP_PREFERRED_CONTENT_RULES.get()).version);
    }

    List prioritize(List kept, FeedItemsFilter.AwemeExtractor extractor) {
        CompiledTopics topics = compiled(Settings.STOP_SLOP_PREFERRED_CONTENT_RULES.get());
        if (topics.terms.isEmpty() || kept.size() < 2) return kept;
        return prioritizeSlots(kept,
                container -> eligible(extractor.extract(container)),
                container -> score(extractor.extract(container), topics));
    }

    static CompiledTopics parse(String source, long version) {
        Set<String> terms = new LinkedHashSet<>();
        String value = source == null ? "" : source;
        if (value.length() > FeedRuleLimits.MAX_UTF8_BYTES
                || FeedRuleLimits.captionViolation(value) != null) {
            return new CompiledTopics(value, version, Collections.emptyList());
        }
        for (String raw : value.split(";", -1)) {
            String term = raw.trim().toLowerCase(Locale.ROOT);
            if (term.isEmpty() || term.charAt(0) == '!') continue;
            terms.add(term);
        }
        return new CompiledTopics(value, version, new ArrayList<>(terms));
    }

    private static CompiledTopics compiled(String source) {
        String value = source == null ? "" : source;
        CompiledTopics current = cachedTopics;
        if (current != null && current.source.equals(value)) return current;
        synchronized (PreferredContentFilter.class) {
            current = cachedTopics;
            if (current == null || !current.source.equals(value)) {
                long version = current == null ? 1 : current.version + 1;
                current = parse(value, version);
                cachedTopics = current;
            }
            return current;
        }
    }

    private static boolean eligible(Aweme item) {
        if (item == null) return false;
        try {
            // Unknown and special item shapes remain fixed in place. Type 0 is TikTok's ordinary
            // video item; ads, lives, and inserted cards are explicitly excluded as well.
            return item.getAwemeType() == 0 && !item.isAd() && !item.isSoftAd()
                    && !item.getIsTikTokStory() && !LiveFilter.isLive(item)
                    && !CardFilters.InsertedCardFilter.isRecUserBigCard(item)
                    && !new ContentMarkerFilters.SeriesFilter().getFiltered(item);
        } catch (Throwable unavailableMetadata) {
            return false;
        }
    }

    private static int score(Aweme item, CompiledTopics topics) {
        if (!eligible(item)) return 0;
        try {
            return scoreCaption(item.getDesc(), topics);
        } catch (Throwable unavailableMetadata) {
            return 0;
        }
    }

    static int scoreCaption(String caption, CompiledTopics topics) {
        if (caption == null || caption.isEmpty()) return 0;
        String folded = caption.toLowerCase(Locale.ROOT);
        int matched = 0;
        for (String term : topics.terms) if (folded.contains(term)) matched++;
        return matched;
    }

    /** Stable-sort only eligible slots; every other item keeps the same output index. */
    static <T> List<T> prioritizeSlots(List<T> source, Eligibility<T> eligible, Score<T> score) {
        List<Ranked<T>> ranked = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int index = 0; index < source.size(); index++) {
            T value = source.get(index);
            if (!eligible.test(value)) continue;
            slots.add(index);
            ranked.add(new Ranked<>(value, score.score(value), index));
        }
        if (ranked.size() < 2) return source;
        // Collections.sort is stable and available at the extension's API 23 floor.
        Collections.sort(ranked, (left, right) -> {
            int byScore = Integer.compare(right.score, left.score);
            return byScore != 0 ? byScore : Integer.compare(left.originalIndex, right.originalIndex);
        });
        List<T> result = new ArrayList<>(source);
        for (int index = 0; index < slots.size(); index++) result.set(slots.get(index), ranked.get(index).value);
        return result;
    }

    // The extension still supports API 23, so use local interfaces instead of API 24 functional types.
    interface Eligibility<T> {
        boolean test(T value);
    }

    interface Score<T> {
        int score(T value);
    }

    private static final class Ranked<T> {
        final T value;
        final int score;
        final int originalIndex;

        Ranked(T value, int score, int originalIndex) {
            this.value = value;
            this.score = score;
            this.originalIndex = originalIndex;
        }
    }

    static final class CompiledTopics {
        final String source;
        final long version;
        final List<String> terms;

        CompiledTopics(String source, long version, List<String> terms) {
            this.source = source;
            this.version = version;
            this.terms = Collections.unmodifiableList(terms);
        }
    }
}
