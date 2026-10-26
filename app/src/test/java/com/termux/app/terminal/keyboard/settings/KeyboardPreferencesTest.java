package com.termux.app.terminal.keyboard.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class KeyboardPreferencesTest {

    private Context context;

    @Before
    public void clearPreferences() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("terminal_keyboard", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void freshInstallDefaultsBuiltInVisibleAndRoundTripsSettings() {
        KeyboardPreferences preferences = new KeyboardPreferences(context);

        assertTrue(preferences.builtIn());
        assertTrue(preferences.visible());
        assertEquals("automatic", preferences.providerMode());
        assertEquals("", preferences.languageTag());
        assertEquals(18, preferences.displayedLandscapeWidthPercent());
        assertEquals(20, preferences.landscapeWidthPercent(23));

        preferences.providerMode("android_system");
        preferences.languageTag(" sl-SI ");
        preferences.setLandscapeWidthPercent(27);
        KeyboardPreferences reloaded = new KeyboardPreferences(context);
        assertEquals("android_system", reloaded.providerMode());
        assertEquals("sl-SI", reloaded.languageTag());
        assertEquals(20, reloaded.landscapeWidthPercent(20));

        preferences.provider(KeyboardPreferences.SYSTEM);
        assertFalse(new KeyboardPreferences(context).builtIn());
        preferences.provider(KeyboardPreferences.BUILT_IN);
        preferences.visible(false);
        assertFalse(new KeyboardPreferences(context).visible());
    }
}
