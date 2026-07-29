/*
 * Copyright 2025, Arno Puder
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

package org.puder.trs80.shared

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.get
import org.puder.trs80.core.TRS80_KEY_DOWN
import org.puder.trs80.core.TRS80_KEY_UP
import org.puder.trs80.core.TRS80_OK
import org.puder.trs80.core.trs80_add_key_event
import org.puder.trs80.core.trs80_is_expanded_mode
import org.puder.trs80.core.trs80_reset
import org.puder.trs80.core.trs80_run
import org.puder.trs80.core.trs80_screen_buffer
import org.puder.trs80.core.trs80_set_running
import org.puder.trs80.core.trs80_set_sound_muted

/**
 * The iOS side of the emulator core: the counterpart of `XTRS` on Android.
 *
 * This is deliberately thin. Everything interesting about the emulator lives in
 * C and is reached through cinterop over `trs80_core.h`; the only job here is to
 * present it as Kotlin that the shared code can drive.
 */
@OptIn(ExperimentalForeignApi::class)
object EmulatorCore {

    /** Whether the machine is drawing wide characters. */
    val isExpandedMode: Boolean
        get() = trs80_is_expanded_mode() != 0

    /**
     * The emulator's screen memory. The pointer is valid for the lifetime of
     * the process, so this can be held indefinitely.
     */
    val screenBuffer: ScreenBuffer
        get() = NativeScreenBuffer(
            requireNotNull(trs80_screen_buffer()) { "The core has no screen buffer." }
        )

    /**
     * Runs the CPU. Blocks until [stop] is called from another thread, so this
     * must not be called on the main thread.
     */
    fun run() = trs80_run()

    fun stop() = trs80_set_running(0)

    fun reset() = trs80_reset()

    fun setSoundMuted(muted: Boolean) = trs80_set_sound_muted(if (muted) 1 else 0)

    /** Queues a key press. [sym] and [key] are the SDL codes the core expects. */
    fun keyDown(sym: Int, key: Int) =
        trs80_add_key_event(TRS80_KEY_DOWN, sym, key)

    /** Queues a key release. */
    fun keyUp(sym: Int, key: Int) =
        trs80_add_key_event(TRS80_KEY_UP, sym, key)

    /** Whether [status] is the core's success code. */
    fun isOk(status: Int): Boolean = status == TRS80_OK
}

/**
 * Reads the emulator's screen memory straight out of the buffer the core owns,
 * so nothing is copied per frame — the same contract as the Android side, where
 * the bytes come from a JNI direct buffer.
 */
@OptIn(ExperimentalForeignApi::class)
private class NativeScreenBuffer(private val buffer: CPointer<UByteVar>) : ScreenBuffer {
    override fun get(index: Int): Byte = buffer[index].toByte()
}
