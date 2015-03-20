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

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class SDLRenderThread extends Thread implements RenderThread {

    private int              model;

    private boolean          run         = false;
    private volatile boolean isRendering = true;
    protected SurfaceHolder  surfaceHolder;
    private Bitmap           surfaceBitmap;

    public SDLRenderThread() {
        surfaceHolder = null;
        Hardware h = TRS80Application.getHardware();
        model = h.getModel();
        surfaceBitmap = Bitmap.createBitmap(Hardware.SDL_SURFACE_WIDTH, Hardware.SDL_SURFACE_HEIGHT,
                Bitmap.Config.RGB_565);
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

            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas == null) {
                continue;
            }
            ByteBuffer bb = TRS80Application.getHardware().xtrsSDLSurface;
            bb.rewind();
            surfaceBitmap.copyPixelsFromBuffer(bb);
            canvas.drawBitmap(surfaceBitmap, 0, 0, null);
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    public synchronized void triggerScreenUpdate() {
        this.notify();
    }

    public synchronized void forceScreenUpdate() {
        this.notify();
    }

    public synchronized Bitmap takeScreenshot() {
        Hardware h = TRS80Application.getHardware();
        Bitmap screenshot = Bitmap.createBitmap(h.getScreenWidth(), h.getScreenHeight(),
                Config.RGB_565);
        Canvas c = new Canvas(screenshot);
        surfaceBitmap.copyPixelsFromBuffer(TRS80Application.getHardware().xtrsSDLSurface);
        c.drawBitmap(surfaceBitmap, 0, 0, null);
        return screenshot;
    }
}
