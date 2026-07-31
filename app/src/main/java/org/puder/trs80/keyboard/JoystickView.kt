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

package org.puder.trs80.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.puder.trs80.EmulatorActivity
import kotlin.math.atan
import kotlin.math.sqrt

/**
 * An eight-way on-screen joystick that drives the TRS-80 cursor keys.
 *
 * The circle around the centre is a dead zone; outside of it the angle towards the touch point
 * is quantised into eight 45 degree slices, the four diagonal ones pressing two cursor keys at
 * once. The hosting context must be an [EmulatorActivity].
 *
 * See http://www.trs-80.com/wordpress/zaps-patches-pokes-tips/internals/#keyboard13
 */
@SuppressLint("ClickableViewAccessibility")
class JoystickView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0) : View(context, attrs, defStyle) {

    private val keyboardManager = (context as EmulatorActivity).keyboardManager
    private val paint = Paint().apply { isAntiAlias = true }

    private val radiusButton = pxFromDp(40f)
    private val triangleSideLength = pxFromDp(15f)

    private var leftKeyPressed = false
    private var rightKeyPressed = false
    private var upKeyPressed = false
    private var downKeyPressed = false

    private var joystickX = -1f
    private var joystickY = -1f
    private var joystickIsPressed = false

    private var radiusJoystick = 0f
    private var joystickBitmap: Bitmap? = null

    init {
        setOnTouchListener { _, event ->
            handleTouch(event)
            true
        }
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2.0f
        val cy = height / 2.0f
        if (joystickX == -1f) {
            joystickX = cx
        }
        if (joystickY == -1f) {
            joystickY = cy
        }
        val bitmap = obtainJoystickBitmap()

        paint.color = if (joystickIsPressed) Color.LTGRAY else Color.GRAY
        paint.alpha = 100
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        val x = joystickX - cx
        val y = joystickY - cy
        if (sqrt((x * x + y * y).toDouble()) > radiusJoystick) {
            // Outside the ring: pin the knob to the ring, in the direction of the touch.
            canvas.translate(cx, cy)
            val angle = computeAngle((joystickX - cx).toDouble(), (cy - joystickY).toDouble())
            canvas.rotate(-angle.toFloat())
            canvas.drawCircle(radiusJoystick, 0f, radiusButton, paint)
        } else {
            canvas.drawCircle(joystickX, joystickY, radiusButton, paint)
        }
    }

    private fun handleTouch(event: MotionEvent) {
        val action = event.action and MotionEvent.ACTION_MASK
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            unpressAllKeys()
            joystickX = -1f
            joystickY = -1f
            joystickIsPressed = false
            invalidate()
            return
        }

        joystickIsPressed = true
        joystickX = event.x
        joystickY = event.y
        invalidate()

