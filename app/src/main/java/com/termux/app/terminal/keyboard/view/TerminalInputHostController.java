package com.termux.app.terminal.keyboard.view;

import android.content.res.Configuration;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.keyboard.config.KeyboardConfigRepository;
import com.termux.app.terminal.keyboard.config.KeyboardConfigSnapshot;
import com.termux.app.terminal.keyboard.config.KeyboardDiagnostic;
import com.termux.app.terminal.keyboard.config.KeyboardDocument;
import com.termux.app.terminal.keyboard.input.KeyboardInputDispatcher;
import com.termux.app.terminal.keyboard.input.KeyboardState;
import com.termux.app.terminal.keyboard.input.TerminalViewKeyboardSink;
import com.termux.app.terminal.keyboard.settings.KeyboardPreferences;
import com.termux.app.terminal.keyboard.speech.AndroidSpeechRecognizerProvider;
import com.termux.app.terminal.keyboard.speech.SpeechInputController;
import com.termux.app.terminal.keyboard.speech.SpeechProviderRegistry;
import com.termux.shared.logger.Logger;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.TerminalColors;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TextStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Activity integration boundary preserving the existing toolbar and Android IME mode. */
public final class TerminalInputHostController
        implements TerminalViewKeyboardSink.AppCallbacks, SpeechInputController.Host {
    private static final String LOG_TAG = "TerminalInputHost";
    private final TermuxActivity activity;
    private final BuiltInKeyboardView keyboard;
    private final BuiltInKeyboardView left;
    private final BuiltInKeyboardView right;
    private final BuiltInKeyboardView[] keyboards;
    private final View splitHost;
    private final View.OnLayoutChangeListener splitHostLayoutListener;
    private final ViewPager legacy;
    private final KeyboardPreferences preferences;
    private final KeyboardConfigRepository repository;
    private final TerminalViewKeyboardSink terminalSink;
    private final SpeechInputController speech;
    private final View speechOverlay;
    private final TextView speechStatus;
    private final TextView speechTranscript;
    private final Button speechStop;
    private final Button speechCancel;
    private KeyboardConfigSnapshot snapshot;
    private KeyboardDocument.Variant splitBaseVariant;

    public TerminalInputHostController(TermuxActivity activity) {
        this.activity = activity;
        keyboard = activity.findViewById(R.id.built_in_keyboard);
        left = activity.findViewById(R.id.built_in_keyboard_left);
        right = activity.findViewById(R.id.built_in_keyboard_right);
        keyboards = new BuiltInKeyboardView[] {keyboard, left, right};
        splitHost = activity.findViewById(R.id.terminal_horizontal_host);
        splitHostLayoutListener =
                (view, l, t, r, b, oldL, oldT, oldR, oldB) -> {
                    if (splitBaseVariant != null && left.getVisibility() == View.VISIBLE) {
                        applyRailWidths(r - l);
                    }
                };
        splitHost.addOnLayoutChangeListener(splitHostLayoutListener);
        legacy = activity.findViewById(R.id.terminal_toolbar_view_pager);
        preferences = new KeyboardPreferences(activity);
        repository = new KeyboardConfigRepository(activity);
        KeyboardState sharedState = keyboard.getKeyboardState();
        left.setKeyboardState(sharedState);
        right.setKeyboardState(sharedState);
        terminalSink = new TerminalViewKeyboardSink(activity.getTerminalView(), this);
        refreshTerminalColors();
        KeyboardInputDispatcher dispatcher = new KeyboardInputDispatcher(sharedState, terminalSink);
        for (BuiltInKeyboardView view : keyboards) {
            view.setDispatcher(dispatcher);
            view.setStateChangeListener(this::refreshKeyboardState);
        }
        speechOverlay = activity.findViewById(R.id.speech_input_overlay);
        speechStatus = activity.findViewById(R.id.speech_input_status);
        speechTranscript = activity.findViewById(R.id.speech_input_transcript);
        speechStop = activity.findViewById(R.id.speech_input_stop);
        speechCancel = activity.findViewById(R.id.speech_input_cancel);
        SpeechProviderRegistry registry =
                new SpeechProviderRegistry(
                        Arrays.asList(
                                new AndroidSpeechRecognizerProvider.Factory(activity, true),
                                new AndroidSpeechRecognizerProvider.Factory(activity, false)));
        speech = new SpeechInputController(registry, preferences, this);
        speechStop.setOnClickListener(v -> speech.toggle());
        speechCancel.setOnClickListener(v -> speech.dismiss());
        applyMode();
        repository.loadInitial(this::onConfigLoaded);
    }

    private void onConfigLoaded(
            KeyboardConfigSnapshot value, List<KeyboardDiagnostic> diagnostics, boolean applied) {
        if (shouldApplyLoadedSnapshot(snapshot, value, applied)) {
            snapshot = value;
            String id = preferences.profile();
            if (!snapshot.profiles.containsKey(id)) id = snapshot.document.defaultProfile;
            preferences.profile(id);
            configureProfile(id);
        }
        if (diagnostics.isEmpty()) preferences.diagnostics("No keyboard configuration errors.");
        else {
            StringBuilder b = new StringBuilder();
            for (KeyboardDiagnostic d : diagnostics) b.append(d).append('\n');
            preferences.diagnostics(b.toString().trim());
            Logger.logError(LOG_TAG, b.toString());
        }
        applyMode();
    }

    static boolean shouldApplyLoadedSnapshot(
            KeyboardConfigSnapshot current,
            KeyboardConfigSnapshot loaded,
            boolean applied) {
        return loaded != null && (applied || current == null);
    }

    private void configureProfile(String id) {
        speech.cancel();
        KeyboardDocument.Profile profile = snapshot.profiles.get(id);
        float widthDp =
                activity.getResources().getDisplayMetrics().widthPixels
                        / activity.getResources().getDisplayMetrics().density;
        KeyboardDocument.Variant activeBaseVariant = snapshot.selectVariant(id, widthDp);
        splitBaseVariant = selectSplitVariant(profile, activeBaseVariant);
        KeyboardDocument.LandscapeSplit split =
                splitBaseVariant == null ? null : splitBaseVariant.landscapeSplit;
        if (split != null) preferences.landscapeLayoutDefault(Math.round(split.railWidthPercent));
        keyboard.setProfile(id, profile);
        if (split != null) {
            left.setProfile(
                    id,
                    profile,
                    railVariant(
                            splitBaseVariant,
                            split,
                            split.left,
                            split.leftUtility,
                            "landscape-left"));
            right.setProfile(
                    id,
                    profile,
                    railVariant(
                            splitBaseVariant,
                            split,
                            split.right,
                            split.rightUtility,
                            "landscape-right"));
        } else {
            left.setProfile(id, profile);
            right.setProfile(id, profile);
        }
    }

    static KeyboardDocument.Variant selectSplitVariant(
            KeyboardDocument.Profile profile, KeyboardDocument.Variant activeVariant) {
        if (activeVariant != null && activeVariant.landscapeSplit != null) return activeVariant;
        for (KeyboardDocument.Variant variant : profile.variants)
            if (variant.landscapeSplit != null) return variant;
        return null;
    }

    static KeyboardDocument.Variant railVariant(
            KeyboardDocument.Variant base,
            KeyboardDocument.LandscapeSplit split,
            List<List<String>> rail,
            String suffix) {
        return railVariant(base, split, rail, null, suffix);
    }

    static KeyboardDocument.Variant railVariant(
            KeyboardDocument.Variant base,
            KeyboardDocument.LandscapeSplit split,
            List<List<String>> rail,
            List<String> utility,
            String suffix) {
        Map<String, KeyboardDocument.Key> keys = new HashMap<>();
        for (KeyboardDocument.Row row : base.rows)
            for (KeyboardDocument.Key key : row.keys) if (key.id != null) keys.put(key.id, key);
        KeyboardDocument.Variant variant = new KeyboardDocument.Variant();
        variant.id = base.id + "-" + suffix;
        variant.minWidthDp = 0;
        variant.rowHeightDp = split.rowHeightDp;
        variant.appearance = split.appearance;
        variant.rows = new ArrayList<>();
        if (utility != null) {
            KeyboardDocument.Row row = railRow(keys, utility);
            row.height = split.utilityRowHeightDp;
            variant.rows.add(row);
        }
        for (List<String> ids : rail) {
            variant.rows.add(railRow(keys, ids));
        }
        return variant;
    }

    private static KeyboardDocument.Row railRow(
            Map<String, KeyboardDocument.Key> keys, List<String> ids) {
        KeyboardDocument.Row row = new KeyboardDocument.Row();
        row.keys = new ArrayList<>();
        KeyboardDocument.Key space = null;
        for (String id : ids) {
            KeyboardDocument.Key key = railKey(keys.get(id));
            row.keys.add(key);
            if ("space".equals(id)) space = key;
        }
        if (space != null) space.width = Math.max(1f, 5f - (ids.size() - 1f));
        return row;
    }

    private static KeyboardDocument.Key railKey(KeyboardDocument.Key source) {
        KeyboardDocument.Key key = new KeyboardDocument.Key();
        key.id = source.id;
        key.label = source.label;
        key.icon = source.icon;
        key.secondaryLabel = source.secondaryLabel;
        key.accessibilityLabel = source.accessibilityLabel;
        key.width = 1f;
        key.spacer = source.spacer;
        key.styleClass = source.styleClass;
        key.style = source.style;
        key.repeat = source.repeat;
        key.cursorGesture = source.cursorGesture;
        key.preview = source.preview;
        key.action = source.action;
        key.alternatives = source.alternatives;
        key.layers = source.layers;
        return key;
    }

    public void reload() {
        preferences.consumeReloadRequest();
        cancelAll();
        repository.reload(this::onConfigLoaded);
    }

    public void configurationChanged() {
        speech.onConfigurationChanged();
        refreshTerminalColors();
        if (snapshot != null) configureProfile(preferences.profile());
        applyMode();
    }

    public void refreshTerminalColors() {
        TerminalSession session = activity.getCurrentSession();
        int[] colors =
                session != null && session.getEmulator() != null
                        ? session.getEmulator().mColors.mCurrentColors
                        : TerminalColors.COLOR_SCHEME.mDefaultColors;
        int foreground = colors[TextStyle.COLOR_INDEX_FOREGROUND];
        int background = colors[TextStyle.COLOR_INDEX_BACKGROUND];
        for (BuiltInKeyboardView view : keyboards) {
            view.setTerminalColors(foreground, background);
        }
    }

    public void refreshMode() {
        boolean wasVisible = anyKeyboardVisible();
        applyMode();
        if (wasVisible != anyKeyboardVisible()) cancelAll();
        if (preferences.consumeReloadRequest()) reload();
    }

    private boolean splitLandscape() {
        if (splitBaseVariant == null) return false;
        KeyboardDocument.LandscapeSplit split = splitBaseVariant.landscapeSplit;
        float heightDp =
                activity.getResources().getDisplayMetrics().heightPixels
                        / activity.getResources().getDisplayMetrics().density;
        return isSplitLayout(
                activity.getResources().getConfiguration().orientation, heightDp, split);
    }

    static boolean isSplitOrientation(int orientation) {
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    static boolean isSplitLayout(
            int orientation, float availableHeightDp, KeyboardDocument.LandscapeSplit split) {
        return split != null
                && isSplitOrientation(orientation)
                && availableHeightDp <= split.maxHeightDp;
    }

    private void applyMode() {
        boolean builtIn = preferences.builtIn();
        boolean visible = preferences.visible();
        boolean split = builtIn && visible && splitLandscape();
        InputSurfaceState surfaces =
                inputSurfaceState(
                        builtIn,
                        visible,
                        split,
                        activity.getPreferences().shouldShowTerminalToolbar());
        keyboard.setVisibility(surfaces.fullKeyboard ? View.VISIBLE : View.GONE);
        left.setVisibility(surfaces.leftRail ? View.VISIBLE : View.GONE);
        right.setVisibility(surfaces.rightRail ? View.VISIBLE : View.GONE);
        legacy.setVisibility(surfaces.legacyToolbar ? View.VISIBLE : View.GONE);
        if (builtIn) {
            KeyboardUtils.disableSoftKeyboard(activity, activity.getTerminalView());
            activity.getTerminalView().requestFocus();
            if (split) applyRailWidths();
        } else {
            speech.cancel();
        }
    }

    static InputSurfaceState inputSurfaceState(
            boolean builtIn,
            boolean keyboardVisible,
            boolean splitLayout,
            boolean showLegacyToolbar) {
        boolean showRails = builtIn && keyboardVisible && splitLayout;
        return new InputSurfaceState(
                builtIn && keyboardVisible && !showRails,
                showRails,
                showRails,
                !builtIn && showLegacyToolbar);
    }

    private void applyRailWidths() {
        int availableWidth = splitHost.getWidth();
        if (availableWidth <= 0)
            availableWidth = activity.getResources().getDisplayMetrics().widthPixels;
        applyRailWidths(availableWidth);
    }

    private void applyRailWidths(int availableWidth) {
        if (availableWidth <= 0) return;
        float density = activity.getResources().getDisplayMetrics().density;
        KeyboardDocument.LandscapeSplit split = splitBaseVariant.landscapeSplit;
        int percent = preferences.landscapeWidthPercent(Math.round(split.railWidthPercent));
        int width = railWidthPixels(availableWidth, density, split, percent);
        setRailWidth(left, width);
        setRailWidth(right, width);
    }

    static int railWidthPixels(
            int availableWidth,
            float density,
            KeyboardDocument.LandscapeSplit split,
            int percent) {
        int requested =
                Math.max(
                        Math.round(availableWidth * percent / 100f),
                        Math.round(split.railMinWidthDp * density));
        return Math.min(requested, Math.round(availableWidth * .20f));
    }

    private static void setRailWidth(View view, int width) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params.width != width) {
            params.width = width;
            view.setLayoutParams(params);
        }
    }

    private boolean anyKeyboardVisible() {
        for (BuiltInKeyboardView view : keyboards)
            if (view.getVisibility() == View.VISIBLE) return true;
        return false;
    }

    private void refreshKeyboardState() {
        for (BuiltInKeyboardView view : keyboards) view.refreshState();
    }

    private void cancelAll() {
        speech.cancel();
        for (BuiltInKeyboardView view : keyboards) view.cancelAll();
    }

    public boolean isBuiltIn() {
        return preferences.builtIn();
    }

    public void toggle() {
        if (!preferences.builtIn()) return;
        boolean show = !anyKeyboardVisible();
        preferences.visible(show);
        applyMode();
        if (!show) cancelAll();
        activity.getTerminalView().requestFocus();
    }

    public void reset() {
        cancelAll();
    }

    public void close() {
        splitHost.removeOnLayoutChangeListener(splitHostLayoutListener);
        speech.destroy();
        for (BuiltInKeyboardView view : keyboards) view.cancelAll();
        repository.close();
    }

    @Override
    public void appAction(String action) {
        switch (action) {
            case "paste":
                activity.getTermuxTerminalSessionClient().onPasteTextFromClipboard(null);
                break;
            case "drawer":
                DrawerLayout drawer = activity.getDrawer();
                if (drawer.isDrawerOpen(Gravity.LEFT)) drawer.closeDrawer(Gravity.LEFT);
                else drawer.openDrawer(Gravity.LEFT);
                break;
            case "hideKeyboard":
                preferences.visible(false);
                applyMode();
                cancelAll();
                break;
            case "speechInput":
                speech.toggle();
                break;
        }
    }

    @Override
    public void profileAction(String profile) {
        if (snapshot == null) return;
        String id = profile;
        if ("cycle".equals(profile)) {
            List<String> ids = snapshot.profileIds();
            int i = ids.indexOf(preferences.profile());
            id = ids.get((i + 1) % ids.size());
        }
        if (snapshot.profiles.containsKey(id)) {
            preferences.profile(id);
            configureProfile(id);
            applyMode();
        }
    }

    @Override
    public boolean hasMicrophonePermission() {
        return activity.hasBuiltInSpeechPermission();
    }

    @Override
    public void requestMicrophonePermission() {
        activity.requestBuiltInSpeechPermission();
    }

    public void onMicrophonePermissionResult(boolean granted) {
        speech.onPermissionResult(granted);
    }

    @Override
    public void insertText(String text) {
        terminalSink.insertLiteralText(text);
        activity.getTerminalView().requestFocus();
    }

    @Override
    public void render(SpeechInputController.State state, String status, String transcript) {
        boolean visible = state != SpeechInputController.State.IDLE;
        speechOverlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        speechStatus.setText(status);
        speechTranscript.setText(transcript);
        boolean canStop =
                state == SpeechInputController.State.STARTING
                        || state == SpeechInputController.State.LISTENING;
        speechStop.setVisibility(canStop ? View.VISIBLE : View.GONE);
        speechCancel.setText(
                state == SpeechInputController.State.RESULT
                                || state == SpeechInputController.State.ERROR
                        ? R.string.termux_speech_dismiss
                        : R.string.termux_speech_cancel);
    }

    @Override
    public void announce(String text) {
        speechOverlay.announceForAccessibility(text);
    }

    static final class InputSurfaceState {
        final boolean fullKeyboard;
        final boolean leftRail;
        final boolean rightRail;
        final boolean legacyToolbar;

        InputSurfaceState(
                boolean fullKeyboard,
                boolean leftRail,
                boolean rightRail,
                boolean legacyToolbar) {
            this.fullKeyboard = fullKeyboard;
            this.leftRail = leftRail;
            this.rightRail = rightRail;
            this.legacyToolbar = legacyToolbar;
        }
    }
}
