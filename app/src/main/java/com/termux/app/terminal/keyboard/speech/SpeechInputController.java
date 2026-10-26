package com.termux.app.terminal.keyboard.speech;

/** Permission, provider selection, UI state, and terminal-safe result orchestration. */
public final class SpeechInputController implements SpeechTranscriptionProvider.Listener {
    public enum State {
        IDLE,
        REQUESTING_PERMISSION,
        STARTING,
        LISTENING,
        PROCESSING,
        RESULT,
        ERROR
    }

    private final SpeechProviderRegistry registry;
    private final Settings settings;
    private final Host host;
    private SpeechTranscriptionProvider provider;
    private String activeProviderId;
    private State state = State.IDLE;
    private boolean permissionPending;

    public SpeechInputController(SpeechProviderRegistry registry, Settings settings, Host host) {
        this.registry = registry;
        this.settings = settings;
        this.host = host;
    }

    public State state() {
        return state;
    }

    public void toggle() {
        if (provider != null && (state == State.STARTING || state == State.LISTENING)) {
            state = State.PROCESSING;
            host.render(state, "Finishing speech…", "");
            provider.stop();
            return;
        }
        if (state == State.PROCESSING) return;
        if (!host.hasMicrophonePermission()) {
            permissionPending = true;
            state = State.REQUESTING_PERMISSION;
            host.render(state, "Microphone permission is required", "");
            host.requestMicrophonePermission();
            return;
        }
        start();
    }

    public void onPermissionResult(boolean granted) {
        if (!permissionPending) return;
        permissionPending = false;
        if (granted) start();
        else
            fail(
                    new SpeechError(
                            SpeechError.Kind.PERMISSION, "Microphone permission was denied", -1));
    }

    private void start() {
        cancelProvider();
        start(registry.resolve(settings.providerMode()), false);
    }

    private void start(SpeechProviderRegistry.Resolution resolution, boolean fallback) {
        if (resolution == null) {
            fail(
                    new SpeechError(
                            SpeechError.Kind.UNAVAILABLE,
                            "No compatible speech recognition provider is available",
                            -1));
            return;
        }
        provider = resolution.provider;
        activeProviderId = resolution.id;
        state = State.STARTING;
        String label = providerLabel(resolution.id);
        settings.diagnostic((fallback ? "Fell back to " : "Using ") + label);
        host.render(
                state,
                (fallback ? "Previous provider unavailable; starting " : "Starting ") + label + "…",
                "");
        host.announce(
                fallback ? "Falling back to another speech provider" : "Starting voice input");
        provider.start(new SpeechRequest(settings.languageTag(), true), this);
    }

    private String providerLabel(String id) {
        if (SpeechProviderRegistry.ANDROID_ON_DEVICE.equals(id))
            return "on-device speech recognition";
        if (SpeechProviderRegistry.ANDROID_SYSTEM.equals(id)) return "Android speech recognition";
        return id;
    }

    @Override
    public void onReady() {
        if (!acceptLiveUpdate()) return;
        state = State.LISTENING;
        host.render(state, "Listening…", "");
        host.announce("Listening for speech");
    }

    @Override
    public void onSpeechStarted() {
        if (!acceptLiveUpdate()) return;
        state = State.LISTENING;
        host.render(state, "Listening…", "");
    }

    @Override
    public void onPartialResult(String text) {
        if (!acceptLiveUpdate()) return;
        state = State.LISTENING;
        host.render(state, "Listening…", text);
    }

    private boolean acceptLiveUpdate() {
        return provider != null && (state == State.STARTING || state == State.LISTENING);
    }

    @Override
    public void onFinalResult(String text) {
        if (provider == null) return;
        provider.destroy();
        provider = null;
        String safe = SpeechTextSanitizer.forTerminal(text);
        if (safe.isEmpty()) {
            fail(
                    new SpeechError(
                            SpeechError.Kind.NO_MATCH,
                            "Speech result contained no insertable text",
                            -1));
            return;
        }
        host.insertText(safe);
        state = State.RESULT;
        host.render(state, "Voice text inserted", safe);
        host.announce("Voice text inserted");
    }

    @Override
    public void onError(SpeechError error) {
        if (provider == null) return;
        provider.destroy();
        provider = null;
        if (SpeechProviderRegistry.AUTOMATIC.equals(settings.providerMode())
                && (error.kind == SpeechError.Kind.UNAVAILABLE
                        || error.kind == SpeechError.Kind.LANGUAGE)) {
            SpeechProviderRegistry.Resolution fallback =
                    registry.resolveAutomaticAfter(activeProviderId);
            if (fallback != null) {
                start(fallback, true);
                return;
            }
        }
        fail(error);
    }

    private void fail(SpeechError error) {
        state = State.ERROR;
        settings.diagnostic(error.kind + ": " + error.message);
        host.render(state, error.message, "");
        host.announce(error.message);
    }

    public void dismiss() {
        cancel();
        state = State.IDLE;
        host.render(state, "", "");
    }

    public void onConfigurationChanged() {
        cancel();
    }

    public void cancel() {
        permissionPending = false;
        cancelProvider();
        state = State.IDLE;
        host.render(state, "", "");
    }

    public void destroy() {
        cancel();
    }

    private void cancelProvider() {
        if (provider != null) {
            provider.cancel();
            provider.destroy();
            provider = null;
        }
        activeProviderId = null;
    }

    public interface Settings {
        String providerMode();

        String languageTag();

        void diagnostic(String value);
    }

    public interface Host {
        boolean hasMicrophonePermission();

        void requestMicrophonePermission();

        void insertText(String text);

        void render(State state, String status, String transcript);

        void announce(String text);
    }
}
