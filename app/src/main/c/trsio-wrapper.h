
#pragma once

#include <inttypes.h>
#include <stdbool.h>

#define TRSIO_PORT 31

#ifdef __cplusplus
extern "C" {
#endif

bool trsio_z80_out(uint8_t byte);
uint8_t trsio_z80_in();

#ifdef __cplusplus
}
#endif

