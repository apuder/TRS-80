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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.puder.trs80.shared.SCREEN_COLUMNS
import org.puder.trs80.shared.SCREEN_ROWS
import org.puder.trs80.shared.ScreenBuffer
import org.jetbrains.compose.resources.stringResource
import org.puder.trs80.shared.ui.theme.Hairline
import org.puder.trs80.shared.ui.theme.ModalPanel
import org.puder.trs80.shared.ui.theme.SectionKicker
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.TextAction
import org.puder.trs80.shared.ui.theme.Trs80Theme
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.cancel
import trs_80.shared.generated.resources.tutorial
import trs_80.shared.generated.resources.tutorial_next
import trs_80.shared.generated.resources.tutorial_step_1
import trs_80.shared.generated.resources.tutorial_step_2
import trs_80.shared.generated.resources.tutorial_step_3
import trs_80.shared.generated.resources.tutorial_step_4
import trs_80.shared.generated.resources.tutorial_step_5
import trs_80.shared.generated.resources.tutorial_step_6
import trs_80.shared.generated.resources.tutorial_step_7
import trs_80.shared.generated.resources.tutorial_step_8

/**
 * The guided tour of a few TRS-80 commands, typed into the machine for the user.
 *
 * It only makes sense on one machine. Every command below assumes what the
 * RetroStore's "TRS-80 Tutorial" program puts on its two disks — a DOS on the
 * first and a formatted disk in the second drive — so it is offered on that
 * machine and nowhere else. The old Android app decided that by the
 * configuration's *name*, which a user is free to change; this goes by where the
 * machine came from, which they are not.
 */
const val TUTORIAL_APP_ID = "2420f832-a7aa-11e7-8132-7343fef39a1f"

/**
 * What the tour types, in order, and what the machine has to be saying first.
 *
 * The second half is what keeps it honest. A command typed at the wrong moment
 * is not a command -- BASIC's SAVE typed at the DOS prompt is an error, and a
 * DIR typed while a program is running is keystrokes into a game. So each step
 * names the prompt it belongs at, and nothing is typed until the machine is
 * sitting at it.
 *
 * Nothing here answers LDOS's "Time ?" -- see [awaitReady], which does it when
 * it meets it. Making it part of the first command is what put the D of DIR
 * into a machine that was still starting up, and left it running "IR :1".
 *
 * In code rather than in the resource file. None of it is translatable -- it is
 * what the machine is told, character for character -- and going through XML
 * cost a visible backslash in front of every quote, which is the sort of thing
 * only a screenshot catches.
 */
private val COMMANDS = listOf(
    "DIR :1" to listOf(DOS_PROMPT),
    "BASIC" to listOf(DOS_PROMPT),
    "10 PRINT \"HELLO WORLD\"" to listOf(BASIC_PROMPT),
    "RUN" to listOf(BASIC_PROMPT),
    "CSAVE \"FIRST/BAS\"" to listOf(BASIC_PROMPT),
    "SAVE \"FIRST/BAS:1\"" to listOf(BASIC_PROMPT),
    "CMD \"S\"" to listOf(BASIC_PROMPT),
    "DIR :1" to listOf(DOS_PROMPT),
)

/** The commands alone, for anything that wants to check them. */
val tutorialCommands: List<String> get() = COMMANDS.map { it.first }

/**
 * What ends a line, as the machine's keyboard would have sent it.
 *
 * A carriage return, not a newline: the same thing the Paste control puts in
 * when it takes text off the clipboard.
 */
const val ENTER_KEY = "\r"

/** What LDOS asks on the way up, before it will do anything else. */
const val BOOT_PROMPT = "Time ?"

/** Where DOS commands belong. */
const val DOS_PROMPT = "LDOS Ready"

/** Where BASIC commands belong. */
const val BASIC_PROMPT = ">"

/** How often the machine's screen is read while the tour waits for a prompt. */
private const val POLL_MILLIS = 150L

/** How long to wait for one. Generous: the first step waits for a disk to boot. */
private const val WAIT_MILLIS = 25_000L

/**
 * One step: a command that gets typed, what it is for, and where it belongs.
 *
 * @property awaits the prompts this command may be typed at. The machine has to
 * be showing one of them, and nothing else, before a key is sent.
 */
data class TutorialStep(
    val command: String,
    val description: String,
    val awaits: List<String>,
)

/** The eight steps, in order: what each types, and what it is for. */
@Composable
fun tutorialSteps(): List<TutorialStep> {
    val descriptions = listOf(
        stringResource(Res.string.tutorial_step_1),
        stringResource(Res.string.tutorial_step_2),
        stringResource(Res.string.tutorial_step_3),
        stringResource(Res.string.tutorial_step_4),
        stringResource(Res.string.tutorial_step_5),
        stringResource(Res.string.tutorial_step_6),
        stringResource(Res.string.tutorial_step_7),
        stringResource(Res.string.tutorial_step_8),
    )
    return COMMANDS.mapIndexed { index, (command, awaits) ->
        TutorialStep(command, descriptions[index], awaits)
    }
}

/** What a command looks like written down. */
fun TutorialStep.asWritten(): String = command.trim()

