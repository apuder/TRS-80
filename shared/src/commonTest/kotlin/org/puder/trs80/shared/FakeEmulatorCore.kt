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
 * A machine that is not one: it answers the [EmulatorCore] contract from Kotlin,
 * recording what it was asked and handing back whatever the test set up.
 *
 * This is the point of the contract being an interface. Everything above it —
 * how the picture is fitted, when a frame is skipped, what happens on the way in
 * and out of a session — is the same code on both platforms, and none of it
 * needs a Z80 to be tested.
 */
class FakeEmulatorCore : EmulatorCore {

    /** What the last [setCellSize] asked for, and how often one arrived. */
    var cellWidth = 0
        private set
    var cellHeight = 0
        private set
    var cellSizeChanges = 0
        private set

    var renderCount = 0
        private set
    var invalidateCount = 0
        private set

    /** What the next [render] reports. A machine sitting idle changes nothing. */
    var hasChanged = true

    /** The byte every pixel is filled with, so a test can tell frames apart. */
    var fill: Byte = 1

    override val isExpandedMode = false

    override val screenBuffer = object : ScreenBuffer {
        override fun get(index: Int): Byte = 0
    }

    override val pixelBuffer = screenBuffer

    override var pixelWidth = 0
        private set

    override var pixelHeight = 0
        private set

    /** What the Model I and III character ROM actually uses. */
    override val romCellWidth = 8
    override val romCellHeight = 12

    override fun setCellSize(width: Int, height: Int) {
        cellWidth = width
        cellHeight = height
        cellSizeChanges++
        pixelWidth = width * SCREEN_COLUMNS
        pixelHeight = height * SCREEN_ROWS
    }

    override fun render(): Boolean {
        renderCount++
        return hasChanged
    }

    override fun invalidateRender() {
        invalidateCount++
    }

    override fun copyPixelsInto(destination: ByteArray) {
        destination.fill(fill)
    }

    override fun boot(
        model: Int,
        romPath: String,
        diskPaths: List<String?>,
        cassettePath: String?,
        entryAddress: Int,
    ) = true

    override fun run() = Unit
    override fun stop() = Unit
    override fun reset() = Unit
    override fun saveState(path: String) = Unit
    override fun loadState(path: String) = Unit
    override fun setSoundMuted(muted: Boolean) = Unit
    override fun paste(text: String) = Unit
    override fun rewindCassette() = Unit
    override fun cassettePosition() = 0f
    override fun keyDown(sym: Int, key: Int) = Unit
    override fun keyUp(sym: Int, key: Int) = Unit
    override fun createBlankDisk(path: String, spec: DiskImageSpec) = true
}
