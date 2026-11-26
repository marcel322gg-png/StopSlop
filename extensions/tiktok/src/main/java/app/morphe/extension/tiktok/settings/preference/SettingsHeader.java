/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 *
 * Modified for hushfeed (TikTok), 2026.
 */
package app.morphe.extension.tiktok.settings.preference;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.diagnostics.BuildDetails;
import app.morphe.extension.tiktok.settings.L10n;

/**
 * The top of the main page: StopSlop's mark, its name in TikTok's cyan and red and the version
 * line, centred over the rows. Nothing on it can be tapped. The bar above the list leaves the name
 * off while this one shows it.
 */
@SuppressWarnings("deprecation")
public final class SettingsHeader extends Preference {
    static final String KEY = "hushfeed_settings_header";
    /** Tags of its parts, for the tests. */
    static final String ICON = "hushfeed-header-icon";
    static final String NAME = "hushfeed-header-name";
    static final String VERSION = "hushfeed-header-version";
    /** The resource patch that gives TikTok Hushfeed's launcher icon. */
    static final String LAUNCHER_ICON_PATCH = "customLauncherIcon";
    /** The product name, which reads the same in every language and is not translated. */
    private static final String NAME_TEXT = "StopSlop";

    SettingsHeader(Context context) {
        super(context);
        setKey(KEY);
        // What it shows, for anything that reads rows by their titles.
        setTitle(NAME_TEXT);
        setPersistent(false);
        setSelectable(false);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        Context context = getContext();
        float density = context.getResources().getDisplayMetrics().density;
        ScreenColors colors = ScreenColors.current();
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(Math.round(24 * density), Math.round(16 * density), Math.round(24 * density),
                Math.round(14 * density));
        column.setBackground(StopSlopUi.brandBadge(context));

        ImageView icon = new ImageView(context);
        icon.setTag(ICON);
        icon.setImageDrawable(new StopSlopMark(colors.light));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        // The name under it says the same.
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int size = Math.round(48 * density);
        column.addView(icon, new LinearLayout.LayoutParams(size, size));

        TextView name = new TextView(context);
        name.setTag(NAME);
        name.setText(NAME_TEXT);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setGravity(Gravity.CENTER);
        name.setTextColor(StopSlopUi.primary(colors.light));
        SettingsUi.markHeading(name);
        LinearLayout.LayoutParams nameSpace = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameSpace.topMargin = Math.round(8 * density);
        column.addView(name, nameSpace);

        TextView description = new TextView(context);
        description.setText(L10n.t(context,
                "TikTok feed filters for ads, AI-generated posts, and low like counts."));
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        description.setTextColor(colors.summary);
        description.setGravity(Gravity.CENTER);
        description.setMaxLines(3);
        LinearLayout.LayoutParams descriptionSpace = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descriptionSpace.topMargin = Math.round(6 * density);
        column.addView(description, descriptionSpace);

        TextView version = new TextView(context);
        version.setTag(VERSION);
        CharSequence line = versionLine(context, Utils.getPatchesReleaseVersion(), Utils.getAppVersionName());
        version.setText(line);
        // An unpatched build has no bundle version to name, and an empty line would only add a gap.
        version.setVisibility(line.length() == 0 ? View.GONE : View.VISIBLE);
        version.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        version.setTextColor(colors.summary);
        version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams versionSpace = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        versionSpace.topMargin = Math.round(4 * density);
        column.addView(version, versionSpace);
        return column;
    }

    /**
     * "Version 1.2.3 for TikTok 45.6.7", each number isolated so a right-to-left sentence keeps it
     * whole. Without the bundle's version it's empty, and without TikTok's it names the bundle alone,
     * the way About does.
     */
    static CharSequence versionLine(Context context, String bundleVersion, String appVersion) {
        if (bundleVersion == null || bundleVersion.isEmpty()) return "";
        // "Unknown" is the English word Utils hands back when the package manager refuses.
        boolean readable = appVersion != null && !appVersion.isEmpty() && !"Unknown".equals(appVersion);
        if (!readable) return L10n.f(context, "Version %1$s", L10n.isolate(bundleVersion));
        return L10n.f(context, "Version %1$s for TikTok %2$s", L10n.isolate(bundleVersion),
                L10n.isolate(appVersion));
    }

    /** Kept for source compatibility with icon checks and launchers that select the patch mark. */
    static Drawable icon(Context context, ScreenColors colors, String metadata) {
        if (BuildDetails.resourcePatchApplied(metadata, LAUNCHER_ICON_PATCH)) {
            Drawable own = context.getApplicationInfo().loadIcon(context.getPackageManager());
            if (own != null) return own;
        }
        return SettingsIcons.icon(context, SettingsIcons.PATCHED, colors.heading);
    }

    /** Code-drawn adaptation of the supplied StopSlop_Mark.svg. */
    private static final class StopSlopMark extends Drawable {
        private final android.graphics.Paint paint = new android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final int contrastColor;
        private int alpha = 255;
        private android.graphics.ColorFilter colorFilter;

        StopSlopMark(boolean light) {
            contrastColor = light ? android.graphics.Color.rgb(54, 38, 75)
                    : android.graphics.Color.rgb(247, 247, 251);
        }

        @Override public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            float size = Math.min(bounds.width(), bounds.height());
            float scale = size / 1024f;
            canvas.save();
            canvas.translate(bounds.exactCenterX() - size / 2f, bounds.exactCenterY() - size / 2f);
            canvas.scale(scale, scale);
            paint.setAlpha(alpha);
            paint.setColorFilter(colorFilter);
            paint.setColor(android.graphics.Color.rgb(139, 92, 246));
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(48f);
            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            canvas.drawCircle(512f, 512f, 382f, paint);

            paint.setColor(contrastColor);
            paint.setStrokeWidth(38f);
            canvas.drawRoundRect(324f, 374f, 700f, 650f, 66f, 66f, paint);
            paint.setStyle(android.graphics.Paint.Style.FILL);
            android.graphics.Path play = new android.graphics.Path();
            play.moveTo(466f, 438f);
            play.lineTo(466f, 586f);
            play.lineTo(594f, 512f);
            play.close();
            canvas.drawPath(play, paint);

            paint.setColor(android.graphics.Color.rgb(139, 92, 246));
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(64f);
            canvas.drawLine(236f, 788f, 788f, 236f, paint);
            canvas.restore();
        }

        @Override public void setAlpha(int alpha) { this.alpha = alpha; invalidateSelf(); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { colorFilter = filter; invalidateSelf(); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
