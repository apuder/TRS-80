#include "trs_xray_state_save.h"

#include "proto/system_state.pb.h"

#include "z80.h"

#include <android/log.h>
#include <fstream>
#include <string>
using namespace std;

extern "C" {

void trs_xray_save_system_state(char* file) {
    TrsXraySystemStateSaver::saveState(file);
}

}

#define  LOG_TAG    "StateSaver_Native"
#define  ALOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define  ALOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

bool TrsXraySystemStateSaver::saveState(char* filename) {
    GOOGLE_PROTOBUF_VERIFY_VERSION;

    trs_protos::NativeSystemState state;

    // Add registers.
    auto registers = state.mutable_registers();
    registers->set_ix(REG_IX);
    registers->set_iy(REG_IY);
    registers->set_pc(REG_PC);
    registers->set_sp(REG_SP);
    registers->set_af(REG_AF);
    registers->set_bc(REG_BC);
    registers->set_de(REG_DE);
    registers->set_hl(REG_HL);
    registers->set_af_prime(REG_AF_PRIME);
    registers->set_bc_prime(REG_BC_PRIME);
    registers->set_de_prime(REG_DE_PRIME);
    registers->set_hl_prime(REG_HL_PRIME);
    registers->set_i(REG_I);

    // TODO: Add memory regions.

    // Write the system state to the requested file.
    fstream output(filename, ios::out | ios::trunc | ios::binary);
    if (!state.SerializeToOstream(&output)) {
        ALOGE("Failed to write system state to file.");
        return false;
    }
    ALOGI("System state file written to %s", filename);
    return true;
}
