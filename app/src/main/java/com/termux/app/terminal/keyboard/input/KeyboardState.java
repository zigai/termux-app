package com.termux.app.terminal.keyboard.input;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Deterministic held/one-shot/locked state for modifiers and arbitrary layers. */
public final class KeyboardState {
    public enum VisualState {
        INACTIVE,
        HELD,
        ARMED,
        LOCKED
    }

    private static final class Entry {
        boolean held;
        boolean armed;
        boolean locked;
        boolean used;
        int pointer = -1;
    }

    private final Map<String, Entry> modifiers = new LinkedHashMap<>();
    private final Map<String, Entry> layers = new LinkedHashMap<>();
    private long generation;

    public void pressModifier(String id, int pointer) {
        press(modifiers, id, pointer);
    }

    public void pressLayer(String id, int pointer) {
        press(layers, id, pointer);
    }

    private void press(Map<String, Entry> map, String id, int pointer) {
        Entry e = entry(map, id);
        if (e.locked) return;
        e.held = true;
        e.used = false;
        e.pointer = pointer;
        generation++;
    }

    public void releaseModifier(String id, int pointer) {
        release(modifiers, id, pointer);
    }

    public void releaseLayer(String id, int pointer) {
        release(layers, id, pointer);
    }

    public void releaseModifier(String id, int pointer, boolean armIfTap) {
        release(modifiers, id, pointer, armIfTap);
    }

    public void releaseLayer(String id, int pointer, boolean armIfTap) {
        release(layers, id, pointer, armIfTap);
    }

    private void release(Map<String, Entry> map, String id, int pointer) {
        release(map, id, pointer, true);
    }

    private void release(Map<String, Entry> map, String id, int pointer, boolean armIfTap) {
        Entry e = entry(map, id);
        if (e.pointer != pointer) return;
        e.held = false;
        e.pointer = -1;
        if (!e.used && !e.locked && armIfTap) e.armed = !e.armed;
        if (!armIfTap) e.armed = false;
        generation++;
    }

    public void lockModifier(String id) {
        lock(modifiers, id);
    }

    public void lockLayer(String id) {
        lock(layers, id);
    }

    private void lock(Map<String, Entry> map, String id) {
        Entry e = entry(map, id);
        e.held = false;
        e.armed = false;
        e.locked = true;
        e.pointer = -1;
        generation++;
    }

    public void tapLockedModifier(String id) {
        unlock(modifiers, id);
    }

    public void tapLockedLayer(String id) {
        unlock(layers, id);
    }

    private void unlock(Map<String, Entry> map, String id) {
        Entry e = entry(map, id);
        if (e.locked) {
            e.locked = false;
            e.held = false;
            e.armed = false;
            generation++;
        }
    }

    /** Mark a terminal output and consume one-shot states, while held chord keys remain active. */
    public void consumeOutput() {
        consume(modifiers);
        consume(layers);
        generation++;
    }

    private void consume(Map<String, Entry> map) {
        for (Entry e : map.values()) {
            if (e.held) e.used = true;
            e.armed = false;
        }
    }

    public boolean activeModifier(String id) {
        return active(modifiers.get(id));
    }

    public boolean activeLayer(String id) {
        return active(layers.get(id));
    }

    private boolean active(Entry e) {
        return e != null && (e.held || e.armed || e.locked);
    }

    public VisualState modifierState(String id) {
        return state(modifiers.get(id));
    }

    public VisualState layerState(String id) {
        return state(layers.get(id));
    }

    private VisualState state(Entry e) {
        if (e == null) return VisualState.INACTIVE;
        if (e.locked) return VisualState.LOCKED;
        if (e.armed) return VisualState.ARMED;
        if (e.held) return VisualState.HELD;
        return VisualState.INACTIVE;
    }

    public Set<String> activeLayers() {
        Set<String> s = new LinkedHashSet<>();
        for (Map.Entry<String, Entry> e : layers.entrySet())
            if (active(e.getValue())) s.add(e.getKey());
        return Collections.unmodifiableSet(s);
    }

    public void reset() {
        modifiers.clear();
        layers.clear();
        generation++;
    }

    public long generation() {
        return generation;
    }

    private Entry entry(Map<String, Entry> map, String id) {
        Entry e = map.get(id);
        if (e == null) {
            e = new Entry();
            map.put(id, e);
        }
        return e;
    }
}
