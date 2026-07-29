/*
 * Copyright 2012-2017, Arno Puder
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

/**
 * Tracks the region of the emulated screen that changed since the last frame, so that only
 * that region has to be redrawn.
 *
 * [top], [left], [bottom] and [right] are in character cells; [clipTop] and friends are the
 * same region in pixels. Everything is mutated in place: an instance of this is touched 60
 * times a second and must not allocate.
 */
class DirtyRect(private val metrics: CellMetrics, private val screenBuffer: ScreenBuffer) {

    /**
     * The contents of every cell as of the last [computeDirtyRect]. Held as shorts so that
     * [Short.MAX_VALUE] can mark a cell whose contents are not known yet; no byte read from
     * [screenBuffer] can collide with it.
     */
    private val lastScreenBuffer = ShortArray(metrics.columns * metrics.rows)

    /** The horizontal step between cells: 2 in expanded (wide character) mode, 1 otherwise. */
    private var colStep = 1

    /** The topmost dirty row. */
    var top = 0
        private set

    /** The leftmost dirty column. */
    var left = 0
        private set

    /** The bottommost dirty row, or -1 if nothing is dirty. */
    var bottom = 0
        private set

    /** The rightmost dirty column. */
    var right = 0
        private set

    /**
     * Whether the emulator draws wide characters. Switching modes invalidates everything
     * that is known about the screen, so the next [computeDirtyRect] reports it all dirty.
     */
    var isExpandedMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                resetLastScreenBuffer()
            }
            colStep = if (value) 2 else 1
        }

    /** Whether nothing changed since the last [computeDirtyRect]. */
    val isEmpty: Boolean
        get() = bottom == -1

    /** The left edge of the dirty region in pixels. Meaningless while [isEmpty]. */
    val clipLeft: Int
        get() = metrics.cellWidth * left * colStep

    /** The top edge of the dirty region in pixels. Meaningless while [isEmpty]. */
    val clipTop: Int
        get() = metrics.cellHeight * top

    /** The right edge of the dirty region in pixels, exclusive. Meaningless while [isEmpty]. */
    val clipRight: Int
        get() = metrics.cellWidth * (right + 1) * colStep

    /** The bottom edge of the dirty region in pixels, exclusive. Meaningless while [isEmpty]. */
    val clipBottom: Int
        get() = metrics.cellHeight * (bottom + 1)

    init {
        resetLastScreenBuffer()
    }

    /**
     * Compares the screen buffer against the previous frame and updates the bounds to cover
     * everything that changed.
     */
    fun computeDirtyRect() {
        var newTop = Int.MAX_VALUE
        var newLeft = Int.MAX_VALUE
        var newBottom = -1
        var newRight = -1
        val step = colStep
        val cols = metrics.columns / step
        val last = lastScreenBuffer
        var i = 0
        for (row in 0 until metrics.rows) {
            for (col in 0 until cols) {
                // Read the cell once: the native side writes to this buffer concurrently,
                // so comparing one read and storing another would lose an update.
                val cell = screenBuffer[i].toShort()
                if (last[i] != cell) {
                    if (newTop > row) newTop = row
                    if (newBottom < row) newBottom = row
                    if (newLeft > col) newLeft = col
                    if (newRight < col) newRight = col
                    last[i] = cell
                }
                i += step
            }
        }
        top = newTop
        left = newLeft
        bottom = newBottom
        right = newRight
    }

    /** Marks the whole screen dirty. */
    fun reset() {
        left = 0
        top = 0
        right = metrics.columns / colStep - 1
        bottom = metrics.rows - 1
    }

    /**
     * Call with the region the host actually locked for drawing, which may be larger than
     * the region it was asked for. When it is, the whole screen has to be redrawn, because
     * the extra area holds a stale frame.
     */
    fun onRegionLocked(lockedLeft: Int, lockedTop: Int, lockedRight: Int, lockedBottom: Int) {
        if (lockedLeft != clipLeft || lockedTop != clipTop ||
            lockedRight != clipRight || lockedBottom != clipBottom
        ) {
            reset()
        }
    }

    private fun resetLastScreenBuffer() = lastScreenBuffer.fill(Short.MAX_VALUE)
}
