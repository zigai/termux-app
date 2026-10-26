package com.termux.app.terminal.keyboard.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;

import com.termux.app.terminal.keyboard.config.KeyboardConfigParser;
import com.termux.app.terminal.keyboard.config.KeyboardConfigSnapshot;
import com.termux.app.terminal.keyboard.config.KeyboardDocument;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public class TerminalInputHostControllerTest {

    @Test
    public void keyboardIsFullWidthInPortraitAndSplitOnLandscapePhones() {
        KeyboardDocument.LandscapeSplit split = new KeyboardDocument.LandscapeSplit();
        split.maxHeightDp = 520;

        assertFalse(
                TerminalInputHostController.isSplitLayout(
                        Configuration.ORIENTATION_PORTRAIT, 400, split));
        assertTrue(
                TerminalInputHostController.isSplitLayout(
                        Configuration.ORIENTATION_LANDSCAPE, 400, split));
        assertFalse(
                TerminalInputHostController.isSplitLayout(
                        Configuration.ORIENTATION_LANDSCAPE, 800, split));
    }

    @Test
    public void wideLandscapeVariantReusesConfiguredCompactRails() {
        KeyboardDocument.Variant compact = new KeyboardDocument.Variant();
        compact.landscapeSplit = new KeyboardDocument.LandscapeSplit();
        KeyboardDocument.Variant wide = new KeyboardDocument.Variant();
        KeyboardDocument.Profile profile = new KeyboardDocument.Profile();
        profile.variants = Arrays.asList(compact, wide);

        assertSame(compact, TerminalInputHostController.selectSplitVariant(profile, wide));
        assertSame(compact, TerminalInputHostController.selectSplitVariant(profile, compact));
    }

    @Test
    public void landscapeRailsAlwaysLeaveAtLeastSixtyPercentForTerminal() {
        KeyboardDocument.LandscapeSplit split = new KeyboardDocument.LandscapeSplit();
        split.railMinWidthDp = 72;

        assertEquals(216, TerminalInputHostController.railWidthPixels(1080, 3, split, 20));
        assertEquals(192, TerminalInputHostController.railWidthPixels(960, 3, split, 12));
        assertEquals(216, TerminalInputHostController.railWidthPixels(1080, 3, split, 99));
        assertTrue(1080 - 2 * TerminalInputHostController.railWidthPixels(1080, 3, split, 99)
                >= 1080 * .60f);
    }

    @Test
    public void invalidReloadDoesNotReapplyTheCurrentSnapshot() throws Exception {
        KeyboardConfigSnapshot snapshot = bundledSnapshot();

        assertTrue(TerminalInputHostController.shouldApplyLoadedSnapshot(null, snapshot, true));
        assertTrue(TerminalInputHostController.shouldApplyLoadedSnapshot(null, snapshot, false));
        assertFalse(
                TerminalInputHostController.shouldApplyLoadedSnapshot(snapshot, snapshot, false));
        assertFalse(TerminalInputHostController.shouldApplyLoadedSnapshot(snapshot, null, true));
    }

    @Test
    public void systemKeyboardModeRetainsTheExistingExtraKeysToolbar() {
        TerminalInputHostController.InputSurfaceState full =
                TerminalInputHostController.inputSurfaceState(true, true, false, true);
        assertTrue(full.fullKeyboard);
        assertFalse(full.leftRail);
        assertFalse(full.rightRail);
        assertFalse(full.legacyToolbar);

        TerminalInputHostController.InputSurfaceState split =
                TerminalInputHostController.inputSurfaceState(true, true, true, true);
        assertFalse(split.fullKeyboard);
        assertTrue(split.leftRail);
        assertTrue(split.rightRail);
        assertFalse(split.legacyToolbar);

        TerminalInputHostController.InputSurfaceState hidden =
                TerminalInputHostController.inputSurfaceState(true, false, true, true);
        assertFalse(hidden.fullKeyboard);
        assertFalse(hidden.leftRail);
        assertFalse(hidden.rightRail);
        assertFalse(hidden.legacyToolbar);

        TerminalInputHostController.InputSurfaceState system =
                TerminalInputHostController.inputSurfaceState(false, true, true, true);
        assertFalse(system.fullKeyboard);
        assertFalse(system.leftRail);
        assertFalse(system.rightRail);
        assertTrue(system.legacyToolbar);

        TerminalInputHostController.InputSurfaceState systemWithoutToolbar =
                TerminalInputHostController.inputSurfaceState(false, true, true, false);
        assertFalse(systemWithoutToolbar.legacyToolbar);
    }

    private static KeyboardConfigSnapshot bundledSnapshot() throws Exception {
        String json =
                new String(
                        Files.readAllBytes(
                                new File(
                                                "src/main/assets/keyboard/"
                                                        + "default-keyboard.json")
                                        .toPath()),
                        StandardCharsets.UTF_8);
        return new KeyboardConfigParser().parse(json, "bundled").snapshot;
    }
}
