package com.termux.app.terminal.keyboard.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.termux.app.terminal.keyboard.config.KeyboardAction;
import com.termux.app.terminal.keyboard.config.KeyboardDocument;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class KeyboardInputDispatcherTest {
    @Test
    public void unicodeCtrlAltShiftAndSpecialKeysDispatchAndConsume() {
        KeyboardState state = new KeyboardState();
        Sink sink = new Sink();
        KeyboardInputDispatcher d = new KeyboardInputDispatcher(state, sink);
        state.pressModifier("CTRL", 1);
        state.releaseModifier("CTRL", 1);
        KeyboardAction text = action("text");
        text.text = "é";
        d.dispatch(text);
        assertEquals("233:true:false", sink.events.get(0));
        assertFalse(state.activeModifier("CTRL"));
        state.lockModifier("ALT");
        KeyboardAction key = action("terminalKey");
        key.key = "LEFT";
        key.modifiers = Arrays.asList("SHIFT");
        d.dispatch(key);
        assertEquals("LEFT:false:true:true", sink.events.get(1));
    }

    @Test
    public void appAndProfileActionsDoNotConsumeOneShot() {
        KeyboardState state = new KeyboardState();
        Sink sink = new Sink();
        KeyboardInputDispatcher d = new KeyboardInputDispatcher(state, sink);
        state.pressModifier("CTRL", 1);
        state.releaseModifier("CTRL", 1);
        KeyboardAction app = action("app");
        app.app = "drawer";
        d.dispatch(app);
        assertTrue(state.activeModifier("CTRL"));
        KeyboardAction profile = action("profile");
        profile.profile = "cycle";
        d.dispatch(profile);
        assertTrue(state.activeModifier("CTRL"));
    }

    @Test
    public void macroOrderingDelayNestingAndCancellationAreDeterministic() {
        KeyboardState state = new KeyboardState();
        Sink sink = new Sink();
        KeyboardInputDispatcher d = new KeyboardInputDispatcher(state, sink);
        KeyboardDocument.Profile profile = new KeyboardDocument.Profile();
        profile.macros = new HashMap<>();
        KeyboardAction a = action("text");
        a.text = "a";
        KeyboardAction delay = action("delay");
        delay.delayMs = 100;
        KeyboardAction enter = action("terminalKey");
        enter.key = "ENTER";
        profile.macros.put("inner", Arrays.asList(delay, enter));
        KeyboardAction inner = action("macro");
        inner.macro = "inner";
        profile.macros.put("go", Arrays.asList(a, inner));
        d.setProfile(profile);
        KeyboardAction macro = action("macro");
        macro.macro = "go";
        d.dispatch(macro);
        ShadowLooper.idleMainLooper();
        assertEquals("97:false:false", sink.events.get(0));
        assertEquals(1, sink.events.size());
        ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS);
        assertEquals("ENTER:false:false:false", sink.events.get(1));
        sink.events.clear();
        d.dispatch(macro);
        ShadowLooper.idleMainLooper();
        d.cancel();
        ShadowLooper.idleMainLooper(200, TimeUnit.MILLISECONDS);
        assertEquals(1, sink.events.size());
    }

    private KeyboardAction action(String type) {
        KeyboardAction a = new KeyboardAction();
        a.type = type;
        return a;
    }

    private static final class Sink implements KeyboardInputDispatcher.Sink {
        final List<String> events = new ArrayList<>();

        public void codePoint(int c, boolean ctrl, boolean alt) {
            events.add(c + ":" + ctrl + ":" + alt);
        }

        public void terminalKey(String k, boolean c, boolean a, boolean s) {
            events.add(k + ":" + c + ":" + a + ":" + s);
        }

        public void appAction(String a) {
            events.add("app:" + a);
        }

        public void profileAction(String p) {
            events.add("profile:" + p);
        }
    }
}
