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

import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.atan

/** The stick is divided into eight equal slices, one per direction. */
private const val SLICE = 360f / 8

/** Half a slice, so that the first slice is centred on the positive x axis. */
private const val START = SLICE / 2

/**
 * Decodes the input of an attached game controller: d-pad key events, and the analogue sticks
 * and hat switch of joystick motion events. Both are reduced to the eight directions plus the
 * centre button, and only the *changes* are reported to [listener].
 */
class GameController(private val listener: GameControllerListener) {

    /** One direction, or the centre button, going down or coming back up. */
    enum class Action {
        LEFT_DOWN, TOP_DOWN, RIGHT_DOWN, BOTTOM_DOWN, CENTER_DOWN,
        LEFT_UP, TOP_UP, RIGHT_UP, BOTTOM_UP, CENTER_UP
    }

    private var leftKeyPressed = false
    private var rightKeyPressed = false
    private var upKeyPressed = false
    private var downKeyPressed = false

    /**
     * Turns a d-pad key event into an [Action].
     *
     * @return Whether the event was a d-pad direction and has been consumed.
     */
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isDpadDevice(event) || event.repeatCount != 0) {
            return false
        }
        val isDown = event.action == KeyEvent.ACTION_DOWN
        val action = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> if (isDown) Action.LEFT_DOWN else Action.LEFT_UP
            KeyEvent.KEYCODE_DPAD_UP -> if (isDown) Action.TOP_DOWN else Action.TOP_UP
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (isDown) Action.RIGHT_DOWN else Action.RIGHT_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> if (isDown) Action.BOTTOM_DOWN else Action.BOTTOM_UP
            KeyEvent.KEYCODE_DPAD_CENTER -> if (isDown) Action.CENTER_DOWN else Action.CENTER_UP
            else -> return false
        }
        listener.onGameControllerAction(action)
        return true
    }

    /**
     * Turns a joystick motion event, including the samples batched into its history, into
     * [Action]s.
     *
     * @return Whether the event came from a joystick and has been consumed.
     */
    fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK ||
            event.action != MotionEvent.ACTION_MOVE
        ) {
            return false
        }
        for (historyPos in 0 until event.historySize) {
            processJoystickInput(event, historyPos)
        }
        processJoystickInput(event, -1)
        return true
    }

    /**
     * Reads the stick position out of [event], preferring the left stick over the hat switch
     * over the right stick, and presses the keys it points at.
     *
     * @param historyPos the index into the event's batched history, or -1 for its current value.
     */
    private fun processJoystickInput(event: MotionEvent, historyPos: Int) {
        val device = event.device

        var x = getCenteredAxis(event, device, MotionEvent.AXIS_X, historyPos)
        if (x == 0f) {
            x = getCenteredAxis(event, device, MotionEvent.AXIS_HAT_X, historyPos)
        }
        if (x == 0f) {
            x = getCenteredAxis(event, device, MotionEvent.AXIS_Z, historyPos)
        }

        var y = getCenteredAxis(event, device, MotionEvent.AXIS_Y, historyPos)
        if (y == 0f) {
            y = getCenteredAxis(event, device, MotionEvent.AXIS_HAT_Y, historyPos)
        }
        if (y == 0f) {
            y = getCenteredAxis(event, device, MotionEvent.AXIS_RZ, historyPos)
        }

        // The screen's y axis grows downwards, the stick's grows upwards.
        pressKeys(x, -y)
    }

    /**
     * Maps the stick offset onto the four direction keys. [dx] grows to the right and [dy] grows
     * upwards; the angle between them is quantised into eight 45 degree slices, the four
     * diagonal ones pressing two keys at once.
     */
    private fun pressKeys(dx: Float, dy: Float) {
        if (dx == 0f && dy == 0f) {
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
     * Brings the four direction keys into the requested state, reporting an [Action] only for
     * those that actually change.
     */
    private fun setKeys(right: Boolean, left: Boolean, up: Boolean, down: Boolean) {
        if (rightKeyPressed != right) {
            rightKeyPressed = right
            listener.onGameControllerAction(if (right) Action.RIGHT_DOWN else Action.RIGHT_UP)
        }
        if (leftKeyPressed != left) {
            leftKeyPressed = left
            listener.onGameControllerAction(if (left) Action.LEFT_DOWN else Action.LEFT_UP)
        }
        if (upKeyPressed != up) {
            upKeyPressed = up
            listener.onGameControllerAction(if (up) Action.TOP_DOWN else Action.TOP_UP)
        }
        if (downKeyPressed != down) {
            downKeyPressed = down
            listener.onGameControllerAction(if (down) Action.BOTTOM_DOWN else Action.BOTTOM_UP)
        }
    }

    private fun unpressAllKeys() {
        if (leftKeyPressed) {
            leftKeyPressed = false
            listener.onGameControllerAction(Action.LEFT_UP)
        }
        if (rightKeyPressed) {
            rightKeyPressed = false
            listener.onGameControllerAction(Action.RIGHT_UP)
        }
        if (upKeyPressed) {
            upKeyPressed = false
            listener.onGameControllerAction(Action.TOP_UP)
        }
        if (downKeyPressed) {
            downKeyPressed = false
            listener.onGameControllerAction(Action.BOTTOM_UP)
        }
    }
}

/**
 * Note the inverted comparison: this reports `true` for events that did *not* come from a d-pad
 * source. It is kept verbatim from the Android game controller sample it was taken from, and
 * [GameController.dispatchKeyEvent] relies on it as written.
 */
private fun isDpadDevice(event: InputEvent): Boolean =
    (event.source and InputDevice.SOURCE_DPAD) != InputDevice.SOURCE_DPAD

/**
 * @return The value of [axis], or 0 if it is inside the device's flat (dead) zone or the device
 * does not report that axis at all.
 */
private fun getCenteredAxis(
    event: MotionEvent,
    device: InputDevice,
    axis: Int,
    historyPos: Int
): Float {
    val range = device.getMotionRange(axis, event.source) ?: return 0f
    val value = if (historyPos < 0) {
        event.getAxisValue(axis)
    } else {
        event.getHistoricalAxisValue(axis, historyPos)
    }
    return if (abs(value) > range.flat) value else 0f
}

/** @return The angle of ([dx], [dy]) in degrees, counter-clockwise from the positive x axis. */
private fun computeAngle(dx: Float, dy: Float): Float {
    if (dx == 0f) {
        return if (dy >= 0) 90f else 270f
    }
    if (dy == 0f) {
        return if (dx >= 0) 0f else 180f
    }
    val atan = Math.toDegrees(atan((dy / dx).toDouble())).toFloat()
    return when {
        dx < 0 -> 180 + atan
        dy < 0 -> 360 + atan
        else -> atan
    }
}
