package com.termux.app.terminal.keyboard.view;

import android.graphics.Color;

import com.termux.app.terminal.keyboard.config.KeyboardDocument;

/** Accessible defaults -> shared theme -> layout -> class -> per-key cascade. */
public final class KeyboardStyleResolver {

    public Resolved resolve(
            KeyboardDocument.Profile profile,
            KeyboardDocument.LayoutStyle layout,
            KeyboardDocument.Key key,
            boolean dark,
            int terminalForeground,
            int terminalBackground) {
        Resolved resolved = defaults(dark, terminalBackground);
        apply(resolved, profile.theme, terminalForeground, terminalBackground);
        apply(resolved, layout, terminalForeground, terminalBackground);
        if (key.styleClass != null && profile.styles != null)
            apply(
                    resolved,
                    profile.styles.get(key.styleClass),
                    terminalForeground,
                    terminalBackground);
        apply(resolved, key.style, terminalForeground, terminalBackground);
        return resolved;
    }

    public Resolved resolveLayout(
            KeyboardDocument.Profile profile,
            KeyboardDocument.LayoutStyle layout,
            boolean dark,
            int terminalForeground,
            int terminalBackground) {
        Resolved resolved = defaults(dark, terminalBackground);
        apply(resolved, profile.theme, terminalForeground, terminalBackground);
        apply(resolved, layout, terminalForeground, terminalBackground);
        return resolved;
    }

    private Resolved defaults(boolean dark, int terminalBackground) {
        Resolved resolved = new Resolved();
        resolved.fill = dark ? 0xff3c4043 : 0xfff1f3f4;
        resolved.border = Color.TRANSPARENT;
        resolved.primary = dark ? Color.WHITE : 0xff202124;
        resolved.secondary = dark ? 0xffbdc1c6 : 0xff5f6368;
        resolved.borderWidth = 0;
        resolved.radius = 8;
        resolved.textSize = 16;
        resolved.iconSize = 24;
        resolved.fontFamily = "sans";
        resolved.fontWeight = "normal";
        resolved.background = terminalBackground;
        resolved.pressed = dark ? 0xff5f6368 : 0xffd2d5d8;
        resolved.armed = 0xff5f7f9f;
        resolved.locked = 0xff76558f;
        resolved.horizontalGap = 5;
        resolved.verticalGap = 6;
        resolved.horizontalPadding = 5;
        resolved.verticalPadding = 6;
        resolved.secondaryTextScale = 0.55f;
        resolved.verticalAlignment = "top";
        resolved.preview = true;
        resolved.haptics = true;
        resolved.sound = false;
        resolved.repeatDelay = 500;
        resolved.repeatInterval = 55;
        return resolved;
    }

    private void apply(
            Resolved resolved, KeyboardDocument.Style style, int foreground, int background) {
        if (style == null) return;
        if (style.fill != null) resolved.fill = color(style.fill, foreground, background);
        if (style.borderColor != null)
            resolved.border = color(style.borderColor, foreground, background);
        if (style.primaryColor != null)
            resolved.primary = color(style.primaryColor, foreground, background);
        if (style.secondaryColor != null)
            resolved.secondary = color(style.secondaryColor, foreground, background);
        if (style.borderWidth != null) resolved.borderWidth = style.borderWidth;
        if (style.cornerRadius != null) resolved.radius = style.cornerRadius;
        if (style.textSize != null) resolved.textSize = style.textSize;
        if (style.iconSize != null) resolved.iconSize = style.iconSize;
        if (style.fontFamily != null) resolved.fontFamily = style.fontFamily;
        if (style.fontWeight != null) resolved.fontWeight = style.fontWeight;

        if (style instanceof KeyboardDocument.LayoutStyle) {
            KeyboardDocument.LayoutStyle layout = (KeyboardDocument.LayoutStyle) style;
            if (layout.horizontalGap != null) resolved.horizontalGap = layout.horizontalGap;
            if (layout.verticalGap != null) resolved.verticalGap = layout.verticalGap;
            if (layout.horizontalPadding != null)
                resolved.horizontalPadding = layout.horizontalPadding;
            if (layout.verticalPadding != null) resolved.verticalPadding = layout.verticalPadding;
            if (layout.secondaryTextScale != null)
                resolved.secondaryTextScale = layout.secondaryTextScale;
            if (layout.verticalAlignment != null)
                resolved.verticalAlignment = layout.verticalAlignment;
        }

        if (style instanceof KeyboardDocument.Theme) {
            KeyboardDocument.Theme theme = (KeyboardDocument.Theme) style;
            if (theme.background != null)
                resolved.background = color(theme.background, foreground, background);
            if (theme.pressedFill != null)
                resolved.pressed = color(theme.pressedFill, foreground, background);
            if (theme.armedFill != null)
                resolved.armed = color(theme.armedFill, foreground, background);
            if (theme.lockedFill != null)
                resolved.locked = color(theme.lockedFill, foreground, background);
            if (theme.preview != null) resolved.preview = theme.preview;
            if (theme.haptics != null) resolved.haptics = theme.haptics;
            if (theme.sound != null) resolved.sound = theme.sound;
            if (theme.repeatDelayMs != null)
                resolved.repeatDelay = Math.max(250, Math.min(1000, theme.repeatDelayMs));
            if (theme.repeatIntervalMs != null)
                resolved.repeatInterval = Math.max(25, Math.min(250, theme.repeatIntervalMs));
        }
    }

    private int color(String value, int foreground, int background) {
        if ("terminalForeground".equals(value)) return foreground;
        if ("terminalBackground".equals(value)) return background;
        return Color.parseColor(value);
    }

    public static final class Resolved {
        public int fill;
        public int border;
        public int primary;
        public int secondary;
        public int background;
        public int pressed;
        public int armed;
        public int locked;
        public float borderWidth;
        public float radius;
        public float textSize;
        public float iconSize;
        public float horizontalGap;
        public float verticalGap;
        public float horizontalPadding;
        public float verticalPadding;
        public float secondaryTextScale;
        public String fontFamily;
        public String fontWeight;
        public String verticalAlignment;
        public boolean preview;
        public boolean haptics;
        public boolean sound;
        public int repeatDelay;
        public int repeatInterval;
    }
}
