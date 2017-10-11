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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;

import org.puder.trs80.cast.RemoteCastScreen;
import org.puder.trs80.cast.RemoteDisplayChannel;

import java.nio.ByteBuffer;
import java.util.Arrays;

class RenderThread extends Thread {
    private final int TARGET_FPS = 60;

    private final FpsLimiter fpsLimiter;

    private boolean isCasting;
    private int model;
    private Bitmap[] font;

    private int trsScreenCols;
    private int trsScreenRows;
    private int trsCharWidth;
    private int trsCharHeight;

    private int dirtyRectTop;
    private int dirtyRectLeft;
    private int dirtyRectBottom;
    private int dirtyRectRight;
    private final Rect clipRect;
    private final Rect adjustedClipRect;

    private volatile boolean run;
    private SurfaceHolder surfaceHolder;
    private ByteBuffer screenBuffer;
    private short[] lastScreenBuffer;

    private StringBuilder screenCharBuffer;

    RenderThread(boolean casting) {
        isCasting = casting;
        run = true;
        surfaceHolder = null;
        screenBuffer = XTRS.getScreenBuffer();
        lastScreenBuffer = new short[0];
        clipRect = new Rect();
        adjustedClipRect = new Rect();
        fpsLimiter = new FpsLimiter(TARGET_FPS);
    }

    synchronized void setSurfaceHolder(SurfaceHolder holder) {
        surfaceHolder = holder;
    }

    void setRunning(boolean run) {
        this.run = run;
    }

    synchronized void setHardwareSpecs(Hardware hardware) {
        model = hardware.getModel();
        font = hardware.getFont();
        trsScreenCols = hardware.getScreenConfiguration().trsScreenCols;
        trsScreenRows = hardware.getScreenConfiguration().trsScreenRows;
        trsCharWidth = hardware.getCharWidth();
        trsCharHeight = hardware.getCharHeight();
        screenCharBuffer = new StringBuilder(trsScreenCols * trsScreenRows + trsScreenRows);
        lastScreenBuffer = new short[trsScreenCols * trsScreenRows];
        Arrays.fill(lastScreenBuffer, Short.MAX_VALUE);
    }

    @Override
    public synchronized void run() {
        while (run) {
            try {
                fpsLimiter.onFrame();
            } catch (InterruptedException e) {
                break;
            }

            boolean expandedMode = XTRS.isExpandedMode();
            int d = expandedMode ? 2 : 1;
            computeDirtyRect(d);

            if (isCasting) {
                renderScreenToCast(RemoteCastScreen.get(), expandedMode);
                continue;
            }

            if (surfaceHolder != null) {
                Canvas canvas = surfaceHolder.lockCanvas();
                if (canvas == null) {
                    continue;
                }
                renderScreenToCanvas(canvas, expandedMode);
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void renderScreenToCanvas(Canvas canvas, boolean expandedMode) {
        if (expandedMode) {
            canvas.scale(2, 1);
        }
        int d = expandedMode ? 2 : 1;

        for (int row = 0; row <= trsScreenRows; row++) {
            for (int col = 0; col <= trsScreenCols; col++) {
                int i = row * trsScreenCols + col * d;
                int ch = screenBuffer.get(i) & 0xff;
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
                int ch = screenBuffer.get(i) & 0xff;
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
        clipRect.left = adjustedClipRect.left = trsCharWidth * dirtyRectLeft * d;
        clipRect.right = adjustedClipRect.right = trsCharWidth * (dirtyRectRight + 1) * d;
        clipRect.top = adjustedClipRect.top = trsCharHeight * dirtyRectTop;
        clipRect.bottom = adjustedClipRect.bottom = trsCharHeight * (dirtyRectBottom + 1);
    }

    Bitmap takeScreenshot(Hardware hardware) {
        Bitmap screenshot =
                Bitmap.createBitmap(hardware.getScreenWidth(),
                        hardware.getScreenHeight(), Config.RGB_565);
        boolean expandedMode = XTRS.isExpandedMode();
        int d = expandedMode ? 2 : 1;
        dirtyRectLeft = dirtyRectTop = 0;
        dirtyRectRight = trsScreenCols / d - 1;
        dirtyRectBottom = trsScreenRows - 1;
        Canvas c = new Canvas(screenshot);
        renderScreenToCanvas(c, expandedMode);
        return screenshot;
    }

    /**
     * Encapsulated logic to limit the frame rate to the given FPS.
     */
    private static final class FpsLimiter {
        private final long frameTimeMillis;
        private long lastFrameTime;

        /**
         * @param fps the frames-per-second to limit to.
         */
        FpsLimiter(long fps) {
            frameTimeMillis = (long) Math.floor(1000.0 / fps);
        }

        /**
         * Call this once per frame-loop. It will wait if necessary to ensure the set max FPS rate.
         */
        void onFrame() throws InterruptedException {
            long now = System.currentTimeMillis();
            long waitFor = Math.max(0, lastFrameTime + frameTimeMillis - now);
            Thread.sleep(waitFor);
            lastFrameTime = now;
        }
    }
}
