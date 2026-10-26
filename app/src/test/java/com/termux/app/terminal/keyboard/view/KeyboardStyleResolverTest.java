package com.termux.app.terminal.keyboard.view;

import static org.junit.Assert.assertEquals;

import com.termux.app.terminal.keyboard.config.KeyboardDocument;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
public class KeyboardStyleResolverTest {

    @Test
    public void sharedLayoutClassAndKeyStylesResolveInOrder() {
        KeyboardDocument.Profile profile = new KeyboardDocument.Profile();
        profile.theme = new KeyboardDocument.Theme();
        profile.theme.fill = "#111111";
        profile.theme.primaryColor = "terminalForeground";
        profile.theme.horizontalGap = 7f;
        profile.theme.fontFamily = "terminal";
        profile.styles = new HashMap<>();
        KeyboardDocument.Style styleClass = new KeyboardDocument.Style();
        styleClass.fill = "#222222";
        profile.styles.put("accent", styleClass);

        KeyboardDocument.LayoutStyle layout = new KeyboardDocument.LayoutStyle();
        layout.horizontalGap = 4f;
        layout.cornerRadius = 11f;
        KeyboardDocument.Key key = new KeyboardDocument.Key();
        key.styleClass = "accent";
        key.style = new KeyboardDocument.Style();
        key.style.fill = "#333333";

        KeyboardStyleResolver.Resolved resolved =
                new KeyboardStyleResolver()
                        .resolve(profile, layout, key, true, 0xffabcdef, 0xff010203);

        assertEquals(0xff333333, resolved.fill);
        assertEquals(0xffabcdef, resolved.primary);
        assertEquals(0xff010203, resolved.background);
        assertEquals(4f, resolved.horizontalGap, 0);
        assertEquals(11f, resolved.radius, 0);
        assertEquals("terminal", resolved.fontFamily);
    }

    @Test
    public void sharedThemeSetsEachAxisIndependently() {
        KeyboardDocument.Profile profile = new KeyboardDocument.Profile();
        profile.theme = new KeyboardDocument.Theme();
        profile.theme.horizontalGap = 3f;
        profile.theme.verticalGap = 6f;

        KeyboardStyleResolver.Resolved resolved =
                new KeyboardStyleResolver()
                        .resolveLayout(profile, null, true, 0xffffffff, 0xff000000);

        assertEquals(3f, resolved.horizontalGap, 0);
        assertEquals(6f, resolved.verticalGap, 0);
    }
}
