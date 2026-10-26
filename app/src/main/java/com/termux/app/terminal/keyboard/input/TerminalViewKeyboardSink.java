package com.termux.app.terminal.keyboard.input;

import android.view.KeyEvent;

import com.termux.app.terminal.keyboard.speech.SpeechTextSanitizer;
import com.termux.terminal.KeyHandler;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import java.util.HashMap;
import java.util.Map;

/** Adapter that deliberately uses TerminalView input APIs instead of writing terminal bytes. */
public final class TerminalViewKeyboardSink implements KeyboardInputDispatcher.Sink {
    private static final Map<String, Integer> KEYS = new HashMap<>();

    static {
        KEYS.put("ENTER", KeyEvent.KEYCODE_ENTER);
        KEYS.put("ESCAPE", KeyEvent.KEYCODE_ESCAPE);
        KEYS.put("TAB", KeyEvent.KEYCODE_TAB);
        KEYS.put("BACKSPACE", KeyEvent.KEYCODE_DEL);
        KEYS.put("UP", KeyEvent.KEYCODE_DPAD_UP);
        KEYS.put("DOWN", KeyEvent.KEYCODE_DPAD_DOWN);
        KEYS.put("LEFT", KeyEvent.KEYCODE_DPAD_LEFT);
        KEYS.put("RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT);
        KEYS.put("HOME", KeyEvent.KEYCODE_MOVE_HOME);
        KEYS.put("END", KeyEvent.KEYCODE_MOVE_END);
        KEYS.put("PAGE_UP", KeyEvent.KEYCODE_PAGE_UP);
        KEYS.put("PAGE_DOWN", KeyEvent.KEYCODE_PAGE_DOWN);
        KEYS.put("INSERT", KeyEvent.KEYCODE_INSERT);
        KEYS.put("DELETE", KeyEvent.KEYCODE_FORWARD_DEL);
        for (int i = 1; i <= 12; i++) KEYS.put("F" + i, KeyEvent.KEYCODE_F1 + i - 1);
    }

    private final TerminalView view;
    private final AppCallbacks callbacks;

    public TerminalViewKeyboardSink(TerminalView view, AppCallbacks callbacks) {
        this.view = view;
        this.callbacks = callbacks;
    }

    @Override
    public void codePoint(int cp, boolean ctrl, boolean alt) {
        view.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_SOFT_KEYBOARD, cp, ctrl, alt);
    }

    @Override
    public void terminalKey(String key, boolean ctrl, boolean alt, boolean shift) {
        Integer code = KEYS.get(key);
        TerminalSession session = view.getCurrentSession();
        if (code == null || session == null || session.getEmulator() == null) return;
        int mod =
                (ctrl ? KeyHandler.KEYMOD_CTRL : 0)
                        | (alt ? KeyHandler.KEYMOD_ALT : 0)
                        | (shift ? KeyHandler.KEYMOD_SHIFT : 0);
        view.handleKeyCode(code, mod);
    }

    @Override
    public void appAction(String action) {
        callbacks.appAction(action);
    }

    @Override
    public void profileAction(String profile) {
        callbacks.profileAction(profile);
    }

    /**
     * Inserts literal user-approved text without applying keyboard modifiers or synthesizing Enter.
     */
    public void insertLiteralText(String text) {
        String safe = SpeechTextSanitizer.forTerminal(text);
        for (int i = 0; i < safe.length(); ) {
            int cp = safe.codePointAt(i);
            view.inputLiteralCodePoint(cp);
            i += Character.charCount(cp);
        }
    }

    public interface AppCallbacks {
        void appAction(String action);

        void profileAction(String profile);
    }
}
