package com.termux.app.terminal.keyboard.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KeyboardConfigParserTest {

    private String bundled() throws Exception {
        return new String(
                Files.readAllBytes(
                        new File("src/main/assets/keyboard/default-keyboard.json").toPath()),
                StandardCharsets.UTF_8);
    }

    @Test
    public void bundledDocumentIsStrictlyValidResponsiveAndImmutable() throws Exception {
        KeyboardConfigParser.Result result = new KeyboardConfigParser().parse(bundled(), "bundled");

        assertTrue(result.diagnostics.toString(), result.isSuccess());
        assertEquals("compact", result.snapshot.selectVariant("default", 320).id);
        assertEquals("compact", result.snapshot.selectVariant("default", 411).id);
        assertEquals("wide", result.snapshot.selectVariant("default", 600).id);
        assertTrue(result.snapshot.defaultProfile().layers.containsKey("altgr"));

        KeyboardDocument.Variant compact = result.snapshot.defaultProfile().variants.get(0);
        KeyboardDocument.LandscapeSplit split = compact.landscapeSplit;
        assertNotNull(split);
        assertEquals(18f, split.railWidthPercent, 0);
        assertEquals(72f, split.railMinWidthDp, 0);
        assertEquals(42f, split.rowHeightDp, 0);
        assertEquals(24f, split.utilityRowHeightDp, 0);
        assertEquals(5, split.left.size());
        assertEquals(5, split.right.size());
        assertEquals(
                Arrays.asList("esc", "tab", "speech", "home"),
                split.leftUtility);
        assertEquals(
                Arrays.asList("ctrl", "minus", "equals", "slash"),
                split.rightUtility);
        assertEquals(Arrays.asList("1", "2", "3", "4", "5"), split.left.get(0));
        assertEquals(Arrays.asList("6", "7", "8", "9", "0"), split.right.get(0));
        assertEquals(Arrays.asList("q", "w", "e", "r", "t"), split.left.get(1));
        assertEquals(Arrays.asList("y", "u", "i", "o", "p"), split.right.get(1));
        assertEquals(
                Arrays.asList("space", "period", "left", "down", "right"),
                split.right.get(4));
        assertTrue(split.left.toString(), split.left.toString().contains("space"));
        assertTrue(split.right.toString(), split.right.toString().contains("space"));
        assertNotSame(compact.appearance, split.appearance);
        assertEquals(4f, compact.appearance.horizontalGap, 0);
        assertEquals(1f, split.appearance.horizontalGap, 0);

        try {
            split.left.get(0).add("q");
            fail("landscape rail rows must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
        try {
            split.leftUtility.add("q");
            fail("landscape utility row must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
        try {
            result.snapshot.defaultProfile().variants.add(null);
            fail("runtime model must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void landscapeRailReferencesGeometryAndSpaceAreStrictlyValidated() throws Exception {
        String unknown =
                bundled()
                        .replaceFirst(
                                "(\"landscapeSplit\"[\\s\\S]*?\"leftUtility\"[\\s\\S]*?)\"esc\"",
                                "$1\"missing\"");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(unknown, "unknown-rail.json")
                        .diagnostics
                        .toString(),
                new KeyboardConfigParser()
                        .parse(unknown, "unknown-rail.json")
                        .diagnostics
                        .toString()
                        .contains("unknown key id"));

        String noSpace =
                bundled()
                        .replaceFirst(
                                "(\"landscapeSplit\"[\\s\\S]*?\"left\"[\\s\\S]*?)\"space\"",
                                "$1\"period\"");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(noSpace, "no-space.json")
                        .diagnostics
                        .toString()
                        .contains("space key"));

        String badWidth =
                bundled().replaceFirst("\"railWidthPercent\": 18", "\"railWidthPercent\": 40");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(badWidth, "width.json")
                        .diagnostics
                        .toString()
                        .contains("between 12 and 20"));

        String tooManyKeys =
                bundled()
                        .replaceFirst(
                                "(\"left\"\\s*:\\s*\\[\\s*\\[\\s*\"1\"\\s*,"
                                        + "\\s*\"2\"\\s*,\\s*\"3\"\\s*,\\s*\"4\"\\s*,"
                                        + "\\s*\"5\")",
                                "$1, \"6\"");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(tooManyKeys, "six-keys.json")
                        .diagnostics
                        .toString()
                        .contains("1..5 key ids"));

        String oneUtility =
                bundled().replaceFirst("\\s*\"rightUtility\"\\s*:\\s*\\[[\\s\\S]*?\\],", "");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(oneUtility, "one-utility.json")
                        .diagnostics
                        .toString()
                        .contains("both be present"));

        String badHeight = bundled().replaceFirst("\"maxHeightDp\": 520", "\"maxHeightDp\": 900");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(badHeight, "height.json")
                        .diagnostics
                        .toString()
                        .contains("between 240 and 720"));
    }

    @Test
    public void unknownFieldsAndUnsupportedVisualTokensProducePathDiagnostics() throws Exception {
        String unknown =
                bundled()
                        .replace(
                                "\"schemaVersion\": 1,",
                                "\"schemaVersion\": 1,\n  \"surprise\": true,");
        KeyboardConfigParser.Result result =
                new KeyboardConfigParser().parse(unknown, "custom.json");
        assertFalse(result.isSuccess());
        assertTrue(result.diagnostics.get(0).toString().contains("surprise"));

        String font =
                bundled().replaceFirst("\"fontFamily\": \"sans\"", "\"fontFamily\": \"comic\"");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(font, "font.json")
                        .diagnostics
                        .toString()
                        .contains("unsupported font family"));

        String icon = bundled().replaceFirst("\"icon\": \"tab\"", "\"icon\": \"unknown\"");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(icon, "icon.json")
                        .diagnostics
                        .toString()
                        .contains("unsupported icon"));

        String color = bundled().replaceFirst("#202124", "blue-ish");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(color, "color.json")
                        .diagnostics
                        .toString()
                        .contains("semantic color"));

        String repeat =
                bundled().replaceFirst("\"repeatIntervalMs\": 55", "\"repeatIntervalMs\": 5");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(repeat, "repeat.json")
                        .diagnostics
                        .toString()
                        .contains("between 25 and 250"));
    }

    @Test
    public void overlappingRangesAndAmbiguousRepeatAreRejected() throws Exception {
        String overlap = bundled().replace("\"minWidthDp\": 520", "\"minWidthDp\": 500");
        KeyboardConfigParser.Result result =
                new KeyboardConfigParser().parse(overlap, "overlap.json");
        assertFalse(result.isSuccess());
        assertTrue(result.diagnostics.toString().contains("overlaps"));

        String ambiguous =
                bundled().replaceFirst("\"alternatives\":", "\"repeat\": true, \"alternatives\":");
        result = new KeyboardConfigParser().parse(ambiguous, "ambiguous.json");
        assertFalse(result.isSuccess());
        assertTrue(result.diagnostics.toString().contains("mutually exclusive"));
    }

    @Test
    public void duplicateIdsUnknownReferencesAndMacroCyclesAreRejected() throws Exception {
        String duplicate = bundled().replace("\"id\": \"2\",", "\"id\": \"1\",");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(duplicate, "duplicate.json")
                        .diagnostics
                        .toString()
                        .contains("duplicate"));

        String badStyle =
                bundled()
                        .replaceFirst("\"styleClass\": \"special\"", "\"styleClass\": \"missing\"");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(badStyle, "reference.json")
                        .diagnostics
                        .toString()
                        .contains("unknown style"));

        String cycle =
                bundled()
                        .replaceFirst(
                                "\"type\": \"text\"",
                                "\"type\": \"macro\", \"macro\": \"interrupt\"");
        assertTrue(
                new KeyboardConfigParser()
                        .parse(cycle, "cycle.json")
                        .diagnostics
                        .toString()
                        .contains("cycle"));
    }

    @Test
    public void bundledPlacementBlendsQwertyAndAndroidControls() throws Exception {
        KeyboardConfigSnapshot snapshot =
                new KeyboardConfigParser().parse(bundled(), "bundled").snapshot;
        assertEquals("#000000", snapshot.defaultProfile().theme.background);

        for (KeyboardDocument.Variant variant : snapshot.defaultProfile().variants) {
            assertEquals(variant.id, 6, variant.rows.size());
            assertEquals("1", firstVisible(variant.rows.get(0)).id);
            assertEquals("backspace", lastVisible(variant.rows.get(0)).id);
            assertEquals("q", firstVisible(variant.rows.get(1)).id);
            assertEquals("p", lastVisible(variant.rows.get(1)).id);
            assertEquals("a", firstVisible(variant.rows.get(2)).id);
            assertEquals("semicolon", lastVisible(variant.rows.get(2)).id);
            assertEquals(
                    "wide".equals(variant.id) ? "shiftLeft" : "shift",
                    firstVisible(variant.rows.get(3)).id);
            assertEquals("enter", lastVisible(variant.rows.get(3)).id);
            assertEquals("esc", firstVisible(variant.rows.get(4)).id);
            assertEquals("end", lastVisible(variant.rows.get(4)).id);
            assertBefore(variant.rows.get(4), "esc", "tab");
            assertBefore(variant.rows.get(4), "tab", "minus");
            assertBefore(variant.rows.get(4), "minus", "equals");
            assertBefore(variant.rows.get(4), "lbracket", "rbracket");
            assertBefore(variant.rows.get(4), "rbracket", "slash");
            assertBefore(variant.rows.get(4), "home", "up");
            assertBefore(variant.rows.get(4), "up", "end");
            assertEquals("ctrl", firstVisible(variant.rows.get(5)).id);
            assertEquals("right", lastVisible(variant.rows.get(5)).id);
            assertBefore(variant.rows.get(5), "ctrl", "alt");
            assertBefore(variant.rows.get(5), "alt", "comma");
            assertBefore(variant.rows.get(5), "comma", "speech");
            assertBefore(variant.rows.get(5), "speech", "space");
            assertBefore(variant.rows.get(5), "space", "period");
            assertBefore(variant.rows.get(5), "period", "left");
            assertBefore(variant.rows.get(5), "left", "down");
            assertBefore(variant.rows.get(5), "down", "right");

            Map<String, KeyboardDocument.Key> keys = keyMap(variant);
            assertEquals("ESC", keys.get("esc").label);
            assertEquals("CTRL", keys.get("ctrl").label);
            assertEquals("ALT", keys.get("alt").label);
            assertEquals("speechInput", keys.get("speech").action.app);
            assertEquals("/", keys.get("slash").action.text);
            assertEquals("HOME", keys.get("home").action.key);
            assertEquals("END", keys.get("end").action.key);
            assertEquals("PAGE_UP", keys.get("home").alternatives.get(0).action.key);
            assertEquals("PAGE_DOWN", keys.get("end").alternatives.get(0).action.key);
            assertEquals("DELETE", keys.get("backspace").layers.get("shift").action.key);
            assertEquals("altgr", keys.get("alt").alternatives.get(0).action.layer);
            assertEquals("/", keys.get("period").alternatives.get(0).action.text);
            assertEquals("\\", keys.get("period").layers.get("altgr").action.text);
        }
    }

    @Test
    public void bundledVariantsUseWiderRowSpecificGeometryWithoutSpacers() throws Exception {
        KeyboardConfigSnapshot snapshot =
                new KeyboardConfigParser().parse(bundled(), "bundled").snapshot;
        for (KeyboardDocument.Variant variant : snapshot.defaultProfile().variants) {
            assertEquals(variant.id, 6, variant.rows.size());
            assertEquals(variant.id, 11.5f, rowWeight(variant.rows.get(0)), 0.001f);
            for (int rowIndex = 1; rowIndex < variant.rows.size(); rowIndex++)
                assertEquals(
                        variant.id,
                        10f,
                        rowWeight(variant.rows.get(rowIndex)),
                        0.001f);
            for (KeyboardDocument.Row row : variant.rows) {
                for (KeyboardDocument.Key key : row.keys)
                    assertFalse(variant.id + " contains a spacer", Boolean.TRUE.equals(key.spacer));
            }

            Map<String, KeyboardDocument.Key> keys = keyMap(variant);
            assertEquals(variant.id, 1.5f, keys.get("backspace").width, 0f);
            assertEquals(variant.id, 1.5f, keys.get("enter").width, 0f);
            assertEquals(
                    variant.id,
                    1.5f,
                    keys.get("wide".equals(variant.id) ? "shiftLeft" : "shift").width,
                    0f);
            assertEquals(variant.id, 1.1f, keys.get("ctrl").width, 0f);
            assertEquals(variant.id, 1.1f, keys.get("alt").width, 0f);
            assertEquals(variant.id, 2.5f, keys.get("space").width, 0f);
            for (char letter = 'a'; letter <= 'z'; letter++)
                assertEquals(
                        variant.id + ":" + letter,
                        1f,
                        keys.get(String.valueOf(letter)).width,
                        0f);
            for (int digit = 0; digit <= 9; digit++)
                assertEquals(
                        variant.id + ":" + digit,
                        1f,
                        keys.get(String.valueOf(digit)).width,
                        0f);
        }
    }

    @Test
    public void everyBundledVariantKeepsTerminalActionsLayersAndAlternatives() throws Exception {
        KeyboardConfigSnapshot snapshot =
                new KeyboardConfigParser().parse(bundled(), "bundled").snapshot;
        String[] secondary = {"!", "\"", "#", "$", "%", "&", "/", "(", ")", "="};
        for (KeyboardDocument.Variant variant : snapshot.defaultProfile().variants) {
            Map<String, KeyboardDocument.Key> keys = keyMap(variant);
            for (String id :
                    new String[] {
                        "tab",
                        "a",
                        "z",
                        "enter",
                        "esc",
                        "ctrl",
                        "alt",
                        "speech",
                        "space",
                        "comma",
                        "period",
                        "slash",
                        "left",
                        "down",
                        "up",
                        "right",
                        "backspace",
                        "home",
                        "end"
                    }) {
                assertTrue(variant.id + ":" + id, keys.containsKey(id));
            }
            for (int index = 0; index < 10; index++) {
                KeyboardDocument.Key key = keys.get(String.valueOf(index == 9 ? 0 : index + 1));
                assertNotNull(key);
                assertNotNull(variant.id + ":" + key.id, key.alternatives);
                assertEquals(secondary[index], key.alternatives.get(0).action.text);
            }
            assertEquals("@", keys.get("2").layers.get("altgr").action.text);
            assertEquals("altgr", keys.get("alt").alternatives.get(0).action.layer);
            assertEquals("DELETE", keys.get("backspace").layers.get("shift").action.key);
            assertEquals("PAGE_UP", keys.get("home").alternatives.get(0).action.key);
            assertEquals("PAGE_DOWN", keys.get("end").alternatives.get(0).action.key);
            assertEquals(Boolean.TRUE, keys.get("space").cursorGesture);
            assertEquals("speechInput", keys.get("speech").action.app);
        }
        assertTrue(
                snapshot.defaultProfile()
                        .variants
                        .get(0)
                        .landscapeSplit
                        .leftUtility
                        .toString()
                        .contains("speech"));
    }

    @Test
    public void everyBundledVariantCanEmitEssentialShellPunctuation() throws Exception {
        KeyboardConfigSnapshot snapshot =
                new KeyboardConfigParser().parse(bundled(), "bundled").snapshot;
        Set<String> required =
                new HashSet<>(
                        Arrays.asList(
                                "_", ":", "?", "+", "*", ";", "<", ">", "'", "[",
                                "]", "{", "}", "\\", "|", "~", "$", "="));
        for (KeyboardDocument.Variant variant : snapshot.defaultProfile().variants) {
            Set<String> emitted = new HashSet<>();
            for (KeyboardDocument.Row row : variant.rows) {
                for (KeyboardDocument.Key key : row.keys) {
                    addText(emitted, key.action);
                    if (key.alternatives != null)
                        for (KeyboardDocument.Alternative alternative : key.alternatives)
                            addText(emitted, alternative.action);
                    if (key.layers != null)
                        for (KeyboardDocument.Override override : key.layers.values())
                            if (override != null) addText(emitted, override.action);
                }
            }
            assertTrue(variant.id + " missing " + difference(required, emitted),
                    emitted.containsAll(required));
        }
    }

    private Map<String, KeyboardDocument.Key> keyMap(KeyboardDocument.Variant variant) {
        Map<String, KeyboardDocument.Key> keys = new HashMap<>();
        for (KeyboardDocument.Row row : variant.rows)
            for (KeyboardDocument.Key key : row.keys) keys.put(key.id, key);
        return keys;
    }

    private void addText(Set<String> output, KeyboardAction action) {
        if (action != null && "text".equals(action.type) && action.text != null)
            output.add(action.text);
    }

    private Set<String> difference(Set<String> required, Set<String> actual) {
        Set<String> missing = new HashSet<>(required);
        missing.removeAll(actual);
        return missing;
    }

    private KeyboardDocument.Key last(KeyboardDocument.Row row) {
        return row.keys.get(row.keys.size() - 1);
    }

    private KeyboardDocument.Key firstVisible(KeyboardDocument.Row row) {
        for (KeyboardDocument.Key key : row.keys)
            if (!Boolean.TRUE.equals(key.spacer)) return key;
        throw new AssertionError("row has no visible key");
    }

    private KeyboardDocument.Key lastVisible(KeyboardDocument.Row row) {
        for (int index = row.keys.size() - 1; index >= 0; index--) {
            KeyboardDocument.Key key = row.keys.get(index);
            if (!Boolean.TRUE.equals(key.spacer)) return key;
        }
        throw new AssertionError("row has no visible key");
    }

    private float rowWeight(KeyboardDocument.Row row) {
        float weight = 0;
        for (KeyboardDocument.Key key : row.keys) weight += key.width;
        return weight;
    }

    private void assertBefore(KeyboardDocument.Row row, String first, String second) {
        int firstIndex = -1;
        int secondIndex = -1;
        for (int index = 0; index < row.keys.size(); index++) {
            if (first.equals(row.keys.get(index).id)) firstIndex = index;
            if (second.equals(row.keys.get(index).id)) secondIndex = index;
        }
        assertTrue(
                first + " should precede " + second, firstIndex >= 0 && firstIndex < secondIndex);
    }
}
