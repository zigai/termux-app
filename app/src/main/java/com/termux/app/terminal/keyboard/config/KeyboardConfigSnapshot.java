package com.termux.app.terminal.keyboard.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validated snapshot whose top-level collections cannot be mutated by consumers. */
public final class KeyboardConfigSnapshot {
    public final KeyboardDocument document;
    public final String source;
    public final Map<String, KeyboardDocument.Profile> profiles;

    KeyboardConfigSnapshot(KeyboardDocument document, String source) {
        freeze(document);
        this.document = document;
        this.source = source;
        this.profiles = Collections.unmodifiableMap(new LinkedHashMap<>(document.profiles));
    }

    public KeyboardDocument.Profile defaultProfile() {
        return profiles.get(document.defaultProfile);
    }

    public KeyboardDocument.Variant selectVariant(String profileId, float widthDp) {
        KeyboardDocument.Profile p = profiles.get(profileId);
        if (p == null) p = defaultProfile();
        for (KeyboardDocument.Variant v : p.variants) {
            if (widthDp >= v.minWidthDp && (v.maxWidthDp == null || widthDp < v.maxWidthDp))
                return v;
        }
        return p.variants.get(0);
    }

    public List<String> profileIds() {
        return Collections.unmodifiableList(new ArrayList<>(profiles.keySet()));
    }

    private static void freeze(KeyboardDocument d) {
        Map<String, KeyboardDocument.Profile> frozenProfiles = new LinkedHashMap<>();
        for (Map.Entry<String, KeyboardDocument.Profile> pe : d.profiles.entrySet()) {
            KeyboardDocument.Profile p = pe.getValue();
            if (p.styles != null)
                p.styles = Collections.unmodifiableMap(new LinkedHashMap<>(p.styles));
            if (p.layers != null)
                p.layers = Collections.unmodifiableMap(new LinkedHashMap<>(p.layers));
            if (p.macros != null) {
                Map<String, List<KeyboardAction>> macros = new LinkedHashMap<>();
                for (Map.Entry<String, List<KeyboardAction>> me : p.macros.entrySet()) {
                    for (KeyboardAction a : me.getValue()) freeze(a);
                    macros.put(
                            me.getKey(),
                            Collections.unmodifiableList(new ArrayList<>(me.getValue())));
                }
                p.macros = Collections.unmodifiableMap(macros);
            }
            List<KeyboardDocument.Variant> variants = new ArrayList<>();
            for (KeyboardDocument.Variant v : p.variants) {
                List<KeyboardDocument.Row> rows = new ArrayList<>();
                for (KeyboardDocument.Row row : v.rows) {
                    List<KeyboardDocument.Key> keys = new ArrayList<>();
                    for (KeyboardDocument.Key key : row.keys) {
                        freeze(key.action);
                        if (key.alternatives != null) {
                            for (KeyboardDocument.Alternative a : key.alternatives)
                                freeze(a.action);
                            key.alternatives =
                                    Collections.unmodifiableList(new ArrayList<>(key.alternatives));
                        }
                        if (key.layers != null) {
                            for (KeyboardDocument.Override o : key.layers.values())
                                if (o != null) freeze(o.action);
                            key.layers =
                                    Collections.unmodifiableMap(new LinkedHashMap<>(key.layers));
                        }
                        keys.add(key);
                    }
                    row.keys = Collections.unmodifiableList(keys);
                    rows.add(row);
                }
                v.rows = Collections.unmodifiableList(rows);
                freezeSplit(v.landscapeSplit);
                variants.add(v);
            }
            p.variants = Collections.unmodifiableList(variants);
            frozenProfiles.put(pe.getKey(), p);
        }
        d.profiles = Collections.unmodifiableMap(frozenProfiles);
    }

    private static void freezeSplit(KeyboardDocument.LandscapeSplit split) {
        if (split == null) return;
        split.leftUtility = freezeRow(split.leftUtility);
        split.rightUtility = freezeRow(split.rightUtility);
        split.left = freezeRail(split.left);
        split.right = freezeRail(split.right);
    }

    private static List<String> freezeRow(List<String> row) {
        return row == null ? null : Collections.unmodifiableList(new ArrayList<>(row));
    }

    private static List<List<String>> freezeRail(List<List<String>> rail) {
        if (rail == null) return null;
        List<List<String>> rows = new ArrayList<>();
        for (List<String> row : rail) rows.add(Collections.unmodifiableList(new ArrayList<>(row)));
        return Collections.unmodifiableList(rows);
    }

    private static void freeze(KeyboardAction action) {
        if (action != null && action.modifiers != null)
            action.modifiers = Collections.unmodifiableList(new ArrayList<>(action.modifiers));
    }
}
