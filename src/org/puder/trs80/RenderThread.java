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

import org.puder.trs80.cast.RemoteCastScreen;
import org.puder.trs80.cast.RemoteDisplayChannel;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

public class RenderThread extends Thread {

    private int           model;

    private int           trsScreenCols;
    private int           trsScreenRows;
    private int           trsCharWidth;
    private int           trsCharHeight;

    private Bitmap        font[];

    private boolean       run         = false;
    private boolean       isRendering = false;
    private SurfaceHolder surfaceHolder;
    private byte[]        screenBuffer;

    private char[]        screenCharBuffer;

    public RenderThread() {
        surfaceHolder = null;
        Hardware h = TRS80Application.getHardware();
        model = h.getModel();
        screenBuffer = h.getScreenBuffer();
        trsScreenCols = h.getScreenConfiguration().trsScreenCols;
        trsScreenRows = h.getScreenConfiguration().trsScreenRows;
        trsCharWidth = h.getCharWidth();
        trsCharHeight = h.getCharHeight();
        font = h.getFont();
        screenCharBuffer = new char[trsScreenCols * trsScreenRows];
    }

    public void setSurfaceHolder(SurfaceHolder holder) {
        surfaceHolder = holder;
    }

    public void setRunning(boolean run) {
        this.run = run;
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

            if (surfaceHolder != null) {
                Canvas c = surfaceHolder.lockCanvas();
                if (c == null) {
                    Log.d("Z80", "Canvas is null");
                    continue;
                }
                renderScreen(c, null);
                surfaceHolder.unlockCanvasAndPost(c);
            } else {
                renderScreen(null, RemoteCastScreen.get());
            }
        }
    }

    private void renderScreen(Canvas canvas, RemoteDisplayChannel remoteDisplay) {
        boolean expandedMode = TRS80Application.getHardware().getExpandedScreenMode();
        int d = expandedMode ? 2 : 1;
        if (canvas != null && expandedMode) {
            canvas.scale(2, 1);
        }

        int i = 0;
        for (int row = 0; row < trsScreenRows; row++) {
            for (int col = 0; col < trsScreenCols / d; col++) {
                int ch = screenBuffer[i] & 0xff;
                // Emulate Radio Shack lowercase mod (for Model 1)
                if (this.model == Hardware.MODEL1 && ch < 0x20) {
                    ch += 0x40;
                }

                if (canvas != null) {
                    int startx = trsCharWidth * col;
                    int starty = trsCharHeight * row;
                    canvas.drawBitmap(font[ch], startx, starty, null);
                }
                // TODO: Choose encoding based on current model.
                screenCharBuffer[i] = CharMapping.m3toUnicode[ch];
                i += d;
            }
        }
        if (remoteDisplay != null) {
            remoteDisplay.sendScreenBuffer(expandedMode, String.valueOf(screenCharBuffer));
        }
    }

    public synchronized void triggerScreenUpdate() {
        this.notify();
    }

    public synchronized Bitmap takeScreenshot() {
        Hardware h = TRS80Application.getHardware();
        Bitmap screenshot = Bitmap.createBitmap(h.getScreenWidth(), h.getScreenHeight(),
                Config.RGB_565);
        Canvas c = new Canvas(screenshot);
        c.drawColor(TRS80Application.getCurrentConfiguration().getScreenColorAsRGB());
        renderScreen(c, null);
        return screenshot;
    }
}
