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

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import org.puder.trs80.keyboard.KeyboardManager

/** The delay between two injected key strokes, in milliseconds. */
private const val KEY_DELAY = 180L

/** The delay a [DELAY_CHAR] in a command stands for, in milliseconds. */
private const val TUTORIAL_DELAY = 1000L

/** Placeholder inside a command that pauses instead of typing a character. */
private const val DELAY_CHAR = '_'

/**
 * One step of the tutorial: a command that is typed for the user, and what it does.
 *
 * @param postCommandDelay how long to leave the command's output on screen, in milliseconds.
 */
private class Step(commandId: Int, descriptionId: Int, val postCommandDelay: Long) {
    val command: String = TRS80Application.getAppContext().getString(commandId)
    val description: String = TRS80Application.getAppContext().getString(descriptionId)
}

private val STEPS = arrayOf(
    Step(R.string.tutorial_step_1_cmd, R.string.tutorial_step_1, 1000),
    Step(R.string.tutorial_step_2_cmd, R.string.tutorial_step_2, 1000),
    Step(R.string.tutorial_step_3_cmd, R.string.tutorial_step_3, 100),
    Step(R.string.tutorial_step_4_cmd, R.string.tutorial_step_4, 100),
    Step(R.string.tutorial_step_5_cmd, R.string.tutorial_step_5, 3500),
    Step(R.string.tutorial_step_6_cmd, R.string.tutorial_step_6, 1800),
    Step(R.string.tutorial_step_7_cmd, R.string.tutorial_step_7, 500),
    Step(R.string.tutorial_step_8_cmd, R.string.tutorial_step_8, 0)
)

/**
 * Walks the user through a handful of TRS-80 commands by typing them into the emulator one key
 * at a time. The on-screen keyboard is hidden for the duration and restored when the tutorial
 * ends or is cancelled.
 *
 * Constructing a tutorial resets the emulated machine and rewinds its cassette; call [show] to
 * start it.
 *
 * @param root the emulator's root view, which has to contain the tutorial overlay.
 */
class Tutorial(
    private val keyboardManager: KeyboardManager,
    root: View
) : View.OnClickListener, Runnable {

    private val tutorialRoot: View = root.findViewById(R.id.tutorial)
    private val keyboardRoot: View = root.findViewById(R.id.keyboard_container)
    private val keyboardSwitchView: View = root.findViewById(R.id.switch_keyboard)
    private val nextButton: Button = tutorialRoot.findViewById(R.id.tutorial_next)
    private val command: TextView = tutorialRoot.findViewById(R.id.tutorial_command)
    private val description: TextView = tutorialRoot.findViewById(R.id.tutorial_description)

    private lateinit var currentStep: Step
    private var currentCommand = ""
    private var currentKeyStroke = 0
    private var nextCommand = 0

    init {
        XTRS.reset()
        XTRS.rewindCassette()
        keyboardRoot.visibility = View.GONE
        keyboardSwitchView.visibility = View.GONE
        nextButton.setOnClickListener(this)
        tutorialRoot.findViewById<ImageView>(R.id.tutorial_cancel).setOnClickListener(this)
        command.typeface = Fonts.getTypeface(Hardware.MODEL3)
    }

    /** Shows the first step. */
    fun show() = showNextCommand()

    override fun onClick(v: View) {
        tutorialRoot.visibility = View.GONE
        if (v.id == R.id.tutorial_next) {
            tutorialRoot.postDelayed(this, KEY_DELAY)
        } else {
            showKeyboard()
        }
    }

    /** Types the next character of the current command, or moves on to the next step. */
    override fun run() {
        if (currentKeyStroke == currentCommand.length) {
            tutorialRoot.postDelayed({ showNextCommand() }, currentStep.postCommandDelay)
            return
        }
        val ch = currentCommand[currentKeyStroke++]
        if (ch == DELAY_CHAR) {
            tutorialRoot.postDelayed(this, TUTORIAL_DELAY)
            return
        }
        keyboardManager.injectKey(ch)
        tutorialRoot.postDelayed(this, KEY_DELAY)
    }

    private fun showNextCommand() {
        if (nextCommand >= STEPS.size) {
            showKeyboard()
            return
        }
        tutorialRoot.visibility = View.VISIBLE
        val step = STEPS[nextCommand++]
        currentStep = step
        nextButton.text = TRS80Application.getAppContext()
            .getString(R.string.tutorial_next, nextCommand, STEPS.size)
        command.text = step.command.replace(DELAY_CHAR.toString(), "")
        description.text = step.description
        currentCommand = step.command + "\n"
        currentKeyStroke = 0
    }

    private fun showKeyboard() {
        keyboardSwitchView.visibility = View.VISIBLE
        keyboardRoot.visibility = View.VISIBLE
        keyboardRoot.requestLayout()
        keyboardRoot.invalidate()
    }
}
