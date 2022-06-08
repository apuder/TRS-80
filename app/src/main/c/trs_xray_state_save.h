// Stores the emulator's system state in a protocol buffer binary file.

#ifndef TRS80_TRS_XRAY_STATE_SAVE_H
#define TRS80_TRS_XRAY_STATE_SAVE_H


#ifdef __cplusplus
extern "C" {
#endif

void trs_xray_save_system_state(char *file);

#ifdef __cplusplus
}

class TrsXraySystemStateSaver {
public:
    static bool saveState(char* file);
};

#endif



#endif //TRS80_TRS_XRAY_STATE_SAVE_H
