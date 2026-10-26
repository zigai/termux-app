package com.termux.app.terminal.keyboard.config;

import com.squareup.moshi.Json;

import java.util.List;
import java.util.Map;

/** Typed DTOs for schema version 1. Semantic validation runs before publication. */
public final class KeyboardDocument {
    @Json(name = "$schema")
    public String schema;

    public int schemaVersion;
    public String defaultProfile;
    public Map<String, Profile> profiles;

    public static final class Profile {
        public Theme theme;
        public Map<String, Style> styles;
        public Map<String, List<KeyboardAction>> macros;
        public Map<String, Layer> layers;
        public List<Variant> variants;
    }

    public static class Style {
        public String fill;
        public String borderColor;
        public Float borderWidth;
        public String primaryColor;
        public String secondaryColor;
        public Float cornerRadius;
        public Float textSize;
        public Float iconSize;
        public String fontFamily;
        public String fontWeight;
    }

    /** Geometry and typography that may override the shared theme for one layout. */
    public static class LayoutStyle extends Style {
        public Float horizontalGap;
        public Float verticalGap;
        public Float horizontalPadding;
        public Float verticalPadding;
        public Float secondaryTextScale;
        public String verticalAlignment;
    }

    public static final class Theme extends LayoutStyle {
        public String background;
        public String pressedFill;
        public String armedFill;
        public String lockedFill;
        public Boolean preview;
        public Boolean haptics;
        public Boolean sound;
        public Integer repeatDelayMs;
        public Integer repeatIntervalMs;
    }

    public static final class Layer {
        public String label;
    }

    public static final class Variant {
        public String id;
        public float minWidthDp;
        public Float maxWidthDp;
        public float rowHeightDp;
        public List<Row> rows;
        public LayoutStyle appearance;
        public LandscapeSplit landscapeSplit;
    }

    /** Optional phone-landscape layout composed from keys declared by the variant. */
    public static final class LandscapeSplit {
        public float railWidthPercent = 18f;
        public float railMinWidthDp = 72f;
        public float maxHeightDp = 520f;
        public float rowHeightDp = 36f;
        public float utilityRowHeightDp = 24f;
        public LayoutStyle appearance;
        public List<String> leftUtility;
        public List<String> rightUtility;
        public List<List<String>> left;
        public List<List<String>> right;
    }

    public static final class Row {
        public float offset;
        public float endOffset;
        public Float horizontalGap;
        public Float verticalGap;
        public Float height;
        public List<Key> keys;
    }

    public static final class Key {
        public String id;
        public String label;
        public String icon;
        public String secondaryLabel;
        public String accessibilityLabel;
        public float width = 1f;
        public Boolean spacer;
        public String styleClass;
        public Style style;
        public Boolean repeat;
        public Boolean cursorGesture;
        public Boolean preview;
        public KeyboardAction action;
        public List<Alternative> alternatives;
        public Map<String, Override> layers;
    }

    public static final class Alternative {
        public String label;
        public KeyboardAction action;
    }

    public static final class Override {
        public String label;
        public String icon;
        public String secondaryLabel;
        public KeyboardAction action;
    }
}
