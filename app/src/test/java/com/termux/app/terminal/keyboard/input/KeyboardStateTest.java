package com.termux.app.terminal.keyboard.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyboardStateTest {
    @Test
    public void tapArmsSecondTapCancelsAndLongPressLocks() {
        KeyboardState s = new KeyboardState();
        s.pressModifier("CTRL", 1);
        s.releaseModifier("CTRL", 1);
        assertEquals(KeyboardState.VisualState.ARMED, s.modifierState("CTRL"));
        s.pressModifier("CTRL", 2);
        s.releaseModifier("CTRL", 2);
        assertEquals(KeyboardState.VisualState.INACTIVE, s.modifierState("CTRL"));
        s.pressModifier("CTRL", 3);
        s.lockModifier("CTRL");
        assertEquals(KeyboardState.VisualState.LOCKED, s.modifierState("CTRL"));
        s.tapLockedModifier("CTRL");
        assertFalse(s.activeModifier("CTRL"));
    }

    @Test
    public void heldChordIsNotConvertedToOneShotAndOutputConsumesArmed() {
        KeyboardState s = new KeyboardState();
        s.pressModifier("ALT", 7);
        s.consumeOutput();
        assertTrue(s.activeModifier("ALT"));
        s.releaseModifier("ALT", 7);
        assertFalse(s.activeModifier("ALT"));
        s.pressLayer("symbols", 8);
        s.releaseLayer("symbols", 8);
        assertTrue(s.activeLayer("symbols"));
        s.consumeOutput();
        assertFalse(s.activeLayer("symbols"));
    }

    @Test
    public void resetClearsEveryStateBoundary() {
        KeyboardState s = new KeyboardState();
        s.pressModifier("SHIFT", 1);
        s.lockLayer("altgr");
        s.reset();
        assertFalse(s.activeModifier("SHIFT"));
        assertFalse(s.activeLayer("altgr"));
    }
}
