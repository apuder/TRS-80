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
import kotlin.test.assertNull

class MediaSummaryTest {

    @Test
    fun readsAsTheRecordTableStatesIt() {
        assertEquals("2 disks · 37.5K", mediaSummary("2 disks", 38400))
    }

    @Test
    fun theWordingIsTakenAsGiven() {
        assertEquals("1 disk · 180K", mediaSummary("1 disk", 184320))
    }

    /** Nothing to say about a machine with no disks in it. */
    @Test
    fun noDisksHasNoSummary() {
        assertNull(mediaSummary("", 0))
    }

    /** The count is known before the files are; the size can wait. */
    @Test
    fun sizeIsLeftOutUntilItIsKnown() {
        assertEquals("2 disks", mediaSummary("2 disks", 0))
    }

    @Test
    fun sizesReadInTheMachinesOwnUnits() {
        assertEquals("512B", byteSize(512))
        assertEquals("1K", byteSize(1024))
        assertEquals("180K", byteSize(184320))
        assertEquals("1.5M", byteSize(1024 * 1024 * 3 / 2))
    }

    /** A tenth of a K stops being worth reading once the whole number is big. */
    @Test
    fun theFractionIsDroppedOnceItStopsCarryingMeaning() {
        assertEquals("101K", byteSize(103500))
    }
}
