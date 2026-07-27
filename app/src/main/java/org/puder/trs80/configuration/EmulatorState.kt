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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.common.base.Optional
import com.google.common.io.Files
import com.google.protobuf.InvalidProtocolBufferException
import org.puder.trs80.Hardware
import org.puder.trs80.XTRS
import org.puder.trs80.io.FileManager
import org.puder.trs80.proto.NativeSystemState
import org.retrostore.client.common.proto.SystemState
import org.retrostore.client.common.proto.Trs80Model
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

private const val TAG = "EmulatorState"
private const val FILE_SCREENSHOT = "screenshot.png"
private const val FILE_STATE = "state"
private const val FILE_XRAY_STATE = "state-xray.pb"
private const val FILE_CASSETTE = "cassette.cas"

/**
 * Persists the state of an emulator session so it can be resumed later.
 */
class EmulatorState private constructor(private val fileManager: FileManager) {

    companion object {
        /**
         * Creates the state storage for the configuration with the given ID.
         *
         * @throws IOException if the storage directory could not be created.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun forConfigId(
            configId: Int,
            fileManagerCreator: FileManager.Creator
        ): EmulatorState {
            val fileManager = fileManagerCreator.createForAppSubDir(configId)
            fileManager.ensureNoMedia()
            return EmulatorState(fileManager)
        }
    }

    private val stateFileName: String
        get() = fileManager.getAbsolutePathForFile(FILE_STATE)

    /** The path of the cassette image that is used when none is configured. */
    val defaultCassettePath: String
        get() = fileManager.getAbsolutePathForFile(FILE_CASSETTE)

    /** The directory in which this state is stored. */
    val basePath: String
        get() = fileManager.getAbsolutePathForFile("")

    fun saveState() = XTRS.saveState(stateFileName)

    fun loadState() = XTRS.loadState(stateFileName)

    /**
     * Converts the state that the emulator dumped for TRS-Xray into the RetroStore format.
     *
     * @param model the emulated model, see [Hardware].
     * @return The converted state, or absent if no valid state was dumped.
     */
    @SuppressLint("CheckResult")
    fun getSystemState(model: Int): Optional<SystemState> {
        if (!fileManager.hasFile(FILE_XRAY_STATE)) {
            return Optional.absent()
        }
        val stateBytes = try {
            Files.toByteArray(File(fileManager.getAbsolutePathForFile(FILE_XRAY_STATE)))
        } catch (e: IOException) {
            Log.e(TAG, "Unable to load xray state file from file.", e)
            return Optional.absent()
        }
        val nativeState = try {
            NativeSystemState.parseFrom(stateBytes)
        } catch (e: InvalidProtocolBufferException) {
            Log.e(TAG, "Unable to parse xray state protocol buffer.", e)
            return Optional.absent()
        }

        val registers = nativeState.registers
        val state = SystemState.newBuilder()
            // Set the model number, which we don't get from the native info.
            .setModel(toRetroStoreModel(model))
            .setRegisters(
                SystemState.Registers.newBuilder()
                    .setIx(registers.ix)
                    .setIy(registers.iy)
                    .setPc(registers.pc)
                    .setSp(registers.sp)
                    .setAf(registers.af)
                    .setBc(registers.bc)
                    .setDe(registers.de)
                    .setHl(registers.hl)
                    .setAfPrime(registers.afPrime)
                    .setBcPrime(registers.bcPrime)
                    .setDePrime(registers.dePrime)
                    .setHlPrime(registers.hlPrime)
                    .setI(registers.i)
                    .setR1(registers.r1)
                    .setR2(registers.r2)
            )

        for (nativeMem in nativeState.memoryRegionsList) {
            state.addMemoryRegions(
                SystemState.MemoryRegion.newBuilder()
                    .setStart(nativeMem.start)
                    .setData(nativeMem.data)
            )
        }

        return Optional.of(state.build())
    }

    /** Whether a saved emulator state exists. */
    fun hasState(): Boolean = fileManager.hasFile(FILE_STATE)

    /** Whether a state dump for TRS-Xray exists. */
    fun hasXrayState(): Boolean = fileManager.hasFile(FILE_XRAY_STATE)

    fun saveScreenshot(screenshot: Bitmap?) {
        if (screenshot == null) {
            // Can happen when NotImplementedException is thrown.
            return
        }
        try {
            ByteArrayOutputStream().use { out ->
                screenshot.compress(Bitmap.CompressFormat.PNG, 90, out)
                fileManager.writeFile(FILE_SCREENSHOT, out.toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to save screenshot.", e)
        }
    }

    /**
     * @return The stored screenshot, or null if there is none.
     */
    fun loadScreenshot(): Bitmap? {
        val screenshot = fileManager.readFile(FILE_SCREENSHOT).orNull() ?: return null
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(screenshot, 0, screenshot.size, options)
    }

    /** Deletes the saved state, but keeps everything else, e.g. the disk images. */
    fun deleteSavedState() {
        fileManager.deleteFile(FILE_STATE)
        fileManager.deleteFile(FILE_XRAY_STATE)
        fileManager.deleteFile(FILE_SCREENSHOT)
    }

    /** Deletes all data of this configuration. */
    fun deleteAll() = fileManager.delete()

    private fun toRetroStoreModel(model: Int): Trs80Model = when (model) {
        Hardware.MODEL1 -> Trs80Model.MODEL_I
        Hardware.MODEL3 -> Trs80Model.MODEL_III
        Hardware.MODEL4 -> Trs80Model.MODEL_4
        Hardware.MODEL4P -> Trs80Model.MODEL_4P
        else -> Trs80Model.UNKNOWN_MODEL
    }
}
