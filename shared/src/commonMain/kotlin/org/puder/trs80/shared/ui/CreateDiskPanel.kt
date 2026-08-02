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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.puder.trs80.shared.ui.theme.Hairline
import org.puder.trs80.shared.ui.theme.ModalPanel
import org.puder.trs80.shared.ui.theme.SectionKicker
import org.puder.trs80.shared.ui.theme.SegmentedToggle
import org.puder.trs80.shared.ui.theme.SettingRow
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.TextAction
import org.puder.trs80.shared.ui.theme.Toggle
import org.puder.trs80.shared.ui.theme.Trs80TextField
import org.puder.trs80.shared.ui.theme.Trs80Theme
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.cancel
import trs_80.shared.generated.resources.create
import trs_80.shared.generated.resources.density
import trs_80.shared.generated.resources.disk_format
import trs_80.shared.generated.resources.ignore_density
import trs_80.shared.generated.resources.ignore_density_detail
import trs_80.shared.generated.resources.mkdisk_bad_name
import trs_80.shared.generated.resources.mkdisk_failed
import trs_80.shared.generated.resources.mkdisk_for_drive
import trs_80.shared.generated.resources.mkdisk_name_hint
import trs_80.shared.generated.resources.mkdisk_name_taken
import trs_80.shared.generated.resources.name
import trs_80.shared.generated.resources.new_disk
import trs_80.shared.generated.resources.sides
import trs_80.shared.generated.resources.size_inches

/**
 * Makes a blank disk image for one drive.
 *
 * A panel over the editor rather than a screen of its own, which is what the
 * Android app had. Going somewhere else and coming back would take the draft
 * being edited with it -- the editor holds its changes in memory until Save --
 * and losing a machine's unsaved settings to fetch a blank disk for it would be
 * a poor trade. It is also simply a short question with an answer, which is
 * what a panel is for.
 *
 * The DMK parameters stay on screen whatever the format is and dim when they do
 * not apply, rather than appearing and disappearing: a panel that changes height
 * as the format is cycled is hard to read, and their being greyed says plainly
 * that DMK is the format that has them.
 *
 * @param error what went wrong with the last attempt, or null before the first.
 */
@Composable
fun CreateDiskPanel(
    drive: Int,
    spec: DiskImageSpec,
    onChange: (DiskImageSpec) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
    error: DiskCreation? = null,
) {
    val colors = Trs80Theme.colors
    ModalPanel(onDismiss = onDismiss) {
        Text(stringResource(Res.string.new_disk), style = Trs80Theme.type.title)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(Res.string.mkdisk_for_drive, drive),
            style = Trs80Theme.type.bodySmall,
            color = colors.muted,
        )

        androidx.compose.foundation.layout.Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
        ) {
            SectionKicker(stringResource(Res.string.name))
            Trs80TextField(
                value = spec.name,
                onValueChange = { onChange(spec.copy(name = it)) },
                placeholder = stringResource(Res.string.mkdisk_name_hint),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionKicker(stringResource(Res.string.disk_format))
            SegmentedToggle(
                options = DiskFormat.entries.map { it.name },
                selected = spec.format.ordinal,
                onSelect = { onChange(spec.copy(format = DiskFormat.entries[it])) },
                fill = true,
            )

            SectionKicker(DiskFormat.DMK.name)
            SettingRow(stringResource(Res.string.sides), enabled = spec.dmkApplies) {
                SegmentedToggle(
                    options = listOf("1", "2"),
                    selected = spec.sides - 1,
                    onSelect = { onChange(spec.copy(sides = it + 1)) },
                    enabled = spec.dmkApplies,
                )
            }
            Hairline()
            SettingRow(stringResource(Res.string.density), enabled = spec.dmkApplies) {
                SegmentedToggle(
                    options = listOf("1", "2"),
                    selected = spec.densityCode - 1,
                    onSelect = { onChange(spec.copy(doubleDensity = it == 1)) },
                    enabled = spec.dmkApplies,
                )
            }
            Hairline()
            SettingRow(stringResource(Res.string.size_inches), enabled = spec.dmkApplies) {
                SegmentedToggle(
                    options = listOf("5", "8"),
                    selected = if (spec.eightInch) 1 else 0,
                    onSelect = { onChange(spec.copy(eightInch = it == 1)) },
                    enabled = spec.dmkApplies,
                )
            }
            Hairline()
            SettingRow(
                label = stringResource(Res.string.ignore_density),
                subtitle = stringResource(Res.string.ignore_density_detail),
                enabled = spec.dmkApplies,
            ) {
                Toggle(
                    checked = spec.ignoreDensity,
                    onCheckedChange = { onChange(spec.copy(ignoreDensity = it)) },
                    enabled = spec.dmkApplies,
                )
            }
        }

        // Said only once there is something to say about: an empty field is
        // where everyone starts, and being told off for it helps nobody.
        val complaint = when {
            error is DiskCreation.NameTaken -> stringResource(Res.string.mkdisk_name_taken)
            error is DiskCreation.Failed -> stringResource(Res.string.mkdisk_failed)
            spec.name.isNotEmpty() && !spec.nameIsLegal ->
                stringResource(Res.string.mkdisk_bad_name)

            else -> null
        }
        if (complaint != null) {
            Spacer(Modifier.height(12.dp))
            Text(complaint, style = Trs80Theme.type.bodySmall, color = colors.danger)
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextAction(
                stringResource(Res.string.cancel),
                onClick = onDismiss,
                color = colors.muted,
                padding = 0.dp,
            )
            Spacer(Modifier.weight(1f))
            TextAction(
                stringResource(Res.string.create),
                onClick = { if (spec.nameIsLegal) onCreate() },
                color = if (spec.nameIsLegal) colors.accentText else colors.muted,
                style = Trs80Theme.type.kicker,
                padding = 0.dp,
            )
        }
    }
}
