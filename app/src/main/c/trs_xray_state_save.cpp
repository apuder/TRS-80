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

#define VIDEO_START	(0x3c00)

// static
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

    // Add video RAM.
    char video_memory[1024];
    readMemory(VIDEO_START, 1024, video_memory);
    auto video_memory_region = state.add_memoryregions();
    video_memory_region->set_start(VIDEO_START);
    video_memory_region->set_data(video_memory, 1024);

    // Add the upper 32k.
    char upper_32k_memory[0x8000];
    readMemory(0x8000, 0x8000, upper_32k_memory);
    auto upper_32k_memory_region = state.add_memoryregions();
    upper_32k_memory_region->set_start(0x8000);
    upper_32k_memory_region->set_data(upper_32k_memory, 0x8000);

    // Add the lower 32k.
    char lower_32k_memory[0x8000];
    readMemory(0, 0x8000, lower_32k_memory);
    auto lower_32k_memory_region = state.add_memoryregions();
    lower_32k_memory_region->set_start(0x0);
    lower_32k_memory_region->set_data(lower_32k_memory, 0x8000);

    // Write the system state to the requested file.
    fstream output(filename, ios::out | ios::trunc | ios::binary);
    if (!state.SerializeToOstream(&output)) {
        ALOGE("Failed to write system state to file.");
        return false;
    }
    ALOGI("System state file written to %s", filename);
    return true;
}

// static
void TrsXraySystemStateSaver::readMemory(int start, int length, char* buffer) {
    for (int addr = start; addr < start + length; ++addr) {
        buffer[addr - start] = static_cast<char>(mem_read(addr));
    }
}
