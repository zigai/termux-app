package com.termux.app.terminal.keyboard.view;

import android.graphics.Typeface;

import com.termux.shared.termux.TermuxConstants;

/** Resolves the allowlisted keyboard font families, including the active Termux font file. */
final class KeyboardTypefaceResolver {

    private KeyboardTypefaceResolver() {}

    static Typeface resolve(String family, String weight) {
        int style = "bold".equals(weight) ? Typeface.BOLD : Typeface.NORMAL;
        if ("terminal".equals(family)) {
            try {
                if (TermuxConstants.TERMUX_FONT_FILE.isFile()
                        && TermuxConstants.TERMUX_FONT_FILE.length() > 0)
                    return Typeface.create(
                            Typeface.createFromFile(TermuxConstants.TERMUX_FONT_FILE), style);
            } catch (RuntimeException ignored) {
                // The terminal itself also falls back when a user font cannot be decoded.
            }
            return Typeface.create(Typeface.MONOSPACE, style);
        }
        if ("monospace".equals(family)) return Typeface.create(Typeface.MONOSPACE, style);
        if ("sansMedium".equals(family) || "medium".equals(weight))
            return Typeface.create("sans-serif-medium", style);
        if ("system".equals(family)) return Typeface.create(Typeface.DEFAULT, style);
        return Typeface.create("sans-serif", style);
    }
}
