/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.settings.preference;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;

import app.morphe.extension.tiktok.feedfilter.FeedRuleLimits;
import app.morphe.extension.tiktok.settings.L10n;
import app.morphe.extension.tiktok.settings.Settings;

/** StopSlop controls and task actions shown on the main settings page. */
@SuppressWarnings("deprecation")
final class StopSlopPlaceholders {
    private StopSlopPlaceholders() { }

    static void addTo(Context context, PreferenceScreen screen) {
        screen.addPreference(SectionHeadingPreference.stopSlop(context, "Advertising"));
        screen.addPreference(new TogglePreference(context, "Ads & sponsorships",
                "Also checks sponsor wording and commerce signals; the Feed filter page keeps separate controls for TikTok's ad, partnership, and music markers.",
                Settings.STOP_SLOP_AD_SPONSORSHIP));

        screen.addPreference(SectionHeadingPreference.stopSlop(context, "AI-generated content"));
        TogglePreference aiGenerated = new TogglePreference(context, "Hide AI-generated posts",
                "Hide posts with explicit AI labels, metadata, or AI-generation tags. Language and low engagement alone do not count.",
                Settings.STOP_SLOP_AI_GENERATED);
        aiGenerated.setChecked(Settings.STOP_SLOP_AI_GENERATED.savedValue()
                || Settings.HIDE_AI_GENERATED.savedValue());
        aiGenerated.setOnPreferenceChangeListener((preference, value) -> {
            Boolean enabled = (Boolean) value;
            return Settings.STOP_SLOP_AI_GENERATED.save(enabled)
                    && Settings.HIDE_AI_GENERATED.save(enabled);
        });
        screen.addPreference(aiGenerated);

        screen.addPreference(SectionHeadingPreference.stopSlop(context, "Feed quality"));
        screen.addPreference(new TogglePreference(context, "Minimum likes",
                "Hide posts with fewer likes than the threshold. Posts without a known like count stay visible.",
                Settings.STOP_SLOP_MIN_LIKES));
        NumberInputPreference threshold = new NumberInputPreference(context, "Like threshold",
                "Posts below this number of likes are hidden while Minimum likes is on.",
                Settings.STOP_SLOP_MIN_LIKES_THRESHOLD, "%1$s like", "%1$s likes").zeroMeansOff();
        screen.addPreference(threshold);
        // Resolve the dependency after both preferences are attached to the same hierarchy.
        threshold.setDependency(Settings.STOP_SLOP_MIN_LIKES.key);

        screen.addPreference(SectionHeadingPreference.stopSlop(context, "Topic filtering"));
        TogglePreference topicFilter = new TogglePreference(context, "Topic Filter",
                "Hide posts whose captions match a phrase or hashtag rule. Exceptions and allowed creators take precedence.",
                Settings.STOP_SLOP_TOPIC_FILTER);
        screen.addPreference(topicFilter);

        InputTextPreference topicRules = new InputTextPreference(context, "Topic rules",
                "One rule per line: phrase, #hashtag, !phrase, !#hashtag, or @username. Matching is case-insensitive.",
                Settings.STOP_SLOP_TOPIC_RULES)
                .withCheck(FeedRuleLimits::captionProblem);
        EditText editor = topicRules.getEditText();
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editor.setSingleLine(false);
        editor.setMinLines(5);
        editor.setMaxLines(10);
        editor.setGravity(Gravity.TOP | Gravity.START);
        screen.addPreference(topicRules);
        topicRules.setDependency(Settings.STOP_SLOP_TOPIC_FILTER.key);
    }

    static void addConvenienceAction(Context context, PreferenceScreen screen) {
        screen.addPreference(SectionHeadingPreference.stopSlop(context, "Actions"));
        screen.addPreference(reloadTikTok(context));
    }

    private static Preference reloadTikTok(Context context) {
        Preference row = new Preference(context);
        row.setKey("stop_slop_reload_tiktok");
        row.setTitle(L10n.t(context, "Reload TikTok"));
        row.setSummary(L10n.t(context,
                "Closes and reopens TikTok's task. It does not stop the app process and may interrupt your current session."));
        row.setPersistent(false);
        row.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(context)
                    .setTitle(L10n.t(context, "Reload TikTok?"))
                    .setMessage(L10n.t(context,
                            "This closes and reopens TikTok's task. It does not stop the app process and may interrupt your current session."))
                    .setNegativeButton(L10n.t(context, "Cancel"), null)
                    .setPositiveButton(L10n.t(context, "Reload"), (dialog, which) -> launchTikTokTask(context))
                    .show();
            return true;
        });
        return row;
    }

    private static void launchTikTokTask(Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch == null) {
            SettingsActionBanner.showNotice(context, L10n.t(context, "Could not open TikTok."));
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try {
            context.startActivity(launch);
        } catch (ActivityNotFoundException | SecurityException failure) {
            SettingsActionBanner.showNotice(context, L10n.t(context, "Could not open TikTok."));
        }
    }
}
