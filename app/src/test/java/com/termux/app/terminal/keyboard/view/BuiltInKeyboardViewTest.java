package com.termux.app.terminal.keyboard.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.termux.app.terminal.keyboard.config.KeyboardAction;
import com.termux.app.terminal.keyboard.config.KeyboardConfigParser;
import com.termux.app.terminal.keyboard.config.KeyboardConfigSnapshot;
import com.termux.app.terminal.keyboard.config.KeyboardDocument;
import com.termux.app.terminal.keyboard.input.KeyboardInputDispatcher;
import com.termux.app.terminal.keyboard.input.KeyboardState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class BuiltInKeyboardViewTest {
    private BuiltInKeyboardView view;
    private KeyboardConfigSnapshot snapshot;
    private RecordingSink sink;

    @Before
    public void setup() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        String json =
                new String(
                        Files.readAllBytes(
                                new File("src/main/assets/keyboard/default-keyboard.json")
                                        .toPath()),
                        StandardCharsets.UTF_8);
        snapshot = new KeyboardConfigParser().parse(json, "asset").snapshot;
        view = new BuiltInKeyboardView(activity);
        sink = new RecordingSink();
        view.setDispatcher(new KeyboardInputDispatcher(view.getKeyboardState(), sink));
        view.setProfile("default", snapshot.defaultProfile());
        activity.setContentView(view);
    }

    @Test
    public void choosesCompactAtPhoneWidthsAndWideAtTabletWidths() {
        measure(320);
        assertEquals("compact", view.getVariant().id);
        assertEquals(59, view.getChildCount());
        measure(360);
        assertEquals("compact", view.getVariant().id);
        measure(411);
        assertEquals("compact", view.getVariant().id);
        measure(600);
        assertEquals("wide", view.getVariant().id);
        assertTrue(view.getChildCount() > 50);
    }

    @Test
    public void keysAreFocusableClickableAndExposeLabels() {
        measure(360);
        View key = view.getChildAt(0);
        assertTrue(key.isFocusable());
        assertTrue(key.isClickable());
        assertNotNull(key.getContentDescription());
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, key.getImportantForAccessibility());
    }

    @Test
    public void terminalSemanticColorsReachTheKeyboardSurface() {
        KeyboardDocument.Profile profile = snapshot.defaultProfile();
        profile.theme.background = "terminalBackground";
        view.setProfile("default", profile);
        view.setTerminalColors(0xff123456, 0xff654321);
        measure(360);
        assertEquals(0xff654321, ((ColorDrawable) view.getBackground()).getColor());

        profile.theme.background = "terminalForeground";
        view.setProfile("default", profile);
        measure(360);
        assertEquals(0xff123456, ((ColorDrawable) view.getBackground()).getColor());
    }

    @Test
    public void compactLayoutBlendsQwertyAndAndroidPlacementWithoutGaps() {
        measure(360);
        assertHybridLayout();

        float density = view.getResources().getDisplayMetrics().density;
        assertTrue(view.getMeasuredHeight() >= 285 * density);
        assertTrue(view.getMeasuredHeight() <= 300 * density);
        android.graphics.Bitmap bitmap =
                android.graphics.Bitmap.createBitmap(
                        view.getMeasuredWidth(),
                        view.getMeasuredHeight(),
                        android.graphics.Bitmap.Config.ARGB_8888);
        view.draw(new android.graphics.Canvas(bitmap));
    }

    @Test
    public void compactLettersUseAWiderTenUnitGridThanNumbers() {
        measure(360);
        View number = find("1");
        View letter = find("q");
        float density = view.getResources().getDisplayMetrics().density;
        assertTrue(number.getWidth() >= 25 * density);
        assertTrue(letter.getWidth() > number.getWidth());
        for (String label : new String[] {"p", "a", "l", "z", "m"}) {
            View key = find(label);
            assertEquals(label + " width", letter.getWidth(), key.getWidth(), 3 * density);
            assertEquals(label + " height", letter.getHeight(), key.getHeight(), 1);
        }
        assertEquals(number.getWidth(), find("0").getWidth(), 3 * density);

        Map<Integer, Integer> keysPerRow = new HashMap<>();
        for (int index = 0; index < view.getChildCount(); index++) {
            View key = view.getChildAt(index);
            Integer count = keysPerRow.get(key.getTop());
            keysPerRow.put(key.getTop(), count == null ? 1 : count + 1);
        }
        assertEquals(6, keysPerRow.size());
        assertEquals(1, Collections.frequency(new ArrayList<>(keysPerRow.values()), 11));
        assertEquals(3, Collections.frequency(new ArrayList<>(keysPerRow.values()), 10));
        assertEquals(2, Collections.frequency(new ArrayList<>(keysPerRow.values()), 9));
    }

    @Test
    public void wideLayoutUsesTheSameQwertyAndroidOrganization() {
        measure(600);
        assertHybridLayout();
        View reference = find("q");
        for (String label : new String[] {"0", "q", "p", "a", "l", "z", "m"}) {
            View key = find(label);
            if (!"0".equals(label))
                assertEquals(label + " width", reference.getWidth(), key.getWidth(), 3);
            assertEquals(label + " height", reference.getHeight(), key.getHeight(), 1);
        }
        assertTrue(reference.getWidth() > find("0").getWidth());
        float density = view.getResources().getDisplayMetrics().density;
        assertTrue(view.getMeasuredHeight() >= 315 * density);
        assertTrue(view.getMeasuredHeight() <= 323 * density);
    }

    private void assertHybridLayout() {
        View one = find("1"),
                backspace = find("Backspace"),
                q = find("q"),
                a = find("a"),
                z = find("z"),
                shift = find("Shift"),
                enter = find("Enter"),
                esc = find("ESC"),
                tab = find("Tab"),
                home = find("Home"),
                up = find("Up arrow"),
                end = find("End"),
                control = find("Control"),
                alt = find("Alt"),
                comma = find(","),
                microphone = find("Voice input"),
                space = find("Space"),
                period = find("."),
                left = find("Left arrow"),
                down = find("Down arrow"),
                right = find("Right arrow");

        assertEquals(one.getTop(), backspace.getTop());
        assertTrue(backspace.getWidth() > one.getWidth() * 1.35f);
        assertTrue(q.getWidth() > one.getWidth());
        assertEquals(q.getWidth(), a.getWidth(), 3);
        assertEquals(q.getWidth(), z.getWidth(), 3);
        assertEquals(z.getTop(), shift.getTop());
        assertEquals(z.getTop(), enter.getTop());
        assertTrue(shift.getWidth() > q.getWidth() * 1.35f);
        assertTrue(enter.getWidth() > q.getWidth() * 1.35f);

        assertEquals(esc.getTop(), tab.getTop());
        assertEquals(esc.getTop(), home.getTop());
        assertEquals(esc.getTop(), up.getTop());
        assertEquals(esc.getTop(), end.getTop());
        assertEquals(control.getTop(), alt.getTop());
        assertEquals(control.getTop(), comma.getTop());
        assertEquals(control.getTop(), microphone.getTop());
        assertEquals(control.getTop(), space.getTop());
        assertEquals(control.getTop(), period.getTop());
        assertEquals(control.getTop(), left.getTop());
        assertEquals(control.getTop(), down.getTop());
        assertEquals(control.getTop(), right.getTop());

        assertTrue(control.getLeft() < alt.getLeft());
        assertTrue(alt.getLeft() < comma.getLeft());
        assertTrue(comma.getLeft() < microphone.getLeft());
        assertTrue(microphone.getLeft() < space.getLeft());
        assertTrue(space.getLeft() < period.getLeft());
        assertTrue(period.getLeft() < left.getLeft());
        assertTrue(left.getLeft() < down.getLeft());
        assertTrue(down.getLeft() < right.getLeft());
        assertTrue(space.getWidth() > q.getWidth() * 2.3f);

        assertEquals(
                home.getLeft() + home.getWidth() / 2f,
                left.getLeft() + left.getWidth() / 2f,
                3f);
        assertEquals(
                up.getLeft() + up.getWidth() / 2f,
                down.getLeft() + down.getWidth() / 2f,
                3f);
        assertEquals(
                end.getLeft() + end.getWidth() / 2f,
                right.getLeft() + right.getWidth() / 2f,
                3f);

        assertTrue(one.getTop() < q.getTop());
        assertTrue(q.getTop() < a.getTop());
        assertTrue(a.getTop() < z.getTop());
        assertTrue(z.getTop() < esc.getTop());
        assertTrue(esc.getTop() < control.getTop());
    }

    @Test
    public void fiveRowLandscapeRailsShowDigitsAndKeepEqualLetters() {
        KeyboardDocument.Variant compact = snapshot.defaultProfile().variants.get(0);
        KeyboardDocument.LandscapeSplit split = compact.landscapeSplit;
        float density = view.getResources().getDisplayMetrics().density;
        int width = Math.round(800 * split.railWidthPercent / 100f * density);
        int height = Math.round(360 * density);

        List<List<List<String>>> rails = Arrays.asList(split.left, split.right);
        List<List<String>> utilities = Arrays.asList(split.leftUtility, split.rightUtility);
        for (int side = 0; side < rails.size(); side++) {
            List<List<String>> rows = rails.get(side);
            List<String> utility = utilities.get(side);
            BuiltInKeyboardView rail = new BuiltInKeyboardView(view.getContext());
            rail.setProfile(
                    "default",
                    snapshot.defaultProfile(),
                    TerminalInputHostController.railVariant(
                            compact, split, rows, utility, "fit-test"));
            measure(rail, width, height);

            int expectedKeys = utility.size();
            for (List<String> row : rows) expectedKeys += row.size();
            assertEquals(expectedKeys, rail.getChildCount());
            assertTrue(rail.getChildAt(0).getTop() > 0);
            assertTrue(rail.getChildAt(rail.getChildCount() - 1).getBottom() < height);
            Set<Integer> rowTops = new HashSet<>();
            for (int index = 0; index < rail.getChildCount(); index++) {
                View key = rail.getChildAt(index);
                rowTops.add(key.getTop());
                assertTrue(key.getLeft() >= 0);
                assertTrue(key.getRight() <= width);
                assertTrue(key.getWidth() >= 27 * density);
            }
            assertEquals(6, rowTops.size());

            View firstUtility = rail.getChildAt(0);
            View reference = find(rail, side == 0 ? "q" : "y");
            View firstDigit = find(rail, side == 0 ? "1" : "6");
            View space = find(rail, "Space");
            assertEquals(split.utilityRowHeightDp * density, firstUtility.getHeight(), 1);
            assertEquals(split.rowHeightDp * density, reference.getHeight(), 1);
            assertTrue(firstUtility.getHeight() < reference.getHeight());
            assertTrue(firstDigit.getTop() < reference.getTop());
            assertNotNull(space);
            if (side == 0) assertTrue(space.getWidth() > reference.getWidth() * 1.8f);
            else assertEquals(reference.getWidth(), space.getWidth(), 1);

            String[] letters =
                    side == 0
                            ? new String[] {"q", "w", "e", "r", "t", "a", "g", "z", "v"}
                            : new String[] {"y", "u", "i", "o", "p", "h", "l", "b", "m"};
            for (String letter : letters)
                assertEquals(letter, reference.getWidth(), find(rail, letter).getWidth(), 1);
            String[] digits =
                    side == 0
                            ? new String[] {"1", "2", "3", "4", "5"}
                            : new String[] {"6", "7", "8", "9", "0"};
            for (String digit : digits)
                assertEquals(digit, reference.getWidth(), find(rail, digit).getWidth(), 1);

            if (side == 1) {
                View up = find(rail, "Up arrow"), down = find(rail, "Down arrow");
                float upCenter = up.getLeft() + up.getWidth() / 2f;
                float downCenter = down.getLeft() + down.getWidth() / 2f;
                assertEquals(upCenter, downCenter, 2 * density);
            }
        }
    }

    @Test
    public void bundledMicrophoneKeyDispatchesSpeechAppAction() {
        measure(360);
        View mic = find("Voice input");
        long now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {mic}, 0));
        view.onTouchEvent(event(now, MotionEvent.ACTION_UP, new View[] {mic}, 0));
        assertEquals("app:speechInput", sink.events.get(0));
    }

    @Test
    public void oneShotShiftUppercasesEveryBundledLetterAndItsOutput() {
        measure(360);
        View shift = find("Shift");
        long now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {shift}, 0));
        view.onTouchEvent(event(now, MotionEvent.ACTION_UP, new View[] {shift}, 0));

        for (char letter = 'A'; letter <= 'Z'; letter++) find(String.valueOf(letter));

        View a = find("A");
        now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {a}, 0));
        view.onTouchEvent(event(now, MotionEvent.ACTION_UP, new View[] {a}, 0));
        assertEquals("65:false:false", sink.events.get(0));
        find("a");
    }

    @Test
    public void accessibilityActivationHonorsEveryModifierMode() {
        assertAccessibleMode("modifier", "CTRL", "momentary", KeyboardState.VisualState.HELD,
                KeyboardState.VisualState.INACTIVE);
        assertAccessibleMode("modifier", "CTRL", "once", KeyboardState.VisualState.ARMED,
                KeyboardState.VisualState.INACTIVE);
        assertAccessibleMode("modifier", "CTRL", "locked", KeyboardState.VisualState.LOCKED,
                KeyboardState.VisualState.INACTIVE);
        assertAccessibleMode("modifier", "CTRL", "smart", KeyboardState.VisualState.ARMED,
                KeyboardState.VisualState.LOCKED, KeyboardState.VisualState.INACTIVE);
    }

    @Test
    public void accessibilityActivationHonorsEveryLayerMode() {
        assertAccessibleMode("layer", "altgr", "momentary", KeyboardState.VisualState.HELD,
                KeyboardState.VisualState.INACTIVE);
        assertAccessibleMode("layer", "altgr", "once", KeyboardState.VisualState.ARMED,
                KeyboardState.VisualState.INACTIVE);
        assertAccessibleMode("layer", "altgr", "locked", KeyboardState.VisualState.LOCKED,
                KeyboardState.VisualState.INACTIVE);
        assertAccessibleMode("layer", "altgr", "smart", KeyboardState.VisualState.ARMED,
                KeyboardState.VisualState.LOCKED, KeyboardState.VisualState.INACTIVE);
    }

    @Test
    public void cancellationClearsLockedState() {
        view.getKeyboardState().lockModifier("CTRL");
        view.cancelAll();
        assertFalse(view.getKeyboardState().activeModifier("CTRL"));
    }

    @Test
    public void twoPointerCtrlChordConsumesHeldState() {
        measure(360);
        View ctrl = find("Control"), q = find("q");
        long now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {ctrl}, 0));
        view.onTouchEvent(event(now, MotionEvent.ACTION_POINTER_DOWN, new View[] {ctrl, q}, 1));
        view.onTouchEvent(event(now, MotionEvent.ACTION_POINTER_UP, new View[] {ctrl, q}, 1));
        view.onTouchEvent(event(now, MotionEvent.ACTION_UP, new View[] {ctrl}, 0));
        assertEquals("113:true:false", sink.events.get(0));
        assertFalse(view.getKeyboardState().activeModifier("CTRL"));
    }

    @Test
    public void splitSurfacesShareStateForCrossRailChord() {
        BuiltInKeyboardView right = new BuiltInKeyboardView(view.getContext());
        right.setKeyboardState(view.getKeyboardState());
        KeyboardInputDispatcher dispatcher =
                new KeyboardInputDispatcher(view.getKeyboardState(), sink);
        view.setDispatcher(dispatcher);
        right.setDispatcher(dispatcher);
        view.setProfile("default", snapshot.defaultProfile(), oneRow("ctrl", "left"));
        right.setProfile("default", snapshot.defaultProfile(), oneRow("q", "right"));
        measure(view, 100, 100);
        measure(right, 100, 100);
        View ctrl = view.getChildAt(0), q = right.getChildAt(0);
        long now = SystemClock.uptimeMillis();
        view.onTouchEvent(single(now, MotionEvent.ACTION_DOWN, ctrl, 0));
        right.onTouchEvent(single(now, MotionEvent.ACTION_DOWN, q, 1));
        right.onTouchEvent(single(now, MotionEvent.ACTION_UP, q, 1));
        view.onTouchEvent(single(now, MotionEvent.ACTION_UP, ctrl, 0));
        assertEquals("113:true:false", sink.events.get(0));
        assertFalse(view.getKeyboardState().activeModifier("CTRL"));
    }

    @Test
    public void repeatStartsAfterBoundedDelayAndStopsOnRelease() {
        measure(360);
        View backspace = find("Backspace");
        long now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {backspace}, 0));
        ShadowLooper.idleMainLooper(700, TimeUnit.MILLISECONDS);
        assertTrue(sink.events.size() > 1);
        view.onTouchEvent(event(now, MotionEvent.ACTION_UP, new View[] {backspace}, 0));
        int count = sink.events.size();
        ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS);
        assertEquals(count, sink.events.size());
    }

    @Test
    public void singleAlternativeLongPressCommitsSecondary() {
        measure(360);
        View one = find("1");
        long now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {one}, 0));
        ShadowLooper.idleMainLooper(600, TimeUnit.MILLISECONDS);
        view.onTouchEvent(event(now, MotionEvent.ACTION_UP, new View[] {one}, 0));
        assertEquals("33:false:false", sink.events.get(0));
    }

    @Test
    public void longPressAltArmsAltGrForTheNextCharacter() {
        measure(360);
        View alt = find("Alt");
        long now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {alt}, 0));
        ShadowLooper.idleMainLooper(600, TimeUnit.MILLISECONDS);
        view.onTouchEvent(event(now, MotionEvent.ACTION_UP, new View[] {alt}, 0));

        View at = find("@");
        now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {at}, 0));
        view.onTouchEvent(event(now, MotionEvent.ACTION_UP, new View[] {at}, 0));

        assertEquals("64:false:false", sink.events.get(0));
        assertFalse(view.getKeyboardState().activeLayer("altgr"));
        assertFalse(view.getKeyboardState().activeModifier("ALT"));
    }

    @Test
    public void longPressAlternativeCommitsAfterSlidingOutsideTheKey() {
        measure(360);
        View minus = find("-");
        long now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {minus}, 0));
        ShadowLooper.idleMainLooper(600, TimeUnit.MILLISECONDS);
        float x = view.getWidth() - 1;
        float y = minus.getTop() + minus.getHeight() / 2f;
        view.onTouchEvent(MotionEvent.obtain(now, SystemClock.uptimeMillis(),
            MotionEvent.ACTION_MOVE, x, y, 0));
        view.onTouchEvent(MotionEvent.obtain(now, SystemClock.uptimeMillis(),
            MotionEvent.ACTION_UP, x, y, 0));

        assertEquals(Collections.singletonList("42:false:false"), sink.events);
    }

    @Test
    public void spaceTapTypesSpaceButHoldAndDragMovesCursor() {
        measure(360);
        View space = find("Space");
        long now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {space}, 0));
        view.onTouchEvent(event(now, MotionEvent.ACTION_UP, new View[] {space}, 0));
        assertEquals("32:false:false", sink.events.get(0));
        sink.events.clear();
        now = SystemClock.uptimeMillis();
        view.onTouchEvent(event(now, MotionEvent.ACTION_DOWN, new View[] {space}, 0));
        ShadowLooper.idleMainLooper(600, TimeUnit.MILLISECONDS);
        float density = view.getResources().getDisplayMetrics().density;
        float x = space.getLeft() + space.getWidth() / 2f + 42 * density;
        float y = space.getTop() + space.getHeight() / 2f;
        view.onTouchEvent(
                MotionEvent.obtain(
                        now, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, x, y, 0));
        assertFalse(sink.events.toString(), sink.events.isEmpty());
        for (String output : sink.events) assertTrue(output.startsWith("RIGHT:"));
        view.onTouchEvent(
                MotionEvent.obtain(
                        now, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0));
        assertFalse(sink.events.contains("32:false:false"));
    }

    private void measure(int widthDp) {
        float density = view.getResources().getDisplayMetrics().density;
        int w = Math.round(widthDp * density);
        view.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST));
        view.layout(0, 0, w, view.getMeasuredHeight());
    }

    private void measure(BuiltInKeyboardView target, int width, int height) {
        target.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        target.layout(0, 0, width, height);
    }

    private KeyboardDocument.Variant oneRow(String id, String variantId) {
        KeyboardDocument.Key found = null;
        for (KeyboardDocument.Row row : snapshot.defaultProfile().variants.get(0).rows)
            for (KeyboardDocument.Key key : row.keys)
                if (id.equals(key.id)) found = key;
        assertNotNull(found);
        KeyboardDocument.Row row = new KeyboardDocument.Row();
        row.keys = Collections.singletonList(found);
        KeyboardDocument.Variant variant = new KeyboardDocument.Variant();
        variant.id = variantId;
        variant.rowHeightDp = 40;
        variant.rows = Collections.singletonList(row);
        return variant;
    }

    private void assertAccessibleMode(
            String type,
            String id,
            String mode,
            KeyboardState.VisualState... expectedStates) {
        KeyboardAction action = new KeyboardAction();
        action.type = type;
        action.mode = mode;
        if ("modifier".equals(type)) action.modifier = id;
        else action.layer = id;

        KeyboardDocument.Key key = new KeyboardDocument.Key();
        key.id = type + "-" + mode;
        key.label = mode;
        key.accessibilityLabel = mode;
        key.width = 1f;
        key.action = action;
        KeyboardDocument.Row row = new KeyboardDocument.Row();
        row.keys = Collections.singletonList(key);
        KeyboardDocument.Variant variant = new KeyboardDocument.Variant();
        variant.id = key.id;
        variant.rowHeightDp = 40;
        variant.rows = Collections.singletonList(row);

        view.setProfile("default", snapshot.defaultProfile(), variant);
        measure(view, 120, 60);
        View accessibleKey = view.getChildAt(0);
        for (KeyboardState.VisualState expected : expectedStates) {
            accessibleKey.performClick();
            KeyboardState.VisualState actual =
                    "modifier".equals(type)
                            ? view.getKeyboardState().modifierState(id)
                            : view.getKeyboardState().layerState(id);
            assertEquals(type + "/" + mode, expected, actual);
        }
    }

    private View find(String description) {
        return find(view, description);
    }

    private View find(BuiltInKeyboardView target, String description) {
        for (int i = 0; i < target.getChildCount(); i++)
            if (description.contentEquals(target.getChildAt(i).getContentDescription()))
                return target.getChildAt(i);
        throw new AssertionError(description);
    }

    private MotionEvent event(long down, int action, View[] keys, int actionIndex) {
        MotionEvent.PointerProperties[] pp = new MotionEvent.PointerProperties[keys.length];
        MotionEvent.PointerCoords[] pc = new MotionEvent.PointerCoords[keys.length];
        for (int i = 0; i < keys.length; i++) {
            pp[i] = new MotionEvent.PointerProperties();
            pp[i].id = i;
            pp[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            pc[i] = new MotionEvent.PointerCoords();
            pc[i].x = keys[i].getLeft() + keys[i].getWidth() / 2f;
            pc[i].y = keys[i].getTop() + keys[i].getHeight() / 2f;
            pc[i].pressure = 1;
            pc[i].size = 1;
        }
        int encoded = action | (actionIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        return MotionEvent.obtain(
                down,
                SystemClock.uptimeMillis(),
                encoded,
                keys.length,
                pp,
                pc,
                0,
                0,
                1,
                1,
                0,
                0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0);
    }

    private MotionEvent single(long down, int action, View key, int pointerId) {
        MotionEvent.PointerProperties pp = new MotionEvent.PointerProperties();
        pp.id = pointerId;
        pp.toolType = MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords pc = new MotionEvent.PointerCoords();
        pc.x = key.getLeft() + key.getWidth() / 2f;
        pc.y = key.getTop() + key.getHeight() / 2f;
        pc.pressure = 1;
        pc.size = 1;
        return MotionEvent.obtain(
                down,
                SystemClock.uptimeMillis(),
                action,
                1,
                new MotionEvent.PointerProperties[] {pp},
                new MotionEvent.PointerCoords[] {pc},
                0,
                0,
                1,
                1,
                0,
                0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0);
    }

    private static final class RecordingSink implements KeyboardInputDispatcher.Sink {
        final List<String> events = new ArrayList<>();

        public void codePoint(int c, boolean x, boolean a) {
            events.add(c + ":" + x + ":" + a);
        }

        public void terminalKey(String k, boolean c, boolean a, boolean s) {
            events.add(k + ":" + c + ":" + a + ":" + s);
        }

        public void appAction(String a) {
            events.add("app:" + a);
        }

        public void profileAction(String p) {}
    }
}
