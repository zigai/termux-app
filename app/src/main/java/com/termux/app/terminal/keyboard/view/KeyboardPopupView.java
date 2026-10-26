package com.termux.app.terminal.keyboard.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.termux.app.terminal.keyboard.config.KeyboardDocument;

import java.util.Collections;
import java.util.List;

/** Clamped long-press/preview popup with slide-selection hit testing. */
public final class KeyboardPopupView {
    private final PopupWindow popup;
    private final LinearLayout row;
    private final float density;
    private List<KeyboardDocument.Alternative> alternatives;
    private KeyboardStyleResolver.Resolved style;
    private int selected;
    private boolean preview;

    public KeyboardPopupView(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipToOutline(true);
        popup =
                new PopupWindow(
                        row,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        false);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setClippingEnabled(true);
        popup.setElevation(dp(8));
    }

    public void showAlternatives(
            View anchor,
            List<KeyboardDocument.Alternative> values,
            float rawX,
            KeyboardStyleResolver.Resolved popupStyle) {
        show(anchor, values, rawX, popupStyle, false);
    }

    public void showPreview(
            View anchor, String label, KeyboardStyleResolver.Resolved popupStyle) {
        if (label == null) return;
        KeyboardDocument.Alternative alternative = new KeyboardDocument.Alternative();
        alternative.label = label;
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        show(
                anchor,
                Collections.singletonList(alternative),
                location[0] + anchor.getWidth() / 2f,
                popupStyle,
                true);
    }

    private void show(
            View anchor,
            List<KeyboardDocument.Alternative> values,
            float rawX,
            KeyboardStyleResolver.Resolved popupStyle,
            boolean isPreview) {
        if (values == null || values.isEmpty() || popupStyle == null) return;
        row.removeAllViews();
        alternatives = values;
        style = popupStyle;
        preview = isPreview;
        selected = 0;
        applyContainerStyle();
        for (KeyboardDocument.Alternative a : values) {
            TextView t = new TextView(anchor.getContext());
            t.setText(a.label);
            t.setContentDescription(a.label);
            t.setTextColor(style.primary);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(18f, style.textSize * 1.2f));
            t.setTypeface(KeyboardTypefaceResolver.resolve(style.fontFamily, style.fontWeight));
            t.setGravity(Gravity.CENTER);
            t.setMinWidth(dp(48));
            t.setMinHeight(dp(48));
            t.setPadding(dp(16), dp(8), dp(16), dp(8));
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(1), 0, dp(1), 0);
            row.addView(t, params);
        }
        applySelection();
        row.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        Rect visible = new Rect();
        anchor.getWindowVisibleDisplayFrame(visible);
        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        int x =
                Math.max(
                        visible.left,
                        Math.min(
                                (int) rawX - row.getMeasuredWidth() / 2,
                                visible.right - row.getMeasuredWidth()));
        int gap = dp(4);
        int y = loc[1] - row.getMeasuredHeight() - gap;
        if (y < visible.top) y = loc[1] + anchor.getHeight() + gap;
        popup.setWidth(row.getMeasuredWidth());
        popup.setHeight(row.getMeasuredHeight());
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
        if (!preview) announce(anchor);
    }

    public void update(float rawX) {
        if (!popup.isShowing() || alternatives == null) return;
        int[] loc = new int[2];
        row.getLocationOnScreen(loc);
        int localX = (int) rawX - loc[0];
        int next = alternatives.size() - 1;
        if (localX <= 0) {
            next = 0;
        } else {
            for (int i = 0; i < row.getChildCount(); i++)
                if (localX < row.getChildAt(i).getRight()) {
                    next = i;
                    break;
                }
        }
        if (selected != next) {
            selected = next;
            applySelection();
        }
    }

    public KeyboardDocument.Alternative selected() {
        return alternatives == null ? null : alternatives.get(selected);
    }

    public void dismiss() {
        popup.dismiss();
        alternatives = null;
        style = null;
        preview = false;
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    LinearLayout contentView() {
        return row;
    }

    private void applyContainerStyle() {
        GradientDrawable background = rounded(style.pressed, style.radius);
        int configuredStroke = Math.round(style.borderWidth * density);
        int strokeWidth = configuredStroke > 0 ? configuredStroke : dp(1);
        int strokeColor =
                configuredStroke > 0 ? style.border : withAlpha(style.primary, 0x48);
        background.setStroke(strokeWidth, strokeColor);
        row.setBackground(background);
        row.setPadding(dp(3), dp(3), dp(3), dp(3));
    }

    private void applySelection() {
        for (int i = 0; i < row.getChildCount(); i++) {
            View option = row.getChildAt(i);
            option.setBackground(
                    !preview && i == selected
                            ? rounded(style.armed, Math.max(0, style.radius - 2))
                            : new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * density);
        return drawable;
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    private void announce(View anchor) {
        StringBuilder s = new StringBuilder("Alternatives: ");
        for (KeyboardDocument.Alternative a : alternatives) {
            if (s.length() > 14) s.append(", ");
            s.append(a.label);
        }
        anchor.announceForAccessibility(s);
    }
}
