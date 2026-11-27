/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/feedfilter/AdsFilter.java
 */
package app.morphe.extension.tiktok.feedfilter;

import app.morphe.extension.shared.diagnostics.FeedFilterCounters;
import app.morphe.extension.tiktok.blockauthor.Reflect;
import app.morphe.extension.tiktok.settings.Settings;
import com.ss.android.ugc.aweme.feed.model.Aweme;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdsFilter implements IFilter {
    private static final String DISCLOSURE_HOOK_FAMILY = "feed ad disclosures";
    private static final String PANEL_DISCLOSURE = "panel_top_disclosure_label";
    private static final int MAX_CAPTION_LENGTH = 2048;
    private static final int MAX_TAGS = 64;
    private static final int MAX_STICKERS = 16;
    private static final int MAX_TAG_LENGTH = 128;
    private static final int MAX_STICKER_TEXT_LENGTH = 512;
    private static final String SHOP_INFO = "placeholder_product_id";
    private static final Pattern URL_PATTERN = Pattern.compile("(?:https?://|www\\.)[^\\s]{3,}");
    private static final Pattern PROMO_CODE_PATTERN = Pattern.compile(
            "(?:promo code|discount code|affiliate code|creator code|referral code|partner code|"
                    + "use (?:my )?code|save with code|промокод|код скидки|код знижки|код автора|код блогера|"
                    + "slevovy kod|partnersky kod)"
                    + "\\s*[:#-]?\\s*([\\p{L}\\p{N}][\\p{L}\\p{N}_-]{2,})");
    private static final Set<String> DISCLOSURE_TAGS = Set.of(
            "ad", "advertisement", "sponsored", "spon", "paidpartnership", "paidpromotion", "paidpromo",
            "commercial", "branddeal", "brandpartner", "brandedcontent", "promotionalcontent", "gifted",
            "giftedby", "giftedcollab", "prpackage", "pressgift", "affiliate", "affiliatelink", "commission",
            "commissionearned", "brandambassador", "ambassador", "реклама", "спонсоровано", "партнерство",
            "платнепартнерство", "бартер", "подароване", "рекламнаінтеграція", "платноепартнерство",
            "подарок", "рекламнаяинтеграция", "spoluprace", "placenepartnerstvi", "placenaspoluprace",
            "darovano", "partnerstvi");
    private static final Set<String> DISCLOSURE_PHRASES = Set.of(
            "ad", "advertisement", "paid partnership", "promotional content", "sponsored by", "brand partnership",
            "gifted collaboration", "placena spoluprace", "placene partnerstvi", "reklama", "реклама",
            "платне партнерство", "спонсоровано", "платное партнерство");
    private static final Set<String> PURCHASE_CALLS_TO_ACTION = Set.of(
            "buy now", "shop now", "order now", "get yours", "use my link", "shop today", "buy today",
            "купити зараз", "замовляй", "замовити", "купить сейчас", "заказывай", "kupte nyni", "objednejte");
    private static final Set<String> PRODUCT_OR_DEMO_WORDING = Set.of(
            "product", "unboxing", "review", "demo", "tutorial", "recommendation", "try this", "new collection",
            "продукт", "распаковка", "обзор", "демо", "купуй", "товар", "розпаковка", "огляд", "kupte", "recenze");
    private static final Set<String> NON_CODE_WORDS = Set.of(
            "for", "with", "here", "below", "today", "now", "link", "save", "discount", "code", "please", "enter");
    /** The filter report's count of ads caught by a signal other than TikTok's own ad flag. */
    static final String AD_SIGNALS_SOURCE = "AdSignals";

    @Override
    public boolean getEnabled() {
        return Settings.STOP_SLOP_AD_SPONSORSHIP.get() && Settings.REMOVE_ADS.get();
    }

    @Override
    public boolean getFiltered(Aweme item) {
        if (!Settings.STOP_SLOP_AD_SPONSORSHIP.get()) return false;
        return item.isAd()
                || item.isSoftAd()
                || item.getAwemeRawAd() != null
                || item.isWithPromotionalMusic()
                || isPseudoAd(item)
                || isLemon8PromoCard(item)
                || hasCreatorCommissionDisclosure(item)
                || ContentMarkerFilters.hasPaidPartnershipMarker(item);
    }

    /** StopSlop's wider rule, shared by the existing feed-filter registration and cache path. */
    static boolean isStopSlopAdOrSponsorship(Aweme item) {
        if (item == null) return false;
        try {
            // These flags apply to Aweme irrespective of whether its media is a video or photo carousel.
            if (item.isAd() || item.isSoftAd() || item.getAwemeRawAd() != null
                    || item.isWithPromotionalMusic() || isPseudoAd(item) || isLemon8PromoCard(item)
                    || ShopFilter.isTikTokShopAd(item)) {
                return true;
            }
            if (hasCreatorCommissionDisclosure(item)
                    || ContentMarkerFilters.hasExplicitPaidPartnershipMarker(item)) {
                return true;
            }
            return hasDisclosureTextOrCombinedCommerceEvidence(item);
        } catch (RuntimeException | LinkageError ignored) {
            // An unreadable candidate is left in the feed. This predicate is called on a hot path.
            return false;
        }
    }

    /** StopSlop-only text and structured commerce evidence, separate from legacy flag detectors. */
    static boolean hasStopSlopDisclosure(Aweme item) {
        if (item == null) return false;
        try {
            return hasDisclosureTextOrCombinedCommerceEvidence(item);
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean hasDisclosureTextOrCombinedCommerceEvidence(Aweme item) {
        DisclosureText evidence = new DisclosureText();
        String caption = limited(Reflect.string(item, "getDesc", "desc"), MAX_CAPTION_LENGTH);
        inspectText(caption, evidence);
        inspectCaptionHashtags(caption, evidence);

        Object extras = Reflect.property(item, "getTextExtra", "textExtra");
        if (extras instanceof Collection) {
            int count = 0;
            for (Object extra : (Collection<?>) extras) {
                if (count++ >= MAX_TAGS) break;
                String tag = limited(Reflect.string(extra, "getHashTagName", "hashTagName"), MAX_TAG_LENGTH);
                if (tag == null) continue;
                String normalizedTag = normalize(tag.startsWith("#") ? tag.substring(1) : tag);
                if (isDisclosureTag(normalizedTag)) evidence.explicitDisclosure = true;
                inspectText(tag, evidence);
            }
        }

        Object stickers = Reflect.property(item, "getInteractStickerStructs", "interactStickerStructs");
        if (stickers instanceof Collection) {
            int count = 0;
            for (Object sticker : (Collection<?>) stickers) {
                if (count++ >= MAX_STICKERS) break;
                Object type = Reflect.property(sticker, "getType", "type");
                if (!(type instanceof Number)) continue;
                int kind = ((Number) type).intValue();
                if (kind != AdvancedFeedRules.TEXT_STICKER_TYPE
                        && kind != AdvancedFeedRules.CAPTIONED_TEXT_STICKER_TYPE) continue;
                inspectText(limited(Reflect.string(sticker, "getTextStruct", "textStruct"), MAX_STICKER_TEXT_LENGTH),
                        evidence);
            }
        }

        if (evidence.explicitDisclosure) return true;

        String shareUrl = item.getShareUrl();
        boolean commerceAnchor = shareUrl != null && shareUrl.contains(SHOP_INFO);
        // Weak cues need two independent signals and a specific code or TikTok Shop anchor.
        return evidence.promoCode
                        && (evidence.purchaseCallToAction || evidence.url || evidence.productOrDemo || commerceAnchor)
                || commerceAnchor && (evidence.purchaseCallToAction || evidence.url || evidence.productOrDemo);
    }

    private static void inspectText(String value, DisclosureText evidence) {
        if (value == null || value.isEmpty()) return;
        String normalized = normalize(value);
        if (containsAnyPhrase(normalized, DISCLOSURE_PHRASES)) evidence.explicitDisclosure = true;
        if (containsAnyPhrase(normalized, PURCHASE_CALLS_TO_ACTION)) evidence.purchaseCallToAction = true;
        if (containsAnyPhrase(normalized, PRODUCT_OR_DEMO_WORDING)) evidence.productOrDemo = true;
        if (URL_PATTERN.matcher(value).find()) evidence.url = true;

        Matcher code = PROMO_CODE_PATTERN.matcher(normalized);
        while (code.find()) {
            String token = code.group(1);
            if (!NON_CODE_WORDS.contains(token)) {
                evidence.promoCode = true;
                break;
            }
        }
    }

    private static void inspectCaptionHashtags(String caption, DisclosureText evidence) {
        if (caption == null) return;
        for (int at = caption.indexOf('#'); at >= 0; at = caption.indexOf('#', at + 1)) {
            int end = at + 1;
            while (end < caption.length()) {
                int codePoint = caption.codePointAt(end);
                int type = Character.getType(codePoint);
                if (!Character.isLetterOrDigit(codePoint) && codePoint != '_' && codePoint != '-'
                        && type != Character.NON_SPACING_MARK && type != Character.COMBINING_SPACING_MARK) break;
                end += Character.charCount(codePoint);
            }
            if (end > at + 1 && isDisclosureTag(normalize(caption.substring(at + 1, end)))) {
                evidence.explicitDisclosure = true;
                return;
            }
        }
    }

    private static boolean isDisclosureTag(String normalizedTag) {
        return DISCLOSURE_TAGS.contains(normalizedTag) || DISCLOSURE_PHRASES.contains(normalizedTag);
    }

    private static boolean containsAnyPhrase(String normalizedText, Set<String> phrases) {
        if (normalizedText.isEmpty()) return false;
        String padded = " " + normalizedText + " ";
        for (String phrase : phrases) {
            if (padded.contains(" " + phrase + " ")) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD).toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(decomposed.length());
        boolean separator = true;
        for (int offset = 0; offset < decomposed.length();) {
            int codePoint = decomposed.codePointAt(offset);
            offset += Character.charCount(codePoint);
            int type = Character.getType(codePoint);
            if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                    || type == Character.ENCLOSING_MARK) continue;
            if (Character.isLetterOrDigit(codePoint)) {
                normalized.appendCodePoint(codePoint);
                separator = false;
            } else if (!separator) {
                normalized.append(' ');
                separator = true;
            }
        }
        int length = normalized.length();
        if (length > 0 && normalized.charAt(length - 1) == ' ') normalized.setLength(length - 1);
        return normalized.toString();
    }

    private static String limited(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static final class DisclosureText {
        boolean explicitDisclosure;
        boolean purchaseCallToAction;
        boolean productOrDemo;
        boolean url;
        boolean promoCode;
    }

    /** The CardInsertInfo card type of the Lemon8 install promo card TikTok slots into For You. */
    static final int LEMON8_CARD_TYPE = 9;

    /** The awemeType of every card TikTok inserts between videos. */
    static final int INSERT_CARD_AWEME_TYPE = 105;

    /** CardInsertInfo's impl type for a card drawn by a Lynx template. */
    static final int LYNX_CARD_IMPL_TYPE = 2;

    /**
     * The card types of the Lynx Lemon8 card ("Your daily dose of inspiration", an embedded
     * Lemon8 post with Not interested and Open Lemon8). TikTok's own filter_lemon8_lynx_card
     * list step (47.5.3) drops a Lynx insert of 40 or 114 from a cached response and of 105 from
     * a live one. A type 40 card reached For You on the S22 on 2026-10-23.
     */
    static final java.util.Set<Integer> LEMON8_LYNX_CARD_TYPES =
            java.util.Collections.unmodifiableSet(new java.util.HashSet<>(java.util.Arrays.asList(40, 105, 114)));

    /**
     * The Lemon8 cards. Neither is a video and none of TikTok's ad flags are set on them. The
     * full-screen "Find fresh ideas on Lemon8" card's handler asks for card type 9 and the
     * server's answer carries the same number. The Lynx card is a type 105 insert drawn by a Lynx
     * template whose card type is one TikTok's own Lemon8 Lynx filter names.
     */
    static boolean isLemon8PromoCard(Aweme item) {
        Object insert = Reflect.property(item, "getCardInsertInfo", "cardInsertInfo");
        if (insert == null) return false;
        Object type = Reflect.property(insert, "getCardType", "cardType");
        if (!(type instanceof Number)) return false;
        int cardType = ((Number) type).intValue();
        boolean lemon = cardType == LEMON8_CARD_TYPE || isLemon8LynxCard(item, insert, cardType);
        if (lemon) FeedFilterCounters.sawKind(AD_SIGNALS_SOURCE, "lemon8 promo card");
        return lemon;
    }

    private static boolean isLemon8LynxCard(Aweme item, Object insert, int cardType) {
        if (!LEMON8_LYNX_CARD_TYPES.contains(cardType) || item.getAwemeType() != INSERT_CARD_AWEME_TYPE) return false;
        Object impl = Reflect.property(insert, "getCardImplType", "cardImplType");
        return impl instanceof Number && ((Number) impl).intValue() == LYNX_CARD_IMPL_TYPE;
    }

    /**
     * A creator's own post that TikTok runs as an ad. isAd and the raw ad stay empty on it, so
     * TikTok's AwemeExtKt.isPseudoAd reads the commerce struct instead, and needs both its
     * isPseudoAd flag and the ad data it carries: either one alone is not an ad to TikTok. This
     * is that same test. The struct itself rides along on most of the feed.
     */
    static boolean isPseudoAd(Aweme item) {
        Object commerce = Reflect.property(item, "getCommerceVideoAuthInfo", "commerceVideoAuthInfo");
        if (commerce == null) return false;
        boolean pseudo = Boolean.TRUE.equals(Reflect.property(commerce, "isPseudoAd", "isPseudoAd"))
                && Reflect.property(commerce, "getPseudoAdData", "pseudoAdData") != null;
        if (pseudo) FeedFilterCounters.sawKind(AD_SIGNALS_SOURCE, "pseudo ad");
        return pseudo;
    }

    /**
     * TikTok builds the visible creator-commission label from this serialized anchor model.
     * Reading its stable JSON keys avoids depending on translated disclosure text or an
     * obfuscated renderer class.
     */
    static boolean hasCreatorCommissionDisclosure(Aweme item) {
        if (hasLocalAllianceDisclosure(item)) return true;
        String extras = anchorsExtras(item);
        if (extras == null) return false;
        if (extras.isEmpty() || !extras.contains("\"" + PANEL_DISCLOSURE + "\"")) return false;

        try {
            JSONObject disclosure = new JSONObject(extras).optJSONObject(PANEL_DISCLOSURE);
            return disclosure != null
                    && (nonBlankString(disclosure.opt("display_text"))
                    || nonBlankString(disclosure.opt("truncatable_text")));
        } catch (JSONException ignored) {
            // Anchor metadata comes from the network. A malformed item is ordinary feed content,
            // not evidence that the hook contract changed.
            return false;
        }
    }

    /**
     * The location-affiliate label uses ContentModel, not shopping's anchorsExtras.
     * PoiCreatorBottomLabelTriggerAssem reads this same named model chain before rendering.
     * showBottomLabel requires allianceItemLabelType == 1 and nonempty label text; a location
     * or an incomplete alliance object alone is not ad evidence. Read at list delivery so
     * the video is removed before the renderer runs, independently of locale or its UI gate.
     * TikTok Lite's Aweme has no ContentModel at all (47.8.2), so there's nothing to read there
     * and nothing to report missing (#48).
     */
    private static boolean hasLocalAllianceDisclosure(Aweme item) {
        if (!FeedItemsFilter.tiktokFeedItemListPresent) return false;
        Object content = Reflect.required(item, "getContentModel", DISCLOSURE_HOOK_FAMILY);
        Object business = Reflect.requiredField(content, "standardBusinessModel", DISCLOSURE_HOOK_FAMILY);
        Object alliance = Reflect.required(business, "getLocalAllianceInfo", DISCLOSURE_HOOK_FAMILY);
        return Boolean.TRUE.equals(Reflect.required(alliance, "showBottomLabel", DISCLOSURE_HOOK_FAMILY));
    }

    /**
     * A cheap signature for ad metadata TikTok can fill after the first delivery callback.
     * FeedItemsFilter uses it to distinguish a true duplicate list from the same Aweme objects
     * after a cache or hydration pass added disclosure data.
     */
    static int evidenceFingerprint(Aweme item) {
        if (item == null) return 0;
        int result = 1;
        try {
            result = 31 * result + (item.isAd() ? 1 : 0);
            result = 31 * result + (item.isSoftAd() ? 1 : 0);
            Object rawAd = item.getAwemeRawAd();
            result = 31 * result + (rawAd == null ? 0 : System.identityHashCode(rawAd));
            result = 31 * result + (item.isWithPromotionalMusic() ? 1 : 0);
        } catch (Throwable ignored) {
            // A host getter that fails must not stop a feed delivery. The serialized disclosure
            // and branded-content checks below can still invalidate the duplicate-list cache.
        }
        String extras = anchorsExtras(item);
        result = 31 * result + (extras == null ? 0 : extras.hashCode());
        result = 31 * result + (hasLocalAllianceDisclosure(item) ? 1 : 0);
        result = 31 * result + (ContentMarkerFilters.hasPaidPartnershipMarker(item) ? 1 : 0);
        result = 31 * result + (ContentMarkerFilters.hasExplicitPaidPartnershipMarker(item) ? 1 : 0);
        result = 31 * result + (hasCreatorCommissionDisclosure(item) ? 1 : 0);
        result = 31 * result + textEvidenceFingerprint(item);
        return result;
    }

    private static int textEvidenceFingerprint(Aweme item) {
        int result = 1;
        try {
            String caption = limited(Reflect.string(item, "getDesc", "desc"), MAX_CAPTION_LENGTH);
            result = 31 * result + (caption == null ? 0 : caption.hashCode());
            Object extras = Reflect.property(item, "getTextExtra", "textExtra");
            if (extras instanceof Collection) {
                int count = 0;
                for (Object extra : (Collection<?>) extras) {
                    if (count++ >= MAX_TAGS) break;
                    String tag = limited(Reflect.string(extra, "getHashTagName", "hashTagName"), MAX_TAG_LENGTH);
                    result = 31 * result + (tag == null ? 0 : tag.hashCode());
                }
            }
            Object stickers = Reflect.property(item, "getInteractStickerStructs", "interactStickerStructs");
            if (stickers instanceof Collection) {
                int count = 0;
                for (Object sticker : (Collection<?>) stickers) {
                    if (count++ >= MAX_STICKERS) break;
                    Object type = Reflect.property(sticker, "getType", "type");
                    if (!(type instanceof Number)) continue;
                    int kind = ((Number) type).intValue();
                    if (kind == AdvancedFeedRules.TEXT_STICKER_TYPE
                            || kind == AdvancedFeedRules.CAPTIONED_TEXT_STICKER_TYPE) {
                        String text = limited(Reflect.string(sticker, "getTextStruct", "textStruct"), MAX_STICKER_TEXT_LENGTH);
                        result = 31 * result + (text == null ? 0 : text.hashCode());
                    }
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // No text metadata means no change to the cache signature.
        }
        return result;
    }

    private static String anchorsExtras(Aweme item) {
        Object value = Reflect.required(item, "getAnchorsExtras", DISCLOSURE_HOOK_FAMILY);
        if (!(value instanceof String)) return null;
        String extras = ((String) value).trim();
        return extras.isEmpty() ? null : extras;
    }

    private static boolean nonBlankString(Object value) {
        return value instanceof String && !((String) value).trim().isEmpty();
    }
}
