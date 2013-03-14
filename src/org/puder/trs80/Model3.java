package org.puder.trs80;

public class Model3 extends Hardware {

    public Model3() {
        setMemorySize(48 * 1024);
        setScreenBuffer(0x3fff - 0x3c00 + 1);
        int sizeROM = memory.loadROM("model3.rom");
        setROMSize(sizeROM);
        int entryAddr = memory.loadCmdFile("defense.cmd");
        setEntryAddress(entryAddr);
    }

}