/**
 * Types [command] at the machine and enters it.
 *
 * Through the machine's own typing -- the core's paste -- and not by pressing
 * keys. Pressing keys means sending a key down and, a tenth of a second later,
 * a key up. Both are events on our clock; the machine applies them on its own,
 * one per timer tick and only while the host is keeping up, and the moment those
 * two drift apart the machine is looking at a key that is being held. It repeats
 * it. That is what filled a line with `HEEEEEEEEEEEE` instead of HELLO WORLD,
 * intermittently, and it is not fixable from this side: the release was sent
 * every time and logged going out.
 *
 * The paste path has no clock of ours in it. It sends the next character only
 * when the program is actually waiting for one, and it releases each key itself
 * a tick later, inside the emulator. The whole command goes in one go, which is
 * also why there are no pauses written into the commands any more: those were
 * there to let the command before this one finish, and waiting for the machine
 * to be ready is now the mechanism rather than something timed around it.
 *
 * @param type sends text to the machine as though it had been typed.
 */
suspend fun typeCommand(command: String, type: suspend (String) -> Unit) {
    type(command + ENTER_KEY)
}

/**
 * How long the machine is left alone after a command, before the next panel.
 *
 * The tour is about what the machine does, and the panel covers the machine. A
 * DIR that has just listed a disk, or a RUN that has just printed HELLO WORLD,
 * is the whole point of the step it belongs to -- so it gets a couple of
 * seconds on its own before something is put in front of it.
 */
private const val SETTLE_MILLIS = 2_000L

/** @see SETTLE_MILLIS */
val tutorialSettle: Long get() = SETTLE_MILLIS

/**
 * How long the first command waits before deciding the machine is not where
 * the tour begins but somewhere else entirely.
 *
 * Much shorter than [WAIT_MILLIS], because it is not waiting for a slow
 * machine: a cold boot answers the time and reaches the prompt in about two
 * seconds. Longer than that means the machine was resumed in the middle of
 * something -- sitting in BASIC, or running a game -- and no amount of further
 * waiting will move it, so the tour stops waiting and restarts the machine
 * instead.
 */
private const val READY_MILLIS = 8_000L

/** @see READY_MILLIS */
val tutorialReadyWait: Long get() = READY_MILLIS

/**
 * What the machine is saying: the last line of its screen with anything on it.
 *
 * The last line is the one that matters, because it is where the cursor is. A
 * prompt further up is history -- the screen still shows every command already
 * typed, so anything that went looking for "&gt;" anywhere would find one
 * immediately and type over a program that was still running.
 *
 * Trimmed at both ends, not just the right. LDOS centres its boot prompt: what
 * is in video RAM is twenty-three spaces and then "Time ?", and comparing that
 * to "Time ?" is how the first attempt at this waited twenty-five seconds and
 * gave up.
 */
fun lastScreenLine(screen: ScreenBuffer): String {
    for (row in SCREEN_ROWS - 1 downTo 0) {
        val line = buildString {
            for (column in 0 until SCREEN_COLUMNS) {
                val code = screen[row * SCREEN_COLUMNS + column].toInt() and 0xff
                append(if (code in 0x20..0x7e) code.toChar() else ' ')
            }
        }.trim()
        if (line.isNotBlank()) {
            return line
        }
    }
    return ""
}

/**
 * Waits until the machine is sitting at one of [prompts].
 *
 * @return whether it got there. False means the tour should stop rather than
 * type into whatever is actually on screen.
 */
suspend fun awaitPrompt(
    prompts: List<String>,
    screen: () -> ScreenBuffer,
    timeoutMillis: Long = WAIT_MILLIS,
): Boolean = awaitReady(prompts, screen, answerBoot = null, timeoutMillis = timeoutMillis)

/**
 * The same, but answering LDOS's "Time ?" if the machine is still asking it.
 *
 * Once, and only when the question is actually on screen. The alternative --
 * beginning the first command with a newline -- sends the answer and the first
 * letter a fifth of a second apart, and LDOS is not listening yet: it took the
 * D and ran "IR :1", which it does not have.
 */
suspend fun awaitReady(
    prompts: List<String>,
    screen: () -> ScreenBuffer,
    answerBoot: (suspend () -> Unit)?,
    timeoutMillis: Long = WAIT_MILLIS,
): Boolean {
    var waited = 0L
    var answered = false
    while (waited < timeoutMillis) {
        val line = lastScreenLine(screen())
        if (line in prompts) {
            return true
        }
        if (line == BOOT_PROMPT && answerBoot != null && !answered) {
            answered = true
            answerBoot()
        }
        delay(POLL_MILLIS)
        waited += POLL_MILLIS
    }
    return false
}

/**
 * The step the user is about to run: what it types, and why.
 *
 * A panel rather than something drawn over the picture, because the picture is
 * what it is talking about and the point is to read this, then watch that.
 */
@Composable
fun TutorialPanel(
    step: TutorialStep,
    number: Int,
    total: Int,
    onNext: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = Trs80Theme.colors
    ModalPanel(onDismiss = onCancel) {
        SectionKicker(stringResource(Res.string.tutorial))
        Column(Modifier.padding(top = 12.dp)) {
            // In the machine's own register, since it is what the machine is
            // about to be told -- but in the shell's monospace, not the screen
            // font, which belongs inside the glass.
            Text(step.asWritten(), style = Trs80Theme.type.title, color = colors.accentText)
            Spacer(Modifier.padding(top = 10.dp))
            Text(step.description, style = Trs80Theme.type.body, color = colors.muted)
        }
        Spacer(Modifier.padding(top = 18.dp))
        Hairline()
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            TextAction(
                stringResource(Res.string.cancel),
                onClick = onCancel,
                color = colors.muted,
                padding = 0.dp,
            )
            Spacer(Modifier.weight(1f))
            TextAction(
                stringResource(Res.string.tutorial_next, number, total),
                onClick = onNext,
                padding = 0.dp,
            )
        }
    }
}
