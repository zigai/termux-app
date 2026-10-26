package com.termux.app.terminal.keyboard.speech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SpeechInputControllerTest {
    @Test
    public void permissionIsContextualThenPartialAndSafeFinalTextAreRendered() {
        Fixture f = new Fixture(false, true);
        f.controller.toggle();
        assertEquals(SpeechInputController.State.REQUESTING_PERMISSION, f.host.state);
        assertEquals(1, f.host.permissionRequests);
        f.host.permission = true;
        f.controller.onPermissionResult(true);
        assertTrue(f.provider.started);
        assertEquals("en-US", f.provider.request.languageTag);
        f.provider.listener.onReady();
        f.provider.listener.onPartialResult("echo hello");
        assertEquals("echo hello", f.host.transcript);
        f.provider.listener.onFinalResult("echo hello\nwhoami\u0000");
        assertEquals("echo hello whoami", f.host.inserted);
        assertEquals(SpeechInputController.State.RESULT, f.host.state);
        assertTrue(f.provider.destroyed);
    }

    @Test
    public void denialAndUnavailableProviderProduceVisibleDiagnostics() {
        Fixture denied = new Fixture(false, true);
        denied.controller.toggle();
        denied.controller.onPermissionResult(false);
        assertEquals(SpeechInputController.State.ERROR, denied.host.state);
        assertTrue(denied.settings.diagnostic.contains("PERMISSION"));
        Fixture unavailable = new Fixture(true, false);
        unavailable.controller.toggle();
        assertEquals(SpeechInputController.State.ERROR, unavailable.host.state);
        assertTrue(unavailable.host.status.contains("No compatible"));
    }

    @Test
    public void secondTapStopsAndLifecycleCancelDestroysProvider() {
        Fixture f = new Fixture(true, true);
        f.controller.toggle();
        f.provider.listener.onReady();
        f.controller.toggle();
        assertTrue(f.provider.stopped);
        assertEquals(SpeechInputController.State.PROCESSING, f.host.state);
        f.provider.listener.onPartialResult("late partial");
        assertEquals(SpeechInputController.State.PROCESSING, f.host.state);
        f.controller.cancel();
        assertTrue(f.provider.cancelled);
        assertTrue(f.provider.destroyed);
        assertEquals(SpeechInputController.State.IDLE, f.host.state);
    }

    @Test
    public void providerErrorsAreVisibleAndDiagnosed() {
        Fixture f = new Fixture(true, true);
        f.controller.toggle();
        f.provider.listener.onError(new SpeechError(SpeechError.Kind.NETWORK, "network down", 2));
        assertEquals(SpeechInputController.State.ERROR, f.host.state);
        assertEquals("network down", f.host.status);
        assertTrue(f.settings.diagnostic.contains("NETWORK"));
    }

    @Test
    public void automaticRecoverableFailureAdvancesToNextRegisteredProvider() {
        FakeProvider first = new FakeProvider();
        FakeProvider second = new FakeProvider();
        SpeechTranscriptionProvider.Factory a = factory("embedded", first, true);
        SpeechTranscriptionProvider.Factory b =
                factory(SpeechProviderRegistry.ANDROID_SYSTEM, second, true);
        FakeHost host = new FakeHost(true);
        FakeSettings settings = new FakeSettings();
        SpeechInputController controller =
                new SpeechInputController(
                        new SpeechProviderRegistry(Arrays.asList(a, b)), settings, host);
        controller.toggle();
        first.listener.onError(new SpeechError(SpeechError.Kind.UNAVAILABLE, "model absent", -1));
        assertTrue(first.destroyed);
        assertTrue(second.started);
        assertTrue(host.status.contains("Previous provider unavailable"));
    }

    @Test
    public void rotationCancelsRecognitionAndInvalidatesPendingPermission() {
        Fixture listening = new Fixture(true, true);
        listening.controller.toggle();
        listening.provider.listener.onReady();
        listening.controller.onConfigurationChanged();
        assertTrue(listening.provider.cancelled);
        assertTrue(listening.provider.destroyed);
        assertEquals(SpeechInputController.State.IDLE, listening.host.state);
        Fixture permission = new Fixture(false, true);
        permission.controller.toggle();
        permission.controller.onConfigurationChanged();
        permission.host.permission = true;
        permission.controller.onPermissionResult(true);
        assertFalse(permission.provider.started);
        assertEquals(SpeechInputController.State.IDLE, permission.host.state);
    }

    private static final class Fixture {
        final FakeProvider provider = new FakeProvider();
        final FakeHost host;
        final FakeSettings settings = new FakeSettings();
        final SpeechInputController controller;

        Fixture(boolean permission, boolean available) {
            host = new FakeHost(permission);
            SpeechTranscriptionProvider.Factory factory =
                    new SpeechTranscriptionProvider.Factory() {
                        public String id() {
                            return SpeechProviderRegistry.ANDROID_SYSTEM;
                        }

                        public boolean isAvailable() {
                            return available;
                        }

                        public SpeechTranscriptionProvider create() {
                            return provider;
                        }
                    };
            controller =
                    new SpeechInputController(
                            new SpeechProviderRegistry(Collections.singletonList(factory)),
                            settings,
                            host);
        }
    }

    private static SpeechTranscriptionProvider.Factory factory(
            String id, FakeProvider provider, boolean available) {
        return new SpeechTranscriptionProvider.Factory() {
            public String id() {
                return id;
            }

            public boolean isAvailable() {
                return available;
            }

            public SpeechTranscriptionProvider create() {
                return provider;
            }
        };
    }

    private static final class FakeProvider implements SpeechTranscriptionProvider {
        Listener listener;
        SpeechRequest request;
        boolean started;
        boolean stopped;
        boolean cancelled;
        boolean destroyed;

        public String id() {
            return SpeechProviderRegistry.ANDROID_SYSTEM;
        }

        public void start(SpeechRequest request, Listener listener) {
            this.request = request;
            this.listener = listener;
            started = true;
        }

        public void stop() {
            stopped = true;
        }

        public void cancel() {
            cancelled = true;
        }

        public void destroy() {
            destroyed = true;
        }
    }

    private static final class FakeSettings implements SpeechInputController.Settings {
        String diagnostic = "";

        public String providerMode() {
            return SpeechProviderRegistry.AUTOMATIC;
        }

        public String languageTag() {
            return "en-US";
        }

        public void diagnostic(String value) {
            diagnostic = value;
        }
    }

    private static final class FakeHost implements SpeechInputController.Host {
        boolean permission;
        int permissionRequests;
        String inserted = "", status = "", transcript = "", announcement = "";
        SpeechInputController.State state = SpeechInputController.State.IDLE;

        FakeHost(boolean p) {
            permission = p;
        }

        public boolean hasMicrophonePermission() {
            return permission;
        }

        public void requestMicrophonePermission() {
            permissionRequests++;
        }

        public void insertText(String text) {
            inserted += text;
        }

        public void render(SpeechInputController.State state, String status, String transcript) {
            this.state = state;
            this.status = status;
            this.transcript = transcript;
        }

        public void announce(String text) {
            announcement = text;
        }
    }
}
