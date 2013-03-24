package org.puder.trs80;

public class Z80ExecutionThread extends Thread {

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

        XTRS.bootTRS80(entryAddr, memBuffer, screenBuffer);
    }
}