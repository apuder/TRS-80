/*
 * Copyright The TRS-80 App Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.puder.trs80.shared.configuration

import okio.ByteString.Companion.toByteString
import org.puder.trs80.shared.MODEL1
import org.puder.trs80.shared.MODEL3
import org.puder.trs80.shared.MODEL4
import org.puder.trs80.shared.MODEL4P
import org.puder.trs80.shared.Log
import org.puder.trs80.shared.xray.NativeSystemState
import org.retrostore.client.common.proto.SystemState
import org.retrostore.client.common.proto.Trs80Model

private const val TAG = "SystemState"

/**
 * Converts the dump the emulator writes for TRS-Xray into the store's format.
 *
 * A port of the Android extension of the same name. The two differ only in
 * which generated protobuf type they read: Android's comes from protobuf-lite
 * and this one from Wire, over the same schema.
 *
 * @param model the emulated model, as this app numbers them.
 * @return the converted state, or null if nothing valid was dumped.
 */
fun EmulatorState.systemState(model: Int): SystemState? {
    val bytes = readXrayState() ?: return null
    val native = try {
        NativeSystemState.ADAPTER.decode(bytes)
    } catch (e: Exception) {
        Log.e(TAG, "Cannot read the TRS-Xray state.", e)
        return null
    }
    val registers = native.registers ?: return null
    return SystemState(
        // The dump does not carry it; the configuration knows it.
        model = model.asStoreModel(),
        registers = SystemState.Registers(
            ix = registers.ix,
            iy = registers.iy,
            pc = registers.pc,
            sp = registers.sp,
            af = registers.af,
            bc = registers.bc,
            de = registers.de,
            hl = registers.hl,
            af_prime = registers.af_prime,
            bc_prime = registers.bc_prime,
            de_prime = registers.de_prime,
            hl_prime = registers.hl_prime,
            i = registers.i,
            r_1 = registers.r_1,
            r_2 = registers.r_2,
        ),
        memoryRegions = native.memoryRegions.map {
            SystemState.MemoryRegion(start = it.start, data_ = it.data_.toByteArray().toByteString())
        },
    )
}

/** @return this app's model constant as the store names it. */
private fun Int.asStoreModel(): Trs80Model = when (this) {
    MODEL1 -> Trs80Model.MODEL_I
    MODEL3 -> Trs80Model.MODEL_III
    MODEL4 -> Trs80Model.MODEL_4
    MODEL4P -> Trs80Model.MODEL_4P
    else -> Trs80Model.UNKNOWN_MODEL
}
