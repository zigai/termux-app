package com.termux.app.terminal.keyboard.input;

import android.os.Handler;
import android.os.Looper;

import com.termux.app.terminal.keyboard.config.KeyboardAction;
import com.termux.app.terminal.keyboard.config.KeyboardDocument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Dispatches typed actions through TerminalView-facing APIs and runs finite structured macros. */
public final class KeyboardInputDispatcher {
    private final KeyboardState state;
    private final Sink sink;
    private final Handler handler;
    private KeyboardDocument.Profile profile;
    private int macroGeneration;

    public KeyboardInputDispatcher(KeyboardState state, Sink sink) {
        this(state, sink, new Handler(Looper.getMainLooper()));
    }

    KeyboardInputDispatcher(KeyboardState state, Sink sink, Handler handler) {
        this.state = state;
        this.sink = sink;
        this.handler = handler;
    }

    public void setProfile(KeyboardDocument.Profile p) {
        cancel();
        profile = p;
        state.reset();
    }

    public boolean dispatch(KeyboardAction a) {
        if (a == null) return false;
        switch (a.type) {
            case "text":
                emitText(a.text, a.modifiers);
                return true;
            case "terminalKey":
                sink.terminalKey(a.key, ctrl(a), alt(a), shift(a));
                state.consumeOutput();
                return true;
            case "modifier":
                return false; // touch state machine owns press/release semantics
            case "layer":
                return false;
            case "macro":
                runMacro(a.macro);
                return true;
            case "app":
                sink.appAction(a.app);
                return true;
            case "profile":
                sink.profileAction(a.profile);
                return true;
            default:
                return false;
        }
    }

    private void emitText(String text, List<String> explicit) {
        if (text == null) return;
        boolean shift = state.activeModifier("SHIFT") || has(explicit, "SHIFT");
        String out = shift ? text.toUpperCase(Locale.ROOT) : text;
        for (int i = 0; i < out.length(); ) {
            int cp = out.codePointAt(i);
            sink.codePoint(
                    cp,
                    state.activeModifier("CTRL") || has(explicit, "CTRL"),
                    state.activeModifier("ALT") || has(explicit, "ALT"));
            i += Character.charCount(cp);
        }
        state.consumeOutput();
    }

    private boolean ctrl(KeyboardAction a) {
        return state.activeModifier("CTRL") || has(a.modifiers, "CTRL");
    }

    private boolean alt(KeyboardAction a) {
        return state.activeModifier("ALT") || has(a.modifiers, "ALT");
    }

    private boolean shift(KeyboardAction a) {
        return state.activeModifier("SHIFT") || has(a.modifiers, "SHIFT");
    }

    private boolean has(List<String> list, String value) {
        return list != null && list.contains(value);
    }

    private void runMacro(String name) {
        if (profile == null || profile.macros == null) return;
        List<KeyboardAction> steps = new ArrayList<>();
        if (!expandMacro(name, steps, new HashSet<>()) || steps.isEmpty()) return;
        int generation = ++macroGeneration;
        runStep(steps, 0, generation);
    }

    private boolean expandMacro(String name, List<KeyboardAction> out, Set<String> visiting) {
        if (!visiting.add(name) || out.size() > 256) return false;
        List<KeyboardAction> source = profile.macros.get(name);
        if (source == null) return false;
        for (KeyboardAction step : source) {
            if ("macro".equals(step.type)) {
                if (!expandMacro(step.macro, out, visiting)) return false;
            } else out.add(step);
        }
        visiting.remove(name);
        return true;
    }

    private void runStep(List<KeyboardAction> steps, int index, int generation) {
        if (generation != macroGeneration || index >= steps.size()) return;
        KeyboardAction step = steps.get(index);
        if ("delay".equals(step.type)) {
            handler.postDelayed(
                    () -> runStep(steps, index + 1, generation),
                    step.delayMs == null ? 0 : step.delayMs);
        } else {
            dispatch(step);
            handler.post(() -> runStep(steps, index + 1, generation));
        }
    }

    public void cancel() {
        macroGeneration++;
        handler.removeCallbacksAndMessages(null);
    }

    public interface Sink {
        void codePoint(int codePoint, boolean ctrl, boolean alt);

        void terminalKey(String key, boolean ctrl, boolean alt, boolean shift);

        void appAction(String action);

        void profileAction(String profile);
    }
}
