package com.termux.app.fragments.settings.termux;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.Preference;

import com.termux.R;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.keyboard.settings.KeyboardPreferences;

@Keep
public class TerminalIOPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalIOPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_terminal_io_preferences, rootKey);

        KeyboardPreferences keyboardPreferences = new KeyboardPreferences(context);
        Preference reload = findPreference("reload_built_in_keyboard");
        if (reload != null) reload.setOnPreferenceClickListener(preference -> {
            keyboardPreferences.requestReload();
            TermuxActivity.updateTermuxActivityStyling(context, false);
            Preference diagnostic = findPreference("built_in_keyboard_diagnostics");
            if (diagnostic != null) diagnostic.setSummary(keyboardPreferences.diagnostics());
            return true;
        });
        Preference diagnostic = findPreference("built_in_keyboard_diagnostics");
        if (diagnostic != null) diagnostic.setSummary(keyboardPreferences.diagnostics());
        Preference speechDiagnostic = findPreference("speech_input_diagnostics");
        if (speechDiagnostic != null) speechDiagnostic.setSummary(keyboardPreferences.speechDiagnostic());
    }

}

class TerminalIOPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TermuxAppSharedPreferences mPreferences;
    private final KeyboardPreferences mKeyboardPreferences;

    private static TerminalIOPreferencesDataStore mInstance;

    private TerminalIOPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TermuxAppSharedPreferences.build(context, true);
        mKeyboardPreferences = new KeyboardPreferences(context);
    }

    @Override public void putString(String key, String value) {
        if ("terminal_input_provider".equals(key)) mKeyboardPreferences.provider(value);
        else if ("speech_provider".equals(key)) mKeyboardPreferences.providerMode(value);
        else if ("speech_language".equals(key)) mKeyboardPreferences.languageTag(value);
    }

    @Override public String getString(String key, String defValue) {
        if ("terminal_input_provider".equals(key)) return mKeyboardPreferences.provider();
        if ("speech_provider".equals(key)) return mKeyboardPreferences.providerMode();
        if ("speech_language".equals(key)) return mKeyboardPreferences.languageTag();
        return defValue;
    }

    @Override public void putInt(String key, int value) {
        if ("landscape_keyboard_width_percent".equals(key)) mKeyboardPreferences.setLandscapeWidthPercent(value);
    }

    @Override public int getInt(String key, int defValue) {
        if ("landscape_keyboard_width_percent".equals(key)) return mKeyboardPreferences.displayedLandscapeWidthPercent();
        return defValue;
    }

    public static synchronized TerminalIOPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TerminalIOPreferencesDataStore(context);
        }
        return mInstance;
    }



    @Override
    public void putBoolean(String key, boolean value) {
        if (mPreferences == null) return;
        if (key == null) return;

        switch (key) {
            case "soft_keyboard_enabled":
                    mPreferences.setSoftKeyboardEnabled(value);
                break;
            case "soft_keyboard_enabled_only_if_no_hardware":
                mPreferences.setSoftKeyboardEnabledOnlyIfNoHardware(value);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences == null) return false;

        switch (key) {
            case "soft_keyboard_enabled":
                return mPreferences.isSoftKeyboardEnabled();
            case "soft_keyboard_enabled_only_if_no_hardware":
                return mPreferences.isSoftKeyboardEnabledOnlyIfNoHardware();
            default:
                return false;
        }
    }

}
