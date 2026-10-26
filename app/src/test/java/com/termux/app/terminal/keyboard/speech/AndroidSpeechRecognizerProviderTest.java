package com.termux.app.terminal.keyboard.speech;

import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AndroidSpeechRecognizerProviderTest {
    @Test
    public void platformErrorsMapToStableProviderNeutralKinds() {
        assertEquals(
                SpeechError.Kind.AUDIO,
                AndroidSpeechRecognizerProvider.mapError(SpeechRecognizer.ERROR_AUDIO).kind);
        assertEquals(
                SpeechError.Kind.NETWORK,
                AndroidSpeechRecognizerProvider.mapError(SpeechRecognizer.ERROR_NETWORK).kind);
        assertEquals(
                SpeechError.Kind.NO_MATCH,
                AndroidSpeechRecognizerProvider.mapError(SpeechRecognizer.ERROR_NO_MATCH).kind);
        assertEquals(
                SpeechError.Kind.PERMISSION,
                AndroidSpeechRecognizerProvider.mapError(
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
                        .kind);
    }

    @Test
    public void nativeRecognizerIntentRequestsFreeFormPartialLocalizedText() {
        Intent intent =
                AndroidSpeechRecognizerProvider.buildIntent(new SpeechRequest("sl-SI", true));
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.getAction());
        assertEquals(
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL));
        assertEquals("sl-SI", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE));
        assertEquals(true, intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false));
        assertEquals(1, intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0));
    }
}
