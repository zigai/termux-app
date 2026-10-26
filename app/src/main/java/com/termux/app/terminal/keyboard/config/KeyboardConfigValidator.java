package com.termux.app.terminal.keyboard.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure Java semantic validator for constraints not expressible conveniently in JSON Schema. */
public final class KeyboardConfigValidator {
    private static final Pattern COLOR =
            Pattern.compile(
                    "^(#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?|terminalForeground|terminalBackground)$");
    private static final Set<String> ACTIONS =
            new HashSet<>(
                    Arrays.asList(
                            "text",
                            "terminalKey",
                            "modifier",
                            "layer",
                            "macro",
                            "app",
                            "profile",
                            "delay"));
    private static final Set<String> MODIFIERS =
            new HashSet<>(Arrays.asList("CTRL", "ALT", "SHIFT"));
    private static final Set<String> MODES =
            new HashSet<>(Arrays.asList("momentary", "once", "locked", "smart"));
    private static final Set<String> APPS =
            new HashSet<>(Arrays.asList("paste", "drawer", "hideKeyboard", "speechInput"));
    private static final Set<String> TERMINAL_KEYS =
            new HashSet<>(
                    Arrays.asList(
                            "ENTER",
                            "ESCAPE",
                            "TAB",
                            "BACKSPACE",
                            "UP",
                            "DOWN",
                            "LEFT",
                            "RIGHT",
                            "HOME",
                            "END",
                            "PAGE_UP",
                            "PAGE_DOWN",
                            "INSERT",
                            "DELETE",
                            "F1",
                            "F2",
                            "F3",
                            "F4",
                            "F5",
                            "F6",
                            "F7",
                            "F8",
                            "F9",
                            "F10",
                            "F11",
                            "F12"));
    private static final Set<String> ICONS =
            new HashSet<>(
                    Arrays.asList(
                            "shift",
                            "backspace",
                            "enter",
                            "microphone",
                            "tab",
                            "space",
                            "arrowUp",
                            "arrowDown",
                            "arrowLeft",
                            "arrowRight"));
    private static final Set<String> FONT_FAMILIES =
            new HashSet<>(Arrays.asList("system", "sans", "sansMedium", "monospace", "terminal"));
    private static final Set<String> FONT_WEIGHTS =
            new HashSet<>(Arrays.asList("normal", "medium", "bold"));
    private static final Set<String> VERTICAL_ALIGNMENTS =
            new HashSet<>(Arrays.asList("top", "center", "bottom"));
    private final String file;
    private final List<KeyboardDiagnostic> errors = new ArrayList<>();

    public KeyboardConfigValidator(String file) {
        this.file = file;
    }

    public List<KeyboardDiagnostic> validate(KeyboardDocument d) {
        errors.clear();
        if (d.schemaVersion != 1) error("$.schemaVersion", "must equal 1", d.schemaVersion);
        if (d.profiles == null || d.profiles.isEmpty())
            return one("$.profiles", "must not be empty", d.profiles);
        if (d.defaultProfile == null || !d.profiles.containsKey(d.defaultProfile))
            error("$.defaultProfile", "unknown profile", d.defaultProfile);
        for (Map.Entry<String, KeyboardDocument.Profile> pe : d.profiles.entrySet())
            validateProfile(d, pe.getKey(), pe.getValue());
        return new ArrayList<>(errors);
    }

