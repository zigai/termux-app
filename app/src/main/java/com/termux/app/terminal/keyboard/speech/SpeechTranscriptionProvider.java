package com.termux.app.terminal.keyboard.speech;

/** Pluggable native transcription engine contract; keyboard UI never depends on an engine API. */
public interface SpeechTranscriptionProvider {
    String id();

    void start(SpeechRequest request, Listener listener);

    void stop();

    void cancel();

    void destroy();

    interface Listener {
        void onReady();

        void onSpeechStarted();

        void onPartialResult(String text);

        void onFinalResult(String text);

        void onError(SpeechError error);
    }

    interface Factory {
        String id();

        boolean isAvailable();

        SpeechTranscriptionProvider create();
    }
}
