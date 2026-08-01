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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.puder.trs80.shared.appVersion
import org.jetbrains.compose.resources.stringResource
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.appearance
import trs_80.shared.generated.resources.back
import trs_80.shared.generated.resources.get_all
import trs_80.shared.generated.resources.not_installed
import trs_80.shared.generated.resources.rom_images
import trs_80.shared.generated.resources.roms_hint
import trs_80.shared.generated.resources.settings
import trs_80.shared.generated.resources.theme
import trs_80.shared.generated.resources.theme_hint
import org.puder.trs80.shared.ui.theme.Hairline
import org.puder.trs80.shared.ui.theme.SectionKicker
import org.puder.trs80.shared.ui.theme.MinimumTouchTarget
import org.puder.trs80.shared.ui.theme.StrokeIcon
import org.puder.trs80.shared.ui.theme.Trs80Icon
import androidx.compose.foundation.layout.size
import org.puder.trs80.shared.ui.theme.ProgressRing
import org.puder.trs80.shared.ui.theme.SettingRow
import org.puder.trs80.shared.ui.theme.SegmentedToggle
import org.puder.trs80.shared.ui.theme.TextAction
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.ThemePreference
import org.puder.trs80.shared.ui.theme.Trs80Theme

/**
 * The app's own settings, as opposed to a machine's.
 *
 * Only appearance so far. What else belongs here — the ROM images, the default
 * keyboard, the cassette behavior — is on the Android settings screen and
 * arrives with the rest of the port; this is the shell those land in.
 */
@Composable
fun SettingsScreen(
    theme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    onBack: () -> Unit,
    roms: List<RomStatus> = emptyList(),
    /** The models being fetched right now. */
    romsDownloading: Set<Int> = emptySet(),
    onDownloadRoms: (() -> Unit)? = null,
    onChooseRom: ((Int) -> Unit)? = null,
    onRedownloadRom: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = Trs80Theme.colors
    val spacing = Trs80Theme.spacing

    Column(
        modifier
            .fillMaxSize()
            .background(colors.ground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            // The start edge is pulled in by the tap target's own padding, so
            // the word still sits on the screen edge optically.
            Modifier
                .fillMaxWidth()
                .padding(start = spacing.screenEdge - 10.dp, end = spacing.screenEdge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextAction(stringResource(Res.string.back), onClick = onBack)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(Res.string.settings), style = Trs80Theme.type.wordmark, color = colors.text)
        }
        Hairline()

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenEdge),
        ) {
            SectionKicker(stringResource(Res.string.appearance))
            Row(
                Modifier.fillMaxWidth().padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.theme), style = Trs80Theme.type.body, color = colors.text)
                Spacer(Modifier.weight(1f))
                SegmentedToggle(
                    options = ThemePreference.entries.map { stringResource(it.label) },
                    selected = ThemePreference.entries.indexOf(theme),
                    onSelect = { onThemeChange(ThemePreference.entries[it]) },
                )
            }
            Text(
                stringResource(Res.string.theme_hint),
                style = Trs80Theme.type.bodySmall,
                color = colors.muted,
            )

            if (roms.isNotEmpty()) {
                val missing = roms.count { !it.present }
                SectionKicker(
                    stringResource(Res.string.rom_images),
                    // Only offered when there is something to fetch: a control
                    // that would do nothing is worse than no control.
                    trailing = onDownloadRoms?.takeIf { missing > 0 }?.let {
                        {
                            TextAction(
                                stringResource(Res.string.get_all),
                                onClick = it,
                                style = Trs80Theme.type.kickerSmall,
                            )
                        }
                    },
                )
                roms.forEach { rom ->
                    SettingRow(
                        label = rom.label,
                        subtitle = rom.filename ?: stringResource(Res.string.not_installed),
                        onClick = onChooseRom?.let { choose -> { choose(rom.model) } },
                    ) {
                        // Fetching this one again, next to the row that opens a
                        // file picker: the two ways to replace a ROM, one each.
                        Box(
                            Modifier.size(MinimumTouchTarget),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (rom.model in romsDownloading) {
                                ProgressRing(progress = null, size = 17.dp)
                            } else {
                                onRedownloadRom?.let { again ->
                                    StrokeIcon(
                                        Trs80Icon.Refresh,
                                        color = colors.accentText,
                                        size = 17.dp,
                                        onClick = { again(rom.model) },
                                    )
                                }
                            }
                        }
                    }
                    Hairline()
                }
                Text(
                    stringResource(Res.string.roms_hint),
                    style = Trs80Theme.type.bodySmall,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        // At the very foot, quiet: it is there to be read back when something
        // has gone wrong, not to be looked at.
        Box(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "TRS-80 ${appVersion()}",
                style = Trs80Theme.type.kickerSmall,
                color = colors.muted,
            )
        }
    }
}