    private void validateProfile(KeyboardDocument d, String id, KeyboardDocument.Profile p) {
        String path = "$.profiles." + id;
        if (p == null) {
            error(path, "profile is null", null);
            return;
        }
        validateTheme(p.theme, path + ".theme");
        if (p.styles != null)
            for (Map.Entry<String, KeyboardDocument.Style> style : p.styles.entrySet())
                validateStyle(style.getValue(), path + ".styles." + style.getKey());
        if (p.variants == null || p.variants.isEmpty())
            error(path + ".variants", "must not be empty", p.variants);
        else {
            Set<String> variantIds = new HashSet<>();
            for (int i = 0; i < p.variants.size(); i++) {
                KeyboardDocument.Variant v = p.variants.get(i);
                String vp = path + ".variants[" + i + "]";
                if (v == null) {
                    error(vp, "variant is null", null);
                    continue;
                }
                if (v.id == null || !variantIds.add(v.id))
                    error(vp + ".id", "missing or duplicate id", v.id);
                if (!finite(v.minWidthDp) || v.minWidthDp < 0)
                    error(vp + ".minWidthDp", "must be finite and non-negative", v.minWidthDp);
                if (v.maxWidthDp != null && (!finite(v.maxWidthDp) || v.maxWidthDp <= v.minWidthDp))
                    error(vp + ".maxWidthDp", "must exceed minWidthDp", v.maxWidthDp);
                if (!finite(v.rowHeightDp) || v.rowHeightDp < 24 || v.rowHeightDp > 96)
                    error(vp + ".rowHeightDp", "must be between 24 and 96", v.rowHeightDp);
                validateLayoutStyle(v.appearance, vp + ".appearance");
                validateRows(d, p, v, vp);
                validateLandscapeSplit(v, vp);
                for (int j = i + 1; j < p.variants.size(); j++)
                    if (overlap(v, p.variants.get(j)))
                        error(vp, "responsive width range overlaps " + p.variants.get(j).id, v.id);
            }
        }
        if (p.macros != null)
            for (Map.Entry<String, List<KeyboardAction>> m : p.macros.entrySet()) {
                if (m.getValue() == null || m.getValue().isEmpty() || m.getValue().size() > 64)
                    error(
                            path + ".macros." + m.getKey(),
                            "macro must contain 1..64 steps",
                            m.getValue());
                else
                    for (int i = 0; i < m.getValue().size(); i++)
                        validateAction(
                                d,
                                p,
                                m.getValue().get(i),
                                path + ".macros." + m.getKey() + "[" + i + "]",
                                true);
            }
        if (p.macros != null)
            for (String macro : p.macros.keySet())
                detectMacroCycle(
                        p, macro, new HashSet<>(), new HashSet<>(), path + ".macros." + macro);
    }

    private void validateLandscapeSplit(KeyboardDocument.Variant variant, String path) {
        KeyboardDocument.LandscapeSplit split = variant.landscapeSplit;
        if (split == null) return;
        String sp = path + ".landscapeSplit";
        if (!finite(split.railWidthPercent)
                || split.railWidthPercent < 12
                || split.railWidthPercent > 20)
            error(sp + ".railWidthPercent", "must be between 12 and 20", split.railWidthPercent);
        if (!finite(split.railMinWidthDp)
                || split.railMinWidthDp < 48
                || split.railMinWidthDp > 120)
            error(sp + ".railMinWidthDp", "must be between 48 and 120", split.railMinWidthDp);
        if (!finite(split.maxHeightDp) || split.maxHeightDp < 240 || split.maxHeightDp > 720)
            error(sp + ".maxHeightDp", "must be between 240 and 720", split.maxHeightDp);
        if (!finite(split.rowHeightDp) || split.rowHeightDp < 24 || split.rowHeightDp > 96)
            error(sp + ".rowHeightDp", "must be between 24 and 96", split.rowHeightDp);
        if (!finite(split.utilityRowHeightDp)
                || split.utilityRowHeightDp < 24
                || split.utilityRowHeightDp > 48)
            error(
                    sp + ".utilityRowHeightDp",
                    "must be between 24 and 48",
                    split.utilityRowHeightDp);
        validateLayoutStyle(split.appearance, sp + ".appearance");
        Set<String> available = new HashSet<>();
        if (variant.rows != null)
            for (KeyboardDocument.Row row : variant.rows)
                if (row != null && row.keys != null)
                    for (KeyboardDocument.Key key : row.keys)
                        if (key != null && !Boolean.TRUE.equals(key.spacer) && key.id != null)
                            available.add(key.id);
        if ((split.leftUtility == null) != (split.rightUtility == null))
            error(
                    sp,
                    "leftUtility and rightUtility must either both be present or both be omitted",
                    null);
        validateRail(
                split.left,
                sp + ".left",
                split.leftUtility,
                sp + ".leftUtility",
                available);
        validateRail(
                split.right,
                sp + ".right",
                split.rightUtility,
                sp + ".rightUtility",
                available);
    }

