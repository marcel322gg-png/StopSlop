/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.settings.preference;

import android.app.AlertDialog;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import app.morphe.extension.tiktok.feedfilter.FeedRuleLimits;
import app.morphe.extension.tiktok.settings.L10n;
import app.morphe.extension.tiktok.settings.Settings;
import app.morphe.extension.tiktok.feedfilter.LocalContentInference;
import app.morphe.extension.tiktok.feedfilter.StopSlopModelManager;
import app.morphe.extension.shared.settings.Setting;

import java.lang.ref.WeakReference;
import java.util.List;

/** StopSlop controls and task actions shown on the main settings page. */
@SuppressWarnings("deprecation")
final class StopSlopPlaceholders {
    private StopSlopPlaceholders() { }

    static boolean onModelImportResult(Activity activity, int requestCode, int resultCode, Intent data) {
        return ModelManagerPreference.onResult(activity, requestCode, resultCode, data);
    }

    static void addTo(Context context, PreferenceScreen screen) {
        migrateAdSettings();
        screen.addPreference(SectionHeadingPreference.stopSlop(context, "Advertising"));
        TogglePreference ads = new TogglePreference(context, "Ads & sponsorships",
                "Hide TikTok ads, paid partnerships, promotional music, and detected sponsorship signals.",
                Settings.STOP_SLOP_AD_SPONSORSHIP);
        ads.setChecked(Settings.STOP_SLOP_AD_SPONSORSHIP.savedValue());
        ads.setOnPreferenceChangeListener((preference, value) -> {
            boolean enabled = (Boolean) value;
            return Settings.STOP_SLOP_AD_SPONSORSHIP.save(enabled);
        });
        screen.addPreference(ads);

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
        screen.addPreference(new ModelManagerPreference(context));
        TogglePreference topicFilter = new TogglePreference(context, "Topic Filter",
                "Hide posts whose captions match a phrase or hashtag rule. Exceptions and allowed creators take precedence.",
                Settings.STOP_SLOP_TOPIC_FILTER);
        screen.addPreference(topicFilter);
        TogglePreference semanticTopicFilter = new TogglePreference(context, "Semantic Topic Filter",
                "Compare caption and hashtag text with your first 16 topic rules using the selected local model. Install a compatible model first.",
                Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED);
        screen.addPreference(semanticTopicFilter);
        Context modelContext = app.morphe.extension.shared.Utils.getContext();
        boolean semanticModelSelected = StopSlopModelManager.selectedModel(modelContext) != null;
        semanticTopicFilter.setEnabled(semanticModelSelected);
        if (!semanticModelSelected) {
            Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED.save(false);
            semanticTopicFilter.setChecked(false);
        }
        semanticTopicFilter.setSummary(modelStatusSummary(context, modelContext));
        LocalContentInference.setEnabled(Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED.savedValue()
                && semanticModelSelected);
        semanticTopicFilter.setOnPreferenceChangeListener((preference, value) -> {
            boolean enable = Boolean.TRUE.equals(value);
            if (enable) {
                Context current = app.morphe.extension.shared.Utils.getContext();
                if (StopSlopModelManager.selectedModel(current) == null) {
                    app.morphe.extension.shared.Utils.showToastLong(
                            L10n.t(context, "Select a compatible model in AI / Models first."));
                    return false;
                }
                if (!LocalContentInference.isReady()) {
                    LocalContentInference.prepareSelectedModel();
                    app.morphe.extension.shared.Utils.showToastLong(
                            L10n.t(context, "The selected model is initializing. Try again when it is ready."));
                    return false;
                }
            }
            if (!Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED.save(enable)) return false;
            LocalContentInference.setEnabled(enable);
            semanticTopicFilter.setSummary(modelStatusSummary(context,
                    app.morphe.extension.shared.Utils.getContext()));
            return true;
        });

        ChoicePreference semanticThreshold = new ChoicePreference(context, "Semantic match threshold",
                Settings.STOP_SLOP_TOPIC_SEMANTIC_THRESHOLD,
                new String[] {"0.80 similarity", "0.84 similarity", "0.88 similarity", "0.92 similarity"},
                new String[] {"0.80", "0.84", "0.88", "0.92"})
                .withNote("Higher values require closer meaning. This is cosine similarity, not a probability.");
        screen.addPreference(semanticThreshold);
        semanticThreshold.setDependency(Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED.key);
        semanticThreshold.setOnPreferenceChangeListener((preference, value) -> {
            try {
                LocalContentInference.updateTopicConfiguration(
                        Settings.STOP_SLOP_TOPIC_RULES.get(), Float.parseFloat(String.valueOf(value)));
            } catch (RuntimeException invalidThreshold) {
                LocalContentInference.updateTopicConfiguration(
                        Settings.STOP_SLOP_TOPIC_RULES.get(), 0.84f);
            }
            return true;
        });
        screen.addPreference(new TopicSemanticStatusPreference(context));

        migrateTopicRuleSeparators();
        InputTextPreference topicRules = new InputTextPreference(context, "Topic rules",
                "Separate rules with semicolons: phrase; #hashtag; !phrase; !#hashtag; or @username. Semantic matching uses at most the first 16 blocked phrases and hashtags.",
                Settings.STOP_SLOP_TOPIC_RULES)
                .withCheck(FeedRuleLimits::captionProblem)
                .keepOpenOnImeAction();
        EditText editor = topicRules.getEditText();
        editor.setInputType(InputType.TYPE_CLASS_TEXT);
        editor.setSingleLine(true);
        screen.addPreference(topicRules);

        screen.addPreference(SectionHeadingPreference.stopSlop(context, "Preferred Content"));
        screen.addPreference(new TogglePreference(context, "Prioritize preferred topics",
                "Move matching eligible videos ahead within the current fetched batch. Other videos stay visible; this does not change TikTok's recommendations.",
                Settings.STOP_SLOP_PREFERRED_CONTENT));
        InputTextPreference preferredTopics = new InputTextPreference(context, "Preferred topics",
                "Enter positive topics separated by semicolons, such as: Dota 2; science; wildlife; programming;",
                Settings.STOP_SLOP_PREFERRED_CONTENT_RULES)
                .withCheck(FeedRuleLimits::captionProblem)
                .keepOpenOnImeAction();
        EditText preferredEditor = preferredTopics.getEditText();
        preferredEditor.setInputType(InputType.TYPE_CLASS_TEXT);
        preferredEditor.setSingleLine(true);
        screen.addPreference(preferredTopics);
        preferredTopics.setDependency(Settings.STOP_SLOP_PREFERRED_CONTENT.key);

        screen.addPreference(SectionHeadingPreference.stopSlop(context, "TikTok's native recommendation settings"));
        screen.addPreference(nativePreferenceGuide(context, "stop_slop_manage_tiktok_topics",
                "Manage TikTok topics",
                "Shows TikTok's own settings path. StopSlop does not read or change TikTok preferences.",
                "In TikTok, go to Profile → Menu (☰) → Settings and privacy → Content preferences → Manage topics. Adjust the sliders for TikTok's predefined categories and tap Save. These controls affect the For You feed and may take time to influence recommendations. They do not set custom weights for terms such as Dota 2. StopSlop Preferred Content remains a separate local feature."));
        screen.addPreference(nativePreferenceGuide(context, "stop_slop_filter_tiktok_keywords",
                "TikTok Filter keywords",
                "Shows TikTok's own settings path. StopSlop does not read or change TikTok preferences.",
                "In TikTok, go to Profile → Menu (☰) → Settings and privacy → Content preferences → Filter keywords. TikTok's filter can remove posts with matching words, variations, and hashtags in supported feeds. Where TikTok offers its AI similar-keyword option, it can also match synonyms and variations. This is TikTok's own negative filter; StopSlop does not read or change its saved keywords."));
    }

