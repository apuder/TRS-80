/*
 * Copyright 2012-2013, Arno Puder
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

import java.util.Arrays;

import org.puder.trs80.cast.RemoteCastScreen;
import org.puder.trs80.cast.RemoteDisplayChannel;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.SurfaceHolder;

public class CharacterRenderThread extends Thread implements RenderThread {

    private int              model;

    private int              trsScreenCols;
    private int              trsScreenRows;
    private int              trsCharWidth;
    private int              trsCharHeight;

    private int              dirtyRectTop;
    private int              dirtyRectLeft;
    private int              dirtyRectBottom;
    private int              dirtyRectRight;
    final private Rect       clipRect;
    final private Rect       adjustedClipRect;

    final private Bitmap     font[];

    private boolean          run         = false;
    private volatile boolean isRendering = true;
    protected SurfaceHolder  surfaceHolder;
    private byte[]           screenBuffer;
    private short[]          lastScreenBuffer;

    private StringBuilder    screenCharBuffer;

    public CharacterRenderThread() {
        surfaceHolder = null;
        Hardware h = TRS80Application.getHardware();
        model = h.getModel();
        screenBuffer = h.getScreenBuffer();
        trsScreenCols = h.getScreenConfiguration().trsScreenCols;
        trsScreenRows = h.getScreenConfiguration().trsScreenRows;
        trsCharWidth = h.getCharWidth();
        trsCharHeight = h.getCharHeight();
        font = h.getFont();
        screenCharBuffer = new StringBuilder(trsScreenCols * trsScreenRows + trsScreenRows);
        lastScreenBuffer = new short[trsScreenCols * trsScreenRows];
        Arrays.fill(lastScreenBuffer, Short.MAX_VALUE);
        clipRect = new Rect();
        adjustedClipRect = new Rect();
    }

    public synchronized void setSurfaceHolder(SurfaceHolder holder) {
        surfaceHolder = holder;
        forceScreenUpdate();
    }

    public void setRunning(boolean run) {
        this.run = run;
        if (run) {
            setPriority(Thread.MAX_PRIORITY);
            start();
        } else {
            boolean retry = true;
            interrupt();
            while (retry) {
                try {
                    join();
                    retry = false;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public boolean isRendering() {
        return this.isRendering;
    }

    @Override
    public synchronized void run() {
        while (run) {
            isRendering = false;
            try {
                this.wait();
            } catch (InterruptedException e) {
                return;
            }
            isRendering = true;

            boolean expandedMode = TRS80Application.getHardware().getExpandedScreenMode();
            int d = expandedMode ? 2 : 1;
            computeDirtyRect(d);
            if (dirtyRectBottom == -1) {
                // Nothing to update
                continue;
            }

            if (surfaceHolder != null) {
                /*
                 * The Android documentation does not mention that lockCanvas()
                 * may adjust the clip rect due to double buffering. Since the
                 * dirty rect might differ from the one that was computed in
                 * computeDirtyRect() we simply invalidate the whole TRS screen
                 * when this happens.
                 */
                Canvas canvas = surfaceHolder.lockCanvas(adjustedClipRect);
                if (canvas == null) {
                    continue;
                }
                if (adjustedClipRect.left != clipRect.left || adjustedClipRect.top != clipRect.top
                        || adjustedClipRect.right != clipRect.right
                        || adjustedClipRect.bottom != clipRect.bottom) {
                    dirtyRectLeft = dirtyRectTop = 0;
                    dirtyRectRight = trsScreenCols / d - 1;
                    dirtyRectBottom = trsScreenRows - 1;
                }
                renderScreenToCanvas(canvas, expandedMode);
                surfaceHolder.unlockCanvasAndPost(canvas);
            } else {
                renderScreenToCast(RemoteCastScreen.get(), expandedMode);
            }
        }
    }

    private void renderScreenToCanvas(Canvas canvas, boolean expandedMode) {
        if (expandedMode) {
            canvas.scale(2, 1);
        }
        int d = expandedMode ? 2 : 1;

        for (int row = dirtyRectTop; row <= dirtyRectBottom; row++) {
            for (int col = dirtyRectLeft; col <= dirtyRectRight; col++) {
                int i = row * trsScreenCols + col * d;
                int ch = screenBuffer[i] & 0xff;
                // Emulate Radio Shack lowercase mod (for Model 1)
                if (this.model == Hardware.MODEL1 && ch < 0x20) {
                    ch += 0x40;
                }
                int startx = trsCharWidth * col;
                int starty = trsCharHeight * row;
                canvas.drawBitmap(font[ch], startx, starty, null);
            }
        }

    }

    private void renderScreenToCast(RemoteDisplayChannel remoteDisplay, boolean expandedMode) {
        int d = expandedMode ? 2 : 1;

        int i = 0;
        screenCharBuffer.setLength(0);
        for (int row = 0; row < trsScreenRows; row++) {
            if (row != 0) {
                screenCharBuffer.append('|');
            }
            if (row < dirtyRectTop || row > dirtyRectBottom) {
                i += trsScreenCols;
                continue;
            }
            for (int col = 0; col < trsScreenCols / d; col++) {
                int ch = screenBuffer[i] & 0xff;
                // Emulate Radio Shack lowercase mod (for Model 1)
                if (this.model == Hardware.MODEL1 && ch < 0x20) {
                    ch += 0x40;
                }

                // TODO: Choose encoding based on current model.
                screenCharBuffer.append(CharMapping.m3toUnicode[ch]);
                i += d;
            }
        }
        remoteDisplay.sendScreenBuffer(expandedMode, String.valueOf(screenCharBuffer));
    }

    private void computeDirtyRect(int d) {
        dirtyRectTop = dirtyRectLeft = Integer.MAX_VALUE;
        dirtyRectBottom = dirtyRectRight = -1;
        int i = 0;
        for (int row = 0; row < trsScreenRows; row++) {
            for (int col = 0; col < trsScreenCols / d; col++) {
                if (lastScreenBuffer[i] != screenBuffer[i]) {
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
                    lastScreenBuffer[i] = screenBuffer[i];
                }
                i += d;
            }
        }
        if (dirtyRectBottom == -1) {
            return;
        }
        clipRect.left = adjustedClipRect.left = trsCharWidth * dirtyRectLeft * d;
        clipRect.right = adjustedClipRect.right = trsCharWidth * (dirtyRectRight + 1) * d;
        clipRect.top = adjustedClipRect.top = trsCharHeight * dirtyRectTop;
        clipRect.bottom = adjustedClipRect.bottom = trsCharHeight * (dirtyRectBottom + 1);
    }

    public synchronized void triggerScreenUpdate() {
        this.notify();
    }

    public synchronized void forceScreenUpdate() {
        Arrays.fill(lastScreenBuffer, Short.MAX_VALUE);
        this.notify();
    }

    public synchronized Bitmap takeScreenshot() {
        Hardware h = TRS80Application.getHardware();
        Bitmap screenshot = Bitmap.createBitmap(h.getScreenWidth(), h.getScreenHeight(),
                Config.RGB_565);
        boolean expandedMode = TRS80Application.getHardware().getExpandedScreenMode();
        int d = expandedMode ? 2 : 1;
        dirtyRectLeft = dirtyRectTop = 0;
        dirtyRectRight = trsScreenCols / d - 1;
        dirtyRectBottom = trsScreenRows - 1;
        Canvas c = new Canvas(screenshot);
        renderScreenToCanvas(c, expandedMode);
        return screenshot;
    }
}
