package org.puder.trs80;

public class Z80ExecutionThread extends Thread {

    public native void setRunning(boolean run);

    private native void bootTRS80(int entryAddr, byte[] mem, byte[] screen);

    private RenderThread renderer;

    public Z80ExecutionThread() {
        this.renderer = null;
    }

    public void setRenderer(RenderThread renderer) {
        this.renderer = renderer;
    }

    public boolean isRendering() {
        return (renderer == null) ? true : renderer.isRendering();
    }

    public void updateScreen() {
        if (renderer != null) {
            renderer.triggerScreenUpdate();
        }
    }

    @Override
    public void run() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Hardware hardware = TRS80Application.getHardware();
        byte[] memBuffer = hardware.getMemoryBuffer();
        byte[] screenBuffer = hardware.getScreenBuffer();
        int entryAddr = hardware.getEntryAddress();

        bootTRS80(entryAddr, memBuffer, screenBuffer);
    }
}