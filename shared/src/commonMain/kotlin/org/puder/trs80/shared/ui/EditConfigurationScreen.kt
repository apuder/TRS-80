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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.puder.trs80.shared.KeyboardLayout
import org.puder.trs80.shared.ScreenColor
import org.puder.trs80.shared.configuration.ConfigurationDraft
import org.jetbrains.compose.resources.stringResource
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.amber
import trs_80.shared.generated.resources.boots_from_disk
import trs_80.shared.generated.resources.cancel
import trs_80.shared.generated.resources.cassette
import trs_80.shared.generated.resources.cassette_none
import trs_80.shared.generated.resources.choose
import trs_80.shared.generated.resources.controls
import trs_80.shared.generated.resources.delete
import trs_80.shared.generated.resources.delete_consequence
import trs_80.shared.generated.resources.delete_entry
import trs_80.shared.generated.resources.delete_question
import trs_80.shared.generated.resources.disks
import trs_80.shared.generated.resources.disks_of
import trs_80.shared.generated.resources.edit_entry
import trs_80.shared.generated.resources.empty_drive
import trs_80.shared.generated.resources.empty_drive_choose
import trs_80.shared.generated.resources.fork_banner
import trs_80.shared.generated.resources.green
import trs_80.shared.generated.resources.keyboard
import trs_80.shared.generated.resources.keyboard_summary
import trs_80.shared.generated.resources.landscape
import trs_80.shared.generated.resources.machine
import trs_80.shared.generated.resources.name
import trs_80.shared.generated.resources.portrait
import trs_80.shared.generated.resources.remove
import trs_80.shared.generated.resources.revert
import trs_80.shared.generated.resources.save
import trs_80.shared.generated.resources.screen
import trs_80.shared.generated.resources.sound
import trs_80.shared.generated.resources.this_entry
import trs_80.shared.generated.resources.untitled
import trs_80.shared.generated.resources.while_running
import trs_80.shared.generated.resources.white
import org.puder.trs80.shared.ui.theme.DestructiveButton
import org.puder.trs80.shared.ui.theme.Hairline
import org.puder.trs80.shared.ui.theme.MinimumTouchTarget
import org.puder.trs80.shared.ui.theme.ModalPanel
import org.puder.trs80.shared.ui.theme.SectionKicker
import org.puder.trs80.shared.ui.theme.SegmentedToggle
import org.puder.trs80.shared.ui.theme.SettingRow
import org.puder.trs80.shared.ui.theme.StrokeIcon
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.TextAction
import org.puder.trs80.shared.ui.theme.Toggle
import org.puder.trs80.shared.ui.theme.Trs80Icon
import org.puder.trs80.shared.ui.theme.Trs80TextField
import org.puder.trs80.shared.ui.theme.Trs80Theme

/** How high one disk row stands; the drag gesture counts in these. */
private val DISK_ROW_HEIGHT = 52.dp

/**
 * What the editor cannot do on its own.
 *
 * The two file-choosing actions are nullable because a host without a file
 * picker has no way to honour them, and a control that does nothing when tapped
 * is worse than one that is not drawn.
 */
data class EditConfigurationActions(
    val onSave: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onRevert: () -> Unit = {},
    val onDelete: () -> Unit = {},
    /** Puts a file in the given drive, replacing whatever is in it. */
    val onChooseDisk: ((drive: Int) -> Unit)? = null,
    val onChooseCassette: (() -> Unit)? = null,
)

/**
 * The configuration editor.
 *
 * Edits apply live to [draft]; the header's Save is what writes them back. The
 * screen holds no state of its own beyond which sections are open, so the host
 * owns the draft and can decide what leaving without saving means.
 *
 * @param original the draft as the editor opened it, which is what Revert
 * restores and what decides whether the fork banner has anything to say.
 * @param models the models worth offering — those with a ROM present, since
 * choosing one without a ROM produces a machine that cannot boot.
 */
