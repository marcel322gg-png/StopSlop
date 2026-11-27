package app.morphe.extension.tiktok.feedfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class PreferredContentFilterTest {
    @Test
    public void parsesSemicolonTopicsAndPreservesPhrases() {
        PreferredContentFilter.CompiledTopics topics = PreferredContentFilter.parse(
                " Dota 2; science; DOTA 2; wildlife; ", 1);

        assertEquals(Arrays.asList("dota 2", "science", "wildlife"), topics.terms);
    }

    @Test
    public void ignoresEmptyEntriesTrailingSeparatorsAndNegativeSyntax() {
        PreferredContentFilter.CompiledTopics topics = PreferredContentFilter.parse(
                ";; politics; !politics; ; cs2;;;", 1);

        assertEquals(Arrays.asList("politics", "cs2"), topics.terms);
    }

    @Test
    public void scoreCountsDistinctTopicsOnceAndReturnsZeroForUnknownText() {
        PreferredContentFilter.CompiledTopics topics = PreferredContentFilter.parse("dota; dota 2;", 1);

        assertEquals(2, PreferredContentFilter.scoreCaption("Dota 2, Dota 2", topics));
        assertEquals(0, PreferredContentFilter.scoreCaption(null, topics));
        assertEquals(0, PreferredContentFilter.scoreCaption("other", topics));
    }

    @Test
    public void prioritizesStablyOnlyInEligibleSlots() {
        List<String> original = Arrays.asList("ordinary-a", "ad-slot", "ordinary-b", "structural", "ordinary-c");
        List<String> prioritized = PreferredContentFilter.prioritizeSlots(original,
                value -> value.startsWith("ordinary"),
                value -> value.equals("ordinary-a") ? 1 : value.equals("ordinary-b") ? 2 : 1);

        assertEquals(Arrays.asList("ordinary-b", "ad-slot", "ordinary-a", "structural", "ordinary-c"), prioritized);
        assertEquals("ad-slot", prioritized.get(1));
        assertEquals("structural", prioritized.get(3));
        assertFalse(prioritized.contains("removed"));
    }

    @Test
    public void nonmatchingEntriesStayEligibleAtTheBackAndTiesKeepInputOrder() {
        List<String> original = Arrays.asList("no-match", "match-a", "match-b");
        List<String> prioritized = PreferredContentFilter.prioritizeSlots(original,
                value -> true,
                value -> value.startsWith("match") ? 1 : 0);

        assertEquals(Arrays.asList("match-a", "match-b", "no-match"), prioritized);
        assertTrue(original.get(0).equals("no-match"));
    }
}
