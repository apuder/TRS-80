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

package org.puder.trs80

import java.nio.ByteBuffer
import org.puder.trs80.shared.EmulatorCore
import org.puder.trs80.shared.ScreenBuffer
import org.puder.trs80.shared.ui.DiskFormat
import org.puder.trs80.shared.ui.DiskImageSpec

/**
 * The emulator core, as the shared UI expects it, over the JNI binding.
 *
 * Thin by design: [XTRS] already wraps the same `trs80_core.h` that iOS reaches
 * through cinterop, so nearly every member here is one call. What little
 * reconciling there is comes from the two wrappers having been written years
 * apart against the same header.
 *
 * This lives in the app module rather than in `shared` because the JNI entry
 * points are named after the class that declares them —
 * `Java_org_puder_trs80_XTRS_*`, twenty-two of them. Moving [XTRS] would mean
 * renaming all twenty-two in C, so instead the shared side asks for an
 * interface and the app answers it here.
 */
object AndroidEmulatorCore : EmulatorCore {

    override val isExpandedMode: Boolean
        get() = XTRS.isExpandedMode()

    override val screenBuffer: ScreenBuffer
        get() = XTRS.screenBuffer

    override val pixelBuffer: ScreenBuffer
        get() = DirectPixels(XTRS.pixelBuffer)

    override val pixelWidth: Int
        get() = XTRS.pixelWidth()

    override val pixelHeight: Int
        get() = XTRS.pixelHeight()

    /*
     * Not read from the core: there is no JNI binding for the two constants and
     * adding one means a C change for a pair of numbers that have never moved.
     * They are TRS80_CELL_WIDTH and TRS80_CELL_HEIGHT in trs80_core.h, and if
     * those ever change these change with them.
     */
    override val romCellWidth: Int get() = 8
    override val romCellHeight: Int get() = 12

    override fun boot(
        model: Int,
        romPath: String,
        diskPaths: List<String?>,
        cassettePath: String?,
        entryAddress: Int,
    ): Boolean = XTRS.boot(
        model,
        romPath,
        entryAddress,
        cassettePath,
        // Addressed by drive rather than filtered: the core takes one path per
        // drive and honours the gaps, so a disk in drive 2 with drive 1 empty
        // has to stay where it is.
        diskPaths.getOrNull(0),
        diskPaths.getOrNull(1),
        diskPaths.getOrNull(2),
        diskPaths.getOrNull(3),
    ) == TRS80_OK

    /**
     * Runs the CPU, returning only once [stop] is called.
     *
     * The flag is set before the loop starts, not after. `trs80_run()` tests a
     * flag it does not itself set, so a caller that starts the thread first and
     * raises the flag afterwards is racing the thread — and losing that race
     * looks exactly like a machine that will not boot. The old EmulatorActivity
     * set it after and got away with it because thread start-up is slower.
     */
    override fun run() {
        XTRS.setRunning(true)
        XTRS.run()
    }

    override fun stop() = XTRS.setRunning(false)

    override fun reset() = XTRS.reset()

    override fun setCellSize(width: Int, height: Int) = XTRS.setCellSize(width, height)

    override fun render(): Boolean = XTRS.render()

    override fun invalidateRender() = XTRS.invalidateRender()

    override fun copyPixelsInto(destination: ByteArray) {
        val bytes = pixelWidth * pixelHeight
        require(destination.size >= bytes) {
            "Destination holds ${destination.size} bytes, need $bytes."
        }
        // A duplicate so that reading leaves the buffer the core handed over
        // where it was; the position this advances is the copy's.
        XTRS.pixelBuffer.duplicate().get(destination, 0, bytes)
    }

    override fun saveState(path: String) = XTRS.saveState(path)

    override fun loadState(path: String) = XTRS.loadState(path)

    override fun setSoundMuted(muted: Boolean) = XTRS.setSoundMuted(muted)

    override fun paste(text: String) = XTRS.paste(text)

    override fun rewindCassette() = XTRS.rewindCassette()

    override fun cassettePosition(): Float = XTRS.getCassettePosition()

    override fun keyDown(sym: Int, key: Int) = XTRS.addKeyEvent(KEY_DOWN, sym, key)

    override fun keyUp(sym: Int, key: Int) = XTRS.addKeyEvent(KEY_UP, sym, key)

    override fun createBlankDisk(path: String, spec: DiskImageSpec): Boolean = when (spec.format) {
        DiskFormat.JV1 -> XTRS.createBlankJV1(path)
        DiskFormat.JV3 -> XTRS.createBlankJV3(path)
        DiskFormat.DMK -> XTRS.createBlankDMK(
            path,
            spec.sides,
            spec.densityCode,
            if (spec.eightInch) 1 else 0,
            if (spec.ignoreDensity) 1 else 0,
        )
    }

    /** What `trs80_init` returns when it accepted the configuration. */
    private const val TRS80_OK = 0

    /** The two event kinds `trs80_add_key_event` takes; they match SDL 1.2. */
    private const val KEY_DOWN = 2
    private const val KEY_UP = 3
}

/**
 * The core's own pixels, read where they lie.
 *
 * JNI hands back a direct buffer addressing the native memory rather than a
 * copy of it, so this is a view and not a snapshot — which is the point, at a
 * megabyte a frame.
 */
private class DirectPixels(private val buffer: ByteBuffer) : ScreenBuffer {
    override fun get(index: Int): Byte = buffer.get(index)
}
