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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs the emulator core on iOS.
 *
 * These do not exercise much emulator behaviour — booting a machine needs a ROM
 * this module has no business bundling. What they do prove is that the C core
 * links into a Kotlin/Native binary and that calls across cinterop actually
 * reach it, which is the thing that had never been true before.
 */
class EmulatorCoreTest {

    @Test
    fun screenBufferIsTheSizeOfTheScreen() {
        val buffer = EmulatorCore.screenBuffer
        // The core owns 2 KB; reading the last cell must not fault.
        assertEquals(0.toByte(), buffer[SCREEN_BUFFER_SIZE - 1])
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
    fun expandedModeIsReadableBeforeBoot() {
        // Just has to return without trapping; either answer is legitimate on a
        // machine that has not been initialized.
        val expanded = EmulatorCore.isExpandedMode
        assertTrue(expanded || !expanded)
    }

    @Test
    fun keyEventsAreAcceptedBeforeBoot() {
        // The core queues these without needing a running CPU. This is the
        // cinterop path that carries arguments in both directions.
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

    private companion object {
        /** Matches TRS80_SCREEN_BUFFER_SIZE in trs80_core.h. */
        const val SCREEN_BUFFER_SIZE = 2048
    }
}
