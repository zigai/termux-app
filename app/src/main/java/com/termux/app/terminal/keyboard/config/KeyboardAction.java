package com.termux.app.terminal.keyboard.config;

import java.util.List;

/** Closed, discriminated keyboard action and macro-step model. */
public final class KeyboardAction {
    public String type;
    public String text;
    public String key;
    public String modifier;
    public String layer;
    public String mode;
    public String macro;
    public String app;
    public String profile;
    public List<String> modifiers;
    public Integer delayMs;
}
