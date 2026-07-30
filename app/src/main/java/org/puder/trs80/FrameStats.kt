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

package org.puder.trs80

/**
 * Accumulates how long the frame loop spends drawing, so the two renderers can be
 * compared on a device rather than by argument.
 *
 * Deliberately measures only the work the host controls: sampling the screen and
 * getting it onto the surface. It says nothing about when the result is actually
 * presented, which the display governs.
 *
 * Only the frames that drew anything are timed. An idle screen skips the work
 * entirely, so including those would report whichever renderer was luckier about
 * how much the emulated program happened to be doing.
 */
class FrameStats(private val windowNanos: Long = 1_000_000_000L) {

    private var windowStartedAt = 0L
    private var drawnFrames = 0
    private var skippedFrames = 0
    private var totalNanos = 0L
    private var worstNanos = 0L

    /** One window's worth of frames. */
    data class Summary(
        /** Frames that had something to draw. */
        val drawn: Int,
        /** Frames where the screen had not changed, so nothing was drawn. */
        val skipped: Int,
        /** Mean time spent drawing one of the [drawn] frames. */
        val averageMillis: Double,
        /** The slowest of the [drawn] frames. */
        val worstMillis: Double,
    ) {
        /** Frames drawn per second over the window. */
        val drawnPerSecond: Int get() = drawn

        override fun toString(): String = "%d fps drawn, %d idle, %.2f ms avg, %.2f ms worst"
            .format(drawn, skipped, averageMillis, worstMillis)
    }

    /** Records one pass of the frame loop. */
    fun record(elapsedNanos: Long, drew: Boolean) {
        if (windowStartedAt == 0L) {
            windowStartedAt = System.nanoTime()
        }
        if (drew) {
            drawnFrames++
            totalNanos += elapsedNanos
            if (elapsedNanos > worstNanos) {
                worstNanos = elapsedNanos
            }
        } else {
            skippedFrames++
        }
    }

    /**
     * @return a summary once a window has elapsed, and null otherwise, so the
     * caller can report at a readable rate without keeping its own clock.
     */
    fun takeSummary(): Summary? {
        val now = System.nanoTime()
        if (windowStartedAt == 0L || now - windowStartedAt < windowNanos) {
            return null
        }
        val summary = Summary(
            drawn = drawnFrames,
            skipped = skippedFrames,
            averageMillis = if (drawnFrames == 0) 0.0 else totalNanos / drawnFrames / 1_000_000.0,
            worstMillis = worstNanos / 1_000_000.0,
        )
        windowStartedAt = now
        drawnFrames = 0
        skippedFrames = 0
        totalNanos = 0
        worstNanos = 0
        return summary
    }
}
