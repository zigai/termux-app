package com.termux.shared.activities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;

import com.termux.shared.R;
import com.termux.shared.models.TextIOInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class TextIOActivityTest {

    @Test
    public void backReturnsTheEditedText() {
        TextIOInfo info = new TextIOInfo("edit", "test");
        info.setText("Original text");
        Intent intent = new Intent().putExtra(TextIOActivity.EXTRA_TEXT_IO_INFO_OBJECT, info);
        try (ActivityController<TextIOActivity> controller =
                 Robolectric.buildActivity(TextIOActivity.class, intent)) {
            controller.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat_NoActionBar);
            controller.setup();
            TextIOActivity activity = controller.get();
            assertFalse(activity.isFinishing());
            ((EditText) activity.findViewById(R.id.text_io_text)).setText("Edited text");

            activity.getOnBackPressedDispatcher().onBackPressed();

            assertTrue(activity.isFinishing());
            assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).getResultCode());
            TextIOInfo result = (TextIOInfo) Shadows.shadowOf(activity).getResultIntent()
                .getSerializableExtra(TextIOActivity.EXTRA_TEXT_IO_INFO_OBJECT);
            assertEquals("Edited text", result.getText());
        }
    }
}