    private void validateRail(
            List<List<String>> rail,
            String path,
            List<String> utility,
            String utilityPath,
            Set<String> available) {
        if (rail == null || rail.isEmpty()) {
            error(path, "must contain at least one row", rail);
            return;
        }
        Set<String> used = new HashSet<>();
        validateRailRow(utility, utilityPath, available, used, true);
        boolean space = false;
        for (int ri = 0; ri < rail.size(); ri++) {
            List<String> row = rail.get(ri);
            String rp = path + "[" + ri + "]";
            if (row != null && row.contains("space")) space = true;
            validateRailRow(row, rp, available, used, false);
        }
        if (!space) error(path, "must include the space key", rail);
    }

    private void validateRailRow(
            List<String> row,
            String path,
            Set<String> available,
            Set<String> used,
            boolean optional) {
        if (row == null && optional) return;
        if (row == null || row.isEmpty() || row.size() > 5) {
            error(path, "row must contain 1..5 key ids", row);
            return;
        }
        for (int ki = 0; ki < row.size(); ki++) {
            String id = row.get(ki);
            if (id == null || !available.contains(id))
                error(path + "[" + ki + "]", "unknown key id", id);
            else if (!used.add(id))
                error(path + "[" + ki + "]", "duplicate key id in rail", id);
        }
    }

    private void detectMacroCycle(
            KeyboardDocument.Profile p,
            String name,
            Set<String> visiting,
            Set<String> visited,
            String path) {
        if (visited.contains(name)) return;
        if (!visiting.add(name)) {
            error(path, "macro reference cycle", name);
            return;
        }
        List<KeyboardAction> steps = p.macros.get(name);
        if (steps != null)
            for (KeyboardAction a : steps)
                if (a != null && "macro".equals(a.type) && p.macros.containsKey(a.macro))
                    detectMacroCycle(p, a.macro, visiting, visited, path + " -> " + a.macro);
        visiting.remove(name);
        visited.add(name);
    }

