// Stores the emulator's system state in a protocol buffer binary file.

#ifndef TRS80_TRS_XRAY_STATE_SAVE_H
#define TRS80_TRS_XRAY_STATE_SAVE_H


#ifdef __cplusplus
extern "C" {
#endif

// From z80.h, implemented in trs_memory.c
int mem_read(int address);
void mem_write(int address, int value);

// Entry points from native.c
void trs_xray_save_system_state(char *file);
void trs_xray_load_system_state(char *file);

#ifdef __cplusplus
}

class TrsXraySystemStateSaver {
public:
    static bool saveState(char* file);
    static bool loadState(char* file);
private:
    // Reads from the current emulator's state's memory.
    static void readMemory(int start, int length, char* buffer);

    // Writes to the current emulator's state's memory.
    static void writeMemory(int start, int length, const char* buffer);
};

#endif



#endif //TRS80_TRS_XRAY_STATE_SAVE_H
