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

import okio.FileSystem
import okio.Path.Companion.toPath
import org.puder.trs80.shared.ui.DiskFormat
import org.puder.trs80.shared.ui.DiskImageSpec
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the blank-disk creator against the real emulator core.
 *
 * The panel that collects the parameters is tested as a value in commonMain;
 * what cannot be tested there is whether the core actually writes anything for
 * them, which is the part with a C library on the other side of it.
 */
class BlankDiskTest {

    private val written = mutableListOf<String>()

    @AfterTest
    fun cleanUp() {
        written.forEach { FileSystem.SYSTEM.delete(it.toPath(), mustExist = false) }
    }

    private fun create(spec: DiskImageSpec, name: String): String {
        val path = NSTemporaryDirectory() + name
        FileSystem.SYSTEM.delete(path.toPath(), mustExist = false)
        written += path
        assertTrue(IosEmulatorCore.createBlankDisk(path, spec), "The core refused to write $name.")
        return path
    }

    private fun sizeOf(path: String): Long =
        FileSystem.SYSTEM.metadata(path.toPath()).size ?: 0L

    /**
     * A JV1 image of nothing is legitimately an empty file: the format is bare
     * sector data with no header, so a blank one has nothing to say.
     */
    @Test
    fun aBlankJv1IsWritten() {
        val path = create(DiskImageSpec(format = DiskFormat.JV1), "trs80-blank-jv1.dsk")

        assertTrue(FileSystem.SYSTEM.exists(path.toPath()))
        assertEquals(0L, sizeOf(path))
    }

    /** JV3 carries a sector-id table, so even a blank one has bytes in it. */
    @Test
    fun aBlankJv3HasItsIdTable() {
        val path = create(DiskImageSpec(format = DiskFormat.JV3), "trs80-blank-jv3.dsk")

        assertTrue(sizeOf(path) > 0L, "A blank JV3 should still carry its id table.")
    }

    /**
     * An unformatted DMK is its 16-byte header and nothing else; the tracks
     * arrive when the machine formats it.
     */
    @Test
    fun aBlankDmkIsItsHeader() {
        val path = create(DiskImageSpec(format = DiskFormat.DMK), "trs80-blank-dmk.dsk")

        assertEquals(DMK_HEADER_SIZE, sizeOf(path))
    }

    /**
     * The DMK parameters have to reach the core rather than only the screen.
     *
     * They do not change the length of a blank image -- they are flags and a
     * track length inside the header -- so the header is what has to be read
     * back. Byte 4 is the options byte: 0x10 single-sided, 0x40 single-density,
     * 0x80 ignore density.
     */
    @Test
    fun theDmkParametersReachTheHeader() {
        val defaults = header(
            create(DiskImageSpec(format = DiskFormat.DMK), "trs80-dmk-default.dsk")
        )
        assertEquals(0x10 or 0x40, defaults[4].toInt() and 0xff, "one side, single density")

        val twoSides = header(
            create(
                DiskImageSpec(format = DiskFormat.DMK, sides = 2, doubleDensity = true),
                "trs80-dmk-2sides.dsk",
            )
        )
        assertEquals(0, twoSides[4].toInt() and 0xff, "two sides, double density")

        val ignoring = header(
            create(
                DiskImageSpec(format = DiskFormat.DMK, ignoreDensity = true),
                "trs80-dmk-ignden.dsk",
            )
        )
        assertTrue(ignoring[4].toInt() and 0x80 != 0, "ignore density")
    }

    /** The disk's size shows up as the track length in bytes 2 and 3. */
    @Test
    fun anEightInchDmkReservesLongerTracks() {
        val five = trackLength(
            create(
                DiskImageSpec(format = DiskFormat.DMK, eightInch = false),
                "trs80-dmk-5inch.dsk",
            )
        )
        val eight = trackLength(
            create(
                DiskImageSpec(format = DiskFormat.DMK, eightInch = true),
                "trs80-dmk-8inch.dsk",
            )
        )

        assertTrue(eight > five, "8 inch ($eight) should reserve more per track than 5 inch ($five)")
    }

    private fun header(path: String): ByteArray =
        FileSystem.SYSTEM.read(path.toPath()) { readByteArray() }

    private fun trackLength(path: String): Int {
        val bytes = header(path)
        return (bytes[2].toInt() and 0xff) or ((bytes[3].toInt() and 0xff) shl 8)
    }

    /** A path into a directory that is not there is refused, not crashed on. */
    @Test
    fun anUnwritablePathIsReportedRatherThanFatal() {
        val path = NSTemporaryDirectory() + "trs80-no-such-dir/blank.dsk"

        assertFalse(IosEmulatorCore.createBlankDisk(path, DiskImageSpec(format = DiskFormat.JV3)))
    }

    private companion object {
        /** What trs_create_blank_dmk writes: a header and no tracks yet. */
        const val DMK_HEADER_SIZE = 16L
    }
}