@Composable
fun EditConfigurationScreen(
    draft: ConfigurationDraft,
    original: ConfigurationDraft,
    models: List<Int>,
    onChange: (ConfigurationDraft) -> Unit,
    actions: EditConfigurationActions,
    modifier: Modifier = Modifier,
) {
    val colors = Trs80Theme.colors
    val spacing = Trs80Theme.spacing
    var controlsOpen by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(colors.ground)) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(end = spacing.screenEdge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StrokeIcon(
                Trs80Icon.ChevronLeft,
                color = colors.accentText,
                onClick = actions.onBack,
            )
            Text(stringResource(Res.string.edit_entry), style = Trs80Theme.type.wordmark, color = colors.text)
            Spacer(Modifier.weight(1f))
            // Save lives in the header rather than floating over the content.
            TextAction(stringResource(Res.string.save), onClick = actions.onSave, style = Trs80Theme.type.kicker)
        }
        Hairline()

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenEdge),
        ) {
            if (draft.isForkedFrom(original)) {
                ForkBanner(name = original.name, onRevert = actions.onRevert)
            }

            SectionKicker(stringResource(Res.string.name))
            Trs80TextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                placeholder = stringResource(Res.string.untitled),
            )

            SectionKicker(stringResource(Res.string.machine))
            SegmentedToggle(
                options = models.map { modelLabel(it).uppercase() },
                selected = models.indexOf(draft.model),
                onSelect = { onChange(draft.copy(model = models[it])) },
                modifier = Modifier.fillMaxWidth(),
                fill = true,
            )
            Hairline()
            // Not in the visual spec, but the app has always had it and the
            // editor is the only place it can be reached.
            SettingRow(stringResource(Res.string.screen)) {
                val phosphors = ScreenColor.entries
                SegmentedToggle(
                    options = phosphors.map {
                        stringResource(
                            when (it) {
                                ScreenColor.Green -> Res.string.green
                                ScreenColor.Amber -> Res.string.amber
                                ScreenColor.White -> Res.string.white
                            }
                        )
                    },
                    selected = phosphors.indexOf(ScreenColor.of(draft.characterColor)),
                    onSelect = {
                        onChange(draft.copy(characterColor = phosphors[it].stored))
                    },
                )
            }

            // The tally says how many drives there are, not just how many are
            // in use: with only one empty drive on show, four slots is otherwise
            // not something the screen ever admits to.
            SectionKicker(
                stringResource(Res.string.disks),
                count = stringResource(
                    Res.string.disks_of,
                    draft.diskCount,
                    draft.diskPaths.size,
                ),
            )
            Disks(draft, onChange, actions.onChooseDisk)

            SectionKicker(stringResource(Res.string.cassette))
            SettingRow(
                label = draft.cassettePath?.let(::fileName) ?: stringResource(Res.string.cassette_none),
                subtitle = if (draft.cassettePath == null) {
                    stringResource(Res.string.boots_from_disk, modelLabel(draft.model))
                } else {
                    null
                },
            ) {
                actions.onChooseCassette?.let {
                    TextAction(stringResource(Res.string.choose), onClick = it, style = Trs80Theme.type.kickerSmall)
                }
            }

            SectionKicker(stringResource(Res.string.controls))
            SettingRow(
                label = stringResource(Res.string.keyboard),
                subtitle = stringResource(
                    Res.string.keyboard_summary,
                    keyboardLabel(draft.keyboardPortrait),
                    keyboardLabel(draft.keyboardLandscape),
                ),
                onClick = { controlsOpen = !controlsOpen },
            )
            if (controlsOpen) {
                Hairline()
                KeyboardChoice(stringResource(Res.string.portrait), draft.keyboardPortrait) {
                    onChange(draft.copy(keyboardPortrait = it))
                }
                Hairline()
                KeyboardChoice(stringResource(Res.string.landscape), draft.keyboardLandscape) {
                    onChange(draft.copy(keyboardLandscape = it))
                }
            }

            SectionKicker(stringResource(Res.string.while_running))
            SettingRow(stringResource(Res.string.sound)) {
                Toggle(
                    checked = !draft.soundMuted,
                    onCheckedChange = { onChange(draft.copy(soundMuted = !it)) },
                )
            }

            SectionKicker(stringResource(Res.string.remove))
            DestructiveButton(
                stringResource(Res.string.delete_entry),
                onClick = { confirmingDelete = true },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.padding(bottom = 24.dp))
        }
    }

        if (confirmingDelete) {
            ConfirmDelete(
                name = draft.name,
                onCancel = { confirmingDelete = false },
                onConfirm = {
                    confirmingDelete = false
                    actions.onDelete()
                },
            )
        }
    }
}

/**
 * Asks before deleting.
 *
 * Drawn over the editor rather than in a platform dialog, so it is the app's own
 * type and palette and behaves the same on both platforms. The scrim swallows
 * taps, which is what stops the screen behind it being operated blind.
 */
