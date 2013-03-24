package org.puder.trs80;

import android.content.Context;
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
        TRS80Application.getZ80Thread().setRenderer(threadRender);
//        threadRender.triggerScreenUpdate();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        TRS80Application.getZ80Thread().setRenderer(null);
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
}
