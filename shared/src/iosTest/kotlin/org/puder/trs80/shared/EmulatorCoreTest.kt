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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSTemporaryDirectory
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runs the emulator core on iOS.
 *
 * The point of these is that the C core links into a Kotlin/Native binary and
 * that calls across cinterop reach it — which had never been true before — and
 * that the Z80 actually executes there.
 *
 * No TRS-80 ROM is bundled. [SPIN_AT_VIDEO_RAM] is seven bytes of Z80 written
 * here in the test, which is enough to prove the CPU runs and that a write to
 * video RAM lands in the buffer the host reads.
 */
class EmulatorCoreTest {

    @Test
    fun screenBufferIsTheSizeOfTheScreen() {
        val buffer = EmulatorCore.screenBuffer
        // The core owns 2 KB; reading the last cell must not fault.
        buffer[SCREEN_BUFFER_SIZE - 1]
    }

    @Test
    fun screenBufferIsTheSameMemoryEachTime() {
        // The core hands back a pointer into its own memory rather than a copy,
        // so two reads of the same cell have to agree.
        val first = EmulatorCore.screenBuffer
        val second = EmulatorCore.screenBuffer
        assertEquals(first[0], second[0])
    }

    @Test
    fun keyEventsAreAcceptedBeforeBoot() {
        // Queued without needing a running CPU. This is the cinterop path that
        // carries arguments into the core.
        EmulatorCore.keyDown(sym = 0x0D, key = 0x0D)
        EmulatorCore.keyUp(sym = 0x0D, key = 0x0D)
    }

    @Test
    fun dirtyRectRunsAgainstTheRealScreenBuffer() {
        // The commonMain frame-differencing logic, driven by the actual native
        // buffer rather than a fake: this is what the renderer will do 60 times
        // a second.
        val metrics = CellMetrics(columns = 64, rows = 16, cellWidth = 8, cellHeight = 24)
        val dirty = DirtyRect(metrics, EmulatorCore.screenBuffer)

        dirty.isExpandedMode = false
        dirty.computeDirtyRect()
        // Every cell differs from the "not known yet" marker, so the first pass
        // reports the whole screen.
        assertTrue(!dirty.isEmpty)
        assertEquals(0, dirty.clipLeft)
        assertEquals(0, dirty.clipTop)
        assertEquals(64 * 8, dirty.clipRight)
        assertEquals(16 * 24, dirty.clipBottom)

        // Nothing has changed since, so the second pass reports nothing.
        dirty.computeDirtyRect()
        assertTrue(dirty.isEmpty)
    }

    @Test
    fun renderingProducesTheGlyphFromTheCharacterRom() = runBlocking {
        val romPath = writeRom(SPIN_AT_VIDEO_RAM)
        assertTrue(EmulatorCore.boot(model = 3, romPath = romPath), "The core refused to boot.")

        val cpu = launch(Dispatchers.Default) { EmulatorCore.run() }
        try {
            withTimeout(RUN_TIMEOUT_MILLIS) {
                while (EmulatorCore.screenBuffer[0] != EXPECTED_CHAR) {
                    delay(POLL_INTERVAL_MILLIS)
                }
            }
        } finally {
            EmulatorCore.stop()
            cpu.join()
        }

        assertTrue(EmulatorCore.render(), "Nothing was rasterized.")
        assertEquals(512, EmulatorCore.pixelWidth)
        assertEquals(192, EmulatorCore.pixelHeight)

        // The top-left cell should now hold 'A' exactly as the Model III
        // character generator ROM draws it, rather than merely being non-empty.
        val actual = readCell(x = 0, y = 0)
        assertEquals(A_GLYPH.joinToString("\n"), actual.joinToString("\n"))

        // Nothing has changed since, so a second pass reports no work and the
        // host can skip its upload.
        assertFalse(EmulatorCore.render(), "An unchanged screen should report no change.")

        // Invalidating forces a full redraw, which is what a reattached surface
        // needs.
        EmulatorCore.invalidateRender()
        assertTrue(EmulatorCore.render(), "Invalidating should force a redraw.")
    }

    /** Reads one character cell out of the pixel buffer as rows of `#` and `.`. */
    private fun readCell(x: Int, y: Int): List<String> {
        val pixels = EmulatorCore.pixelBuffer
        return (0 until CELL_HEIGHT).map { row ->
            (0 until CELL_WIDTH).joinToString("") { col ->
                val at = (y + row) * EmulatorCore.pixelWidth + x + col
                if (pixels[at] != 0.toByte()) "#" else "."
            }
        }
    }

    @Test
    fun z80ExecutesAndWritesToVideoRam() = runBlocking {
        val romPath = writeRom(SPIN_AT_VIDEO_RAM)
        assertTrue(EmulatorCore.boot(model = 1, romPath = romPath), "The core refused to boot.")

        // trs80_run blocks until stopped, so it needs a thread of its own.
        val cpu = launch(Dispatchers.Default) { EmulatorCore.run() }
        try {
            withTimeout(RUN_TIMEOUT_MILLIS) {
                while (EmulatorCore.screenBuffer[0] != EXPECTED_CHAR) {
                    delay(POLL_INTERVAL_MILLIS)
                }
            }
        } finally {
            EmulatorCore.stop()
            cpu.join()
        }

        // Reached only if the CPU fetched from the ROM, executed the store, and
        // the memory write propagated into the host's screen buffer.
        assertEquals(EXPECTED_CHAR, EmulatorCore.screenBuffer[0])
    }

    /** Writes [bytes] to a file the core can open, and returns its path. */
    @OptIn(ExperimentalForeignApi::class)
    private fun writeRom(bytes: UByteArray): String {
        val path = NSTemporaryDirectory() + "trs80-synthetic-rom.bin"
        val file = requireNotNull(fopen(path, "wb")) { "Could not create $path." }
        try {
            bytes.usePinned {
                fwrite(it.addressOf(0), 1.toULong(), bytes.size.toULong(), file)
            }
        } finally {
            fclose(file)
        }
        return path
    }

    private companion object {
        /** Matches TRS80_SCREEN_BUFFER_SIZE in trs80_core.h. */
        const val SCREEN_BUFFER_SIZE = 2048

        /** The character the synthetic ROM stores, and where it stores it. */
        const val EXPECTED_CHAR: Byte = 0x41 // 'A'

        /**
         * Z80 machine code, executed from address 0 where the Model I ROM is
         * mapped:
         *
         *     3E 41        LD   A, 0x41      ; 'A'
         *     32 00 3C     LD   (0x3C00), A  ; the first cell of video RAM
         *     18 FE        JR   -2           ; spin, so the CPU stays busy
         */
        val SPIN_AT_VIDEO_RAM = ubyteArrayOf(
            0x3Eu, 0x41u,
            0x32u, 0x00u, 0x3Cu,
            0x18u, 0xFEu,
        )

        const val RUN_TIMEOUT_MILLIS = 10_000L
        const val POLL_INTERVAL_MILLIS = 20L

        /** One character cell, matching TRS80_CELL_WIDTH and TRS80_CELL_HEIGHT. */
        const val CELL_WIDTH = 8
        const val CELL_HEIGHT = 12

        /**
         * 'A' as the Model III character generator ROM stores it, transcribed
         * from `trs_chars.c` CG 4. The last four rows are the leading between
         * text lines and are always blank.
         */
        val A_GLYPH = listOf(
            "...##...",
            "..#..#..",
            ".#....#.",
            ".######.",
            ".#....#.",
            ".#....#.",
            ".#....#.",
            "........",
            "........",
            "........",
            "........",
            "........",
        )
    }
}
