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

package org.puder.trs80.shared.configuration

import org.puder.trs80.shared.MODEL3
import org.puder.trs80.shared.ScreenColor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigurationDraftTest {

    /**
     * The numbering already written into every configuration on every device.
     * Amber was added afterwards and had to take the next free number, not the
     * next position.
     */
    @Test
    fun theStoredScreenColorsKeepTheirOldNumbers() {
        assertEquals(0, ScreenColor.Green.stored)
        assertEquals(1, ScreenColor.White.stored)
        assertEquals(2, ScreenColor.Amber.stored)
        assertEquals(ScreenColor.White, ScreenColor.of(1))
        assertEquals(ScreenColor.Green, ScreenColor.of(99))
    }

    /** The editor offers the two phosphors together, then white. */
    @Test
    fun theyAreOfferedInPhosphorOrder() {
        assertContentEquals(
            listOf(ScreenColor.Green, ScreenColor.Amber, ScreenColor.White),
            ScreenColor.entries,
        )
    }

    private fun draft(
        disks: List<String?> = listOf(null, null, null, null),
        wasCustom: Boolean = false,
    ) = ConfigurationDraft(
        id = 1,
        name = "BEAST",
        model = MODEL3,
        diskPaths = disks,
        cassettePath = null,
        keyboardPortrait = null,
        keyboardLandscape = null,
        characterColor = 0,
        soundMuted = false,
        wasCustom = wasCustom,
    )

    @Test
    fun choosingPutsTheDiskInThatDrive() {
        val d = draft().withDiskIn(2, "c.dsk")

        assertContentEquals(listOf(null, null, "c.dsk", null), d.diskPaths)
    }

    @Test
    fun choosingReplacesWhatIsInThatDrive() {
        val d = draft(listOf("a", "b", null, null)).withDiskIn(0, "c")

        assertContentEquals(listOf("c", "b", null, null), d.diskPaths)
    }

    @Test
    fun choosingOutsideTheDrivesChangesNothing() {
        val d = draft(listOf("a", null, null, null))

        assertEquals(d, d.withDiskIn(4, "b"))
        assertEquals(d, d.withDiskIn(-1, "b"))
    }

    /**
     * Drives are fixed positions, not a list that closes up. The emulator takes
     * one path per drive, so shuffling the others down would change which disk
     * the machine boots from.
     */
    @Test
    fun ejectingLeavesEveryOtherDriveWhereItIs() {
        val d = draft(listOf("a", "b", "c", null)).withDiskEjected(0)

        assertContentEquals(listOf(null, "b", "c", null), d.diskPaths)
    }

    @Test
    fun aGapInTheMiddleIsAnHonestConfiguration() {
        val d = draft(listOf("a", "b", null, null)).withDiskEjected(1)

        assertContentEquals(listOf("a", null, null, null), d.diskPaths)
        assertEquals(1, d.diskCount)
    }

    @Test
    fun ejectingAnEmptyDriveChangesNothing() {
        val d = draft(listOf("a", null, null, null))

        assertEquals(d, d.withDiskEjected(2))
        assertEquals(d, d.withDiskEjected(9))
    }

    /** Dragging onto drive 0 is asking for that disk to be the boot disk. */
    @Test
    fun movingSwapsTheTwoDrives() {
        val d = draft(listOf("a", "b", "c", null)).withDiskMoved(from = 2, to = 0)

        assertContentEquals(listOf("c", "b", "a", null), d.diskPaths)
    }

    @Test
    fun movingOntoAnEmptyDriveTakesTheDiskThere() {
        val d = draft(listOf("a", null, null, null)).withDiskMoved(from = 0, to = 3)

        assertContentEquals(listOf(null, null, null, "a"), d.diskPaths)
    }

    @Test
    fun movingOutsideTheDrivesChangesNothing() {
        val d = draft(listOf("a", "b", null, null))

        assertEquals(d, d.withDiskMoved(from = 0, to = 4))
        assertEquals(d, d.withDiskMoved(from = 0, to = 0))
    }

    /** The banner speaks only for something that came from the catalog. */
    @Test
    fun aCatalogEntryIsForkedOnceItDiffers() {
        val original = draft()

        assertFalse(original.isForkedFrom(original))
        assertTrue(original.copy(name = "BEAST 2").isForkedFrom(original))
    }

    @Test
    fun theUsersOwnCopyIsNeverForked() {
        val original = draft(wasCustom = true)

        assertFalse(original.copy(name = "changed").isForkedFrom(original))
    }
}
