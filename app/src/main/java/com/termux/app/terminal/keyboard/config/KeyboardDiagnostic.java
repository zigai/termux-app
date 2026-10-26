package com.termux.app.terminal.keyboard.config;

import androidx.annotation.NonNull;

/** A safe, path-addressed parse or semantic validation error. */
public final class KeyboardDiagnostic {
    public final String file;
    public final String path;
    public final String rule;
    public final String value;

    public KeyboardDiagnostic(String file, String path, String rule, Object value) {
        this.file = file;
        this.path = path;
        this.rule = rule;
        String text = String.valueOf(value);
        this.value = text.length() > 120 ? text.substring(0, 120) + "…" : text;
    }

    @NonNull
    @Override
    public String toString() {
        return file + ": " + path + ": " + rule + " (" + value + ")";
    }
}
