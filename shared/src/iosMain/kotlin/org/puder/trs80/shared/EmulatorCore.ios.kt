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

package org.puder.trs80.shared

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import org.puder.trs80.core.TRS80_KEY_DOWN
import org.puder.trs80.core.TRS80_KEY_UP
import org.puder.trs80.core.TRS80_OK
import org.puder.trs80.core.trs80_add_key_event
import org.puder.trs80.core.trs80_config
import org.puder.trs80.core.trs80_init
import org.puder.trs80.core.trs80_is_expanded_mode
import org.puder.trs80.core.trs80_reset
import org.puder.trs80.core.TRS80_CELL_HEIGHT
import org.puder.trs80.core.TRS80_CELL_WIDTH
import kotlinx.cinterop.readBytes
import org.puder.trs80.core.trs80_invalidate_render
import org.puder.trs80.core.trs80_pixel_buffer
import org.puder.trs80.core.trs80_pixel_height
import org.puder.trs80.core.trs80_pixel_width
import org.puder.trs80.core.trs80_set_cell_size
import org.puder.trs80.core.trs80_render
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

    /** The rasterized screen's dimensions, in pixels; see [setCellSize]. */
    val pixelWidth: Int get() = trs80_pixel_width()
    val pixelHeight: Int get() = trs80_pixel_height()

    /**
     * The character ROM's own cell size, which is what the core rasterizes at
     * until it is told otherwise. Not the current cell size — that is whatever
     * was last passed to [setCellSize], and a caller that needs it should keep it.
     */
    val romCellWidth: Int get() = TRS80_CELL_WIDTH
    val romCellHeight: Int get() = TRS80_CELL_HEIGHT

    /**
     * Sets the size one character cell is drawn at, so the core rasterizes
     * straight to it and nothing has to be scaled afterwards.
     */
    fun setCellSize(width: Int, height: Int) = trs80_set_cell_size(width, height)

    /**
     * The rasterized screen as bytes, for handing to a bitmap.
     *
     * This copies, unlike [pixelBuffer], because the graphics stack wants its own
     * storage. It is one memcpy of the mask per drawn frame, which measured at
     * 0.05 ms on Android for the same data.
     */
    fun pixelBytes(): ByteArray =
        requireNotNull(trs80_pixel_buffer()) { "The core has no pixel buffer." }
            .readBytes(pixelWidth * pixelHeight)

    /**
     * Rasterizes video RAM into the pixel buffer, redrawing only what changed.
     *
     * Must be called from the thread that reads [pixelBuffer], never from the one
     * running [run].
     *
     * @return whether anything changed, so an unchanged screen costs no upload.
     */
    fun render(): Boolean = trs80_render() != 0

    /** Makes the next [render] redraw the whole screen. */
    fun invalidateRender() = trs80_invalidate_render()

    /**
     * The rasterized screen: one coverage byte per pixel, which the host tints
     * and scales.
     */
    val pixelBuffer: ScreenBuffer
        get() = NativeScreenBuffer(
            requireNotNull(trs80_pixel_buffer()) { "The core has no pixel buffer." }
        )

    /**
     * Boots a machine.
     *
     * @param model 1, 3, 4 or 5, matching the `model` field of `trs80_config`.
     * @param romPath the ROM image for [model].
     * @param diskPaths up to four disk images; missing entries leave a drive
     * empty. A `.cmd` file in the first drive is loaded into memory directly
     * and its entry point overrides [entryAddress].
     * @return whether the core accepted the configuration.
     */
    fun boot(
        model: Int,
        romPath: String,
        diskPaths: List<String?> = emptyList(),
        cassettePath: String? = null,
        entryAddress: Int = 0,
    ): Boolean = memScoped {
        val config = alloc<trs80_config>()
        config.model = model
        config.rom_path = romPath.cstr.ptr
        config.cassette_path = cassettePath?.cstr?.ptr
        for (drive in 0 until DRIVE_COUNT) {
            config.disk_path[drive] = diskPaths.getOrNull(drive)?.cstr?.ptr
        }
        config.entry_addr = entryAddress.toUShort()
        trs80_init(config.ptr) == TRS80_OK
    }

    /**
     * Runs the CPU. Blocks until [stop] is called from another thread, so this
     * must not be called on the main thread.
     *
     * The core's run loop tests a flag that `trs80_run()` does not itself set,
     * so this sets it first. Calling the C function alone returns immediately
     * and looks indistinguishable from a machine that will not boot.
     */
    fun run() {
        trs80_set_running(1)
        trs80_run()
    }

    /** Asks [run] to return. Safe to call from any thread. */
    fun stop() = trs80_set_running(0)

    fun reset() = trs80_reset()

    fun setSoundMuted(muted: Boolean) = trs80_set_sound_muted(if (muted) 1 else 0)

    /** Queues a key press. [sym] and [key] are the SDL codes the core expects. */
    fun keyDown(sym: Int, key: Int) =
        trs80_add_key_event(TRS80_KEY_DOWN, sym, key)

    /** Queues a key release. */
    fun keyUp(sym: Int, key: Int) =
        trs80_add_key_event(TRS80_KEY_UP, sym, key)

    /** The number of disk drives a machine has. */
    private const val DRIVE_COUNT = 4
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
