package org.puder.trs80;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

public class Screen extends View {

    final static private int   SCREEN_WIDTH  = 64;
    final static private int   SCREEN_HEIGHT = 16;

    final static private int   FONT_ROWS     = 8;
    final static private int   FONT_COLS     = 32;
    private int                pixelByCol;
    private int                pixelByRow;
    private Bitmap[]           font          = new Bitmap[FONT_ROWS * FONT_COLS];

    private Z80ExecutionThread thread;
    private Memory             mem;

    public Screen(Context context, Memory mem, int entryAddr) {
        super(context);
        this.mem = mem;
        generateFontInformation();
        thread = new Z80ExecutionThread(this, mem, entryAddr);
        thread.setRunning(true);
        thread.start();
    }

    public void surfaceDestroyed() {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    private Rect dirtyRect = new Rect();

    @Override
    public void onDraw(Canvas canvas) {
        canvas.getClipBounds(dirtyRect);
        int startCol = dirtyRect.left / pixelByCol;
        int startRow = dirtyRect.top / pixelByRow;
        int numCols = (dirtyRect.right - dirtyRect.left) / pixelByCol;
        int numRows = (dirtyRect.bottom - dirtyRect.top) / pixelByRow;
        for (int row = startRow; row < startRow + numRows; row++) {
            for (int col = startCol; col < startCol + numCols; col++) {
                int startx = pixelByCol * col;
                int starty = pixelByRow * row;
                int addr = 0x3c00 + row * SCREEN_WIDTH + col;
                int ch = mem.peek(addr) & 0xff;
                if (ch < FONT_ROWS * FONT_COLS) {
                    canvas.drawBitmap(font[ch], startx, starty, null);
                }
            }
        }
    }

    private void generateFontInformation() {
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.trs80font);
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

    public Rect computeBounds(int addr) {
        int ofs = addr - 0x3c00;
        int col = ofs % SCREEN_WIDTH;
        int row = ofs / SCREEN_WIDTH;
        int startx = pixelByCol * col;
        int starty = pixelByRow * row;
        return new Rect(startx, starty, startx + pixelByCol, starty + pixelByRow);
    }

}
