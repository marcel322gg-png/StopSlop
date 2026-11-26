/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/feedfilter/LikeCountFilter.java
 */
package app.morphe.extension.tiktok.feedfilter;

import app.morphe.extension.tiktok.settings.Settings;
import com.ss.android.ugc.aweme.feed.model.Aweme;
import com.ss.android.ugc.aweme.feed.model.AwemeStatistics;

public final class LikeCountFilter implements IFilter {
    private static final int MAX_STOP_SLOP_THRESHOLD = 1_000_000;
    private static final long[] NO_LEGACY_RANGE = {0L, Long.MAX_VALUE};

    @Override
    public boolean getEnabled() {
        long[] minMax = legacyRange();
        return minMax[0] != 0L || minMax[1] != Long.MAX_VALUE
                || Settings.STOP_SLOP_MIN_LIKES.get();
    }

    @Override
    public boolean getFiltered(Aweme item) {
        try {
            AwemeStatistics statistics = item == null ? null : item.getStatistics();
            if (statistics == null) return false;

            long likeCount = statistics.getDiggCount();
            // Negative values cannot be a real like total, so treat them as unavailable.
            if (likeCount < 0L) return false;

            long[] minMax = legacyRange();
            boolean legacyRangeMatch = likeCount < minMax[0] || likeCount > minMax[1];
            int stopSlopThreshold = Settings.STOP_SLOP_MIN_LIKES.get()
                    ? Settings.STOP_SLOP_MIN_LIKES_THRESHOLD.get() : 0;
            boolean validStopSlopThreshold = stopSlopThreshold >= 0
                    && stopSlopThreshold <= MAX_STOP_SLOP_THRESHOLD;
            return legacyRangeMatch || validStopSlopThreshold
                    && stopSlopThreshold > 0 && likeCount < stopSlopThreshold;
        } catch (RuntimeException | LinkageError unreadableStatistics) {
            return false;
        }
    }

    /** Include live settings in the processed-list cache key so edits re-evaluate the next read. */
    String cacheToken() {
        long[] minMax = legacyRange();
        return minMax[0] + "-" + minMax[1] + ":"
                + Settings.STOP_SLOP_MIN_LIKES.get() + ":"
                + Settings.STOP_SLOP_MIN_LIKES_THRESHOLD.get();
    }

    /** Parse without repairing storage: feed evaluation must never write preferences to disk. */
    private static long[] legacyRange() {
        String[] minMax = Settings.MIN_MAX_LIKES.get().split("-", -1);
        if (minMax.length == 2) {
            try {
                long min = Long.parseLong(minMax[0].trim());
                long max = Long.parseLong(minMax[1].trim());
                if (min >= 0L && min <= max) return new long[]{min, max};
            } catch (NumberFormatException ignored) {
                // An unreadable range behaves like the default, without a hot-path write.
            }
        }
        return NO_LEGACY_RANGE;
    }

    /** Like counts may hydrate or change on an existing Aweme instance. */
    static int evidenceFingerprint(Aweme item) {
        if (item == null) return 0;
        try {
            AwemeStatistics statistics = item.getStatistics();
            return statistics == null ? 0 : Long.hashCode(statistics.getDiggCount());
        } catch (RuntimeException | LinkageError unreadableStatistics) {
            return 0;
        }
    }
}

