/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.settings.preference;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.preference.Preference;
import android.view.View;
import android.widget.TextView;

import app.morphe.extension.tiktok.settings.Settings;

/** Visual tokens and treatments scoped to StopSlop's settings controls. */
final class StopSlopUi {
    private static final int DARK_VIOLET = Color.rgb(193, 166, 255);
    private static final int LIGHT_VIOLET = Color.rgb(91, 55, 158);
    private static final int DARK_CYAN = Color.rgb(119, 225, 239);
    private static final int LIGHT_CYAN = Color.rgb(0, 105, 124);

    private StopSlopUi() { }

    static boolean isPreference(Object item) {
        if (!(item instanceof Preference)) return false;
        String key = ((Preference) item).getKey();
        return Settings.STOP_SLOP_AD_SPONSORSHIP.key.equals(key)
                || Settings.STOP_SLOP_AI_GENERATED.key.equals(key)
                || Settings.STOP_SLOP_MIN_LIKES.key.equals(key)
                || Settings.STOP_SLOP_MIN_LIKES_THRESHOLD.key.equals(key)
                || Settings.STOP_SLOP_TOPIC_FILTER.key.equals(key)
                || Settings.STOP_SLOP_TOPIC_RULES.key.equals(key)
                || "stop_slop_reload_tiktok".equals(key);
    }

    static int primary(boolean light) {
        return light ? LIGHT_VIOLET : DARK_VIOLET;
    }

    static int secondary(boolean light) {
        return light ? LIGHT_CYAN : DARK_CYAN;
    }

    static Drawable cardBackground(View row, boolean selectable, boolean enabled,
                                   boolean first, boolean last) {
        boolean light = ScreenColors.current().light;
        int top = enabled
                ? light ? Color.rgb(255, 255, 255) : Color.rgb(43, 37, 55)
                : light ? Color.rgb(246, 244, 248) : Color.rgb(34, 32, 40);
        int bottom = enabled
                ? light ? Color.rgb(244, 239, 250) : Color.rgb(27, 24, 35)
                : light ? Color.rgb(237, 235, 241) : Color.rgb(26, 25, 31);
        int outline = enabled
                ? light ? Color.rgb(211, 197, 232) : Color.rgb(87, 69, 116)
                : light ? Color.rgb(211, 207, 218) : Color.rgb(60, 57, 67);

        float radius = SettingsUi.dp(row.getContext(), 15);
        float topRadius = first ? radius : 0;
        float bottomRadius = last ? radius : 0;
        GradientDrawable surface = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        surface.setCornerRadii(new float[]{topRadius, topRadius, topRadius, topRadius,
                bottomRadius, bottomRadius, bottomRadius, bottomRadius});
        surface.setStroke(SettingsUi.dp(row.getContext(), 1), outline);

        int side = SettingsUi.dp(row.getContext(), 18);
        Drawable inset = new InsetDrawable(surface, side,
                first ? SettingsUi.dp(row.getContext(), 4) : -SettingsUi.dp(row.getContext(), 1),
                side, last ? SettingsUi.dp(row.getContext(), 4) : 0);
        return selectable && enabled
                ? new RippleDrawable(ScreenColors.interactionRipple(primary(light)), inset, null).mutate()
                : inset;
    }

    static Drawable brandBadge(Context context) {
        boolean light = ScreenColors.current().light;
        GradientDrawable badge = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                light
                        ? new int[]{Color.rgb(255, 255, 255), Color.rgb(239, 231, 250)}
                        : new int[]{Color.rgb(51, 43, 68), Color.rgb(28, 24, 37)});
        badge.setCornerRadius(SettingsUi.dp(context, 18));
        badge.setStroke(SettingsUi.dp(context, 1), light
                ? Color.rgb(204, 185, 232) : Color.rgb(94, 74, 126));
        return badge;
    }

    static void paintHeading(TextView title, boolean brand) {
        boolean light = ScreenColors.current().light;
        title.setTextColor(primary(light));
        if (brand) title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        GradientDrawable marker = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                brand ? new int[]{primary(light), secondary(light)}
                        : new int[]{secondary(light), primary(light)});
        marker.setCornerRadius(SettingsUi.dp(title.getContext(), 2));
        marker.setBounds(0, 0, SettingsUi.dp(title.getContext(), brand ? 4 : 3),
                SettingsUi.dp(title.getContext(), 14));
        title.setCompoundDrawablePadding(SettingsUi.dp(title.getContext(), 8));
        title.setCompoundDrawablesRelative(marker, null, null, null);
    }

    static void clearHeading(TextView title) {
        title.setCompoundDrawablesRelative(null, null, null, null);
        title.setCompoundDrawablePadding(0);
    }
}
