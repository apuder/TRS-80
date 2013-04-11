/*
 * Copyright 2012-2013, webappbooster.org
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
        final int screenshotWidth = 300;
        int screenshotHeight = (int) (300 * ((float) h.getScreenHeight() / (float) h
                .getScreenWidth()));
        float sx = (float) screenshotWidth / h.getScreenWidth();
        float sy = (float) screenshotHeight / h.getScreenHeight();
        Bitmap screenshot = Bitmap.createBitmap(screenshotWidth, screenshotHeight, Config.RGB_565);
        Canvas c = new Canvas(screenshot);
        c.scale(sx, sy);
        threadRender.renderScreen(c);
        return screenshot;
    }
}