    private static String modelStatusSummary(Context displayContext, Context modelContext) {
        if (modelContext == null) modelContext = displayContext;
        String key = StopSlopModelManager.activeKey(modelContext);
        List<StopSlopModelManager.Model> models = StopSlopModelManager.installed(modelContext);
        long storage = StopSlopModelManager.installedBytes(modelContext) / (1024 * 1024);
        if (key.isEmpty()) {
            if (!models.isEmpty()) {
                return L10n.f(displayContext, "Installed but not selected. Storage used: %1$d MiB", storage);
            }
            return StopSlopModelManager.hasModelFiles(modelContext)
                    ? L10n.t(displayContext, "Model files exist but are invalid or incomplete. Re-import a compatible E5 package.")
                    : L10n.t(displayContext, "AI model: Not installed. Browse or import a compatible E5 package.");
        }

        StopSlopModelManager.Model selected = StopSlopModelManager.selectedModel(modelContext);
        if (selected == null) {
            return L10n.t(displayContext,
                    "A selected model is missing or incompatible. Choose another installed model.");
        }
        switch (LocalContentInference.getStatus()) {
            case LOADING:
                return L10n.f(displayContext,
                        "Selected model is initializing: %1$s. Semantic filtering stays fail-open.", selected.name);
            case READY:
                if (LocalContentInference.isReady()) {
                    return Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED.savedValue()
                            ? L10n.f(displayContext,
                                    "Semantic filtering is ready with %1$s. Storage used: %2$d MiB", selected.name, storage)
                            : L10n.f(displayContext,
                                    "Selected model is ready: %1$s. Semantic filtering stays off until enabled.", selected.name);
                }
                return L10n.f(displayContext,
                        "Selected model is initializing: %1$s. Semantic filtering stays fail-open.", selected.name);
            case UNAVAILABLE:
                return L10n.f(displayContext,
                        "Semantic model initialization failed: %1$s. Filtering stays fail-open.", selected.name);
            case NOT_INSTALLED:
                return L10n.f(displayContext,
                        "Selected model: %1$s. Semantic filter is off; runtime is idle. Storage used: %2$d MiB",
                        selected.name, storage);
            case OFF:
            default:
                return L10n.f(displayContext,
                        "Selected model: %1$s. Semantic filter is off; runtime is idle. Storage used: %2$d MiB",
                        selected.name, storage);
        }
    }

