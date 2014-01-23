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

    public RenderThread(SurfaceHolder holder) {
        this.surfaceHolder = holder;
        Hardware h = TRS80Application.getHardware();
        model = h.getModel();
        screenBuffer = h.getScreenBuffer();
        trsScreenCols = h.getScreenCols();
        trsScreenRows = h.getScreenRows();
        trsCharWidth = h.getCharWidth();
        trsCharHeight = h.getCharHeight();
        font = h.getFont();
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
            Canvas c = surfaceHolder.lockCanvas();
            if (c == null) {
                Log.d("Z80", "Canvas is null");
                continue;
            }
            renderScreen(c);
            surfaceHolder.unlockCanvasAndPost(c);
        }
    }

    public synchronized void renderScreen(Canvas canvas) {
        int i = 0;
        for (int row = 0; row < trsScreenRows; row++) {
            for (int col = 0; col < trsScreenCols; col++) {
                int ch = screenBuffer[i] & 0xff;
                // Emulate Radio Shack lowercase mod (for Model 1)
                if (this.model == Hardware.MODEL1 && ch < 0x20) {
                    ch += 0x40;
                }
                int startx = trsCharWidth * col;
                int starty = trsCharHeight * row;
                if (font[ch] == null) {
                    Log.d("Z80", "font[" + ch + "] == null");
                    continue;
                }
                canvas.drawBitmap(font[ch], startx, starty, null);
                i++;
            }
        }
    }

    public synchronized void triggerScreenUpdate() {
        this.notify();
    }

}
