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

package org.puder.trs80.shared.io

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.WindowManager
import org.puder.trs80.shared.Log
import org.puder.trs80.shared.ui.Direction
import org.puder.trs80.shared.ui.TiltDirection

private const val TAG = "PlayerInput"

/**
 * How far a stick has to be pushed before it counts as a direction.
 *
 * A physical stick rarely rests at exactly zero, and a game steered by one that
 * never quite centers is unplayable.
 */
private const val DEAD_ZONE = 0.5f

/**
 * The gamepad that is listening, if one is.
 *
 * Android delivers controller input as activity events rather than as something
 * pollable, so the host hands them here and this is what they reach. One at a
 * time, because only one machine is ever on screen.
 */
private var listening: GamepadInput? = null

/**
 * Offers the host's key event to the gamepad, if one is listening.
 *
 * @return whether it was a controller's and has been dealt with. An event this
 * returns false for is somebody else's -- most importantly a real keyboard's,
 * whose arrow keys carry the same codes.
 */
fun dispatchGamepadKeyEvent(event: KeyEvent): Boolean =
    listening?.onKeyEvent(event) == true

/** Offers the host's motion event to the gamepad, if one is listening. */
fun dispatchGamepadMotionEvent(event: MotionEvent): Boolean =
    listening?.onMotionEvent(event) == true

actual class GamepadInput actual constructor(
    private val onDirection: (Direction) -> Unit,
    private val onFire: (Boolean) -> Unit,
) {
    private var pressed = Direction.NONE
    private var firing = false

    /** @return whether a controller is connected right now. */
    actual fun start(): Boolean {
        listening = this
        return InputDevice.getDeviceIds().any { id ->
            val sources = InputDevice.getDevice(id)?.sources ?: 0
            sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        }
    }

    actual fun stop() {
        if (listening === this) {
            listening = null
        }
        report(Direction.NONE)
        fire(false)
    }

    /**
     * A d-pad press, or a face button.
     *
     * Only from something that is actually a controller: a real keyboard sends
     * the same key codes for its arrow keys, and those belong to the machine's
     * own keyboard rather than to the pad.
     */
    internal fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.repeatCount != 0) {
            // A held key repeats; the machine already knows it is down.
            return isFromPad(event.source) && event.keyCode.isPadKey()
        }
        if (!isFromPad(event.source) || !event.keyCode.isPadKey()) {
            return false
        }
        val down = event.action == KeyEvent.ACTION_DOWN
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> report(pressed.copy(left = down))
            KeyEvent.KEYCODE_DPAD_RIGHT -> report(pressed.copy(right = down))
            KeyEvent.KEYCODE_DPAD_UP -> report(pressed.copy(up = down))
            KeyEvent.KEYCODE_DPAD_DOWN -> report(pressed.copy(down = down))
            else -> fire(down)
        }
        return true
    }

    /**
     * A stick, a hat switch, or the second stick -- whichever the pad reports.
     *
     * All three steer, because a player will reach for whatever their controller
     * has and none of them is wrong.
     */
    internal fun onMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK ||
            event.action != MotionEvent.ACTION_MOVE
        ) {
            return false
        }
        val x = axis(event, MotionEvent.AXIS_X)
            ?: axis(event, MotionEvent.AXIS_HAT_X)
            ?: axis(event, MotionEvent.AXIS_Z)
            ?: 0f
        val y = axis(event, MotionEvent.AXIS_Y)
            ?: axis(event, MotionEvent.AXIS_HAT_Y)
            ?: axis(event, MotionEvent.AXIS_RZ)
            ?: 0f
        // The stick's y grows downwards, and up is up.
        report(Direction.of(x, -y, DEAD_ZONE))
        return true
    }

    /** @return the axis's value, or null if it is centered or the pad has no such axis. */
    private fun axis(event: MotionEvent, axis: Int): Float? {
        val device = event.device ?: return null
        val range = device.getMotionRange(axis, event.source) ?: return null
        val value = event.getAxisValue(axis)
        // Inside the pad's own declared flat spot, so it is not being pushed.
        return value.takeIf { kotlin.math.abs(it) > range.flat }
    }

    private fun report(direction: Direction) {
        if (direction != pressed) {
            pressed = direction
            onDirection(direction)
        }
    }

    private fun fire(down: Boolean) {
        if (down != firing) {
            firing = down
            onFire(down)
        }
    }
}

private fun isFromPad(source: Int): Boolean =
    source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
        source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK ||
        source and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD

/** Whether this is a direction or a face button; every face button fires. */
private fun Int.isPadKey(): Boolean = when (this) {
    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BUTTON_A,
    KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X,
    KeyEvent.KEYCODE_BUTTON_Y -> true

    else -> false
}

/**
 * How the accelerometer's device axes map onto screen axes, per display
 * rotation: negate x, negate y, which reading is x, which reading is y.
 *
 * Taken from the old EmulatorActivity unchanged. The accelerometer reports in
 * the device's own frame, which does not turn when the screen does.
 */
private val AXIS_SWAP = arrayOf(
    intArrayOf(1, -1, 0, 1),  // ROTATION_0
    intArrayOf(-1, -1, 1, 0), // ROTATION_90
    intArrayOf(-1, 1, 0, 1),  // ROTATION_180
    intArrayOf(1, 1, 1, 0),   // ROTATION_270
)

actual class MotionInput actual constructor(private val onDirection: (Direction) -> Unit) {

    private val tilt = TiltDirection()
    private var manager: SensorManager? = null
    private var rotation = Surface.ROTATION_0

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val axes = AXIS_SWAP[rotation]
            onDirection(
                tilt.update(
                    x = axes[0] * event.values[axes[2]],
                    y = axes[1] * event.values[axes[3]],
                )
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /** @return whether there is an accelerometer to listen to. */
    actual fun start(): Boolean {
        val context = androidContext() ?: return false
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensors == null || accelerometer == null) {
            Log.i(TAG, "No accelerometer on this device.")
            return false
        }
        rotation = displayRotation(context)
        manager = sensors
        // SENSOR_DELAY_GAME, which is what this is: roughly a frame's worth of
        // readings, and the rate the old app steered by.
        return sensors.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    actual fun stop() {
        manager?.unregisterListener(listener)
        manager = null
        tilt.reset()
        onDirection(Direction.NONE)
    }
}

/**
 * Which way up the screen is.
 *
 * Read when the tilt starts rather than watched: the layout that steers by
 * tilting is rebuilt when the device turns, which stops and starts this.
 */
@Suppress("DEPRECATION")
private fun displayRotation(context: Context): Int {
    val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        // Null on a context with no display of its own, which the application
        // context is; the window manager below answers for those.
        context.display
    } else {
        null
    }
    val rotation = display?.rotation
        ?: (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            ?.defaultDisplay
            ?.rotation
    return rotation ?: Surface.ROTATION_0
}
