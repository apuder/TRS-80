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
import org.puder.trs80.shared.configuration.ConfigurationDraft
import org.puder.trs80.shared.ui.theme.DestructiveButton
import org.puder.trs80.shared.ui.theme.Hairline
import org.puder.trs80.shared.ui.theme.MinimumTouchTarget
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
            Text("Edit entry", style = Trs80Theme.type.wordmark, color = colors.text)
            Spacer(Modifier.weight(1f))
            // Save lives in the header rather than floating over the content.
            TextAction("SAVE", onClick = actions.onSave, style = Trs80Theme.type.kicker)
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

            SectionKicker("Name")
            Trs80TextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                placeholder = "Untitled",
            )

            SectionKicker("Machine")
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
            SettingRow("Screen") {
                SegmentedToggle(
                    options = listOf("GREEN", "WHITE"),
                    selected = draft.characterColor.coerceIn(0, 1),
                    onSelect = { onChange(draft.copy(characterColor = it)) },
                )
            }

            // The tally says how many drives there are, not just how many are
            // in use: with only one empty drive on show, four slots is otherwise
            // not something the screen ever admits to.
            SectionKicker(
                "Disks",
                count = "${draft.disks.size} of ${draft.diskPaths.size}",
                trailing = actions.onChooseDisk?.takeIf { draft.disks.size < draft.diskPaths.size }
                    ?.let {
                        {
                            TextAction(
                                "ADD",
                                onClick = { it(draft.disks.size) },
                                style = Trs80Theme.type.kickerSmall,
                            )
                        }
                    },
            )
            Disks(draft, onChange, actions.onChooseDisk)

            SectionKicker("Cassette")
            SettingRow(
                label = draft.cassettePath?.let(::fileName) ?: "None loaded",
                subtitle = if (draft.cassettePath == null) {
                    "${modelLabel(draft.model)} boots from disk"
                } else {
                    null
                },
            ) {
                actions.onChooseCassette?.let {
                    TextAction("CHOOSE", onClick = it, style = Trs80Theme.type.kickerSmall)
                }
            }

            SectionKicker("Controls")
            SettingRow(
                label = "Keyboard",
                subtitle = "${keyboardLabel(draft.keyboardPortrait)} portrait, " +
                    "${keyboardLabel(draft.keyboardLandscape)} landscape",
                onClick = { controlsOpen = !controlsOpen },
            )
            if (controlsOpen) {
                Hairline()
                KeyboardChoice("Portrait", draft.keyboardPortrait) {
                    onChange(draft.copy(keyboardPortrait = it))
                }
                Hairline()
                KeyboardChoice("Landscape", draft.keyboardLandscape) {
                    onChange(draft.copy(keyboardLandscape = it))
                }
            }

            SectionKicker("While running")
            SettingRow("Sound") {
                Toggle(
                    checked = !draft.soundMuted,
                    onCheckedChange = { onChange(draft.copy(soundMuted = !it)) },
                )
            }

            SectionKicker("Remove")
            DestructiveButton(
                "Delete this entry",
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
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onCancel,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(28.dp)
                .background(colors.ground)
                .border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.25f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                )
                .padding(20.dp),
        ) {
            Text("Delete ${name.ifEmpty { "this entry" }}?", style = Trs80Theme.type.title)
            Spacer(Modifier.padding(top = 8.dp))
            Text(
                "Its disks and any saved state go with it. This cannot be undone.",
                style = Trs80Theme.type.bodySmall,
                color = colors.muted,
            )
            Spacer(Modifier.padding(top = 18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextAction("CANCEL", onClick = onCancel, color = colors.muted, padding = 0.dp)
                Spacer(Modifier.weight(1f))
                DestructiveButton("Delete", onClick = onConfirm, filled = true, icon = null)
            }
        }
    }
}

/**
 * States what the first edit did.
 *
 * Only for something that came from the catalogue: it explains why editing did
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
            "This is now your copy of ${name.ifEmpty { "this entry" }}. " +
                "The catalogue original is untouched.",
            style = Trs80Theme.type.bodySmall,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        TextAction("REVERT", onClick = onRevert, style = Trs80Theme.type.kickerSmall, padding = 4.dp)
    }
}

/** The drives, in order, with one empty drive shown when there is room. */
@Composable
private fun Disks(
    draft: ConfigurationDraft,
    onChange: (ConfigurationDraft) -> Unit,
    onChooseDisk: ((Int) -> Unit)?,
) {
    val colors = Trs80Theme.colors
    val disks = draft.disks
    val rowHeightPx = with(LocalDensity.current) { DISK_ROW_HEIGHT.toPx() }
    var dragging by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    disks.forEachIndexed { drive, path ->
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
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StrokeIcon(
                Trs80Icon.DragHandle,
                color = colors.muted,
                size = 16.dp,
                modifier = Modifier.pointerInput(drive, disks.size) {
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
                                        to = (drive + moved).coerceIn(0, disks.lastIndex),
                                    )
                                )
                            }
                        },
                    ) { _, delta -> dragOffset += delta.y }
                },
            )
            Spacer(Modifier.width(6.dp))
            // The drive number is one of the two places the accent fills.
            Text(
                drive.toString(),
                style = Trs80Theme.type.kickerSmall,
                color = colors.accentText,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                fileName(path),
                style = Trs80Theme.type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            StrokeIcon(
                Trs80Icon.Eject,
                color = colors.muted,
                size = 17.dp,
                onClick = { onChange(draft.withDiskEjected(drive)) },
            )
        }
    }
    if (disks.size < draft.diskPaths.size) {
        if (disks.isNotEmpty()) {
            Hairline()
        }
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (onChooseDisk != null) {
                        Modifier.clickable { onChooseDisk(disks.size) }
                    } else {
                        Modifier
                    }
                )
                .heightIn(min = MinimumTouchTarget)
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(22.dp))
            Text(
                disks.size.toString(),
                style = Trs80Theme.type.kickerSmall,
                color = colors.muted,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (onChooseDisk != null) "Empty drive — choose a disk" else "Empty drive",
                style = Trs80Theme.type.body,
                color = colors.muted,
            )
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
