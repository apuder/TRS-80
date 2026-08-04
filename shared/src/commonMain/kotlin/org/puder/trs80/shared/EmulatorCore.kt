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

import org.puder.trs80.shared.ui.DiskImageSpec

/**
 * The emulator, as the app talks to it.
 *
 * One C library sits under both platforms — `trs80_core.h`, the whole
 * host-facing API — and each reaches it a different way: iOS through cinterop,
 * Android through JNI. The two wrappers turned out to have nearly the same
 * surface already, having been written against the same header, so this is less
 * a new abstraction than a written-down account of what both were doing.
 *
 * An interface rather than `expect`/`actual` for two reasons. The Android
 * binding lives in the app module, because its JNI entry points are named after
 * the class that declares them and moving the class means renaming twenty-two
 * functions in C — twice, with a package rename coming. And a screen that takes
 * its machine as a parameter can be given a fake one, which an `expect val`
 * reaching for a global cannot.
 *
 * There is only ever one machine running, so implementations are singletons;
 * that is a fact about the C library, not something this interface asks for.
 */
interface EmulatorCore {

    /** Whether the machine is in expanded mode; the keyboard cares. */
    val isExpandedMode: Boolean

    /** Video RAM, one byte per screen cell, owned by the core. */
    val screenBuffer: ScreenBuffer

    /** The rasterized screen as a coverage mask, owned by the core. */
    val pixelBuffer: ScreenBuffer

    /** The size [pixelBuffer] currently holds. */
    val pixelWidth: Int
    val pixelHeight: Int

    /** The cell size the ROM's glyphs are drawn at, before any scaling. */
    val romCellWidth: Int
    val romCellHeight: Int

    /**
     * Boots a machine.
     *
     * @param model 1, 3, 4 or 5, matching the `model` field of `trs80_config`.
     * @param romPath the ROM image for [model].
     * @param diskPaths up to four disk images; a missing entry leaves that drive
     * empty, and the gaps matter — the core takes one path per drive.
     * @return whether the core accepted the configuration.
     */
    fun boot(
        model: Int,
        romPath: String,
        diskPaths: List<String?> = emptyList(),
        cassettePath: String? = null,
        entryAddress: Int = 0,
    ): Boolean

    /**
     * Runs the CPU, returning only once [stop] is called from another thread.
     *
     * Never on the main thread: this does not return for the life of the
     * session.
     */
    fun run()

    /** Asks [run] to return. Safe to call from any thread. */
    fun stop()

    /** Restarts the machine as if the power had been cycled. */
    fun reset()

    /** Tells the core how large one character cell is drawn, so it rasterizes to it. */
    fun setCellSize(width: Int, height: Int)

    /**
     * Rasterizes video RAM into [pixelBuffer], redrawing only what changed.
     *
     * @return whether anything changed, so an unchanged screen costs nothing.
     */
    fun render(): Boolean

    /** Makes the next [render] redraw everything. */
    fun invalidateRender()

    /**
     * Copies the rasterized screen into [destination], which must hold at least
     * [pixelWidth] * [pixelHeight] bytes.
     *
     * Takes a buffer rather than returning one: the caller draws every frame,
     * and at a phone's screen size a fresh array per frame measured at three
     * quarters of the process's CPU time.
     */
    fun copyPixelsInto(destination: ByteArray)

    /** Writes the machine's whole state, so the session can be picked up later. */
    fun saveState(path: String)

    /** Reads back a state written by [saveState]. */
    fun loadState(path: String)

    fun setSoundMuted(muted: Boolean)

    /** Types [text] at the machine, as its own keyboard would have sent it. */
    fun paste(text: String)

    fun rewindCassette()

    /** How far through the tape the machine is, 0 to 1. */
    fun cassettePosition(): Float

    /** Queues a key press. [sym] and [key] are the SDL codes the core expects. */
    fun keyDown(sym: Int, key: Int)

    /** Queues a key release. */
    fun keyUp(sym: Int, key: Int)

    /**
     * Writes a blank disk image at [path].
     *
     * Independent of any running machine: it formats a file and touches nothing
     * else, so it may be called before one has ever been booted.
     *
     * @return whether the image was written.
     */
    fun createBlankDisk(path: String, spec: DiskImageSpec): Boolean
}

/**
 * The scancode nothing is mapped to; see [releaseAllKeys].
 *
 * Every key in [KeyboardMapping] has a code of its own and none of them is
 * zero, which is what makes zero usable as a scancode that was never pressed.
 */
private const val UNUSED_SCANCODE = 0

/**
 * Lets go of every key the machine believes is held down.
 *
 * The core releases whatever it last saw pressed at a given scancode, and for
 * one it has never seen that works out to "all keys up" -- which is exactly the
 * thing needed here, without a new call through the JNI and the cinterop both.
 *
 * Worth having because a held key survives being put away: what the machine has
 * down is part of the saved session, so a key that was pressed and never
 * released comes back held every time that session is resumed, and the machine
 * repeats it forever. That is a state a person cannot get out of from inside
 * the emulator -- the machine is busy typing E at itself -- so anything that
 * restores a session clears the keyboard on the way in.
 */
fun EmulatorCore.releaseAllKeys() = keyUp(sym = 0, key = UNUSED_SCANCODE)