@Composable
private fun ConfirmDelete(name: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val colors = Trs80Theme.colors
    ModalPanel(onDismiss = onCancel) {
        Text(stringResource(Res.string.delete_question, name.ifEmpty { stringResource(Res.string.this_entry) }), style = Trs80Theme.type.title)
        Spacer(Modifier.padding(top = 8.dp))
        Text(
            stringResource(Res.string.delete_consequence),
            style = Trs80Theme.type.bodySmall,
            color = colors.muted,
        )
        Spacer(Modifier.padding(top = 18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextAction(stringResource(Res.string.cancel), onClick = onCancel, color = colors.muted, padding = 0.dp)
            Spacer(Modifier.weight(1f))
            DestructiveButton(stringResource(Res.string.delete), onClick = onConfirm, filled = true, icon = null)
        }
    }
}

/**
 * States what the first edit did.
 *
 * Only for something that came from the catalog: it explains why editing did
 * not change what everyone else sees, and offers the way back.
 */
@Composable
private fun ForkBanner(name: String, onRevert: () -> Unit) {
    val colors = Trs80Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .background(colors.accent.copy(alpha = 0.10f))
            .border(Trs80Theme.spacing.hairline, colors.accent.copy(alpha = 0.55f))
            .padding(12.dp),
    ) {
        StrokeIcon(Trs80Icon.Info, color = colors.accentText, size = 16.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(
                Res.string.fork_banner,
                name.ifEmpty { stringResource(Res.string.this_entry) },
            ),
            style = Trs80Theme.type.bodySmall,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        TextAction(stringResource(Res.string.revert), onClick = onRevert, style = Trs80Theme.type.kickerSmall, padding = 4.dp)
    }
}

/**
 * Every drive, whether or not it holds anything.
 *
 * All four are always shown, because all four are always there. The emulator
 * takes one path per drive and a machine can perfectly well have a disk in
 * drive 2 with drive 1 empty, so this cannot present them as a list that fills
 * from the top.
 */
@Composable
private fun Disks(
    draft: ConfigurationDraft,
    onChange: (ConfigurationDraft) -> Unit,
    onChooseDisk: ((Int) -> Unit)?,
) {
    val colors = Trs80Theme.colors
    val rowHeightPx = with(LocalDensity.current) { DISK_ROW_HEIGHT.toPx() }
    var dragging by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    draft.diskPaths.forEachIndexed { drive, path ->
        if (drive > 0) {
            Hairline()
        }
        val isDragging = drive == dragging
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                .then(
                    if (onChooseDisk != null) {
                        Modifier.clickable { onChooseDisk(drive) }
                    } else {
                        Modifier
                    }
                )
                .heightIn(min = MinimumTouchTarget)
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (path != null) {
                StrokeIcon(
                    Trs80Icon.DragHandle,
                    color = colors.muted,
                    size = 16.dp,
                    modifier = Modifier.pointerInput(drive) {
                        detectDragGestures(
                            onDragStart = { dragging = drive; dragOffset = 0f },
                            onDragCancel = { dragging = -1; dragOffset = 0f },
                            onDragEnd = {
                                val moved = (dragOffset / rowHeightPx).toInt()
                                dragging = -1
                                dragOffset = 0f
                                if (moved != 0) {
                                    onChange(
                                        draft.withDiskMoved(
                                            from = drive,
                                            to = (drive + moved)
                                                .coerceIn(0, draft.diskPaths.lastIndex),
                                        )
                                    )
                                }
                            },
                        ) { _, delta -> dragOffset += delta.y }
                    },
                )
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Spacer(Modifier.width(6.dp))
            // The drive number is one of the two places the accent fills, and
            // it is the drive's real number rather than its position in a list.
            Text(
                drive.toString(),
                style = Trs80Theme.type.kickerSmall,
                color = if (path != null) colors.accentText else colors.muted,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                path?.let(::fileName)
                    ?: if (onChooseDisk != null) {
                        stringResource(Res.string.empty_drive_choose)
                    } else {
                        stringResource(Res.string.empty_drive)
                    },
                style = Trs80Theme.type.body,
                color = if (path != null) colors.text else colors.muted,
                modifier = Modifier.weight(1f),
            )
            if (path != null) {
                StrokeIcon(
                    Trs80Icon.Eject,
                    color = colors.muted,
                    size = 17.dp,
                    onClick = { onChange(draft.withDiskEjected(drive)) },
                )
            }
        }
    }
}

/** One keyboard layout choice, cycling through the layouts the app draws. */
@Composable
private fun KeyboardChoice(
    label: String,
    layout: KeyboardLayout?,
    onChange: (KeyboardLayout) -> Unit,
) {
    val options = KeyboardLayout.entries
    SettingRow(label) {
        SegmentedToggle(
            options = options.map { keyboardLabel(it).uppercase() },
            selected = options.indexOf(layout).coerceAtLeast(0),
            onSelect = { onChange(options[it]) },
        )
    }
}

/** The last path segment, which is the only part of a disk path worth showing. */
private fun fileName(path: String): String = path.substringAfterLast('/')