    private static String modelRowStateLabel(Context context,
            boolean selected) {
        if (!selected) return L10n.t(context, "Installed, not selected");
        switch (LocalContentInference.getStatus()) {
            case LOADING:
                return L10n.t(context, "Selected · initializing");
            case READY:
                return LocalContentInference.isReady()
                        ? L10n.t(context, "Selected · ready")
                        : L10n.t(context, "Selected · initializing");
            case UNAVAILABLE:
                return L10n.t(context, "Selected · initialization failed");
            case OFF:
            case NOT_INSTALLED:
            default:
                return L10n.t(context, "Selected · semantic filter off");
        }
    }

    private static final class ModelManagerPreference extends Preference {
        private static final int REQUEST_MODEL_PACKAGE = 7357;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final LocalContentInference.StatusListener statusListener;

        ModelManagerPreference(Context context) {
            super(context);
            setTitle(L10n.t(context, "AI / Models"));
            setSummary(modelSummary(context));
            WeakReference<ModelManagerPreference> target = new WeakReference<>(this);
            statusListener = status -> mainHandler.post(() -> {
                ModelManagerPreference current = target.get();
                if (current != null) {
                    current.setSummary(current.modelSummary(current.getContext()));
                    current.refreshTopicControl();
                }
            });
            LocalContentInference.addStatusListener(statusListener);
            setOnPreferenceClickListener(preference -> {
                showActions();
                return true;
            });
        }

        private String modelSummary(Context context) {
            return modelStatusSummary(context, app.morphe.extension.shared.Utils.getContext());
        }

        private void showActions() {
            String message = L10n.t(getContext(), "The official catalog lists compatible E5 packages from StopSlop GitHub Releases. Downloads are explicit and verified against the catalog; model files stay in TikTok private storage. Inference runs locally and AI stays off until enabled.")
                    + "\n\n" + modelSummary(getContext());
            new AlertDialog.Builder(getContext())
                    .setTitle("AI / Models")
                    .setMessage(message)
                    .setPositiveButton(L10n.t(getContext(), "Browse models"), (dialog, which) -> showCatalog())
                    .setNegativeButton(L10n.t(getContext(), "Import .ssmodel"), (dialog, which) -> confirmImport())
                    .setNeutralButton(L10n.t(getContext(), "Installed models"), (dialog, which) -> showInstalled())
                    .show();
        }

