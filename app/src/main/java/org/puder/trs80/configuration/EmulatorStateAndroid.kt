/*
 * Copyright 2012-2013, Arno Puder
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

package org.puder.trs80.configuration

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.protobuf.InvalidProtocolBufferException
import okio.ByteString.Companion.toByteString
import org.puder.trs80.Hardware
import org.puder.trs80.XTRS
import org.puder.trs80.proto.NativeSystemState
import org.puder.trs80.shared.configuration.EmulatorState
import org.retrostore.client.common.proto.SystemState
import org.retrostore.client.common.proto.Trs80Model

private const val TAG = "EmulatorState"

/*
 * The platform half of EmulatorState.
 *
 * The storage half moved to `commonMain`; these three pieces could not follow
 * it. Saving and loading go through the JNI bridge, which iOS reaches by a
 * different route; the screenshot is an `android.graphics.Bitmap`; and the
 * TRS-Xray dump is parsed with the Java protobuf runtime, which has no
 * multiplatform build. The first two dissolve once the UI is Compose and the
 * core is behind a common seam. The third needs the native message moved from
 * protobuf-lite to Wire, which the RetroStore messages already use.
 */

/** Tells the core to write its state out. */
fun EmulatorState.saveState() = XTRS.saveState(stateFilePath)

/** Tells the core to read a previously saved state back in. */
fun EmulatorState.loadState() = XTRS.loadState(stateFilePath)

/** Encodes [screenshot] as PNG and stores it. */
fun EmulatorState.saveScreenshot(screenshot: Bitmap?) {
    if (screenshot == null) {
        // Can happen when NotImplementedException is thrown.
        return
    }
    try {
        java.io.ByteArrayOutputStream().use { out ->
            screenshot.compress(Bitmap.CompressFormat.PNG, 90, out)
            writeScreenshot(out.toByteArray())
        }
    } catch (e: Exception) {
        Log.e(TAG, "Unable to save screenshot.", e)
    }
}

/** @return The stored screenshot, or null if there is none. */
fun EmulatorState.loadScreenshot(): Bitmap? {
    val screenshot = readScreenshot() ?: return null
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(screenshot, 0, screenshot.size, options)
}

/**
 * Converts the state that the emulator dumped for TRS-Xray into the RetroStore format.
 *
 * @param model the emulated model, see [Hardware].
 * @return The converted state, or null if no valid state was dumped.
 */
fun EmulatorState.getSystemState(model: Int): SystemState? {
    val stateBytes = readXrayState() ?: return null
    val nativeState = try {
        NativeSystemState.parseFrom(stateBytes)
    } catch (e: InvalidProtocolBufferException) {
        Log.e(TAG, "Unable to parse xray state protocol buffer.", e)
        return null
    }

    val registers = nativeState.registers
    return SystemState(
        // Set the model number, which we don't get from the native info.
        model = toRetroStoreModel(model),
        registers = SystemState.Registers(
            ix = registers.ix,
            iy = registers.iy,
            pc = registers.pc,
            sp = registers.sp,
            af = registers.af,
            bc = registers.bc,
            de = registers.de,
            hl = registers.hl,
            af_prime = registers.afPrime,
            bc_prime = registers.bcPrime,
            de_prime = registers.dePrime,
            hl_prime = registers.hlPrime,
            i = registers.i,
            r_1 = registers.r1,
            r_2 = registers.r2
        ),
        memoryRegions = nativeState.memoryRegionsList.map { nativeMem ->
            SystemState.MemoryRegion(
                start = nativeMem.start,
                data_ = nativeMem.data.toByteArray().toByteString()
            )
        }
    )
}

private fun toRetroStoreModel(model: Int): Trs80Model = when (model) {
    Hardware.MODEL1 -> Trs80Model.MODEL_I
    Hardware.MODEL3 -> Trs80Model.MODEL_III
    Hardware.MODEL4 -> Trs80Model.MODEL_4
    Hardware.MODEL4P -> Trs80Model.MODEL_4P
    else -> Trs80Model.UNKNOWN_MODEL
}
