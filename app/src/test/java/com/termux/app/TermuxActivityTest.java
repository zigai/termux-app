package com.termux.app;

import com.termux.shared.termux.data.TermuxUrlUtils;

import android.os.Looper;
import android.view.Gravity;
import android.view.View;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;

@RunWith(RobolectricTestRunner.class)
public class TermuxActivityTest {

    @Test
    public void backClosesDrawerBeforeFinishingWhenServiceIsUnavailable() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication())
            .setThrowInBindService(new SecurityException("Service unavailable"));
        try (ActivityController<TermuxActivity> controller =
                 Robolectric.buildActivity(TermuxActivity.class).setup()) {
            TermuxActivity activity = controller.get();
            Assert.assertFalse(activity.isFinishing());
            activity.getDrawer().measure(
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY));
            activity.getDrawer().layout(0, 0, 400, 800);
            activity.getDrawer().openDrawer(Gravity.LEFT, false);

            activity.getOnBackPressedDispatcher().onBackPressed();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            activity.getDrawer().computeScroll();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            Assert.assertFalse(activity.getDrawer().isDrawerOpen(Gravity.LEFT));
            Assert.assertFalse(activity.isFinishing());

            activity.getOnBackPressedDispatcher().onBackPressed();

            Assert.assertTrue(activity.isFinishing());
        }
    }

    private void assertUrlsAre(String text, String... urls) {
        LinkedHashSet<String> expected = new LinkedHashSet<>();
        Collections.addAll(expected, urls);
        Assert.assertEquals(expected, TermuxUrlUtils.extractUrls(text));
    }

    @Test
    public void testExtractUrls() {
        assertUrlsAre("hello http://example.com world", "http://example.com");

        assertUrlsAre("http://example.com\nhttp://another.com", "http://example.com", "http://another.com");

        assertUrlsAre("hello http://example.com world and http://more.example.com with secure https://more.example.com",
            "http://example.com", "http://more.example.com", "https://more.example.com");

        assertUrlsAre("hello https://example.com/#bar https://example.com/foo#bar",
            "https://example.com/#bar", "https://example.com/foo#bar");
    }

}
