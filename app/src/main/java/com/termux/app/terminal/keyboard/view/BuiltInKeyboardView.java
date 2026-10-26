package com.termux.app.terminal.keyboard.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.termux.app.terminal.keyboard.config.KeyboardAction;
import com.termux.app.terminal.keyboard.config.KeyboardDocument;
import com.termux.app.terminal.keyboard.input.KeyResolver;
import com.termux.app.terminal.keyboard.input.KeyboardInputDispatcher;
import com.termux.app.terminal.keyboard.input.KeyboardState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weighted, responsive keyboard surface with one deterministic parent-owned pointer state machine.
 */
public final class BuiltInKeyboardView extends ViewGroup {
    private static final int ACCESSIBILITY_POINTER = -9;
    private static final KeyboardAction CURSOR_LEFT = terminalKey("LEFT");
    private static final KeyboardAction CURSOR_RIGHT = terminalKey("RIGHT");
    private final KeyboardGeometry geometry = new KeyboardGeometry();
    private final KeyboardStyleResolver styles = new KeyboardStyleResolver();
    private final KeyResolver resolver = new KeyResolver();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<Integer, Press> presses = new HashMap<>();
    private final KeyboardPopupView popup;
    private final List<KeyboardKeyView> keyViews = new ArrayList<>();
    private KeyboardState state = new KeyboardState();
    private KeyboardDocument.Profile profile;
    private KeyboardDocument.Variant variant;
    private KeyboardDocument.Variant forcedVariant;
    private String profileId;
    private KeyboardInputDispatcher dispatcher;
    private List<KeyboardGeometry.Cell> cells = new ArrayList<>();
    private KeyboardStyleResolver.Resolved layoutStyle;
    private int terminalFg = 0xffffffff;
    private int terminalBg = 0xff000000;
    private Runnable stateChangeListener;

    public BuiltInKeyboardView(Context c) {
        this(c, null);
    }

    public BuiltInKeyboardView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        setWillNotDraw(false);
        setClickable(true);
        popup = new KeyboardPopupView(c);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setDispatcher(KeyboardInputDispatcher d) {
        dispatcher = d;
        if (d != null && profile != null) d.setProfile(profile);
    }

    public void setKeyboardState(KeyboardState shared) {
        if (shared == null || shared == state) return;
        cancelAll();
        state = shared;
    }

    public void setStateChangeListener(Runnable listener) {
        stateChangeListener = listener;
    }

    public void refreshState() {
        refreshKeys();
    }

    public void setProfile(String id, KeyboardDocument.Profile p) {
        setProfile(id, p, null);
    }

    public void setProfile(String id, KeyboardDocument.Profile p, KeyboardDocument.Variant forced) {
        cancelAll();
        profileId = id;
        profile = p;
        forcedVariant = forced;
        state.reset();
        if (dispatcher != null) dispatcher.setProfile(p);
        requestLayout();
        invalidate();
    }

    public String getProfileId() {
        return profileId;
    }

    public KeyboardState getKeyboardState() {
        return state;
    }

    public KeyboardDocument.Variant getVariant() {
        return variant;
    }

    public void setTerminalColors(int foreground, int background) {
        if (terminalFg == foreground && terminalBg == background) return;
        terminalFg = foreground;
        terminalBg = background;
        requestLayout();
        invalidate();
    }

