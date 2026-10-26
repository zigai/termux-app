package com.termux.app.terminal.keyboard.speech;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves user choices without coupling settings or keyboard actions to provider implementations.
 */
public final class SpeechProviderRegistry {
    public static final String AUTOMATIC = "automatic",
            ANDROID_ON_DEVICE = "android_on_device",
            ANDROID_SYSTEM = "android_system";
    private final Map<String, SpeechTranscriptionProvider.Factory> factories;

    public SpeechProviderRegistry(List<SpeechTranscriptionProvider.Factory> factories) {
        Map<String, SpeechTranscriptionProvider.Factory> map = new LinkedHashMap<>();
        for (SpeechTranscriptionProvider.Factory factory : factories)
            map.put(factory.id(), factory);
        this.factories = Collections.unmodifiableMap(map);
    }

    public Resolution resolve(String requested) {
        String mode = requested == null ? AUTOMATIC : requested;
        if (AUTOMATIC.equals(mode)) return resolveAutomaticAfter(null);
        return available(mode);
    }

    public Resolution resolveAutomaticAfter(String currentId) {
        boolean past = currentId == null;
        for (SpeechTranscriptionProvider.Factory factory : factories.values()) {
            if (!past) {
                if (factory.id().equals(currentId)) past = true;
                continue;
            }
            if (factory.isAvailable()) return new Resolution(factory.id(), factory.create());
        }
        return null;
    }

    private Resolution available(String id) {
        SpeechTranscriptionProvider.Factory factory = factories.get(id);
        return factory != null && factory.isAvailable()
                ? new Resolution(id, factory.create())
                : null;
    }

    public List<String> availableProviderIds() {
        List<String> ids = new ArrayList<>();
        for (SpeechTranscriptionProvider.Factory factory : factories.values())
            if (factory.isAvailable()) ids.add(factory.id());
        return Collections.unmodifiableList(ids);
    }

    public static final class Resolution {
        public final String id;
        public final SpeechTranscriptionProvider provider;

        Resolution(String id, SpeechTranscriptionProvider provider) {
            this.id = id;
            this.provider = provider;
        }
    }
}