        private void showCatalog() {
            ProgressDialog loading = new ProgressDialog(getContext());
            loading.setTitle(L10n.t(getContext(), "Available models"));
            loading.setMessage(L10n.t(getContext(), "Loading official model catalog…"));
            loading.setIndeterminate(true);
            loading.setCancelable(false);
            loading.show();
            StopSlopModelManager.fetchCatalog(getContext(), (entries, fromCache, warning) -> mainHandler.post(() -> {
                if (loading.isShowing()) loading.dismiss();
                if (entries.isEmpty()) {
                    String message = warning == null ? "No compatible models are currently listed." : warning;
                    new AlertDialog.Builder(getContext()).setTitle(L10n.t(getContext(), "Model catalog unavailable"))
                            .setMessage(L10n.f(getContext(), "The official model catalog could not be loaded: %1$s", message))
                            .setPositiveButton(L10n.t(getContext(), "Refresh catalog"), (dialog, which) -> showCatalog())
                            .setNegativeButton(L10n.t(getContext(), "Manual HTTPS URL"), (dialog, which) -> promptDownloadUrl())
                            .setNeutralButton(L10n.t(getContext(), "Import .ssmodel"), (dialog, which) -> confirmImport())
                            .show();
                    return;
                }
                showCatalogEntries(entries, fromCache, warning);
            }));
        }

        private void showCatalogEntries(List<StopSlopModelManager.CatalogEntry> entries,
                boolean fromCache, String warning) {
            List<StopSlopModelManager.Model> installed = StopSlopModelManager.installed(getContext());
            String activeKey = StopSlopModelManager.activeKey(getContext());
            String[] labels = new String[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                StopSlopModelManager.CatalogEntry entry = entries.get(i);
                boolean isInstalled = false;
                for (StopSlopModelManager.Model model : installed) {
                    if (entry.id.equals(model.id) && entry.sourceRevision.equals(model.revision)) isInstalled = true;
                }
                boolean isActive = (entry.id + "/" + entry.packageVersion).equals(activeKey);
                labels[i] = entry.name + " · " + entry.packageVersion + " · "
                        + (entry.packageBytes / (1024 * 1024)) + " MiB · "
                        + (isActive ? L10n.t(getContext(), "Active model")
                                : isInstalled ? L10n.t(getContext(), "Installed")
                                : entry.compatible ? L10n.t(getContext(), "Available")
                                : L10n.t(getContext(), "Not compatible"));
            }
            String note = fromCache
                    ? L10n.f(getContext(), "Offline: showing cached catalog. Refresh failed: %1$s", warning == null ? "network unavailable" : warning)
                    : L10n.t(getContext(), "Official catalog · metadata and downloads use HTTPS");
            Context context = getContext();
            float density = context.getResources().getDisplayMetrics().density;
            int padding = Math.round(16 * density);
            LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            TextView status = new TextView(context);
            status.setText(note);
            status.setPadding(padding, padding, padding, Math.round(8 * density));
            content.addView(status, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ListView modelList = new ListView(context);
            modelList.setAdapter(new ArrayAdapter<>(context,
                    android.R.layout.simple_list_item_1, labels));
            modelList.setOnItemClickListener((parent, view, index, rowId) ->
                    showCatalogEntry(entries.get(index)));
            int maxListHeight = Math.min(Math.round(320 * density),
                    Math.round(context.getResources().getDisplayMetrics().heightPixels * 0.42f));
            int contentListHeight = Math.round(Math.max(1, labels.length) * 48 * density);
            int listHeight = Math.min(contentListHeight, maxListHeight);
            content.addView(modelList, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, listHeight)));
            new AlertDialog.Builder(context).setTitle(L10n.t(context, "Available models"))
                    .setView(content)
                    .setPositiveButton(L10n.t(getContext(), "Refresh catalog"), (dialog, which) -> showCatalog())
                    .setNegativeButton(L10n.t(getContext(), "Manual HTTPS URL"), (dialog, which) -> promptDownloadUrl())
                    .setNeutralButton(L10n.t(getContext(), "Import .ssmodel"), (dialog, which) -> confirmImport())
                    .show();
        }

