package org.puder.trs80;

import android.graphics.Bitmap;
import android.view.SurfaceHolder;

public interface RenderThread {

    public boolean isRendering();

    public void triggerScreenUpdate();

    public void forceScreenUpdate();

    public void setSurfaceHolder(SurfaceHolder holder);

    public Bitmap takeScreenshot();

    public void setRunning(boolean run);

}
