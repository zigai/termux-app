package com.termux.app.terminal.keyboard.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.app.terminal.keyboard.config.KeyboardDocument;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class KeyboardPopupViewTest {
    @Test
    public void slideSelectionClampsToLastAlternativeAndDismisses() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        View anchor = new View(activity);
        activity.setContentView(anchor);
        anchor.layout(0, 0, 80, 50);
        KeyboardDocument.Alternative a = alternative("a"), b = alternative("b");
        KeyboardPopupView popup = new KeyboardPopupView(activity);
        popup.showAlternatives(anchor, Arrays.asList(a, b), 40, style());
        popup.update(Float.MAX_VALUE);
        assertSame(b, popup.selected());
        popup.dismiss();
        assertFalse(popup.isShowing());
    }

    @Config(sdk = 28)
    @Test
    public void popupUsesResolvedColorsTypographyAndCornerRadius() {
        if (Build.VERSION.SDK_INT < 24)
            throw new AssertionError("Drawable inspection requires API 24 or newer");
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        View anchor = new View(activity);
        activity.setContentView(anchor);
        anchor.layout(0, 0, 80, 50);
        KeyboardStyleResolver.Resolved style = style();
        KeyboardPopupView popup = new KeyboardPopupView(activity);

        popup.showPreview(anchor, "a", style);

        LinearLayout content = popup.contentView();
        GradientDrawable background = (GradientDrawable) content.getBackground();
        float density = activity.getResources().getDisplayMetrics().density;
        assertEquals(style.pressed, background.getColor().getDefaultColor());
        assertEquals(style.radius * density, background.getCornerRadius(), 0.01f);
        TextView option = (TextView) content.getChildAt(0);
        assertEquals(style.primary, option.getCurrentTextColor());

        popup.dismiss();
        popup.showAlternatives(
                anchor,
                Arrays.asList(alternative("a"), alternative("b")),
                40,
                style);
        GradientDrawable selected =
                (GradientDrawable) popup.contentView().getChildAt(0).getBackground();
        assertEquals(style.armed, selected.getColor().getDefaultColor());
    }

    private KeyboardDocument.Alternative alternative(String label) {
        KeyboardDocument.Alternative a = new KeyboardDocument.Alternative();
        a.label = label;
        return a;
    }

    private KeyboardStyleResolver.Resolved style() {
        KeyboardStyleResolver.Resolved style = new KeyboardStyleResolver.Resolved();
        style.primary = 0xfff7f7f7;
        style.border = 0xff010203;
        style.pressed = 0xff4b4e52;
        style.armed = 0xff486987;
        style.borderWidth = 0;
        style.radius = 13;
        style.textSize = 16;
        style.fontFamily = "sans";
        style.fontWeight = "normal";
        return style;
    }
}
