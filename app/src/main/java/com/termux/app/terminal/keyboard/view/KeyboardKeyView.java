package com.termux.app.terminal.keyboard.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.termux.app.terminal.keyboard.input.KeyboardState;

import java.util.Locale;

/** Lightweight drawn, focusable, TalkBack-activatable terminal key. */
public final class KeyboardKeyView extends View {

    private static final float LABEL_HORIZONTAL_INSET_DP = 6f;
    private static final float SECONDARY_INSET_DP = 5f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF keyBounds = new RectF();
    private String label;
    private String icon;
    private String secondary;
    private KeyboardStyleResolver.Resolved style;
    private KeyboardState.VisualState state = KeyboardState.VisualState.INACTIVE;
    private Runnable activation;

    public KeyboardKeyView(Context context) {
        this(context, null);
    }

    public KeyboardKeyView(Context context, @Nullable AttributeSet attributes) {
        super(context, attributes);
        setFocusable(true);
        setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void bind(
            String label,
            String icon,
            String secondary,
            String accessible,
            KeyboardStyleResolver.Resolved style,
            KeyboardState.VisualState state,
            Runnable activation) {
        this.label = label;
        this.icon = icon;
        this.secondary = secondary;
        this.style = style;
        this.state = state;
        this.activation = activation;
        setContentDescription(accessible == null ? label : accessible);
        ViewCompat.setStateDescription(
                this,
                state == KeyboardState.VisualState.INACTIVE
                        ? null
                        : state.name().toLowerCase(Locale.ROOT));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (style == null) return;

        float density = getResources().getDisplayMetrics().density;
        int fill =
                isPressed()
                        ? style.pressed
                        : state == KeyboardState.VisualState.LOCKED
                                ? style.locked
                                : state == KeyboardState.VisualState.ARMED
                                        ? style.armed
                                        : style.fill;
        float borderWidth = style.borderWidth * density;
        float borderInset = borderWidth / 2f;
        float radius = style.radius * density;
        keyBounds.set(
                borderInset, borderInset, getWidth() - borderInset, getHeight() - borderInset);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawRoundRect(keyBounds, radius, radius, paint);
        if (borderWidth > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(
                    borderWidth + (state == KeyboardState.VisualState.HELD ? 2 * density : 0));
            paint.setColor(style.border);
            canvas.drawRoundRect(keyBounds, radius, radius, paint);
        }

        drawPrimary(canvas, density);
        drawSecondary(canvas, density);
        drawStateIndicator(canvas, density);
    }

    private void drawPrimary(Canvas canvas, float density) {
        if (icon != null) {
            Drawable drawable = KeyboardIconRegistry.load(getContext(), icon, state, style.primary);
            if (drawable == null) return;
            int requested = Math.round(style.iconSize * density);
            int size =
                    Math.max(
                            1,
                            Math.min(
                                    requested,
                                    Math.min(
                                            getWidth() - Math.round(12 * density),
                                            getHeight() - Math.round(10 * density))));
            int left = (getWidth() - size) / 2;
            int top = (getHeight() - size) / 2;
            drawable.setBounds(left, top, left + size, top + size);
            drawable.draw(canvas);
            return;
        }
        if (label == null || label.isEmpty()) return;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(style.primary);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(KeyboardTypefaceResolver.resolve(style.fontFamily, style.fontWeight));
        paint.setTextScaleX(1f);
        paint.setTextSize(style.textSize * getResources().getDisplayMetrics().scaledDensity);

        float maxWidth = Math.max(1, getWidth() - 2 * LABEL_HORIZONTAL_INSET_DP * density);
        float maxHeight = Math.max(1, getHeight() - 8 * density);
        float textWidth = paint.measureText(label);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float textHeight = metrics.descent - metrics.ascent;
        float scale =
                Math.min(
                        1f,
                        Math.min(
                                maxWidth / Math.max(1, textWidth),
                                maxHeight / Math.max(1, textHeight)));
        paint.setTextSize(paint.getTextSize() * scale);
        metrics = paint.getFontMetrics();
        float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(label, getWidth() / 2f, baseline, paint);
    }

    private void drawSecondary(Canvas canvas, float density) {
        if (secondary == null || secondary.isEmpty()) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(KeyboardTypefaceResolver.resolve(style.fontFamily, "normal"));
        paint.setTextScaleX(1f);
        paint.setTextSize(
                style.textSize
                        * style.secondaryTextScale
                        * getResources().getDisplayMetrics().scaledDensity);
        paint.setColor(style.secondary);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(
                secondary,
                getWidth() - SECONDARY_INSET_DP * density,
                SECONDARY_INSET_DP * density - paint.ascent(),
                paint);
    }

    private void drawStateIndicator(Canvas canvas, float density) {
        paint.setColor(style.primary);
        if (state == KeyboardState.VisualState.HELD) {
            float inset = 3 * density;
            float radius = Math.max(0, style.radius * density - inset);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f * density);
            canvas.drawRoundRect(
                    inset, inset, getWidth() - inset, getHeight() - inset, radius, radius, paint);
        } else if (state == KeyboardState.VisualState.ARMED) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2 * density);
            canvas.drawLine(
                    7 * density,
                    getHeight() - 5 * density,
                    getWidth() - 7 * density,
                    getHeight() - 5 * density,
                    paint);
        } else if (state == KeyboardState.VisualState.LOCKED) {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(
                    getWidth() - 7 * density, getHeight() - 7 * density, 2.5f * density, paint);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (activation != null) activation.run();
        return true;
    }

    public void haptic() {
        if (style != null && style.haptics)
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }
}
