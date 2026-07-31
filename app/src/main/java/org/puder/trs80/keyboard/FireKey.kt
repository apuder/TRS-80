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
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.puder.trs80.EmulatorActivity

/**
 * The fire button of the joystick and tilt keyboard layouts. It is wired to the space key, which
 * is what TRS-80 games use as their fire button.
 *
 * The hosting context must be an [EmulatorActivity].
 */
@SuppressLint("ClickableViewAccessibility")
class FireKey @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0) : View(context, attrs, defStyle) {

    private val keyboard = (context as EmulatorActivity).keyboardManager

    init {
        setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> keyboard.pressKeySpace()
                MotionEvent.ACTION_UP -> keyboard.unpressKeySpace()
            }
            true
        }
    }
}
