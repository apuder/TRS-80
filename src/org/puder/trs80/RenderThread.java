package org.puder.trs80;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

public class RenderThread extends Thread {

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
            Canvas c = surfaceHolder.lockCanvas(null);
            if (c == null) {
                Log.d("Z80", "Canvas is null");
                continue;
            }
            int i = 0;
            for (int row = 0; row < trsScreenRows; row++) {
                for (int col = 0; col < trsScreenCols; col++) {
                    int ch = screenBuffer[i] & 0xff;
                    int startx = trsCharWidth * col;
                    int starty = trsCharHeight * row;
                    c.drawBitmap(font[ch], startx, starty, null);
                    i++;
                }
            }
            surfaceHolder.unlockCanvasAndPost(c);
        }
    }

    public synchronized void triggerScreenUpdate() {
        this.notify();
    }

}
