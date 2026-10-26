package com.termux.app.terminal.keyboard.view;

import android.graphics.RectF;

import com.termux.app.terminal.keyboard.config.KeyboardDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Weighted geometry whose hit bands absorb visual gaps and outer padding. */
public final class KeyboardGeometry {

    public static final class Cell {
        public final KeyboardDocument.Key key;
        public final RectF visual;
        public final RectF hit;

        Cell(KeyboardDocument.Key key, RectF visual, RectF hit) {
            this.key = key;
            this.visual = visual;
            this.hit = hit;
        }
    }

    public List<Cell> layout(
            KeyboardDocument.Variant variant,
            int width,
            int height,
            float density,
            KeyboardStyleResolver.Resolved style) {
        if (variant == null || variant.rows == null) return Collections.emptyList();

        float horizontalPadding = style.horizontalPadding * density;
        float verticalPadding = style.verticalPadding * density;
        float contentHeight = contentHeight(variant, density, style.verticalGap);
        float desiredHeight = contentHeight + verticalPadding * 2;
        float y = verticalPadding;
        if (height > desiredHeight && "center".equals(style.verticalAlignment))
            y += (height - desiredHeight) / 2f;
        else if (height > desiredHeight && "bottom".equals(style.verticalAlignment))
            y += height - desiredHeight;

        List<Cell> result = new ArrayList<>();
        float previousVerticalGap = 0;
        for (int rowIndex = 0; rowIndex < variant.rows.size(); rowIndex++) {
            KeyboardDocument.Row row = variant.rows.get(rowIndex);
            float rowHeight = (row.height == null ? variant.rowHeightDp : row.height) * density;
            float horizontalGap = horizontalGap(row, style.horizontalGap) * density;
            float verticalGap = verticalGap(row, style.verticalGap) * density;
            float rowLeft = horizontalPadding + row.offset * density;
            float rowRight = width - horizontalPadding - row.endOffset * density;
            float weights = 0;
            for (KeyboardDocument.Key key : row.keys) weights += key.width;
            float rowWidth = Math.max(0, rowRight - rowLeft);
            float consumedWeight = 0;
            for (int keyIndex = 0; keyIndex < row.keys.size(); keyIndex++) {
                KeyboardDocument.Key key = row.keys.get(keyIndex);
                float cellLeft = rowLeft + rowWidth * consumedWeight / weights;
                consumedWeight += key.width;
                float cellRight =
                        keyIndex == row.keys.size() - 1
                                ? rowRight
                                : rowLeft + rowWidth * consumedWeight / weights;
                float visualLeft = keyIndex == 0 ? cellLeft : cellLeft + horizontalGap / 2f;
                float visualRight =
                        keyIndex == row.keys.size() - 1
                                ? cellRight
                                : cellRight - horizontalGap / 2f;
                if (visualRight < visualLeft) {
                    float center = (cellLeft + cellRight) / 2f;
                    visualLeft = center;
                    visualRight = center;
                }
                RectF visual = new RectF(visualLeft, y, visualRight, y + rowHeight);
                float hitTop = rowIndex == 0 ? 0 : y - previousVerticalGap / 2f;
                float hitBottom =
                        rowIndex == variant.rows.size() - 1
                                ? height
                                : y + rowHeight + verticalGap / 2f;
                float hitLeft = keyIndex == 0 ? 0 : cellLeft;
                float hitRight = keyIndex == row.keys.size() - 1 ? width : cellRight;
                result.add(new Cell(key, visual, new RectF(hitLeft, hitTop, hitRight, hitBottom)));
            }
            y += rowHeight + verticalGap;
            previousVerticalGap = verticalGap;
        }
        return result;
    }

    public static float height(
            KeyboardDocument.Variant variant, float density, KeyboardStyleResolver.Resolved style) {
        if (variant == null || variant.rows == null) return 0;
        return contentHeight(variant, density, style.verticalGap)
                + style.verticalPadding * density * 2;
    }

    private static float contentHeight(
            KeyboardDocument.Variant variant, float density, float defaultVerticalGapDp) {
        float height = 0;
        for (int rowIndex = 0; rowIndex < variant.rows.size(); rowIndex++) {
            KeyboardDocument.Row row = variant.rows.get(rowIndex);
            height += (row.height == null ? variant.rowHeightDp : row.height) * density;
            if (rowIndex < variant.rows.size() - 1)
                height += verticalGap(row, defaultVerticalGapDp) * density;
        }
        return height;
    }

    private static float horizontalGap(KeyboardDocument.Row row, float fallback) {
        if (row.horizontalGap != null) return row.horizontalGap;
        return fallback;
    }

    private static float verticalGap(KeyboardDocument.Row row, float fallback) {
        if (row.verticalGap != null) return row.verticalGap;
        return fallback;
    }
}
