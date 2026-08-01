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
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigurationDraftTest {

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
    fun addingPutsTheDiskInTheFirstFreeDrive() {
        val d = draft().withDiskAdded("a.dsk").withDiskAdded("b.dsk")

        assertContentEquals(listOf("a.dsk", "b.dsk", null, null), d.diskPaths)
    }

    @Test
    fun addingIsIgnoredWhenEveryDriveIsFull() {
        val full = draft(listOf("a", "b", "c", "d"))

        assertEquals(full, full.withDiskAdded("e"))
    }

    /** A hole in the middle has no way to show itself in an ordered stack. */
    @Test
    fun ejectingClosesTheGapBehindIt() {
        val d = draft(listOf("a", "b", "c", null)).withDiskEjected(0)

        assertContentEquals(listOf("b", "c", null, null), d.diskPaths)
    }

    @Test
    fun ejectingAnEmptyDriveChangesNothing() {
        val d = draft(listOf("a", null, null, null))

        assertEquals(d, d.withDiskEjected(2))
    }

    @Test
    fun choosingReplacesWhatIsInThatDrive() {
        val d = draft(listOf("a", "b", null, null)).withDiskIn(drive = 0, path = "c")

        assertContentEquals(listOf("c", "b", null, null), d.diskPaths)
    }

    /** The drive past the last one is the empty row the editor shows. */
    @Test
    fun choosingInTheEmptyDriveFillsIt() {
        val d = draft(listOf("a", null, null, null)).withDiskIn(drive = 1, path = "b")

        assertContentEquals(listOf("a", "b", null, null), d.diskPaths)
    }

    @Test
    fun choosingBeyondTheEmptyDriveChangesNothing() {
        val d = draft(listOf("a", null, null, null))

        assertEquals(d, d.withDiskIn(drive = 2, path = "b"))
        assertEquals(d, d.withDiskIn(drive = 4, path = "b"))
    }

    @Test
    fun movingShiftsTheOthersAlong() {
        val d = draft(listOf("a", "b", "c", null)).withDiskMoved(from = 2, to = 0)

        assertContentEquals(listOf("c", "a", "b", null), d.diskPaths)
    }

    @Test
    fun movingOutsideTheOccupiedDrivesChangesNothing() {
        val d = draft(listOf("a", "b", null, null))

        assertEquals(d, d.withDiskMoved(from = 0, to = 3))
        assertEquals(d, d.withDiskMoved(from = 0, to = 0))
    }

    /** The banner speaks only for something that came from the catalogue. */
    @Test
    fun aCatalogueEntryIsForkedOnceItDiffers() {
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
