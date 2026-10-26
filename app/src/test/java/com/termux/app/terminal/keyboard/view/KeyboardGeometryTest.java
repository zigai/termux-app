package com.termux.app.terminal.keyboard.view;

import static org.junit.Assert.assertEquals;

import com.termux.app.terminal.keyboard.config.KeyboardDocument;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class KeyboardGeometryTest {

    @Test
    public void weightedWidthsOffsetsAndGapHitBandsAreDeterministic() {
        KeyboardDocument.Row row = new KeyboardDocument.Row();
        row.offset = 10;
        row.horizontalGap = 4f;
        row.keys = Arrays.asList(key("a", 1), key("b", 2));
        KeyboardDocument.Variant variant = variant(40, row);

        KeyboardStyleResolver.Resolved style = new KeyboardStyleResolver.Resolved();
        style.horizontalGap = 3;
        style.verticalGap = 3;
        List<KeyboardGeometry.Cell> cells =
                new KeyboardGeometry().layout(variant, 310, 40, 1, style);

        assertEquals(2, cells.size());
        assertEquals(10, cells.get(0).visual.left, .01);
        assertEquals(98, cells.get(0).visual.width(), .1);
        assertEquals(198, cells.get(1).visual.width(), .1);
        assertEquals(cells.get(0).hit.right, cells.get(1).hit.left, .01);
        assertEquals(40, KeyboardGeometry.height(variant, 1, style), .01);
    }

    @Test
    public void horizontalAndVerticalSpacingPaddingAndCenteringAreIndependent() {
        KeyboardDocument.Row first = new KeyboardDocument.Row();
        first.keys = Arrays.asList(key("a", 1), key("b", 1));
        KeyboardDocument.Row second = new KeyboardDocument.Row();
        second.keys = Arrays.asList(key("c", 1));
        KeyboardDocument.Variant variant = variant(40, first, second);
        KeyboardStyleResolver.Resolved style = new KeyboardStyleResolver.Resolved();
        style.horizontalGap = 4;
        style.verticalGap = 8;
        style.horizontalPadding = 5;
        style.verticalPadding = 6;
        style.verticalAlignment = "center";

        List<KeyboardGeometry.Cell> cells =
                new KeyboardGeometry().layout(variant, 110, 140, 1, style);

        assertEquals(100, KeyboardGeometry.height(variant, 1, style), .01);
        assertEquals(5, cells.get(0).visual.left, .01);
        assertEquals(4, cells.get(1).visual.left - cells.get(0).visual.right, .01);
        assertEquals(26, cells.get(0).visual.top, .01);
        assertEquals(8, cells.get(2).visual.top - cells.get(0).visual.bottom, .01);
        assertEquals(cells.get(0).hit.bottom, cells.get(2).hit.top, .01);
        assertEquals(0, cells.get(0).hit.top, .01);
        assertEquals(140, cells.get(2).hit.bottom, .01);
    }

    @Test
    public void endOffsetKeepsStaggeredRowsCentered() {
        KeyboardDocument.Row row = new KeyboardDocument.Row();
        row.offset = 12;
        row.endOffset = 12;
        row.keys = Arrays.asList(key("a", 1));
        KeyboardDocument.Variant variant = variant(40, row);

        KeyboardStyleResolver.Resolved style = new KeyboardStyleResolver.Resolved();
        List<KeyboardGeometry.Cell> cells =
                new KeyboardGeometry().layout(variant, 100, 40, 1, style);

        assertEquals(12, cells.get(0).visual.left, .01);
        assertEquals(88, cells.get(0).visual.right, .01);
        assertEquals(0, cells.get(0).hit.left, .01);
        assertEquals(100, cells.get(0).hit.right, .01);
    }

    private KeyboardDocument.Variant variant(float rowHeight, KeyboardDocument.Row... rows) {
        KeyboardDocument.Variant variant = new KeyboardDocument.Variant();
        variant.rowHeightDp = rowHeight;
        variant.rows = Arrays.asList(rows);
        return variant;
    }

    private KeyboardDocument.Key key(String id, float width) {
        KeyboardDocument.Key key = new KeyboardDocument.Key();
        key.id = id;
        key.label = id;
        key.width = width;
        return key;
    }
}