    // Geometry changes only during a measure pass; the resulting cells are reused for drawing.
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        selectVariant(width);
        if (profile == null || variant == null) {
            setMeasuredDimension(resolveSize(width, widthSpec), resolveSize(0, heightSpec));
            cells = new ArrayList<>();
            ensureChildren();
            return;
        }
        boolean dark = isDarkMode();
        layoutStyle =
                styles.resolveLayout(profile, variant.appearance, dark, terminalFg, terminalBg);
        float density = getResources().getDisplayMetrics().density;
        int desired = Math.round(KeyboardGeometry.height(variant, density, layoutStyle));
        setMeasuredDimension(resolveSize(width, widthSpec), resolveSize(desired, heightSpec));
        cells =
                geometry.layout(
                        variant, getMeasuredWidth(), getMeasuredHeight(), density, layoutStyle);
        ensureChildren();
        for (int i = 0; i < keyViews.size(); i++) {
            KeyboardGeometry.Cell cell = visualForView(i);
            keyViews.get(i)
                    .measure(
                            MeasureSpec.makeMeasureSpec(
                                    Math.max(0, Math.round(cell.visual.width())),
                                    MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(
                                    Math.max(0, Math.round(cell.visual.height())),
                                    MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < keyViews.size(); i++) {
            KeyboardGeometry.Cell c = visualForView(i);
            keyViews.get(i)
                    .layout(
                            Math.round(c.visual.left),
                            Math.round(c.visual.top),
                            Math.round(c.visual.right),
                            Math.round(c.visual.bottom));
        }
        refreshKeys();
    }

    private void selectVariant(int width) {
        if (forcedVariant != null) {
            variant = forcedVariant;
            return;
        }
        if (profile == null || profile.variants == null || profile.variants.isEmpty()) {
            variant = null;
            return;
        }
        float dp = width / getResources().getDisplayMetrics().density;
        for (KeyboardDocument.Variant v : profile.variants)
            if (dp >= v.minWidthDp && (v.maxWidthDp == null || dp < v.maxWidthDp)) {
                variant = v;
                return;
            }
        variant = profile.variants.get(0);
    }

    private void ensureChildren() {
        int count = 0;
        for (KeyboardGeometry.Cell c : cells) if (!Boolean.TRUE.equals(c.key.spacer)) count++;
        if (count == keyViews.size()) return;
        removeAllViews();
        keyViews.clear();
        for (int i = 0; i < count; i++) {
            KeyboardKeyView v = new KeyboardKeyView(getContext());
            keyViews.add(v);
            addView(v);
        }
    }

    private KeyboardGeometry.Cell visualForView(int viewIndex) {
        int n = -1;
        for (int i = 0; i < cells.size(); i++)
            if (!Boolean.TRUE.equals(cells.get(i).key.spacer) && ++n == viewIndex)
                return cells.get(i);
        throw new IndexOutOfBoundsException();
    }

    private KeyboardKeyView viewForCell(int cellIndex) {
        int n = -1;
        for (int i = 0; i <= cellIndex; i++) if (!Boolean.TRUE.equals(cells.get(i).key.spacer)) n++;
        return n < 0 ? null : keyViews.get(n);
    }

    private void refreshKeys() {
        if (profile == null) return;
        boolean dark = isDarkMode();
        for (int i = 0; i < keyViews.size(); i++) {
            KeyboardDocument.Key key = visualForView(i).key;
            KeyResolver.Resolved resolved = resolver.resolve(key, state);
            KeyboardStyleResolver.Resolved style = styleFor(key, dark);
            KeyboardState.VisualState vs = visualState(resolved.action);
            String accessible =
                    key.accessibilityLabel == null ? resolved.label : key.accessibilityLabel;
            keyViews.get(i)
                    .bind(
                            resolved.label,
                            resolved.icon,
                            resolved.secondaryLabel,
                            accessible,
                            style,
                            vs,
                            () -> activateAccessible(key));
        }
        if (layoutStyle != null) setBackgroundColor(layoutStyle.background);
    }

    private boolean isDarkMode() {
        return (getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private KeyboardState.VisualState visualState(KeyboardAction a) {
        if (a == null) return KeyboardState.VisualState.INACTIVE;
        if ("modifier".equals(a.type)) return state.modifierState(a.modifier);
        if ("layer".equals(a.type)) return state.layerState(a.layer);
        return KeyboardState.VisualState.INACTIVE;
    }

    private void activateAccessible(KeyboardDocument.Key key) {
        KeyResolver.Resolved r = resolver.resolve(key, state);
        if (r.action == null) return;
        if (isControl(r.action)) activateAccessibleControl(r.action);
        else if (dispatcher != null) dispatcher.dispatch(r.action);
        stateChanged();
    }

    private void activateAccessibleControl(KeyboardAction action) {
        KeyboardState.VisualState current = controlState(action);
        String mode = action.mode == null ? "smart" : action.mode;
        switch (mode) {
            case "momentary":
                if (current == KeyboardState.VisualState.HELD) {
                    releaseControl(action, false);
                } else if (current == KeyboardState.VisualState.LOCKED) {
                    unlockControl(action);
                } else if (current == KeyboardState.VisualState.ARMED) {
                    pressControl(action);
                    releaseControl(action, false);
                } else {
                    pressControl(action);
                }
                break;
            case "once":
                if (current == KeyboardState.VisualState.LOCKED) {
                    unlockControl(action);
                } else {
                    pressControl(action);
                    releaseControl(action, true);
                }
                break;
            case "locked":
                if (current == KeyboardState.VisualState.LOCKED)
                    unlockControl(action);
                else
                    lockControl(action);
                break;
            case "smart":
            default:
                if (current == KeyboardState.VisualState.LOCKED) {
                    unlockControl(action);
                } else if (current == KeyboardState.VisualState.ARMED
                        || current == KeyboardState.VisualState.HELD) {
                    lockControl(action);
                } else {
                    pressControl(action);
                    releaseControl(action, true);
                }
                break;
        }
    }

    private KeyboardState.VisualState controlState(KeyboardAction action) {
        return isModifier(action)
                ? state.modifierState(action.modifier)
                : state.layerState(action.layer);
    }

    private void pressControl(KeyboardAction action) {
        if (isModifier(action)) state.pressModifier(action.modifier, ACCESSIBILITY_POINTER);
        else state.pressLayer(action.layer, ACCESSIBILITY_POINTER);
    }

    private void releaseControl(KeyboardAction action, boolean armIfTap) {
        if (isModifier(action))
            state.releaseModifier(action.modifier, ACCESSIBILITY_POINTER, armIfTap);
        else state.releaseLayer(action.layer, ACCESSIBILITY_POINTER, armIfTap);
    }

    private void lockControl(KeyboardAction action) {
        if (isModifier(action)) state.lockModifier(action.modifier);
        else state.lockLayer(action.layer);
    }

    private void unlockControl(KeyboardAction action) {
        if (isModifier(action)) state.tapLockedModifier(action.modifier);
        else state.tapLockedLayer(action.layer);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        int index = e.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            down(e, index);
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            move(e);
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            up(e, index, false);
            if (action == MotionEvent.ACTION_UP) performClick();
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            cancelAllPointers();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void down(MotionEvent e, int index) {
        int pointer = e.getPointerId(index);
        int cellIndex = hit(e.getX(index), e.getY(index));
        if (cellIndex < 0 || Boolean.TRUE.equals(cells.get(cellIndex).key.spacer)) return;
        KeyboardDocument.Key key = cells.get(cellIndex).key;
        KeyboardKeyView view = viewForCell(cellIndex);
        KeyResolver.Resolved resolved = resolver.resolve(key, state);
        KeyboardStyleResolver.Resolved popupStyle = styleFor(key, isDarkMode());
        Press p = new Press(pointer, cellIndex, key, view, resolved, popupStyle);
        presses.put(pointer, p);
        view.setPressed(true);
        view.haptic();
        if (layoutStyle != null && layoutStyle.sound) playSoundEffect(SoundEffectConstants.CLICK);
        if (isModifier(resolved.action)) {
            p.wasLocked =
                    state.modifierState(resolved.action.modifier)
                            == KeyboardState.VisualState.LOCKED;
            if (!p.wasLocked) state.pressModifier(resolved.action.modifier, pointer);
        } else if (isLayer(resolved.action)) {
            p.wasLocked =
                    state.layerState(resolved.action.layer) == KeyboardState.VisualState.LOCKED;
            if (!p.wasLocked) state.pressLayer(resolved.action.layer, pointer);
        }
        if (!p.wasLocked) scheduleLong(p, e.getRawX());
        if (layoutStyle != null
                && layoutStyle.preview
                && key.preview != Boolean.FALSE
                && key.alternatives == null
                && resolved.icon == null
                && !isControl(resolved.action)) {
            popup.showPreview(view, resolved.label, p.style);
        }
        stateChanged();
    }

    private void scheduleLong(Press p, float rawX) {
        int delay = ViewConfiguration.getLongPressTimeout();
        p.longRunnable =
                () -> {
                    if (!presses.containsKey(p.pointer)) return;
                    if (isControl(p.resolved.action) && !"smart".equals(p.resolved.action.mode))
                        return;
                    if (Boolean.TRUE.equals(p.key.cursorGesture)) {
                        p.longTriggered = true;
                        p.cursorGesture = true;
                        p.cursorAnchorX = rawX;
                        popup.dismiss();
                        p.view.haptic();
                        announceForAccessibility("Cursor control");
                        return;
                    }
                    if (p.key.alternatives != null && !p.key.alternatives.isEmpty()) {
                        p.longTriggered = true;
                        popup.dismiss();
                        popup.showAlternatives(p.view, p.key.alternatives, rawX, p.style);
                        p.view.haptic();
                    } else if (isModifier(p.resolved.action)) {
                        p.longTriggered = true;
                        state.lockModifier(p.resolved.action.modifier);
                        p.view.haptic();
                        stateChanged();
                    } else if (isLayer(p.resolved.action)) {
                        p.longTriggered = true;
                        state.lockLayer(p.resolved.action.layer);
                        p.view.haptic();
                        stateChanged();
                    } else if (Boolean.TRUE.equals(p.key.repeat)) {
                        p.longTriggered = true;
                        repeat(p);
                    }
                };
        handler.postDelayed(
                p.longRunnable,
                Math.max(delay, layoutStyle == null ? delay : layoutStyle.repeatDelay));
    }

    private void repeat(Press p) {
        if (!presses.containsKey(p.pointer)) return;
        if (dispatcher != null) dispatcher.dispatch(p.resolved.action);
        p.repeated = true;
        p.repeatRunnable = () -> repeat(p);
        handler.postDelayed(
                p.repeatRunnable, layoutStyle == null ? 55 : layoutStyle.repeatInterval);
    }

    private void move(MotionEvent e) {
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        for (int i = 0; i < e.getPointerCount(); i++) {
            Press p = presses.get(e.getPointerId(i));
            if (p == null) continue;
            p.inside = hit(e.getX(i), e.getY(i)) == p.cell;
            float rawX = loc[0] + e.getX(i);
            if (p.cursorGesture) moveCursor(p, rawX);
            else if (popup.isShowing() && p.longTriggered && p.key.alternatives != null)
                popup.update(rawX);
        }
    }

    private void moveCursor(Press p, float rawX) {
        float step = 14 * getResources().getDisplayMetrics().density;
        int count = (int) ((rawX - p.cursorAnchorX) / step);
        if (count == 0 || dispatcher == null) return;
        KeyboardAction action = count < 0 ? CURSOR_LEFT : CURSOR_RIGHT;
        for (int i = 0; i < Math.abs(count); i++) dispatcher.dispatch(action);
        p.cursorAnchorX += count * step;
    }

    private void up(MotionEvent e, int index, boolean cancelled) {
        int pointer = e.getPointerId(index);
        Press p = presses.remove(pointer);
        if (p == null) return;
        if (p.longRunnable != null) handler.removeCallbacks(p.longRunnable);
        if (p.repeatRunnable != null) handler.removeCallbacks(p.repeatRunnable);
        p.view.setPressed(false);
        if (p.longTriggered && p.key.alternatives != null) {
            if (isModifier(p.resolved.action))
                state.releaseModifier(p.resolved.action.modifier, pointer, false);
            else if (isLayer(p.resolved.action))
                state.releaseLayer(p.resolved.action.layer, pointer, false);
            if (!cancelled) {
                KeyboardDocument.Alternative a = popup.selected();
                if (a != null) {
                    if (isControl(a.action)) activateAccessibleControl(a.action);
                    else if (dispatcher != null) dispatcher.dispatch(a.action);
                }
            }
        } else if (isModifier(p.resolved.action)) releaseModifier(p);
        else if (isLayer(p.resolved.action)) releaseLayer(p);
        else if (!cancelled && p.inside && !p.repeated) {
            if (!p.longTriggered && dispatcher != null)
                dispatcher.dispatch(p.resolved.action);
        }
        popup.dismiss();
        stateChanged();
    }

    private void releaseModifier(Press p) {
        String id = p.resolved.action.modifier;
        if (p.wasLocked) {
            state.tapLockedModifier(id);
            return;
        }
        String mode = p.resolved.action.mode;
        if ("locked".equals(mode)) state.lockModifier(id);
        else state.releaseModifier(id, p.pointer, !"momentary".equals(mode));
    }

    private void releaseLayer(Press p) {
        String id = p.resolved.action.layer;
        if (p.wasLocked) {
            state.tapLockedLayer(id);
            return;
        }
        String mode = p.resolved.action.mode;
        if ("locked".equals(mode)) state.lockLayer(id);
        else state.releaseLayer(id, p.pointer, !"momentary".equals(mode));
    }

    private int hit(float x, float y) {
        for (int i = 0; i < cells.size(); i++) if (cells.get(i).hit.contains(x, y)) return i;
        return -1;
    }

    private boolean isModifier(KeyboardAction a) {
        return a != null && "modifier".equals(a.type);
    }

    private boolean isLayer(KeyboardAction a) {
        return a != null && "layer".equals(a.type);
    }

    private boolean isControl(KeyboardAction a) {
        return isModifier(a) || isLayer(a);
    }

    private void cancelAllPointers() {
        for (Press p : new ArrayList<>(presses.values())) {
            if (p.longRunnable != null) handler.removeCallbacks(p.longRunnable);
            if (p.repeatRunnable != null) handler.removeCallbacks(p.repeatRunnable);
            p.view.setPressed(false);
        }
        presses.clear();
        popup.dismiss();
        handler.removeCallbacksAndMessages(null);
        state.reset();
        if (dispatcher != null) dispatcher.cancel();
        stateChanged();
    }

    private void stateChanged() {
        refreshKeys();
        if (stateChangeListener != null) stateChangeListener.run();
    }

    private KeyboardStyleResolver.Resolved styleFor(KeyboardDocument.Key key, boolean dark) {
        return styles.resolve(profile, variant.appearance, key, dark, terminalFg, terminalBg);
    }

    public void cancelAll() {
        cancelAllPointers();
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAll();
        super.onDetachedFromWindow();
    }

    private static KeyboardAction terminalKey(String key) {
        KeyboardAction action = new KeyboardAction();
        action.type = "terminalKey";
        action.key = key;
        return action;
    }

    private static final class Press {
        final int pointer;
        final int cell;
        final KeyboardDocument.Key key;
        final KeyboardKeyView view;
        final KeyResolver.Resolved resolved;
        final KeyboardStyleResolver.Resolved style;
        boolean inside = true;
        boolean longTriggered;
        boolean repeated;
        boolean wasLocked;
        boolean cursorGesture;
        float cursorAnchorX;
        Runnable longRunnable;
        Runnable repeatRunnable;

        Press(
                int p,
                int c,
                KeyboardDocument.Key k,
                KeyboardKeyView v,
                KeyResolver.Resolved r,
                KeyboardStyleResolver.Resolved s) {
            pointer = p;
            cell = c;
            key = k;
            view = v;
            resolved = r;
            style = s;
        }
    }
}
