package com.termux.app.terminal.keyboard.speech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;

public class SpeechProviderRegistryTest {
    @Test
    public void automaticPrefersOnDeviceAndFallsBackToSystem() {
        FakeFactory onDevice = new FakeFactory(SpeechProviderRegistry.ANDROID_ON_DEVICE, true),
                system = new FakeFactory(SpeechProviderRegistry.ANDROID_SYSTEM, true);
        SpeechProviderRegistry registry =
                new SpeechProviderRegistry(Arrays.asList(onDevice, system));
        assertEquals(
                SpeechProviderRegistry.ANDROID_ON_DEVICE,
                registry.resolve(SpeechProviderRegistry.AUTOMATIC).id);
        onDevice.available = false;
        assertEquals(
                SpeechProviderRegistry.ANDROID_SYSTEM,
                registry.resolve(SpeechProviderRegistry.AUTOMATIC).id);
    }

    @Test
    public void explicitModeNeverSilentlyChangesProvider() {
        FakeFactory onDevice = new FakeFactory(SpeechProviderRegistry.ANDROID_ON_DEVICE, false),
                system = new FakeFactory(SpeechProviderRegistry.ANDROID_SYSTEM, true);
        SpeechProviderRegistry registry =
                new SpeechProviderRegistry(Arrays.asList(onDevice, system));
        assertNull(registry.resolve(SpeechProviderRegistry.ANDROID_ON_DEVICE));
        assertEquals(
                SpeechProviderRegistry.ANDROID_SYSTEM,
                registry.resolve(SpeechProviderRegistry.ANDROID_SYSTEM).id);
    }

    @Test
    public void automaticOrderIsRegistryDrivenForFutureProviders() {
        FakeFactory embedded = new FakeFactory("embedded", true),
                system = new FakeFactory(SpeechProviderRegistry.ANDROID_SYSTEM, true);
        SpeechProviderRegistry registry =
                new SpeechProviderRegistry(Arrays.asList(embedded, system));
        assertEquals("embedded", registry.resolve(SpeechProviderRegistry.AUTOMATIC).id);
        assertEquals(
                SpeechProviderRegistry.ANDROID_SYSTEM,
                registry.resolveAutomaticAfter("embedded").id);
    }

    private static final class FakeFactory implements SpeechTranscriptionProvider.Factory {
        final String id;
        boolean available;

        FakeFactory(String id, boolean available) {
            this.id = id;
            this.available = available;
        }

        public String id() {
            return id;
        }

        public boolean isAvailable() {
            return available;
        }

        public SpeechTranscriptionProvider create() {
            return new FakeProvider(id);
        }
    }

    private static final class FakeProvider implements SpeechTranscriptionProvider {
        final String id;

        FakeProvider(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public void start(SpeechRequest r, Listener l) {}

        public void stop() {}

        public void cancel() {}

        public void destroy() {}
    }
}