    private void validateRows(
            KeyboardDocument d,
            KeyboardDocument.Profile p,
            KeyboardDocument.Variant v,
            String path) {
        if (v.rows == null || v.rows.isEmpty()) {
            error(path + ".rows", "must not be empty", v.rows);
            return;
        }
        Set<String> ids = new HashSet<>();
        for (int ri = 0; ri < v.rows.size(); ri++) {
            KeyboardDocument.Row row = v.rows.get(ri);
            String rp = path + ".rows[" + ri + "]";
            if (row == null || row.keys == null || row.keys.isEmpty()) {
                error(rp, "row must have keys", row);
                continue;
            }
            if (!finite(row.offset) || row.offset < 0)
                error(rp + ".offset", "must be finite and non-negative", row.offset);
            if (!finite(row.endOffset) || row.endOffset < 0)
                error(rp + ".endOffset", "must be finite and non-negative", row.endOffset);
            validateOptionalRange(row.horizontalGap, rp + ".horizontalGap", 0, 24);
            validateOptionalRange(row.verticalGap, rp + ".verticalGap", 0, 24);
            validateOptionalRange(row.height, rp + ".height", 24, 96);
            for (int ki = 0; ki < row.keys.size(); ki++) {
                KeyboardDocument.Key k = row.keys.get(ki);
                String kp = rp + ".keys[" + ki + "]";
                if (k == null) {
                    error(kp, "key is null", null);
                    continue;
                }
                boolean spacer = Boolean.TRUE.equals(k.spacer);
                if (!finite(k.width) || k.width <= 0)
                    error(kp + ".width", "must be finite and positive", k.width);
                if (!spacer && (k.id == null || !ids.add(k.id)))
                    error(kp + ".id", "missing or duplicate key id", k.id);
                if (!spacer && empty(k.label) && empty(k.icon))
                    error(kp, "either label or icon is required", k.id);
                if (!spacer && k.icon != null && !ICONS.contains(k.icon))
                    error(kp + ".icon", "unsupported icon", k.icon);
                if (!spacer && k.icon != null && empty(k.accessibilityLabel))
                    error(
                            kp + ".accessibilityLabel",
                            "required for icon-only keys",
                            k.accessibilityLabel);
                if (!spacer) validateAction(d, p, k.action, kp + ".action", false);
                if (Boolean.TRUE.equals(k.repeat)
                        && k.alternatives != null
                        && !k.alternatives.isEmpty())
                    error(kp, "repeat and long-press alternatives are mutually exclusive", k.id);
                if (Boolean.TRUE.equals(k.cursorGesture)
                        && (Boolean.TRUE.equals(k.repeat)
                                || (k.alternatives != null && !k.alternatives.isEmpty())))
                    error(
                            kp,
                            "cursor gesture, repeat, and long-press alternatives are mutually"
                                    + " exclusive",
                            k.id);
                if (k.styleClass != null
                        && (p.styles == null || !p.styles.containsKey(k.styleClass)))
                    error(kp + ".styleClass", "unknown style", k.styleClass);
                validateStyle(k.style, kp + ".style");
                if (k.alternatives != null)
                    for (int ai = 0; ai < k.alternatives.size(); ai++) {
                        KeyboardDocument.Alternative a = k.alternatives.get(ai);
                        if (a == null || a.label == null)
                            error(kp + ".alternatives[" + ai + "]", "label required", a);
                        else
                            validateAction(
                                    d, p, a.action, kp + ".alternatives[" + ai + "].action", false);
                    }
                if (k.layers != null)
                    for (Map.Entry<String, KeyboardDocument.Override> o : k.layers.entrySet()) {
                        if (p.layers == null || !p.layers.containsKey(o.getKey()))
                            error(kp + ".layers." + o.getKey(), "unknown layer", o.getKey());
                        if (o.getValue() != null
                                && o.getValue().icon != null
                                && !ICONS.contains(o.getValue().icon))
                            error(
                                    kp + ".layers." + o.getKey() + ".icon",
                                    "unsupported icon",
                                    o.getValue().icon);
                        if (o.getValue() != null && o.getValue().action != null)
                            validateAction(
                                    d,
                                    p,
                                    o.getValue().action,
                                    kp + ".layers." + o.getKey() + ".action",
                                    false);
                    }
            }
        }
    }

    private void validateAction(
            KeyboardDocument d,
            KeyboardDocument.Profile p,
            KeyboardAction a,
            String path,
            boolean macroStep) {
        if (a == null || a.type == null || !ACTIONS.contains(a.type)) {
            error(path + ".type", "unknown or missing action type", a == null ? null : a.type);
            return;
        }
        switch (a.type) {
            case "text":
                if (a.text == null) error(path + ".text", "required", null);
                break;
            case "terminalKey":
                if (!TERMINAL_KEYS.contains(a.key))
                    error(path + ".key", "unsupported terminal key", a.key);
                break;
            case "modifier":
                if (!MODIFIERS.contains(a.modifier) || !mode(a.mode))
                    error(path, "invalid modifier or mode", a.modifier + "/" + a.mode);
                break;
            case "layer":
                if (p.layers == null || !p.layers.containsKey(a.layer) || !mode(a.mode))
                    error(path, "unknown layer or mode", a.layer + "/" + a.mode);
                break;
            case "macro":
                if (p.macros == null || !p.macros.containsKey(a.macro))
                    error(path + ".macro", "unknown macro", a.macro);
                break;
            case "profile":
                if (d.profiles == null
                        || (!"cycle".equals(a.profile) && !d.profiles.containsKey(a.profile)))
                    error(path + ".profile", "unknown profile", a.profile);
                break;
            case "app":
                if (!APPS.contains(a.app))
                    error(path + ".app", "app action is not allowlisted", a.app);
                break;
            case "delay":
                if (!macroStep || a.delayMs == null || a.delayMs < 0 || a.delayMs > 2000)
                    error(path + ".delayMs", "macro delay must be 0..2000ms", a.delayMs);
                break;
        }
        if (!macroStep && "delay".equals(a.type))
            error(path, "delay is only valid in macros", a.type);
        if (a.modifiers != null)
            for (String m : a.modifiers)
                if (!MODIFIERS.contains(m)) error(path + ".modifiers", "unknown modifier", m);
    }

