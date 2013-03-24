package org.puder.trs80;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.view.Window;

public class Model3 extends Hardware {

    private int      trsScreenCols = 64;
    private int      trsScreenRows = 16;
    private float    aspectRatio   = 1.5f;

    private int      trsScreenWidth;
    private int      trsScreenHeight;
    private int      trsCharWidth;
    private int      trsCharHeight;

    private int      keyWidth;
    private int      keyMargin;

    private Bitmap[] font;

    public Model3(Activity mainActivity) {
        trsScreenCols = 64;
        trsScreenRows = 16;
        aspectRatio = 1.5f;
        font = new Bitmap[256];
        setMemorySize(48 * 1024);
        setScreenBuffer(0x3fff - 0x3c00 + 1);
        int sizeROM = memory.loadROM("model3.rom");
        setROMSize(sizeROM);
        int entryAddr = 0;// memory.loadCmdFile("defense.cmd");
        setEntryAddress(entryAddr);
//        computeFontDimensions(mainActivity.getWindow());
    }

    public Bitmap[] getFont() {
        return font;
    }

    @Override
    public void computeFontDimensions(Window window) {
        Rect rect = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        int StatusBarHeight = rect.top;
        int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int TitleBarHeight = contentViewTop - StatusBarHeight;
        int contentHeight = rect.bottom - contentViewTop;
        int contentWidth = rect.right;
        if ((contentWidth / trsScreenCols) * aspectRatio > (contentHeight / trsScreenRows)) {
            // Screen height is not sufficient to let the TRS80 screen span the
            // whole width
            trsCharHeight = contentHeight / trsScreenRows;
            // Make sure trsCharHeight is divisible by 3
            while (trsCharHeight % 3 != 0) {
                trsCharHeight--;
            }
            trsScreenHeight = trsCharHeight * trsScreenRows;
            trsCharWidth = (int) (trsCharHeight / aspectRatio);
            trsScreenWidth = trsCharWidth * trsScreenCols;
        } else {
            // Screen width is not sufficient to let the TRS80 screen span the
            // whole height
            trsCharWidth = contentWidth / trsScreenCols;
            while (trsCharWidth % 2 != 0) {
                trsCharWidth--;
            }
            trsScreenWidth = trsCharWidth * trsScreenCols;
            trsCharHeight = (int) (trsCharWidth * aspectRatio);
            trsScreenHeight = trsCharHeight * trsScreenRows;
        }

        // Compute size of keyboard keys
        final int MAX_BOXES = 15;
        int boxWidth = rect.right / MAX_BOXES;
        keyWidth = (int) (boxWidth * 0.9f);
        keyMargin = (boxWidth - keyWidth) / 2;
        
        generateGraphicsFont();
        generateASCIIFont();
    }

    private void generateASCIIFont() {
        String ascii = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        Paint p = new Paint();
        p.setTextAlign(Align.CENTER);
        Typeface tf = TRS80Application.getTypeface();
        p.setTypeface(tf);
        p.setTextScaleX(1.0f);
        p.setTextSize(trsCharHeight);
        p.setColor(Color.GREEN);
        int xPos = trsCharWidth / 2;
        int yPos = (int) ((trsCharHeight / 2) - ((p.descent() + p.ascent()) / 2));
        for (int i = 0; i < ascii.length(); i++) {
            Bitmap b = Bitmap.createBitmap(trsCharWidth, trsCharHeight, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(b);
            c.drawColor(Color.BLACK);
            c.drawText(ascii.substring(i, i + 1), xPos, yPos, p);
            font[i + 32] = b;
        }
    }

    private void generateGraphicsFont() {
        Paint p = new Paint();
        for (int i = 128; i <= 191; i++) {
            Bitmap b = Bitmap.createBitmap(trsCharWidth, trsCharHeight, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(b);
            c.drawColor(Color.BLACK);
            p.setColor(Color.GREEN);
            Rect r = new Rect();
            // Top-left
            if ((i & 1) != 0) {
                r.left = r.top = 0;
                r.right = trsCharWidth / 2;
                r.bottom = trsCharHeight / 3;
                c.drawRect(r, p);
            }

            // Top-right
            if ((i & 2) != 0) {
                r.left = trsCharWidth / 2;
                r.right = trsCharWidth;
                r.top = 0;
                r.bottom = trsCharHeight / 3;
                c.drawRect(r, p);
            }

            // Middle-left
            if ((i & 4) != 0) {
                r.left = 0;
                r.right = trsCharWidth / 2;
                r.top = trsCharHeight / 3;
                r.bottom = trsCharHeight / 3 * 2;
                c.drawRect(r, p);
            }

            // Middle-right
            if ((i & 8) != 0) {
                r.left = trsCharWidth / 2;
                r.right = trsCharWidth;
                r.top = trsCharHeight / 3;
                r.bottom = trsCharHeight / 3 * 2;
                c.drawRect(r, p);
            }

            // Bottom-left
            if ((i & 16) != 0) {
                r.left = 0;
                r.right = trsCharWidth / 2;
                r.top = trsCharHeight / 3 * 2;
                r.bottom = trsCharHeight;
                c.drawRect(r, p);
            }

            // Bottom-right
            if ((i & 32) != 0) {
                r.left = trsCharWidth / 2;
                r.right = trsCharWidth;
                r.top = trsCharHeight / 3 * 2;
                r.bottom = trsCharHeight;
                c.drawRect(r, p);
            }

            font[i] = b;
        }
    }

    @Override
    public int getScreenCols() {
        return trsScreenCols;
    }

    @Override
    public int getScreenRows() {
        return trsScreenRows;
    }

    @Override
    public int getScreenWidth() {
        return trsScreenWidth;
    }

    @Override
    public int getScreenHeight() {
        return trsScreenHeight;
    }

    @Override
    public int getCharWidth() {
        return trsCharWidth;
    }

    @Override
    public int getCharHeight() {
        return trsCharHeight;
    }

    @Override
    public int getKeyWidth() {
        return keyWidth;
    }

    @Override
    public int getKeyMargin() {
        return keyMargin;
    }

}
