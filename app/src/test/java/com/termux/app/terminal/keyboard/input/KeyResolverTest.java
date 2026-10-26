package com.termux.app.terminal.keyboard.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.termux.app.terminal.keyboard.config.KeyboardAction;
import com.termux.app.terminal.keyboard.config.KeyboardDocument;

import org.junit.Test;

import java.util.HashMap;

public class KeyResolverTest {

    @Test
    public void shiftChangesCaseWhenKeyHasNoLayerMap() {
        KeyboardDocument.Key key = new KeyboardDocument.Key();
        key.label = "a";
        key.action = text("a");
        KeyboardState state = new KeyboardState();

        state.pressModifier("SHIFT", 1);
        KeyResolver.Resolved resolved = new KeyResolver().resolve(key, state);

        assertEquals("A", resolved.label);
        assertEquals("A", resolved.action.text);
    }

    @Test
    public void shiftPreservesAnUnlabelledTextKey() {
        KeyboardDocument.Key key = new KeyboardDocument.Key();
        key.action = text(" ");
        KeyboardState state = new KeyboardState();

        state.pressModifier("SHIFT", 1);
        KeyResolver.Resolved resolved = new KeyResolver().resolve(key, state);

        assertNull(resolved.label);
        assertEquals(" ", resolved.action.text);
    }

    @Test
    public void shiftChangesCaseAndGeneralLayerOverridesPresentationAndAction() {
        KeyboardDocument.Key key = new KeyboardDocument.Key();
        key.label = "a";
        key.action = text("a");
        key.layers = new HashMap<>();
        KeyboardDocument.Override symbols = new KeyboardDocument.Override();
        symbols.label = null;
        symbols.icon = "arrowUp";
        symbols.action = text("@");
        key.layers.put("symbols", symbols);
        KeyboardState state = new KeyboardState();

        state.pressModifier("SHIFT", 1);
        KeyResolver.Resolved resolved = new KeyResolver().resolve(key, state);
        assertEquals("A", resolved.label);
        assertEquals("A", resolved.action.text);

        state.reset();
        state.lockLayer("symbols");
        resolved = new KeyResolver().resolve(key, state);
        assertEquals("arrowUp", resolved.icon);
        assertEquals("@", resolved.action.text);
    }

    private KeyboardAction text(String value) {
        KeyboardAction action = new KeyboardAction();
        action.type = "text";
        action.text = value;
        return action;
    }
}
