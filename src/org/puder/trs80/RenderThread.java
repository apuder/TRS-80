package org.puder.trs80;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class RenderThread extends Thread {

    final static private int SCREEN_WIDTH  = 64;
    final static private int SCREEN_HEIGHT = 16;

    final static private int FONT_ROWS     = 8;
    final static private int FONT_COLS     = 32;
    private int              pixelByCol;
    private int              pixelByRow;
    private Bitmap[]         font          = new Bitmap[FONT_ROWS * FONT_COLS];

    private boolean          run           = false;
    private boolean          isRendering   = false;
    private SurfaceHolder    surfaceHolder;
    private Context          context;
    private byte[]           screenBuffer;
    private Object           lock          = new Object();

    public RenderThread(Context context, SurfaceHolder holder, Memory mem) {
        this.context = context;
        this.surfaceHolder = holder;
        this.screenBuffer = mem.getScreenBuffer();
        generateFontInformation();
    }

    public void setRunning(boolean run) {
        this.run = run;
    }

    public boolean isRendering() {
        return this.isRendering;
    }

    @Override
    public void run() {
        while (run) {
            try {
                synchronized (lock) {
                    isRendering = false;
                    lock.wait();
                    isRendering = true;
                }
            } catch (InterruptedException e) {
                return;
            }
            Canvas c = surfaceHolder.lockCanvas(null);
            if (c == null) {
                return;
            }
            int i = 0;
            for (int row = 0; row < SCREEN_HEIGHT; row++) {
                for (int col = 0; col < SCREEN_WIDTH; col++) {
                    int ch = screenBuffer[i] & 0xff;
                    int startx = pixelByCol * col;
                    int starty = pixelByRow * row;
                    c.drawBitmap(font[ch], startx, starty, null);
                    i++;
                }
            }
            surfaceHolder.unlockCanvasAndPost(c);
        }
    }

    public void triggerScreenUpdate() {
        synchronized (lock) {
            lock.notify();
        }
    }

    private void generateFontInformation() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.trs80font, opts);
        final int width = b.getWidth();
        final int height = b.getHeight();

        pixelByCol = width / FONT_COLS;
        pixelByRow = height / FONT_ROWS;
        int i = 0;
        for (int row = 0; row < FONT_ROWS; row++) {
            for (int col = 0; col < FONT_COLS; col++) {
                int startx = pixelByCol * col;
                int starty = pixelByRow * row;
                font[i++] = Bitmap.createBitmap(b, startx, starty, pixelByCol, pixelByRow);
            }
        }
    }

}
