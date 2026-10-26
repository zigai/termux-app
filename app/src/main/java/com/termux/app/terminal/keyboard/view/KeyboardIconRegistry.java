package com.termux.app.terminal.keyboard.view;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.termux.R;
import com.termux.app.terminal.keyboard.input.KeyboardState;

/** Closed, bundled icon set for keys whose meaning is clearer as a phone-keyboard symbol. */
final class KeyboardIconRegistry {

    private KeyboardIconRegistry() {}

    @Nullable
    static Drawable load(Context context, String icon, KeyboardState.VisualState state, int tint) {
        @DrawableRes int resource = resource(icon, state);
        if (resource == 0) return null;
        Drawable drawable = AppCompatResources.getDrawable(context, resource);
        if (drawable == null) return null;
        drawable = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(drawable, tint);
        return drawable;
    }

    @DrawableRes
    private static int resource(String icon, KeyboardState.VisualState state) {
        if (icon == null) return 0;
        switch (icon) {
            case "shift":
                return state == KeyboardState.VisualState.LOCKED
                        ? R.drawable.keyboard_key_shift_lock
                        : R.drawable.keyboard_key_shift;
            case "backspace":
                return R.drawable.keyboard_key_backspace;
            case "enter":
                return R.drawable.keyboard_key_enter;
            case "microphone":
                return R.drawable.keyboard_key_microphone;
            case "tab":
                return R.drawable.keyboard_key_tab;
            case "space":
                return R.drawable.keyboard_key_space;
            case "arrowUp":
                return R.drawable.keyboard_key_arrow_up;
            case "arrowDown":
                return R.drawable.keyboard_key_arrow_down;
            case "arrowLeft":
                return R.drawable.keyboard_key_arrow_left;
            case "arrowRight":
                return R.drawable.keyboard_key_arrow_right;
            default:
                return 0;
        }
    }
}