    private boolean mode(String mode) {
        return mode != null && MODES.contains(mode);
    }

    private void validateTheme(KeyboardDocument.Theme theme, String path) {
        validateLayoutStyle(theme, path);
        if (theme == null) return;
        validateColor(theme.background, path + ".background");
        validateColor(theme.pressedFill, path + ".pressedFill");
        validateColor(theme.armedFill, path + ".armedFill");
        validateColor(theme.lockedFill, path + ".lockedFill");
        if (theme.repeatDelayMs != null
                && (theme.repeatDelayMs < 250 || theme.repeatDelayMs > 1000))
            error(path + ".repeatDelayMs", "must be between 250 and 1000", theme.repeatDelayMs);
        if (theme.repeatIntervalMs != null
                && (theme.repeatIntervalMs < 25 || theme.repeatIntervalMs > 250))
            error(path + ".repeatIntervalMs", "must be between 25 and 250", theme.repeatIntervalMs);
    }

    private void validateLayoutStyle(KeyboardDocument.LayoutStyle style, String path) {
        validateStyle(style, path);
        if (style == null) return;
        validateOptionalRange(style.horizontalGap, path + ".horizontalGap", 0, 24);
        validateOptionalRange(style.verticalGap, path + ".verticalGap", 0, 24);
        validateOptionalRange(style.horizontalPadding, path + ".horizontalPadding", 0, 32);
        validateOptionalRange(style.verticalPadding, path + ".verticalPadding", 0, 32);
        validateOptionalRange(style.secondaryTextScale, path + ".secondaryTextScale", 0.35f, 1f);
        if (style.verticalAlignment != null
                && !VERTICAL_ALIGNMENTS.contains(style.verticalAlignment))
            error(
                    path + ".verticalAlignment",
                    "must be top, center, or bottom",
                    style.verticalAlignment);
    }

    private void validateStyle(KeyboardDocument.Style style, String path) {
        if (style == null) return;
        validateColor(style.fill, path + ".fill");
        validateColor(style.borderColor, path + ".borderColor");
        validateColor(style.primaryColor, path + ".primaryColor");
        validateColor(style.secondaryColor, path + ".secondaryColor");
        validateOptionalRange(style.borderWidth, path + ".borderWidth", 0, 8);
        validateOptionalRange(style.cornerRadius, path + ".cornerRadius", 0, 32);
        validateOptionalRange(style.textSize, path + ".textSize", 8, 32);
        validateOptionalRange(style.iconSize, path + ".iconSize", 12, 40);
        if (style.fontFamily != null && !FONT_FAMILIES.contains(style.fontFamily))
            error(path + ".fontFamily", "unsupported font family", style.fontFamily);
        if (style.fontWeight != null && !FONT_WEIGHTS.contains(style.fontWeight))
            error(path + ".fontWeight", "must be normal, medium, or bold", style.fontWeight);
    }

    private void validateColor(String value, String path) {
        if (value != null && !COLOR.matcher(value).matches())
            error(path, "must be a hex or terminal semantic color", value);
    }

    private void validateOptionalRange(Float value, String path, float min, float max) {
        if (value != null && (!finite(value) || value < min || value > max))
            error(path, "must be between " + min + " and " + max, value);
    }

    private boolean empty(String value) {
        return value == null || value.isEmpty();
    }

    private boolean overlap(KeyboardDocument.Variant a, KeyboardDocument.Variant b) {
        if (a == null || b == null) return false;
        float ae = a.maxWidthDp == null ? Float.POSITIVE_INFINITY : a.maxWidthDp;
        float be = b.maxWidthDp == null ? Float.POSITIVE_INFINITY : b.maxWidthDp;
        return a.minWidthDp < be && b.minWidthDp < ae;
    }

    private boolean finite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }

    private void error(String path, String rule, Object value) {
        errors.add(new KeyboardDiagnostic(file, path, rule, value));
    }

    private List<KeyboardDiagnostic> one(String path, String rule, Object value) {
        error(path, rule, value);
        return new ArrayList<>(errors);
    }
}
