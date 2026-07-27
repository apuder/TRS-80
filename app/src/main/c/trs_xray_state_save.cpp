/*
 * Writes the emulator's system state as a trs_protos.NativeSystemState
 * protocol buffer, for TRS-Xray / RetroStore.
 *
 * This used to pull in the full protobuf runtime - ~271k lines of vendored
 * C++ - to serialize one message with three field types. It now encodes the
 * wire format directly. The output is byte-for-byte what the generated
 * serializer produced, and is still parsed on the Java side by the javalite
 * bindings generated from system_state.proto.
 *
 * Schema (see app/src/main/proto/system_state.proto):
 *
 *   NativeSystemState { Registers registers = 2;
 *                       repeated MemoryRegion memoryRegions = 3; }
 *   Registers         { int32 ix = 1 ... int32 r_2 = 15; }
 *   MemoryRegion      { int32 start = 1; bytes data = 2; }
 *
 * Note the proto3 rule that a field holding its default value is omitted
 * entirely: a register that happens to be zero, and the memory region based
 * at address zero, do not appear on the wire at all.
 */

#include "trs_xray_state_save.h"

#include "z80.h"

#include <stdint.h>
#include <stdio.h>

extern "C" {
void trs_xray_save_system_state(char* file) {
    TrsXraySystemStateSaver::saveState(file);
}
}

#define VIDEO_START (0x3c00)

namespace {

const int WIRE_VARINT = 0;
const int WIRE_LENGTH_DELIMITED = 2;

void writeVarint(FILE* out, uint64_t value) {
    do {
        uint8_t byte = value & 0x7f;
        value >>= 7;
        if (value != 0) {
            byte |= 0x80;
        }
        fputc(byte, out);
    } while (value != 0);
}

size_t varintSize(uint64_t value) {
    size_t size = 1;
    while ((value >>= 7) != 0) {
        size++;
    }
    return size;
}

uint64_t tagOf(int fieldNumber, int wireType) {
    return ((uint64_t) fieldNumber << 3) | (uint64_t) wireType;
}

void writeTag(FILE* out, int fieldNumber, int wireType) {
    writeVarint(out, tagOf(fieldNumber, wireType));
}

/* int32 sign-extends to 64 bits on the wire, so negatives take ten bytes. */
uint64_t zigzagFreeInt32(int32_t value) {
    return (uint64_t) (int64_t) value;
}

void writeInt32(FILE* out, int fieldNumber, int32_t value) {
    if (value == 0) {
        return;  /* proto3: default-valued fields are not serialized. */
    }
    writeTag(out, fieldNumber, WIRE_VARINT);
    writeVarint(out, zigzagFreeInt32(value));
}

size_t int32Size(int fieldNumber, int32_t value) {
    if (value == 0) {
        return 0;
    }
    return varintSize(tagOf(fieldNumber, WIRE_VARINT))
           + varintSize(zigzagFreeInt32(value));
}

/*
 * The Registers submessage. Field numbers follow system_state.proto; r_1 (14)
 * and r_2 (15) have never been populated.
 */
struct RegisterField {
    int fieldNumber;
    int32_t value;
};

int collectRegisters(RegisterField* fields) {
    int n = 0;
    fields[n++] = (RegisterField) {1, (int32_t) REG_IX};
    fields[n++] = (RegisterField) {2, (int32_t) REG_IY};
    fields[n++] = (RegisterField) {3, (int32_t) REG_PC};
    fields[n++] = (RegisterField) {4, (int32_t) REG_SP};
    fields[n++] = (RegisterField) {5, (int32_t) REG_AF};
    fields[n++] = (RegisterField) {6, (int32_t) REG_BC};
    fields[n++] = (RegisterField) {7, (int32_t) REG_DE};
    fields[n++] = (RegisterField) {8, (int32_t) REG_HL};
    fields[n++] = (RegisterField) {9, (int32_t) REG_AF_PRIME};
    fields[n++] = (RegisterField) {10, (int32_t) REG_BC_PRIME};
    fields[n++] = (RegisterField) {11, (int32_t) REG_DE_PRIME};
    fields[n++] = (RegisterField) {12, (int32_t) REG_HL_PRIME};
    fields[n++] = (RegisterField) {13, (int32_t) REG_I};
    return n;
}

void writeRegisters(FILE* out) {
    RegisterField fields[15];
    int count = collectRegisters(fields);

    size_t payloadSize = 0;
    for (int i = 0; i < count; i++) {
        payloadSize += int32Size(fields[i].fieldNumber, fields[i].value);
    }

    writeTag(out, 2, WIRE_LENGTH_DELIMITED);
    writeVarint(out, payloadSize);
    for (int i = 0; i < count; i++) {
        writeInt32(out, fields[i].fieldNumber, fields[i].value);
    }
}

/*
 * One MemoryRegion. The contents are streamed straight out of emulated memory
 * rather than staged in a buffer, which is what let the old implementation put
 * 64KB of memory images on the stack.
 */
void writeMemoryRegion(FILE* out, int start, int length) {
    size_t dataSize = varintSize(tagOf(2, WIRE_LENGTH_DELIMITED))
                      + varintSize((uint64_t) length)
                      + (size_t) length;
    size_t payloadSize = int32Size(1, start) + dataSize;

    writeTag(out, 3, WIRE_LENGTH_DELIMITED);
    writeVarint(out, payloadSize);

    writeInt32(out, 1, start);

    writeTag(out, 2, WIRE_LENGTH_DELIMITED);
    writeVarint(out, (uint64_t) length);
    for (int addr = start; addr < start + length; addr++) {
        fputc((unsigned char) mem_read(addr), out);
    }
}

}  // namespace

// static
bool TrsXraySystemStateSaver::saveState(char* filename) {
    FILE* out = fopen(filename, "wb");
    if (out == NULL) {
        return false;
    }

    writeRegisters(out);
    writeMemoryRegion(out, VIDEO_START, 1024);
    writeMemoryRegion(out, 0x8000, 0x8000);
    writeMemoryRegion(out, 0x0000, 0x8000);

    bool ok = (ferror(out) == 0);
    ok = (fclose(out) == 0) && ok;
    return ok;
}