        val cx = width / 2.0f
        val cy = height / 2.0f
        pressKeys((joystickX - cx).toDouble(), (cy - joystickY).toDouble())
    }

    /**
     * Maps the offset of the touch point from the centre onto the cursor keys. [dx] grows to the
     * right and [dy] grows upwards, both in pixels.
     */
    private fun pressKeys(dx: Double, dy: Double) {
        if (sqrt(dx * dx + dy * dy) < radiusButton) {
            // Inside the dead zone around the centre.
            unpressAllKeys()
            return
        }

        val angle = computeAngle(dx, dy)
        when {
            angle < START || angle >= START + 7 * SLICE ->
                setKeys(right = true, left = false, up = false, down = false)

            angle < START + SLICE ->
                setKeys(right = true, left = false, up = true, down = false)

            angle < START + 2 * SLICE ->
                setKeys(right = false, left = false, up = true, down = false)

            angle < START + 3 * SLICE ->
                setKeys(right = false, left = true, up = true, down = false)

            angle < START + 4 * SLICE ->
                setKeys(right = false, left = true, up = false, down = false)

            angle < START + 5 * SLICE ->
                setKeys(right = false, left = true, up = false, down = true)

            angle < START + 6 * SLICE ->
                setKeys(right = false, left = false, up = false, down = true)

            else ->
                setKeys(right = true, left = false, up = false, down = true)
        }
    }

    /**
     * Brings the four cursor keys into the requested state, sending a key event only for those
     * that actually change.
     */
    private fun setKeys(right: Boolean, left: Boolean, up: Boolean, down: Boolean) {
        if (rightKeyPressed != right) {
            rightKeyPressed = right
            if (right) keyboardManager.pressKeyRight() else keyboardManager.unpressKeyRight()
        }
        if (leftKeyPressed != left) {
            leftKeyPressed = left
            if (left) keyboardManager.pressKeyLeft() else keyboardManager.unpressKeyLeft()
        }
        if (upKeyPressed != up) {
            upKeyPressed = up
            if (up) keyboardManager.pressKeyUp() else keyboardManager.unpressKeyUp()
        }
        if (downKeyPressed != down) {
            downKeyPressed = down
            if (down) keyboardManager.pressKeyDown() else keyboardManager.unpressKeyDown()
        }
    }

    private fun unpressAllKeys() {
        if (leftKeyPressed) {
            leftKeyPressed = false
            keyboardManager.unpressKeyLeft()
        }
        if (rightKeyPressed) {
            rightKeyPressed = false
            keyboardManager.unpressKeyRight()
        }
        if (upKeyPressed) {
            upKeyPressed = false
            keyboardManager.unpressKeyUp()
        }
        if (downKeyPressed) {
            downKeyPressed = false
            keyboardManager.unpressKeyDown()
        }
    }

    /** @return The angle of ([dx], [dy]) in degrees, counter-clockwise from the positive x axis. */
    private fun computeAngle(dx: Double, dy: Double): Double {
        if (dx == 0.0) {
            return if (dy >= 0) 90.0 else 270.0
        }
        val atan = Math.toDegrees(atan(dy / dx))
        return when {
            // Note that dy == 0.0 with dx < 0 deliberately falls through to `atan`, i.e. reports
            // 0 rather than 180 degrees. Preserved from the original implementation.
            dx < 0 && dy != 0.0 -> 180 + atan
            dx > 0 && dy < 0 -> 360 + atan
            else -> atan
        }
    }

    /** @return The cached backdrop bitmap, redrawing it if the view has been resized. */
    private fun obtainJoystickBitmap(): Bitmap {
        joystickBitmap?.let {
            if (it.width == width && it.height == height) {
                return it
            }
        }
        return createJoystickBitmap().also { joystickBitmap = it }
    }

    /** Draws the ring, the dead zone circle and the eight direction arrows. */
    private fun createJoystickBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        radiusJoystick = (width - CIRCLE_STROKE_WIDTH) / 2 - radiusButton
        val cx = width / 2.0f
        val cy = height / 2.0f

        val bitmapPaint = Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = CIRCLE_STROKE_WIDTH
        }
        canvas.drawCircle(cx, cy, radiusJoystick, bitmapPaint)
        bitmapPaint.strokeWidth = 1f
        canvas.drawCircle(cx, cy, radiusButton, bitmapPaint)

        bitmapPaint.style = Paint.Style.FILL_AND_STROKE
        canvas.save()
        canvas.translate(cx, cy)
        val arrow = Path().apply {
            moveTo(radiusJoystick, -triangleSideLength / 2.0f)
            lineTo(radiusJoystick, triangleSideLength / 2.0f)
            lineTo(radiusJoystick + triangleSideLength * 0.667f, 0f)
            close()
        }
        repeat(8) {
            canvas.drawPath(arrow, bitmapPaint)
            canvas.rotate(45f)
        }
        canvas.restore()
        return bitmap
    }

    private fun pxFromDp(dp: Float): Float = dp * context.resources.displayMetrics.density

    private companion object {
        private const val CIRCLE_STROKE_WIDTH = 10f

        /** The joystick is divided into eight equal slices, one per direction. */
        private const val SLICE = 45.0

        /** Half a slice, so that the first slice is centred on the positive x axis. */
        private const val START = SLICE / 2
    }
}
