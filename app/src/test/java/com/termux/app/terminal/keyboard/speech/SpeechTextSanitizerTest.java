package com.termux.app.terminal.keyboard.speech;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SpeechTextSanitizerTest {
    @Test
    public void removesTerminalControlsAndFlattensLineBreaksWithoutEnter() {
        assertEquals(
                "echo hello rm -rf",
                SpeechTextSanitizer.forTerminal("\u0000echo hello\nrm\t-rf\r\u007f"));
    }

    @Test
    public void preservesUnicodeCodePoints() {
        assertEquals("héllo 😀", SpeechTextSanitizer.forTerminal(" héllo 😀 "));
    }
}
