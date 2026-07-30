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
import android.graphics.Paint
import android.graphics.Rect
import android.os.Looper
import android.view.Choreographer
import android.view.SurfaceHolder
import org.puder.trs80.cast.RemoteCastScreen
import org.puder.trs80.cast.RemoteDisplayChannel
import org.puder.trs80.shared.DirtyRect
import org.puder.trs80.shared.ScreenBuffer

/**
 * Draws the emulated screen onto a [SurfaceHolder] or, while casting, into a
 * character buffer sent to the remote display.
 *
 * The frame loop is the hottest code in the app. It must not allocate: everything it needs
 * is set up by [setHardwareSpecs] before the thread is started.
 */
internal class RenderThread(private val isCasting: Boolean) : Thread() {

    private val screenBuffer: ScreenBuffer = XTRS.screenBuffer

    /** The core's rasterized screen, read straight out of its own memory. */
    private lateinit var pixelBuffer: java.nio.ByteBuffer

    /** Set once the frame loop's looper exists, so [quit] can stop it. */
    @Volatile
    private var looper: Looper? = null

    /**
     * The mask the core rasterizes into, as a bitmap the canvas can draw. `ALPHA_8`
     * because the emulated screen is monochrome: the bytes are coverage and the
     * colour comes from [maskPaint].
     */
    private var screenMask: Bitmap? = null

    /** The whole mask, and where on the surface it is stretched to. */
    private val maskBounds = Rect()
    private val screenBounds = Rect()

    /**
     * Filtering off, deliberately: this is pixel art, and the emulated picture is
     * scaled by whole multiples, so nearest-neighbour is both correct and crisper.
     */
    private val maskPaint = Paint().apply { isFilterBitmap = false }

    private var screenColor = 0


    /** Set to `false` to make the frame loop return after the current frame. */
    @Volatile
    var isRunning = true

    /** The surface to draw into, or `null` while there is none. */
    @Volatile
    var surfaceHolder: SurfaceHolder? = null

    private var model = Hardware.MODEL_NONE
    private var trsScreenCols = 0
    private var trsScreenRows = 0

    private lateinit var dirtyRect: DirtyRect
    private lateinit var screenCharBuffer: StringBuilder

    /** Adopts the geometry and font of [hardware]. Must be called before the thread starts. */
    fun setHardwareSpecs(hardware: Hardware) {
        val screenConfig = hardware.screenConfiguration
        model = hardware.model

        trsScreenCols = screenConfig.trsScreenCols
        trsScreenRows = screenConfig.trsScreenRows
        screenCharBuffer = StringBuilder(trsScreenCols * trsScreenRows + trsScreenRows)
        dirtyRect = DirtyRect(hardware.cellMetrics, screenBuffer)

        screenColor = hardware.screenColor
        maskPaint.color = hardware.characterColor
        // Rasterize at the size this screen is actually drawn at, so the mask is
        // blitted one-to-one. Scaling it here instead would land the ROM's
        // one-pixel stems on one or two output pixels depending where they fall,
        // which is what made the strokes uneven.
        XTRS.setCellSize(hardware.charWidth, hardware.charHeight)
        val maskWidth = XTRS.pixelWidth()
        val maskHeight = XTRS.pixelHeight()
        maskBounds.set(0, 0, maskWidth, maskHeight)
        screenBounds.set(0, 0, maskWidth, maskHeight)
        screenMask = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
        // Taken after the cell size is set: the buffer moves when it is resized.
        pixelBuffer = XTRS.pixelBuffer
        // The surface is new, so nothing the core drew before still applies.
        XTRS.invalidateRender()
    }

    /**
     * Samples and draws the screen, paced by the display rather than by a timer.
     *
     * The emulated machine has no frames: a program writes video RAM whenever it
     * likes and the picture follows immediately, so there is nothing to
     * synchronize with and the host simply samples at its own rate. The rate that
     * minimizes latency is the display's own, because anything else leaves two
     * unaligned waits in the path — one until the next sample, one until the next
     * refresh — which together jitter and beat against each other. A
     * [Choreographer] on this thread's own [Looper] removes the first wait by
     * phase-locking sampling to presentation, and follows the panel's real refresh
     * rate rather than a hard-coded 60.
     */
    override fun run() {
        Looper.prepare()
        looper = Looper.myLooper()
        val choreographer = Choreographer.getInstance()

        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isRunning) {
                    looper?.quitSafely()
                    return
                }
                // Queued before the work, so a slow frame delays the next one
                // rather than dropping the loop.
                choreographer.postFrameCallback(this)
                drawFrame()
            }
        }
        choreographer.postFrameCallback(frameCallback)
        Looper.loop()
    }

    /** Stops the frame loop. Safe to call from any thread. */
    fun quit() {
        isRunning = false
        looper?.quitSafely()
    }

    private fun drawFrame() {
        if (isCasting) {
            // The remote display takes characters rather than pixels, so casting
            // still samples the character buffer directly.
            val expandedMode = XTRS.isExpandedMode()
            val dirty = dirtyRect
            dirty.isExpandedMode = expandedMode
            dirty.computeDirtyRect()
            if (!dirty.isEmpty) {
                renderScreenToCast(RemoteCastScreen.get(), expandedMode)
            }
            return
        }

        drawFromCoreMask()
    }

    /**
     * Draws the mask the core rasterized: one tinted, scaled image.
     *
     * @return whether anything was drawn.
     */
    private fun drawFromCoreMask(): Boolean {
        // The core reports whether anything moved, so an idle screen costs
        // neither an upload nor a draw.
        if (!XTRS.render()) {
            return false
        }
        // Read the holder once; it is replaced from the UI thread.
        val holder = surfaceHolder ?: return false
        val surface = holder.surface
        if (surface == null || !surface.isValid) {
            return false
        }
        /*
         * A hardware canvas, so the GPU does the scale. lockCanvas() hands back a
         * software one, which made this a CPU upscale of the whole screen every
         * frame - measured at 7-11 ms, against 0.05 ms for getting the mask out of
         * the core. It also has to redraw the whole surface, which suits a
         * renderer that draws one image anyway.
         */
        val canvas = surface.lockHardwareCanvas() ?: return false
        try {
            blitScreen(canvas)
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
        return true
    }

    /**
     * Draws the emulated screen as a single tinted, scaled image.
     *
     * The core produces an 8-bit coverage mask at the character ROM's own
     * resolution, so this is one `drawBitmap` — and the colours are applied here
     * rather than baked into glyph bitmaps, which is why changing them no longer
     * means re-rasterizing anything.
     */
    private fun blitScreen(canvas: Canvas) {
        val mask = screenMask ?: return
        pixelBuffer.rewind()
        mask.copyPixelsFromBuffer(pixelBuffer)
        canvas.drawColor(screenColor)
        canvas.drawBitmap(mask, maskBounds, screenBounds, maskPaint)
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

    /**
     * Renders the whole emulated screen into a new bitmap.
     *
     * Forces a full rasterize first, because the core only redraws what changed
     * and the screenshot needs every cell present rather than just the ones that
     * moved since the last frame.
     */
    fun takeScreenshot(hardware: Hardware): Bitmap {
        val screenshot = Bitmap.createBitmap(
            hardware.screenWidth, hardware.screenHeight, Bitmap.Config.RGB_565
        )
        XTRS.invalidateRender()
        XTRS.render()
        blitScreen(Canvas(screenshot))
        return screenshot
    }

}
