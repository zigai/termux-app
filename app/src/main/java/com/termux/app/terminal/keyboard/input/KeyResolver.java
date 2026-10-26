package com.termux.app.terminal.keyboard.input;

import com.termux.app.terminal.keyboard.config.KeyboardAction;
import com.termux.app.terminal.keyboard.config.KeyboardDocument;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

    /** Resolves dynamic legend/action once whenever state changes. */
public final class KeyResolver {
    public Resolved resolve(KeyboardDocument.Key key, KeyboardState state) {
        String label = key.label;
        String icon = key.icon;
        String secondary = key.secondaryLabel;
        KeyboardAction action = key.action;
        Set<String> active = state.activeLayers();
        if (state.activeModifier("SHIFT")) active = withShift(active);
        for (String layer : active) {
            KeyboardDocument.Override o = key.layers == null ? null : key.layers.get(layer);
            if (o != null) {
                if (o.label != null) label = o.label;
                if (o.icon != null) icon = o.icon;
                if (o.secondaryLabel != null) secondary = o.secondaryLabel;
                if (o.action != null) action = o.action;
            }
        }
        if (state.activeModifier("SHIFT")
                && action != null
                && "text".equals(action.type)
                && action.text != null
                && (key.layers == null || !key.layers.containsKey("shift"))) {
            String upper = action.text.toUpperCase(Locale.ROOT);
            KeyboardAction copy = copy(action);
            copy.text = upper;
            action = copy;
            if (label != null) label = label.toUpperCase(Locale.ROOT);
        }
        return new Resolved(label, icon, secondary, action);
    }

    private Set<String> withShift(Set<String> s) {
        LinkedHashSet<String> r = new LinkedHashSet<>(s);
        r.add("shift");
        return r;
    }

    private KeyboardAction copy(KeyboardAction a) {
        KeyboardAction b = new KeyboardAction();
        b.type = a.type;
        b.text = a.text;
        b.key = a.key;
        b.modifier = a.modifier;
        b.layer = a.layer;
        b.mode = a.mode;
        b.macro = a.macro;
        b.app = a.app;
        b.profile = a.profile;
        b.modifiers = a.modifiers;
        b.delayMs = a.delayMs;
        return b;
    }

    public static final class Resolved {
        public final String label;
        public final String icon;
        public final String secondaryLabel;
        public final KeyboardAction action;

        Resolved(String l, String i, String s, KeyboardAction a) {
            label = l;
            icon = i;
            secondaryLabel = s;
            action = a;
        }
    }
}
