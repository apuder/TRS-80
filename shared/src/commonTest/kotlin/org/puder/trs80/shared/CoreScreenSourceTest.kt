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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Covers the frame loop that used to be written once per platform.
 *
 * Everything here is about what the source does *not* do: not resizing when the
 * cell has not moved, not drawing a frame the machine did not change, not
 * offering a screenshot of a screen nothing has been drawn on yet. Those are the
 * parts that cost something when they go wrong, and none of them need a real
 * bitmap to check.
 */
class CoreScreenSourceTest {

    private val core = FakeEmulatorCore()
    private val masks = mutableListOf<RecordingMask>()
    private val source = CoreScreenSource(core) { width, height ->
        RecordingMask(width, height).also { masks += it }
    }

    /** A phone in portrait, roughly: wide enough for the screen to be height-bound. */
    private fun layOut(width: Int = 1080, height: Int = 810) = source.resize(width, height)

    @Test
    fun theCoreRasterizesAtTheSizeItWillBeDrawnAt() {
        layOut(width = 1080, height = 810)

        val expected = fitCellSize(1080, 810)
        assertEquals(expected.cellWidth, core.cellWidth)
        assertEquals(expected.cellHeight, core.cellHeight)
        assertEquals(expected.cellWidth * SCREEN_COLUMNS, source.width)
        assertEquals(expected.cellHeight * SCREEN_ROWS, source.height)
    }

    /**
     * Layout happens constantly and almost never moves the cell, since its size
     * is rounded down to whole pixels. Rebuilding the bitmap for each one would
     * throw away the picture several times a second.
     */
    @Test
    fun aLayoutThatDoesNotMoveTheCellChangesNothing() {
        layOut(width = 1080, height = 810)
        val afterFirst = core.cellSizeChanges

        // Not enough to gain a whole pixel per cell in either direction.
        source.resize(1081, 811)
        source.resize(1080, 810)

        assertEquals(afterFirst, core.cellSizeChanges)
        assertEquals(1, masks.size)
    }

    @Test
    fun aRealResizeMakesAMaskOfTheNewSizeAndRedrawsEverything() {
        layOut(width = 1080, height = 810)
        source.refresh()
        val invalidatesBefore = core.invalidateCount

        source.resize(640, 480)

        assertEquals(2, masks.size)
        assertEquals(source.width, masks.last().width)
        assertEquals(source.height, masks.last().height)
        assertTrue(masks.first().closed, "The mask it replaced should have been released.")
        // The core only redraws what changed, and at a new size that is nothing
        // it drew before.
        assertEquals(invalidatesBefore + 1, core.invalidateCount)
        assertNull(source.image(), "The old picture is the wrong size now.")
    }

    /**
     * A machine can sit at a READY prompt for an hour. The core says nothing
     * moved and the frame ends there -- no copy out of it, no upload.
     */
    @Test
    fun anUnchangedScreenCostsNothing() {
        layOut()
        source.refresh()
        val drawn = masks.single().installs

        core.hasChanged = false

        assertEquals(false, source.refresh())
        assertEquals(drawn, masks.single().installs)
        assertNotNull(source.image(), "The last picture should still be there.")
    }

    /** Before the first layout there is still a machine to show. */
    @Test
    fun aFrameBeforeTheFirstLayoutIsRasterizedAtTheRomsOwnSize() {
        assertEquals(true, source.refresh())

        assertEquals(core.romCellWidth, core.cellWidth)
        assertEquals(core.romCellHeight, core.cellHeight)
        assertNotNull(source.image())
    }

    @Test
    fun theFrameIsTheCoresPixels() {
        layOut()
        core.fill = 7

        source.refresh()

        val mask = masks.single()
        assertEquals(source.width * source.height, mask.lastPixels?.size)
        assertTrue(mask.lastPixels?.all { it == 7.toByte() } == true)
    }

    /**
     * A screenshot of a screen nothing has been drawn on is a rectangle of
     * glass, and it would replace the one from the session before it.
     */
    @Test
    fun thereIsNoScreenshotUntilSomethingHasBeenDrawn() {
        assertNull(source.snapshot(Color.Green, Color.Black))

        layOut()
        assertNull(source.snapshot(Color.Green, Color.Black), "A mask, but no frame in it yet.")

        source.refresh()
        assertNotNull(source.snapshot(Color.Green, Color.Black))
    }

    /** The screenshot carries color; the drawn picture deliberately does not. */
    @Test
    fun theScreenshotIsColoredAndTheFrameIsNot() {
        layOut()
        source.refresh()

        source.snapshot(Color.Green, Color.Black)

        val mask = masks.single()
        assertEquals(Color.Green to Color.Black, mask.lastColors)
        assertSame(mask.image, source.image(), "The drawn frame should be the mask itself.")
    }
}

/** A [ScreenMask] that keeps what it was given instead of a bitmap. */
private class RecordingMask(val width: Int, val height: Int) : ScreenMask {

    val image: ImageBitmap = StubImage(width, height)
    var installs = 0
        private set
    var lastPixels: ByteArray? = null
        private set
    var lastColors: Pair<Color, Color>? = null
        private set
    var closed = false
        private set

    override fun install(pixels: ByteArray): ImageBitmap {
        installs++
        lastPixels = pixels.copyOf()
        return image
    }

    override fun colorized(
        pixels: ByteArray,
        characterColor: Color,
        screenColor: Color,
    ): ImageBitmap {
        lastColors = characterColor to screenColor
        return StubImage(width, height)
    }

    override fun close() {
        closed = true
    }
}

/**
 * An image with no pixels behind it.
 *
 * Nothing here draws, and a real [ImageBitmap] means a platform bitmap -- which
 * on the JVM side of these tests means the Android framework, which is not
 * there.
 */
private class StubImage(override val width: Int, override val height: Int) : ImageBitmap {
    override val config = ImageBitmapConfig.Alpha8
    override val colorSpace: ColorSpace = ColorSpaces.Srgb
    override val hasAlpha = true

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int,
    ) = Unit

    override fun prepareToDraw() = Unit
}
