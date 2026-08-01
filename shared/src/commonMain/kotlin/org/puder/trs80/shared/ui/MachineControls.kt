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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.puder.trs80.shared.ui.theme.Hairline
import org.puder.trs80.shared.ui.theme.ModalPanel
import org.puder.trs80.shared.ui.theme.SectionKicker
import org.puder.trs80.shared.ui.theme.SettingRow
import org.puder.trs80.shared.ui.theme.StrokeIcon
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.TextAction
import org.puder.trs80.shared.ui.theme.Toggle
import org.puder.trs80.shared.ui.theme.Trs80Icon
import org.puder.trs80.shared.ui.theme.Trs80Theme
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.done
import trs_80.shared.generated.resources.help
import trs_80.shared.generated.resources.help_emulator
import trs_80.shared.generated.resources.machine_controls
import trs_80.shared.generated.resources.paste
import trs_80.shared.generated.resources.paste_detail
import trs_80.shared.generated.resources.paste_empty
import trs_80.shared.generated.resources.reset
import trs_80.shared.generated.resources.reset_detail
import trs_80.shared.generated.resources.rewind_cassette
import trs_80.shared.generated.resources.rewind_cassette_detail
import trs_80.shared.generated.resources.sound

/**
 * What can be done to a machine while it is running.
 *
 * Leaving is not among them. On Android that menu's Pause was `finish()` — the
 * state is written on the way out and picked up again next time — so the
 * scaffold's Back already is pause, and a second control saying so would be two
 * names for one thing.
 *
 * @param onPaste types the clipboard into the machine; false when there was
 * nothing on it to type.
 */
data class MachineActions(
    val onReset: () -> Unit,
    val onRewindCassette: () -> Unit,
    val onPaste: () -> Boolean,
    val soundMuted: Boolean,
    val onSoundMutedChange: (Boolean) -> Unit,
)

/**
 * The control that opens the panel, for the emulator's top bar.
 *
 * A panel rather than a row of icons: these are all things done rarely and
 * deliberately, and the screen belongs to the machine.
 *
 * The panel itself is drawn by [MachinePanel] at the root of the screen rather
 * than here. A modal opened from inside a top bar is laid out inside that bar,
 * so its scrim covers the bar and nothing else.
 */
@Composable
internal fun MachineControlsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    StrokeIcon(
        Trs80Icon.Overflow,
        modifier = modifier,
        color = Trs80Theme.colors.text,
        onClick = onClick,
    )
}

@Composable
internal fun MachinePanel(actions: MachineActions, onDismiss: () -> Unit) {
    val colors = Trs80Theme.colors
    var helping by remember { mutableStateOf(false) }
    var pasteNote by remember { mutableStateOf(false) }

    ModalPanel(onDismiss = onDismiss) {
        if (helping) {
            Text(stringResource(Res.string.help), style = Trs80Theme.type.title)
            Spacer(Modifier.padding(top = 10.dp))
            Text(
                stringResource(Res.string.help_emulator),
                style = Trs80Theme.type.bodySmall,
                color = colors.muted,
            )
            Spacer(Modifier.padding(top = 18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextAction(
                    stringResource(Res.string.done),
                    onClick = { helping = false },
                    padding = 0.dp,
                )
            }
            return@ModalPanel
        }

        SectionKicker(stringResource(Res.string.machine_controls))
        SettingRow(
            label = stringResource(Res.string.reset),
            subtitle = stringResource(Res.string.reset_detail),
            onClick = { actions.onReset(); onDismiss() },
        )
        Hairline()
        SettingRow(
            label = stringResource(Res.string.rewind_cassette),
            subtitle = stringResource(Res.string.rewind_cassette_detail),
            onClick = { actions.onRewindCassette(); onDismiss() },
        )
        Hairline()
        SettingRow(
            label = stringResource(Res.string.paste),
            subtitle = if (pasteNote) {
                stringResource(Res.string.paste_empty)
            } else {
                stringResource(Res.string.paste_detail)
            },
            // Says so rather than doing nothing: an action that silently
            // declines is indistinguishable from one that is broken.
            onClick = { if (actions.onPaste()) onDismiss() else pasteNote = true },
        )
        Hairline()
        SettingRow(label = stringResource(Res.string.sound)) {
            Toggle(
                checked = !actions.soundMuted,
                onCheckedChange = { actions.onSoundMutedChange(!it) },
            )
        }
        Hairline()
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            TextAction(
                stringResource(Res.string.help),
                onClick = { helping = true },
                color = colors.muted,
                padding = 0.dp,
            )
            Spacer(Modifier.weight(1f))
            TextAction(stringResource(Res.string.done), onClick = onDismiss, padding = 0.dp)
        }
    }
}
