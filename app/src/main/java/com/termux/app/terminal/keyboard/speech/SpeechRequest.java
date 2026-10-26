package com.termux.app.terminal.keyboard.speech;

/** Provider-neutral transcription request. An empty language uses the provider default. */
public final class SpeechRequest {
    public final String languageTag;
    public final boolean partialResults;

    public SpeechRequest(String languageTag, boolean partialResults) {
        this.languageTag = languageTag == null ? "" : languageTag.trim();
        this.partialResults = partialResults;
    }
}
