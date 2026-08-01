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

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertContentEquals

/** What a screenshot's pixels have to look like for Skia to read them back. */
class ScreenSnapshotTest {

    private fun bytesOf(argb: Int): ByteArray =
        ByteArray(4).also { writeN32(it, 0, Color(argb).toN32()) }

    /**
     * Blue first, alpha last, as N32 is on Apple platforms.
     *
     * Amber is the colour that catches this: red and blue at opposite ends, so
     * writing them the wrong way round turns it blue.
     */
    @Test
    fun amberIsWrittenBlueFirst() {
        assertContentEquals(
            byteArrayOf(0x00, 0xB0.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            bytesOf(ScreenColors.AMBER),
        )
    }

    /** The two that hid the bug: both are unchanged by a red and blue swap. */
    @Test
    fun greenAndWhiteCannotTellTheDifference() {
        assertContentEquals(
            byteArrayOf(0x00, 0xFF.toByte(), 0x00, 0xFF.toByte()),
            bytesOf(ScreenColors.GREEN),
        )
        assertContentEquals(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            bytesOf(ScreenColors.WHITE),
        )
    }

    @Test
    fun theGlassIsWrittenTheSameWay() {
        assertContentEquals(
            byteArrayOf(0x44, 0x44, 0x44, 0xFF.toByte()),
            bytesOf(ScreenColors.DARK_GRAY),
        )
    }
}
