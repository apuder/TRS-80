package org.puder.trs80;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Screen extends SurfaceView implements SurfaceHolder.Callback {

    private Z80ExecutionThread threadZ80;
    private RenderThread       threadRender;

    public Screen(Context context, AttributeSet attr) {
        super(context, attr);
        getHolder().addCallback(this);
    }

    public void createThreads() {
        threadRender = new RenderThread(getHolder());
        threadZ80 = new Z80ExecutionThread(threadRender);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        threadRender.setRunning(true);
        threadRender.start();
        threadZ80.setRunning(true);
        threadZ80.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        threadZ80.setRunning(false);
        while (retry) {
            try {
                threadZ80.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }

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
