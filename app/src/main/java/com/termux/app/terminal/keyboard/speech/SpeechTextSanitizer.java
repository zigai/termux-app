package com.termux.app.terminal.keyboard.speech;

/** Prevents dictated text from executing terminal commands or injecting controls. */
public final class SpeechTextSanitizer {
    private SpeechTextSanitizer() {}

    public static String forTerminal(String input) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder();
        boolean pendingSpace = false;
        for (int i = 0; i < input.length(); ) {
            int cp = input.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '\n' || cp == '\r' || cp == '\t') {
                pendingSpace = out.length() > 0;
                continue;
            }
            if (cp < 0x20 || cp == 0x7f) continue;
            if (pendingSpace && out.charAt(out.length() - 1) != ' ') out.append(' ');
            pendingSpace = false;
            out.appendCodePoint(cp);
        }
        return out.toString().trim();
    }
}
