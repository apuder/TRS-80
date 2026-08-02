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

package org.puder.trs80.shared.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiskImageTest {

    @Test
    fun theSuffixIsAddedButNotDoubled() {
        assertEquals("scratch.dsk", DiskImageSpec(name = "scratch").filename)
        assertEquals("scratch.dsk", DiskImageSpec(name = "scratch.dsk").filename)
        assertEquals("scratch.DSK", DiskImageSpec(name = "scratch.DSK").filename)
    }

    /** The name is opened by path from C, so it stays narrow. */
    @Test
    fun onlyPlainNamesAreAccepted() {
        assertTrue(DiskImageSpec(name = "boot-1_v2.dsk").nameIsLegal)
        assertFalse(DiskImageSpec(name = "").nameIsLegal)
        assertFalse(DiskImageSpec(name = "my disk").nameIsLegal)
        assertFalse(DiskImageSpec(name = "../escape").nameIsLegal)
        assertFalse(DiskImageSpec(name = "sub/dir").nameIsLegal)
        assertFalse(DiskImageSpec(name = "quote\"it").nameIsLegal)
    }

    @Test
    fun anUnusableNameHasNoFilename() {
        assertNull(DiskImageSpec(name = "my disk").filename)
        assertNull(DiskImageSpec(name = "").filename)
    }

    @Test
    fun onlyDmkHasParametersThatApply() {
        assertTrue(DiskImageSpec(format = DiskFormat.DMK).dmkApplies)
        assertFalse(DiskImageSpec(format = DiskFormat.JV1).dmkApplies)
        assertFalse(DiskImageSpec(format = DiskFormat.JV3).dmkApplies)
    }

    /** The core counts density as 1 or 2, not as a flag. */
    @Test
    fun densityIsGivenToTheCoreAsACount() {
        assertEquals(1, DiskImageSpec(doubleDensity = false).densityCode)
        assertEquals(2, DiskImageSpec(doubleDensity = true).densityCode)
    }

    /** Changing format and back must not quietly forget the DMK settings. */
    @Test
    fun theDmkSettingsSurviveALookAtAnotherFormat() {
        val dmk = DiskImageSpec(name = "x", format = DiskFormat.DMK, sides = 2, eightInch = true)

        val roundTrip = dmk.copy(format = DiskFormat.JV1).copy(format = DiskFormat.DMK)

        assertEquals(dmk, roundTrip)
    }
}