        private void showCatalogEntry(StopSlopModelManager.CatalogEntry entry) {
            StopSlopModelManager.Model installedModel = null;
            for (StopSlopModelManager.Model model : StopSlopModelManager.installed(getContext())) {
                if (entry.id.equals(model.id) && entry.sourceRevision.equals(model.revision)) installedModel = model;
            }
            String details = entry.description + "\n\n" + entry.publisher + "\n" + entry.languages + "\n"
                    + entry.quantization + " " + entry.format + " · " + entry.packageBytes + " bytes · " + entry.packageVersion + "\n"
                    + L10n.f(getContext(), "Runtime profile: %1$s · Android API %2$d+", entry.runtimeProfile, entry.minimumAndroidApi) + "\n"
                    + entry.runtimeVersion + "\n"
                    + entry.memoryEstimateLabel + " · " + (entry.memoryEstimateBytes / (1024 * 1024)) + " MiB\n"
                    + L10n.f(getContext(), "License: %1$s", entry.license) + "\n" + entry.licenseUrl + "\n"
                    + L10n.f(getContext(), "Source revision: %1$s @ %2$s", entry.sourceRepository, entry.sourceRevision) + "\n"
                    + L10n.f(getContext(), "SHA-256: %1$s", entry.sha256) + "\n"
                    + (entry.compatible ? L10n.t(getContext(), "Package hashes are checked before activation. SHA-256 is not a publisher signature.")
                            : entry.incompatibility);
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext()).setTitle(entry.name).setMessage(details)
                    .setNegativeButton(L10n.t(getContext(), "Close"), null);
            if (installedModel != null) {
                StopSlopModelManager.Model model = installedModel;
                dialog.setPositiveButton(L10n.t(getContext(), StopSlopModelManager.activeKey(getContext()).equals(model.key())
                        ? "Active model" : "Select model"), (popup, which) -> {
                    if (!StopSlopModelManager.activeKey(getContext()).equals(model.key())
                            && StopSlopModelManager.select(getContext(), model.key())) {
                        setSummary(modelSummary(getContext()));
                        refreshTopicControl();
                    }
                });
            } else if (entry.compatible) {
                dialog.setPositiveButton(L10n.t(getContext(), "Download verified package"),
                        (popup, which) -> runPackageOperation(callback ->
                                StopSlopModelManager.downloadCatalogModel(getContext(), entry, callback)));
            }
            dialog.show();
        }

        private void promptDownloadUrl() {
            EditText url = new EditText(getContext());
            url.setSingleLine(true);
            url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
            url.setHint("https://…/model.ssmodel");
            new AlertDialog.Builder(getContext()).setTitle("Download model package")
                    .setMessage(L10n.t(getContext(), "Enter a direct HTTPS URL for a data-only .ssmodel package. The host is not publisher-verified; package structure and hashes are checked before install."))
                    .setView(url)
                    .setPositiveButton(L10n.t(getContext(), "Continue"), (dialog, which) -> confirmDownload(url.getText().toString().trim()))
                    .setNegativeButton(L10n.t(getContext(), "Cancel"), null)
                    .show();
        }

        private void confirmDownload(String source) {
            new AlertDialog.Builder(getContext()).setTitle("Unverified model source")
                    .setMessage(L10n.t(getContext(), "Only the supported E5 model profile is accepted. The package publisher is not authenticated; continue only if you trust this HTTPS host. StopSlop never uploads feed content."))
                    .setPositiveButton(L10n.t(getContext(), "Download"), (dialog, which) -> startDownload(source))
                    .setNegativeButton(L10n.t(getContext(), "Cancel"), null)
                    .show();
        }

        private void confirmImport() {
            new AlertDialog.Builder(getContext()).setTitle("Import model package")
                    .setMessage(L10n.t(getContext(), "Choose a data-only .ssmodel package. Unverified publishers require your confirmation; unsupported model formats and files are rejected."))
                    .setPositiveButton(L10n.t(getContext(), "Choose file"), (dialog, which) -> openPicker())
                    .setNegativeButton(L10n.t(getContext(), "Cancel"), null)
                    .show();
        }

        private void openPicker() {
            Activity activity = findActivity(getContext());
            if (activity == null) {
                app.morphe.extension.shared.Utils.showToastLong(L10n.t(getContext(), "The system file picker is unavailable."));
                return;
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"application/zip", "application/octet-stream"});
            try {
                activity.startActivityForResult(intent, REQUEST_MODEL_PACKAGE);
            } catch (ActivityNotFoundException unavailable) {
                app.morphe.extension.shared.Utils.showToastLong(L10n.t(getContext(), "The system file picker is unavailable."));
            }
        }

        private void startDownload(String source) {
            runPackageOperation(callback -> StopSlopModelManager.downloadPackage(getContext(), source, callback));
        }

        private void runPackageOperation(OperationStarter starter) {
            ProgressDialog progress = new ProgressDialog(getContext());
            progress.setTitle(L10n.t(getContext(), "Installing local model"));
            progress.setMessage(L10n.t(getContext(), "Preparing package…"));
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setIndeterminate(true);
            progress.setCancelable(true);
            final StopSlopModelManager.Operation[] operation = new StopSlopModelManager.Operation[1];
            progress.setOnCancelListener(dialog -> { if (operation[0] != null) operation[0].cancel(); });
            progress.show();
            operation[0] = starter.start(new StopSlopModelManager.Callback() {
                @Override public void onProgress(long current, long total) {
                    mainHandler.post(() -> {
                        if (!progress.isShowing()) return;
                        if (total > 0L) {
                            progress.setIndeterminate(false);
                            progress.setMax(1000);
                            progress.setProgress((int) Math.min(1000L, current * 1000L / total));
                            progress.setMessage(L10n.f(getContext(), "Received %1$d of %2$d MiB",
                                    current / (1024 * 1024), total / (1024 * 1024)));
                        } else progress.setMessage(L10n.f(getContext(), "Received %1$d MiB", current / (1024 * 1024)));
                    });
                }
                @Override public void onComplete(StopSlopModelManager.Model model, Exception failure) {
                    mainHandler.post(() -> {
                        if (progress.isShowing()) progress.dismiss();
                        String message = failure == null && model != null
                                ? L10n.t(getContext(), "Model installed and verified. Select it in Installed models to use it.")
                                : L10n.f(getContext(), "Model package was not installed: %1$s",
                                        failure == null ? "unknown error" : String.valueOf(failure.getMessage()));
                        app.morphe.extension.shared.Utils.showToastLong(message);
                        setSummary(modelSummary(getContext()));
                    });
                }
            });
        }

        private void showInstalled() {
            List<StopSlopModelManager.Model> models = StopSlopModelManager.installed(getContext());
            if (models.isEmpty()) {
                app.morphe.extension.shared.Utils.showToastLong(L10n.t(getContext(), "No compatible model is installed."));
                return;
            }
            String activeKey = StopSlopModelManager.activeKey(getContext());
            CharSequence[] labels = new CharSequence[models.size()];
            for (int i = 0; i < models.size(); i++) {
                StopSlopModelManager.Model model = models.get(i);
                String revision = model.revision.length() > 8
                        ? model.revision.substring(0, 8) : model.revision;
                String state = model.key().equals(activeKey)
                        ? modelRowStateLabel(getContext(), true)
                        : L10n.t(getContext(), "Installed, not selected");
                labels[i] = model.name + " · " + model.version + " · " + revision
                        + " · " + (model.installedBytes / (1024 * 1024)) + " MiB · " + state;
            }
            new AlertDialog.Builder(getContext()).setTitle(L10n.t(getContext(), "Installed models"))
                    .setItems(labels, (dialog, index) -> showModelActions(models.get(index)))
                    .setNegativeButton(L10n.t(getContext(), "Close"), null).show();
        }

        private void showModelActions(StopSlopModelManager.Model model) {
            String activeKey = StopSlopModelManager.activeKey(getContext());
            boolean selected = model.key().equals(activeKey);
            String details = L10n.f(getContext(), "Package version: %1$s", model.version)
                    + "\n" + modelRowStateLabel(getContext(), selected)
                    + "\n" + L10n.f(getContext(), "Storage: %1$d MiB", model.installedBytes / (1024 * 1024));
            new AlertDialog.Builder(getContext())
                    .setTitle(model.name).setMessage(details)
                    .setPositiveButton(L10n.t(getContext(), selected ? "Selected" : "Select model"),
                            selected ? null : (dialog, which) -> {
                if (StopSlopModelManager.select(getContext(), model.key())) {
                    String selectionMessage = Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED.savedValue()
                            ? "Selected. Semantic filtering stays fail-open while the new model initializes."
                            : "Selected. Semantic filtering remains off until you enable it.";
                    app.morphe.extension.shared.Utils.showToastLong(L10n.t(getContext(), selectionMessage));
                    setSummary(modelSummary(getContext()));
                    refreshTopicControl();
                }
            })
                    .setNeutralButton(L10n.t(getContext(), "Details"), (dialog, which) -> showModelDetails(model))
                    .setNegativeButton(L10n.t(getContext(), "Delete model"),
                            (dialog, which) -> confirmRemove(model, selected))
                    .show();
        }

        private void showModelDetails(StopSlopModelManager.Model model) {
            String details = L10n.f(getContext(), "Package version: %1$s", model.version)
                    + "\n" + L10n.f(getContext(), "Source revision: %1$s", model.revision)
                    + "\n" + L10n.f(getContext(), "Publisher: %1$s", model.publisher)
                    + "\n" + L10n.f(getContext(), "Languages: %1$s", model.languages)
                    + "\n" + L10n.f(getContext(), "License: %1$s", model.license)
                    + "\n" + L10n.f(getContext(), "Storage: %1$d MiB", model.installedBytes / (1024 * 1024))
                    + "\n" + L10n.t(getContext(), model.builtInVerified
                            ? "Content matches app-pinned E5 hashes." : "Publisher is not verified.")
                    + "\n" + L10n.t(getContext(),
                            "Model files are removed if TikTok is uninstalled or app data is cleared.");
            new AlertDialog.Builder(getContext()).setTitle(L10n.t(getContext(), "Model details"))
                    .setMessage(details)
                    .setPositiveButton(L10n.t(getContext(), "Close"), null).show();
        }

        private void confirmRemove(StopSlopModelManager.Model model, boolean active) {
            new AlertDialog.Builder(getContext()).setTitle("Delete local model?")
                    .setMessage(L10n.t(getContext(), "Delete this model package from TikTok's private storage?"))
                    .setPositiveButton(L10n.t(getContext(), "Delete"), (dialog, which) -> {
                        if (active) {
                            StopSlopModelManager.Model fallback = null;
                            for (StopSlopModelManager.Model candidate : StopSlopModelManager.installed(getContext())) {
                                if (!candidate.key().equals(model.key())) {
                                    fallback = candidate;
                                    break;
                                }
                            }
                            if (fallback != null) {
                                if (!StopSlopModelManager.select(getContext(), fallback.key())) {
                                    app.morphe.extension.shared.Utils.showToastLong(L10n.t(getContext(),
                                            "Could not switch to the fallback model; the selected model was kept."));
                                    return;
                                }
                            } else {
                                Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED.save(false);
                                LocalContentInference.setEnabled(false);
                                if (!StopSlopModelManager.clearActive(getContext())) {
                                    app.morphe.extension.shared.Utils.showToastLong(L10n.t(getContext(),
                                            "Could not clear the selected model; it was kept."));
                                    return;
                                }
                            }
                        }
                        boolean queued = StopSlopModelManager.remove(getContext(), model.key());
                        app.morphe.extension.shared.Utils.showToastLong(L10n.t(getContext(), queued
                                ? "Model removal was queued after inference stops." : "Could not remove this model."));
                        setSummary(modelSummary(getContext()));
                        refreshTopicControl();
                    })
                    .setNegativeButton(L10n.t(getContext(), "Cancel"), null).show();
        }

        static boolean onResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (requestCode != REQUEST_MODEL_PACKAGE) return false;
            if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return true;
            Uri uri = data.getData();
            ModelManagerPreference holder = new ModelManagerPreference(activity);
            holder.runPackageOperation(callback -> StopSlopModelManager.importPackage(activity, uri, callback));
            return true;
        }

        private static Activity findActivity(Context context) {
            Context current = context;
            while (current instanceof ContextWrapper) {
                if (current instanceof Activity) return (Activity) current;
                Context next = ((ContextWrapper) current).getBaseContext();
                if (next == current) break;
                current = next;
            }
            return current instanceof Activity ? (Activity) current : null;
        }

        private void refreshTopicControl() {
            PreferenceGroup group = getParent();
            if (group == null) return;
            Preference semantic = group.findPreference(Settings.STOP_SLOP_TOPIC_SEMANTIC_ENABLED.key);
            if (semantic != null) {
                Context modelContext = app.morphe.extension.shared.Utils.getContext();
                semantic.setEnabled(StopSlopModelManager.selectedModel(modelContext) != null);
                semantic.setSummary(modelStatusSummary(semantic.getContext(), modelContext));
            }
        }

        private interface OperationStarter {
            StopSlopModelManager.Operation start(StopSlopModelManager.Callback callback);
        }
    }

    private static final class TopicSemanticStatusPreference extends Preference {
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final LocalContentInference.StatusListener listener;

        TopicSemanticStatusPreference(Context context) {
            super(context);
            setTitle("Local model status");
            WeakReference<TopicSemanticStatusPreference> target = new WeakReference<>(this);
            listener = status -> {
                TopicSemanticStatusPreference preference = target.get();
                if (preference != null) {
                    preference.mainHandler.post(() -> {
                        TopicSemanticStatusPreference current = target.get();
                        if (current != null) current.refreshStatus(status);
                    });
                }
            };
            LocalContentInference.addStatusListener(listener);
            refreshStatus(LocalContentInference.getStatus());
        }

        private void refreshStatus(LocalContentInference.Status status) {
            setSummary(modelStatusSummary(getContext(), app.morphe.extension.shared.Utils.getContext()));
        }

        @Override
        public void setTitle(CharSequence title) {
            super.setTitle(L10n.t(getContext(), title));
        }

        @Override
        public void setSummary(CharSequence summary) {
            super.setSummary(L10n.t(getContext(), summary));
        }
    }

    private static void migrateTopicRuleSeparators() {
        String saved = Settings.STOP_SLOP_TOPIC_RULES.savedValue();
        if (saved == null || !saved.matches("(?s).*\\R.*")) return;
        Settings.STOP_SLOP_TOPIC_RULES.save(saved.replaceAll("\\s*[\\r\\n]+\\s*", "; "));
    }

    private static Preference nativePreferenceGuide(Context context, String key, String title,
            String summary, String instructions) {
        Preference row = new Preference(context);
        row.setKey(key);
        row.setTitle(L10n.t(context, title));
        row.setSummary(L10n.t(context, summary));
        row.setPersistent(false);
        row.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(context)
                    .setTitle(L10n.t(context, title))
                    .setMessage(L10n.t(context, instructions))
                    .setPositiveButton(L10n.t(context, "OK"), null)
                    .show();
            return true;
        });
        return row;
    }

    private static void migrateAdSettings() {
        SharedPreferences preferences = Setting.preferences.preferences;
        String marker = "stop_slop_ad_settings_migrated_v1";
        if (preferences.getBoolean(marker, false)) return;

        boolean canonicalWasStored = preferences.contains(Settings.STOP_SLOP_AD_SPONSORSHIP.key);
        boolean migrated = canonicalWasStored
                ? Settings.STOP_SLOP_AD_SPONSORSHIP.savedValue()
                : Settings.REMOVE_ADS.savedValue() || Settings.HIDE_PAID_PARTNERSHIP.savedValue()
                        || Settings.HIDE_PROMOTIONAL_MUSIC.savedValue();
        // Legacy signal switches are deliberately left untouched: the canonical switch is a
        // master gate, not a command to enable every independently configured signal category.
        if (Settings.STOP_SLOP_AD_SPONSORSHIP.save(migrated)) {
            preferences.edit().putBoolean(marker, true).apply();
        }
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
