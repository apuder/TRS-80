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
import org.puder.trs80.core.trs80_create_blank_dmk
import org.puder.trs80.core.trs80_create_blank_jv1
import org.puder.trs80.core.trs80_create_blank_jv3
import org.puder.trs80.shared.ui.DiskFormat
import org.puder.trs80.shared.ui.DiskImageSpec
import org.puder.trs80.core.trs80_init
import org.puder.trs80.core.trs80_is_expanded_mode
import org.puder.trs80.core.trs80_cassette_position
import org.puder.trs80.core.trs80_paste
import org.puder.trs80.core.trs80_reset
import org.puder.trs80.core.trs80_rewind_cassette
import org.puder.trs80.core.trs80_save_state
import org.puder.trs80.core.trs80_load_state
import org.puder.trs80.core.TRS80_CELL_HEIGHT
import org.puder.trs80.core.TRS80_CELL_WIDTH
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
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
object IosEmulatorCore : EmulatorCore {

    /** Whether the machine is drawing wide characters. */
    override val isExpandedMode: Boolean
        get() = trs80_is_expanded_mode() != 0

    /**
     * The emulator's screen memory. The pointer is valid for the lifetime of
     * the process, so this can be held indefinitely.
     */
    override val screenBuffer: ScreenBuffer
        get() = NativeScreenBuffer(
            requireNotNull(trs80_screen_buffer()) { "The core has no screen buffer." }
        )

    /** The rasterized screen's dimensions, in pixels; see [setCellSize]. */
    override val pixelWidth: Int get() = trs80_pixel_width()
    override val pixelHeight: Int get() = trs80_pixel_height()

    /**
     * The character ROM's own cell size, which is what the core rasterizes at
     * until it is told otherwise. Not the current cell size — that is whatever
     * was last passed to [setCellSize], and a caller that needs it should keep it.
     */
    override val romCellWidth: Int get() = TRS80_CELL_WIDTH
    override val romCellHeight: Int get() = TRS80_CELL_HEIGHT

    /**
     * Sets the size one character cell is drawn at, so the core rasterizes
     * straight to it and nothing has to be scaled afterwards.
     */
    override fun setCellSize(width: Int, height: Int) = trs80_set_cell_size(width, height)

    /**
     * Copies the rasterized screen into [destination], which must be at least
     * [pixelWidth] * [pixelHeight] bytes.
     *
     * Takes a buffer rather than returning one, because the caller draws every
     * frame and the mask is about a megabyte at a phone's screen size: returning
     * a fresh array made a megabyte of garbage per frame, which measured at 75%
     * of the process's CPU time. The copy itself is not the expensive part.
     */
    override fun copyPixelsInto(destination: ByteArray) {
        val source = requireNotNull(trs80_pixel_buffer()) { "The core has no pixel buffer." }
        val bytes = pixelWidth * pixelHeight
        require(destination.size >= bytes) {
            "Destination holds ${destination.size} bytes, need $bytes."
        }
        destination.usePinned { memcpy(it.addressOf(0), source, bytes.convert()) }
    }

    /**
     * Rasterizes video RAM into the pixel buffer, redrawing only what changed.
     *
     * Must be called from the thread that reads [pixelBuffer], never from the one
     * running [run].
     *
     * @return whether anything changed, so an unchanged screen costs no upload.
     */
    override fun render(): Boolean = trs80_render() != 0

    /** Makes the next [render] redraw the whole screen. */
    override fun invalidateRender() = trs80_invalidate_render()

    /**
     * The rasterized screen: one coverage byte per pixel, which the host tints
     * and scales.
     */
    override val pixelBuffer: ScreenBuffer
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
    override fun boot(
        model: Int,
        romPath: String,
        diskPaths: List<String?>,
        cassettePath: String?,
        entryAddress: Int,
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
    override fun run() {
        trs80_set_running(1)
        trs80_run()
    }

    /** Asks [run] to return. Safe to call from any thread. */
    override fun stop() = trs80_set_running(0)

    override fun reset() = trs80_reset()

    /**
     * Writes the machine's whole state to [path], so the session can be picked
     * up later exactly where it was left.
     */
    override fun saveState(path: String) = trs80_save_state(path)

    /** Reads back a state written by [saveState]. */
    override fun loadState(path: String) = trs80_load_state(path)

    override fun setSoundMuted(muted: Boolean) = trs80_set_sound_muted(if (muted) 1 else 0)

    /**
     * Types [text] into the machine as if it had been typed at the keyboard.
     *
     * The length is in bytes rather than characters, which is what the core
     * asks for and what a UTF-8 string actually occupies.
     */
    override fun paste(text: String) = trs80_paste(text, text.encodeToByteArray().size)

    /** Winds the tape back to the start, which CLOAD needs before it can read. */
    override fun rewindCassette() = trs80_rewind_cassette()

    /** How far through the tape the machine is, 0 to 1. */
    override fun cassettePosition(): Float = trs80_cassette_position()

    /** Queues a key press. [sym] and [key] are the SDL codes the core expects. */
    override fun keyDown(sym: Int, key: Int) =
        trs80_add_key_event(TRS80_KEY_DOWN, sym, key)

    /** Queues a key release. */
    override fun keyUp(sym: Int, key: Int) =
        trs80_add_key_event(TRS80_KEY_UP, sym, key)

    /**
     * Writes a blank disk image at [path].
     *
     * Independent of any running machine: it formats a file and touches nothing
     * else, so it may be called while a machine is running or before one has
     * ever been booted.
     *
     * @return whether the image was written.
     */
    override fun createBlankDisk(path: String, spec: DiskImageSpec): Boolean = when (spec.format) {
        DiskFormat.JV1 -> trs80_create_blank_jv1(path)
        DiskFormat.JV3 -> trs80_create_blank_jv3(path)
        DiskFormat.DMK -> trs80_create_blank_dmk(
            path,
            spec.sides,
            spec.densityCode,
            if (spec.eightInch) 1 else 0,
            if (spec.ignoreDensity) 1 else 0,
        )
    } != 0

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
