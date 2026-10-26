package com.termux.app.terminal.keyboard.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.graphics.Typeface;

import com.termux.app.terminal.keyboard.input.KeyboardState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class KeyboardVisualResourcesTest {
    @Test
    public void everyAllowlistedKeyIconLoadsFromBundledResources() {
        Context context = RuntimeEnvironment.getApplication();
        String[] icons = {
            "shift",
            "backspace",
            "enter",
            "microphone",
            "tab",
            "space",
            "arrowUp",
            "arrowDown",
            "arrowLeft",
            "arrowRight"
        };

        for (String icon : icons) {
            assertNotNull(
                    icon,
                    KeyboardIconRegistry.load(
                            context,
                            icon,
                            KeyboardState.VisualState.INACTIVE,
                            0xfff1f3f4));
        }
        assertNotNull(
                KeyboardIconRegistry.load(
                        context,
                        "shift",
                        KeyboardState.VisualState.LOCKED,
                        0xfff1f3f4));
        assertNull(
                KeyboardIconRegistry.load(
                        context,
                        "notAllowlisted",
                        KeyboardState.VisualState.INACTIVE,
                        0xfff1f3f4));
        assertNull(
                KeyboardIconRegistry.load(
                        context,
                        null,
                        KeyboardState.VisualState.INACTIVE,
                        0xfff1f3f4));
    }

    @Test
    public void allowlistedFontFamiliesResolveWithRequestedBoldWeight() {
        for (String family :
                new String[] {"system", "sans", "sansMedium", "monospace", "terminal"}) {
            assertNotNull(family, KeyboardTypefaceResolver.resolve(family, "normal"));
            Typeface bold = KeyboardTypefaceResolver.resolve(family, "bold");
            assertNotNull(family, bold);
            assertEquals(family, Typeface.BOLD, bold.getStyle());
        }
    }
}
