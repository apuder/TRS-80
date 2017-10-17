/*
 * Copyright 2012-2017, Arno Puder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.puder.trs80;

import android.graphics.Rect;

import java.nio.ByteBuffer;
import java.util.Arrays;

class DirtyRect {
    private int trsScreenCols;
    private int trsScreenRows;
    private int trsCharWidth;
    private int trsCharHeight;

    private int dirtyRectTop;
    private int dirtyRectLeft;
    private int dirtyRectBottom;
    private int dirtyRectRight;
    private final Rect clipRect;
    private final Rect originalClipRect;
    private ByteBuffer screenBuffer;
    private short[] lastScreenBuffer;
    private boolean expandedMode;
    private int d;

    DirtyRect(Hardware hardware, ByteBuffer screenBuffer) {
        this.screenBuffer = screenBuffer;
        trsScreenCols = hardware.getScreenConfiguration().trsScreenCols;
        trsScreenRows = hardware.getScreenConfiguration().trsScreenRows;
        trsCharWidth = hardware.getCharWidth();
        trsCharHeight = hardware.getCharHeight();
        clipRect = new Rect();
        originalClipRect = new Rect();
        lastScreenBuffer = new short[trsScreenCols * trsScreenRows];
        resetLastScreenBuffer();
    }

    private void resetLastScreenBuffer() {
        Arrays.fill(lastScreenBuffer, Short.MAX_VALUE);
    }

    void setExpandedMode(boolean newExpandedMode) {
        if (expandedMode != newExpandedMode) {
            expandedMode = newExpandedMode;
            resetLastScreenBuffer();
        }
        d = expandedMode ? 2: 1;
    }

    void computeDirtyRect() {
        dirtyRectTop = dirtyRectLeft = Integer.MAX_VALUE;
        dirtyRectBottom = dirtyRectRight = -1;
        int i = 0;
        for (int row = 0; row < trsScreenRows; row++) {
            for (int col = 0; col < trsScreenCols / d; col++) {
                if (lastScreenBuffer[i] != screenBuffer.get(i)) {
                    if (dirtyRectTop > row) {
                        dirtyRectTop = row;
                    }
                    if (dirtyRectBottom < row) {
                        dirtyRectBottom = row;
                    }
                    if (dirtyRectLeft > col) {
                        dirtyRectLeft = col;
                    }
                    if (dirtyRectRight < col) {
                        dirtyRectRight = col;
                    }
                    lastScreenBuffer[i] = screenBuffer.get(i);
                }
                i += d;
            }
        }
        if (dirtyRectBottom == -1) {
            return;
        }
        clipRect.left = originalClipRect.left = trsCharWidth * dirtyRectLeft * d;
        clipRect.right = originalClipRect.right = trsCharWidth * (dirtyRectRight + 1) * d;
        clipRect.top = originalClipRect.top = trsCharHeight * dirtyRectTop;
        clipRect.bottom = originalClipRect.bottom = trsCharHeight * (dirtyRectBottom + 1);
    }

    boolean isEmpty() {
        return dirtyRectBottom == -1;
    }

    public void reset() {
        dirtyRectLeft = dirtyRectTop = 0;
        dirtyRectRight = trsScreenCols / d - 1;
        dirtyRectBottom = trsScreenRows - 1;
    }

    public int bottom() {
        return dirtyRectBottom;
    }

    public int top() {
        return dirtyRectTop;
    }

    public int left() {
        return dirtyRectLeft;
    }

    public int right() {
        return dirtyRectRight;
    }

    Rect getClipRect() {
        return clipRect;
    }

    void adjustClipRect() {
        if (originalClipRect.left != clipRect.left || originalClipRect.top != clipRect.top
                || originalClipRect.right != clipRect.right
                || originalClipRect.bottom != clipRect.bottom) {
            reset();
        }
    }
}
