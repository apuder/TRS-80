package org.puder.trs80;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Screen extends SurfaceView implements SurfaceHolder.Callback {

    private RenderThread threadRender;

    public Screen(Context context, AttributeSet attr) {
        super(context, attr);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        threadRender = new RenderThread(getHolder());
        threadRender.setRunning(true);
        threadRender.start();
        XTRS.setRenderer(threadRender);
        // threadRender.triggerScreenUpdate();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        XTRS.setRenderer(null);
        boolean retry = true;
        threadRender.setRunning(false);
        threadRender.interrupt();
        while (retry) {
            try {
                threadRender.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Hardware h = TRS80Application.getHardware();
        setMeasuredDimension(h.getScreenWidth() | MeasureSpec.EXACTLY, h.getScreenHeight()
                | MeasureSpec.EXACTLY);
    }

    public Bitmap takeScreenshot() {
        Hardware h = TRS80Application.getHardware();
        Bitmap screenshot = Bitmap.createBitmap(h.getScreenWidth(), h.getScreenHeight(), Config.RGB_565);
        Canvas c = new Canvas(screenshot);
        threadRender.renderScreen(c);
        return screenshot;
    }
}
