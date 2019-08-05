
#include "trs-io.h"
#include "trsio-wrapper.h"

extern "C" {

bool trsio_z80_out(uint8_t byte) {
  bool needMore = TrsIO::outZ80(byte);
  if (!needMore) {
    TrsIO::processInBackground();
  }
  return needMore;
}

uint8_t trsio_z80_in() {
  return TrsIO::inZ80();
}

}

