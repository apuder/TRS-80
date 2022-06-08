#include "trs_xray_state_save.h"

#include "proto/system_state.pb.h"

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

    // FIXME: Add the fields we need.


    // Write the system state to the requested file.
    fstream output(filename, ios::out | ios::trunc | ios::binary);
    if (!state.SerializeToOstream(&output)) {
        ALOGE("Failed to write system state to file.");
        return false;
    }
    ALOGI("System state file written to %s", filename);
    return true;
}
