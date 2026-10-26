package com.termux.app.terminal.keyboard.speech;

/** Stable error independent of a specific transcription implementation. */
public final class SpeechError {
    public enum Kind {
        PERMISSION,
        UNAVAILABLE,
        AUDIO,
        NETWORK,
        NO_MATCH,
        TIMEOUT,
        LANGUAGE,
        BUSY,
        CLIENT,
        UNKNOWN
    }

    public final Kind kind;
    public final String message;
    public final int providerCode;

    public SpeechError(Kind kind, String message, int providerCode) {
        this.kind = kind;
        this.message = message;
        this.providerCode = providerCode;
    }
}
