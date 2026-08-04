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

package org.puder.trs80.shared.ui

import kotlinx.coroutines.test.currentTime
import org.puder.trs80.shared.SCREEN_COLUMNS
import org.puder.trs80.shared.SCREEN_ROWS
import org.puder.trs80.shared.ScreenBuffer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers what the tutorial types.
 *
 * The steps themselves are strings and the panel is a drawing; what can go
 * wrong and stay unnoticed is the typing -- a missing carriage return would
 * leave every command sitting on the prompt unexecuted, and a character the
 * machine has no key for would stop the line where it stood.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TutorialTest {

    private suspend fun typed(command: String): String {
        val sent = StringBuilder()
        typeCommand(command) { sent.append(it) }
        return sent.toString()
    }

    /**
     * The command goes to the machine whole, and ends with a carriage return.
     *
     * A carriage return rather than a newline: it is what the machine's keyboard
     * sends, and what the Paste control puts in for the same reason.
     */
    @Test
    fun theCommandIsTypedWholeAndEntered() = runTest {
        assertEquals("BASIC\r", typed("BASIC"))
    }

    /** Quotes and spaces go through as they are; it is what the machine is told. */
    @Test
    fun theCommandIsNotRewrittenOnItsWayThrough() = runTest {
        assertEquals("10 PRINT \"HELLO WORLD\"\r", typed("10 PRINT \"HELLO WORLD\""))
    }

    /** The one machine it is offered on, by where it came from rather than its name. */
    @Test
    fun theTourBelongsToTheRetroStoreTutorial() {
        assertEquals("2420f832-a7aa-11e7-8132-7343fef39a1f", TUTORIAL_APP_ID)
    }

    /** What the panel shows is the command, tidied of nothing but its edges. */
    @Test
    fun theCommandIsShownAsTheMachineGetsIt() {
        val step = TutorialStep("SAVE \"FIRST/BAS:1\"", "Save it to disk too.", listOf(">"))

        assertEquals("SAVE \"FIRST/BAS:1\"", step.asWritten())
    }

    /**
     * What the machine is saying is its last line with anything on it.
     *
     * Not any line: the screen still shows every command already typed, so a
     * prompt further up is history. Something looking anywhere would find the
     * BASIC prompt while a program was running and type over it.
     */
    @Test
    fun theMachineIsReadFromItsLastLine() {
        val screen = screenOf("LDOS Ready", "DIR :1", "FIRST/BAS", ">")

        assertEquals(">", lastScreenLine(screen))
    }

    /** LDOS centres its boot prompt; what is in video RAM is mostly spaces. */
    @Test
    fun aCentredPromptIsStillThePrompt() {
        val screen = screenOf("LDOS", "", "                       Mon, Aug 3, 1926", "                       Time ?")

        assertEquals("Time ?", lastScreenLine(screen))
    }

    @Test
    fun trailingBlankLinesAreNotWhereTheCursorIs() {
        val screen = screenOf("READY", ">", "", "")

        assertEquals(">", lastScreenLine(screen))
    }

    @Test
    fun aBusyMachineIsNotAtAnyPrompt() = runTest {
        val screen = screenOf("LDOS Ready", "BASIC", "LBASIC - Version 5.3.1", "HELLO WORLD")

        assertEquals(
            false,
            awaitPrompt(listOf(DOS_PROMPT, BASIC_PROMPT), { screen }, timeoutMillis = 600),
        )
    }

    @Test
    fun aPromptIsWaitedForRatherThanAssumed() = runTest {
        var lines = listOf("LDOS Ready", "BASIC", "LBASIC - Version 5.3.1")
        val start = currentTime

        // The prompt turns up while it is waiting, as it would on a real boot.
        val screen = object : ScreenBuffer {
            override fun get(index: Int): Byte {
                if (currentTime - start > 400) {
                    lines = listOf("READY", ">")
                }
                return screenOf(*lines.toTypedArray())[index]
            }
        }

        assertEquals(true, awaitPrompt(listOf(BASIC_PROMPT), { screen }))
        assertTrue(currentTime - start >= 400, "it should have waited for the prompt")
    }

    /**
     * The boot question is answered when it is on screen, and then the tour
     * waits for the prompt that follows it.
     *
     * Beginning the first command with a newline instead sent the answer and the
     * D of DIR a fifth of a second apart, and LDOS -- not listening yet -- ran
     * "IR :1" and said it had no such program.
     */
    @Test
    fun theBootQuestionIsAnsweredBeforeAnythingIsTyped() = runTest {
        var lines = listOf("LDOS", "                       Time ?")
        var answers = 0
        val screen = object : ScreenBuffer {
            override fun get(index: Int): Byte = screenOf(*lines.toTypedArray())[index]
        }

        val ready = awaitReady(
            listOf(DOS_PROMPT),
            { screen },
            answerBoot = {
                answers++
                // What answering it does, a moment later.
                lines = listOf("LDOS", "LDOS Ready")
            },
        )

        assertTrue(ready, "it should have got to the prompt")
        assertEquals(1, answers, "the question is answered once, not on every poll")
    }

    /** Nothing types at a machine that never gets anywhere. */
    @Test
    fun aMachineThatNeverArrivesIsGivenUpOn() = runTest {
        val screen = screenOf("LDOS", "                       Time ?")
        var answers = 0

        val ready = awaitReady(
            listOf(DOS_PROMPT),
            { screen },
            answerBoot = { answers++ },
            timeoutMillis = 900,
        )

        assertEquals(false, ready)
        assertEquals(1, answers)
    }

    /** A screen holding [lines], top-aligned, as video RAM would. */
    private fun screenOf(vararg lines: String): ScreenBuffer {
        val cells = ByteArray(SCREEN_COLUMNS * SCREEN_ROWS) { ' '.code.toByte() }
        for ((row, line) in lines.withIndex()) {
            for ((column, character) in line.withIndex()) {
                cells[row * SCREEN_COLUMNS + column] = character.code.toByte()
            }
        }
        return object : ScreenBuffer {
            override fun get(index: Int): Byte = cells[index]
        }
    }

    /**
     * Every key the tour types has to be one the machine has.
     *
     * Over the real commands, not a copy of them: a stray character in one --
     * the backslash that XML escaping put in front of every quote, for instance
     * -- is invisible until somebody watches all eight steps.
     */
    @Test
    fun everyKeyInEveryCommandExists() {
        assertEquals(8, tutorialCommands.size)
        for (command in tutorialCommands) {
            for (character in command) {
                assertNotNull(
                    trs80KeyForCharacter(character),
                    "the machine has no key for '$character' in \"$command\"",
                )
            }
        }
    }
}
