/*
 * Copyright 2012-2013, Arno Puder
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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.SurfaceHolder
import org.puder.trs80.cast.RemoteCastScreen
import org.puder.trs80.cast.RemoteDisplayChannel
import java.nio.ByteBuffer
import kotlin.math.floor

private const val TARGET_FPS = 60L

/**
 * Draws the emulated screen at [TARGET_FPS], either onto a [SurfaceHolder] or, while
 * casting, into a character buffer sent to the remote display.
 *
 * The frame loop is the hottest code in the app. It must not allocate: everything it needs
 * is set up by [setHardwareSpecs] before the thread is started.
 */
internal class RenderThread(private val isCasting: Boolean) : Thread() {

    private val fpsLimiter = FpsLimiter(TARGET_FPS)
    private val screenBuffer: ByteBuffer = XTRS.screenBuffer

    /** Set to `false` to make the frame loop return after the current frame. */
    @Volatile
    var isRunning = true

    /** The surface to draw into, or `null` while there is none. */
    @Volatile
    var surfaceHolder: SurfaceHolder? = null

    private var model = Hardware.MODEL_NONE
    private var font: Array<Bitmap?> = emptyArray()
    private var trsScreenCols = 0
    private var trsScreenRows = 0
    private var trsCharWidth = 0
    private var trsCharHeight = 0

    private lateinit var dirtyRect: DirtyRect
    private lateinit var screenCharBuffer: StringBuilder

    /** Adopts the geometry and font of [hardware]. Must be called before the thread starts. */
    fun setHardwareSpecs(hardware: Hardware) {
        val screenConfig = hardware.screenConfiguration
        model = hardware.model
        font = hardware.font
        trsScreenCols = screenConfig.trsScreenCols
        trsScreenRows = screenConfig.trsScreenRows
        trsCharWidth = hardware.charWidth
        trsCharHeight = hardware.charHeight
        screenCharBuffer = StringBuilder(trsScreenCols * trsScreenRows + trsScreenRows)
        dirtyRect = DirtyRect(hardware, screenBuffer)
    }

    override fun run() {
        while (isRunning) {
            try {
                fpsLimiter.onFrame()
            } catch (e: InterruptedException) {
                break
            }

            val expandedMode = XTRS.isExpandedMode()
            val dirty = dirtyRect
            dirty.isExpandedMode = expandedMode
            dirty.computeDirtyRect()
            if (dirty.isEmpty) {
                // Nothing to update.
                continue
            }

            if (isCasting) {
                renderScreenToCast(RemoteCastScreen.get(), expandedMode)
                continue
            }

            // Read the holder once; it is replaced from the UI thread.
            val holder = surfaceHolder ?: continue
            val canvas = holder.lockCanvas(dirty.clipRect) ?: continue
            dirty.adjustClipRect()
            renderScreenToCanvas(canvas, expandedMode)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    /** Renders the current dirty region of the emulated screen into [canvas]. */
    private fun renderScreenToCanvas(canvas: Canvas, expandedMode: Boolean) {
        if (expandedMode) {
            canvas.scale(2f, 1f)
        }
        val step = if (expandedMode) 2 else 1
        val dirty = dirtyRect
        val glyphs = font
        val isModel1 = model == Hardware.MODEL1
        for (row in dirty.top..dirty.bottom) {
            val rowStart = row * trsScreenCols
            val y = (trsCharHeight * row).toFloat()
            for (col in dirty.left..dirty.right) {
                var ch = screenBuffer[rowStart + col * step].toInt() and 0xff
                // Emulate Radio Shack lowercase mod (for Model 1).
                if (isModel1 && ch < 0x20) {
                    ch += 0x40
                }
                val glyph = glyphs[ch] ?: continue
                canvas.drawBitmap(glyph, (trsCharWidth * col).toFloat(), y, null)
            }
        }
    }

    /**
     * Sends the emulated screen to [remoteDisplay] as text, one '|'-separated row per screen
     * row. Rows outside the dirty region are sent empty.
     */
    private fun renderScreenToCast(remoteDisplay: RemoteDisplayChannel, expandedMode: Boolean) {
        val step = if (expandedMode) 2 else 1
        val dirty = dirtyRect
        val chars = screenCharBuffer
        val isModel1 = model == Hardware.MODEL1
        val cols = trsScreenCols / step

        var i = 0
        chars.setLength(0)
        for (row in 0 until trsScreenRows) {
            if (row != 0) {
                chars.append('|')
            }
            if (row < dirty.top || row > dirty.bottom) {
                i += trsScreenCols
                continue
            }
            for (col in 0 until cols) {
                var ch = screenBuffer[i].toInt() and 0xff
                // Emulate Radio Shack lowercase mod (for Model 1).
                if (isModel1 && ch < 0x20) {
                    ch += 0x40
                }

                // TODO: Choose encoding based on current model.
                chars.append(CharMapping.m3toUnicode[ch])
                i += step
            }
        }
        remoteDisplay.sendScreenBuffer(expandedMode, chars.toString())
    }

    /** Renders the whole emulated screen into a new bitmap. */
    fun takeScreenshot(hardware: Hardware): Bitmap {
        val screenshot = Bitmap.createBitmap(
            hardware.screenWidth, hardware.screenHeight, Bitmap.Config.RGB_565
        )
        val expandedMode = XTRS.isExpandedMode()
        dirtyRect.isExpandedMode = expandedMode
        dirtyRect.reset()
        renderScreenToCanvas(Canvas(screenshot), expandedMode)
        return screenshot
    }

    /** Encapsulated logic to limit the frame rate to the given FPS. */
    private class FpsLimiter(fps: Long) {
        private val frameTimeMillis = floor(1000.0 / fps).toLong()
        private var lastFrameTime = 0L

        /** Call once per frame-loop iteration; waits out the rest of the frame budget. */
        fun onFrame() {
            val now = System.currentTimeMillis()
            val waitFor = (lastFrameTime + frameTimeMillis - now).coerceAtLeast(0L)
            Thread.sleep(waitFor)
            lastFrameTime = now
        }
    }
}
