package app.morphe.extension.tiktok.feedfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TopicFilterParserTest {
    private static TopicFilter.CompiledRules parse(String source) {
        return TopicFilter.parse(source, 1L);
    }

    @Test public void keepsMultiWordPhrasesAndTrimsEntries() {
        TopicFilter.CompiledRules rules = parse(" dota 2 ; counter strike ; ");
        assertTrue(rules.blockedPhrases.contains("dota 2"));
        assertTrue(rules.blockedPhrases.contains("counter strike"));
        assertEquals(2, rules.blockedPhrases.size());
    }

    @Test public void ignoresEmptyEntriesAndTrailingSeparators() {
        TopicFilter.CompiledRules rules = parse("; dota;;; ; politics; ");
        assertEquals(2, rules.blockedPhrases.size());
        assertTrue(parse(" ; ; ").blocksNothing());
    }

    @Test public void parsesExclusionsHashtagsAndCreatorAllowlist() {
        TopicFilter.CompiledRules rules = parse("!politics; #dota; !#dota; @Some_User;");
        assertTrue(rules.allowedPhrases.contains("politics"));
        assertTrue(rules.blockedHashtags.contains("dota"));
        assertTrue(rules.allowedHashtags.contains("dota"));
        assertTrue(rules.allowedCreators.contains("some_user"));
    }

    @Test public void acceptsLegacyNewlineSeparatedRulesDuringMigration() {
        TopicFilter.CompiledRules rules = parse("dota 2\r\n!politics\nanime");
        assertTrue(rules.blockedPhrases.contains("dota 2"));
        assertTrue(rules.allowedPhrases.contains("politics"));
        assertTrue(rules.blockedPhrases.contains("anime"));
    }
}
