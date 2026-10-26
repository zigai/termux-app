package com.termux.app.terminal.keyboard.speech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.View;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RunWith(RobolectricTestRunner.class)
public class SpeechManifestAndAccessibilityTest {

    @Test
    public void manifestDeclaresOptionalMicrophonePermissionAndNativeRecognitionQuery()
            throws Exception {
        String manifest =
                new String(
                        Files.readAllBytes(new File("src/main/AndroidManifest.xml").toPath()),
                        StandardCharsets.UTF_8);
        assertTrue(manifest.contains("android.permission.RECORD_AUDIO"));
        assertTrue(manifest.contains("android.hardware.microphone"));
        assertTrue(manifest.contains("android:required=\"false\""));
        assertTrue(manifest.contains("android.speech.RecognitionService"));
    }

    @Test
    public void activityLayoutMeasuresAndSpeechOverlayIsAccessible() {
        org.robolectric.android.controller.ActivityController<Activity> controller =
                Robolectric.buildActivity(Activity.class);
        Activity activity = controller.get();
        activity.setTheme(R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        controller.setup();
        activity.setContentView(R.layout.activity_termux);

        View root = activity.findViewById(R.id.activity_termux_root_view);
        root.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2340, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1080, 2340);

        View overlay = activity.findViewById(R.id.speech_input_overlay);
        assertEquals(View.GONE, overlay.getVisibility());
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, overlay.getAccessibilityLiveRegion());
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, overlay.getImportantForAccessibility());
        assertNotNull(overlay.getContentDescription());
        assertTrue(activity.findViewById(R.id.speech_input_stop).isClickable());
        assertTrue(activity.findViewById(R.id.speech_input_cancel).isClickable());
    }
}
