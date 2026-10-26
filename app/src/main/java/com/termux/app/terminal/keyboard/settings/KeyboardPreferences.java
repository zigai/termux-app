package com.termux.app.terminal.keyboard.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.termux.app.terminal.keyboard.speech.SpeechInputController;
import com.termux.app.terminal.keyboard.speech.SpeechProviderRegistry;

/** Built-in keyboard preferences, isolated from the existing Termux preference storage. */
public final class KeyboardPreferences implements SpeechInputController.Settings {
    public static final String BUILT_IN = "built_in";
    public static final String SYSTEM = "system";
    private final SharedPreferences preferences;

    public KeyboardPreferences(Context c) {
        preferences = c.getSharedPreferences("terminal_keyboard", Context.MODE_PRIVATE);
    }

    public String provider() {
        return preferences.getString("input_provider", BUILT_IN);
    }

    public void provider(String value) {
        preferences.edit().putString("input_provider", value).apply();
    }

    public boolean builtIn() {
        return BUILT_IN.equals(provider());
    }

    public boolean visible() {
        return preferences.getBoolean("visible", true);
    }

    public void visible(boolean value) {
        preferences.edit().putBoolean("visible", value).apply();
    }

    public String profile() {
        return preferences.getString("profile", "default");
    }

    public void profile(String value) {
        preferences.edit().putString("profile", value).apply();
    }

    public String diagnostics() {
        return preferences.getString("diagnostics", "No keyboard configuration errors.");
    }

    public void diagnostics(String value) {
        preferences.edit().putString("diagnostics", value).apply();
    }

    public int landscapeWidthPercent(int layoutDefault) {
        boolean overridden =
                preferences.getBoolean("landscape_width_overridden", false);
        return clampLandscapeWidth(
                overridden
                        ? preferences.getInt("landscape_width_percent", layoutDefault)
                        : layoutDefault);
    }

    public int displayedLandscapeWidthPercent() {
        boolean overridden =
                preferences.getBoolean("landscape_width_overridden", false);
        return clampLandscapeWidth(
                overridden
                        ? preferences.getInt("landscape_width_percent", 18)
                        : preferences.getInt("landscape_width_layout_default", 18));
    }

    public void landscapeLayoutDefault(int value) {
        preferences
                .edit()
                .putInt("landscape_width_layout_default", clampLandscapeWidth(value))
                .apply();
    }

    public void setLandscapeWidthPercent(int value) {
        preferences
                .edit()
                .putInt("landscape_width_percent", clampLandscapeWidth(value))
                .putBoolean("landscape_width_overridden", true)
                .apply();
    }

    private static int clampLandscapeWidth(int value) {
        return Math.max(12, Math.min(20, value));
    }

    @Override
    public String providerMode() {
        return preferences.getString("speech_provider", SpeechProviderRegistry.AUTOMATIC);
    }

    public void providerMode(String value) {
        preferences.edit().putString("speech_provider", value).apply();
    }

    @Override
    public String languageTag() {
        return preferences.getString("speech_language", "");
    }

    public void languageTag(String value) {
        preferences.edit().putString("speech_language", value == null ? "" : value.trim()).apply();
    }

    public String speechDiagnostic() {
        return preferences.getString("speech_diagnostic", "No speech input errors.");
    }

    @Override
    public void diagnostic(String value) {
        preferences.edit().putString("speech_diagnostic", value).apply();
    }

    public void requestReload() {
        preferences.edit().putBoolean("reload_requested", true).apply();
    }

    public boolean consumeReloadRequest() {
        boolean requested = preferences.getBoolean("reload_requested", false);
        if (requested) preferences.edit().putBoolean("reload_requested", false).apply();
        return requested;
    }
}
