package com.termux.app.terminal.keyboard.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

/** Native Android RecognitionService adapter for system and API 31+ on-device engines. */
public final class AndroidSpeechRecognizerProvider implements SpeechTranscriptionProvider {
    private final Context context;
    private final boolean onDevice;
    private SpeechRecognizer recognizer;
    private Listener listener;
    private int generation;

    private AndroidSpeechRecognizerProvider(Context context, boolean onDevice) {
        this.context = context.getApplicationContext();
        this.onDevice = onDevice;
    }

    @Override
    public String id() {
        return onDevice
                ? SpeechProviderRegistry.ANDROID_ON_DEVICE
                : SpeechProviderRegistry.ANDROID_SYSTEM;
    }

    @Override
    public void start(SpeechRequest request, Listener callback) {
        cancel();
        listener = callback;
        int current = ++generation;
        try {
            recognizer = createRecognizer();
            recognizer.setRecognitionListener(new Callback(current));
            recognizer.startListening(buildIntent(request));
        } catch (RuntimeException e) {
            destroyRecognizer();
            if (listener != null)
                listener.onError(
                        new SpeechError(
                                SpeechError.Kind.UNAVAILABLE,
                                "Speech recognition could not start",
                                -1));
        }
    }

    private SpeechRecognizer createRecognizer() {
        if (onDevice) {
            if (Build.VERSION.SDK_INT < 31) throw new UnsupportedOperationException();
            return Api31.createOnDevice(context);
        }
        return SpeechRecognizer.createSpeechRecognizer(context);
    }

    static Intent buildIntent(SpeechRequest request) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, request.partialResults);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        if (!request.languageTag.isEmpty())
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, request.languageTag);
        return intent;
    }

    @Override
    public void stop() {
        if (recognizer != null)
            try {
                recognizer.stopListening();
            } catch (RuntimeException ignored) {
            }
    }

    @Override
    public void cancel() {
        generation++;
        if (recognizer != null)
            try {
                recognizer.cancel();
            } catch (RuntimeException ignored) {
            }
        destroyRecognizer();
        listener = null;
    }

    @Override
    public void destroy() {
        cancel();
    }

    private void destroyRecognizer() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }

    private boolean active(int value) {
        return value == generation && listener != null;
    }

    private String first(Bundle bundle) {
        ArrayList<String> results =
                bundle == null
                        ? null
                        : bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return results == null || results.isEmpty() ? "" : results.get(0);
    }

    private final class Callback implements RecognitionListener {
        private final int callbackGeneration;

        Callback(int generation) {
            callbackGeneration = generation;
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            if (active(callbackGeneration)) listener.onReady();
        }

        @Override
        public void onBeginningOfSpeech() {
            if (active(callbackGeneration)) listener.onSpeechStarted();
        }

        @Override
        public void onPartialResults(Bundle results) {
            if (active(callbackGeneration)) {
                String text = first(results);
                if (!text.isEmpty()) listener.onPartialResult(text);
            }
        }

        @Override
        public void onResults(Bundle results) {
            if (!active(callbackGeneration)) return;
            String text = first(results);
            Listener callback = listener;
            destroyRecognizer();
            listener = null;
            if (text.isEmpty())
                callback.onError(
                        new SpeechError(
                                SpeechError.Kind.NO_MATCH,
                                "No speech was recognized",
                                SpeechRecognizer.ERROR_NO_MATCH));
            else callback.onFinalResult(text);
        }

        @Override
        public void onError(int error) {
            if (!active(callbackGeneration)) return;
            Listener callback = listener;
            destroyRecognizer();
            listener = null;
            callback.onError(mapError(error));
        }

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onEvent(int eventType, Bundle params) {}
    }

    static SpeechError mapError(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return new SpeechError(SpeechError.Kind.AUDIO, "Microphone audio error", error);
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return new SpeechError(
                        SpeechError.Kind.NETWORK, "Speech service network error", error);
            case SpeechRecognizer.ERROR_NO_MATCH:
                return new SpeechError(
                        SpeechError.Kind.NO_MATCH, "No speech was recognized", error);
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return new SpeechError(SpeechError.Kind.TIMEOUT, "No speech was heard", error);
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return new SpeechError(SpeechError.Kind.BUSY, "Speech recognizer is busy", error);
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return new SpeechError(
                        SpeechError.Kind.PERMISSION, "Microphone permission is required", error);
            case SpeechRecognizer.ERROR_CLIENT:
                return new SpeechError(
                        SpeechError.Kind.CLIENT, "Speech recognition was cancelled", error);
            default:
                if (Build.VERSION.SDK_INT >= 31
                        && (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                                || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
                    return new SpeechError(
                            SpeechError.Kind.LANGUAGE,
                            "Requested speech language is unavailable",
                            error);
                return new SpeechError(
                        SpeechError.Kind.UNKNOWN,
                        "Speech recognition failed (" + error + ")",
                        error);
        }
    }

    public static final class Factory implements SpeechTranscriptionProvider.Factory {
        private final Context context;
        private final boolean onDevice;

        public Factory(Context context, boolean onDevice) {
            this.context = context.getApplicationContext();
            this.onDevice = onDevice;
        }

        @Override
        public String id() {
            return onDevice
                    ? SpeechProviderRegistry.ANDROID_ON_DEVICE
                    : SpeechProviderRegistry.ANDROID_SYSTEM;
        }

        @Override
        public boolean isAvailable() {
            return onDevice
                    ? Build.VERSION.SDK_INT >= 31 && Api31.onDeviceAvailable(context)
                    : SpeechRecognizer.isRecognitionAvailable(context);
        }

        @Override
        public SpeechTranscriptionProvider create() {
            return new AndroidSpeechRecognizerProvider(context, onDevice);
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static final class Api31 {
        static boolean onDeviceAvailable(Context context) {
            return SpeechRecognizer.isOnDeviceRecognitionAvailable(context);
        }

        static SpeechRecognizer createOnDevice(Context context) {
            return SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
        }
    }
}
