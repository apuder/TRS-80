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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import org.puder.trs80.shared.ui.theme.SegmentedToggle
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.ThemePreference
import org.puder.trs80.shared.ui.theme.Trs80Theme

/**
 * The app's own settings, as opposed to a machine's.
 *
 * Only appearance so far. What else belongs here — the ROM images, the default
 * keyboard, the cassette behaviour — is on the Android settings screen and
 * arrives with the rest of the port; this is the shell those land in.
 */
@Composable
fun SettingsScreen(
    theme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    onBack: () -> Unit,
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
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenEdge, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Back",
                style = Trs80Theme.type.body,
                color = colors.accentText,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Spacer(Modifier.width(14.dp))
            Text("Settings", style = Trs80Theme.type.wordmark, color = colors.text)
        }
        Divider()

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenEdge),
        ) {
            SettingsSection("Appearance")
            Row(
                Modifier.fillMaxWidth().padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Theme", style = Trs80Theme.type.body, color = colors.text)
                Spacer(Modifier.weight(1f))
                SegmentedToggle(
                    options = ThemePreference.entries.map { it.name.uppercase() },
                    selected = ThemePreference.entries.indexOf(theme),
                    onSelect = { onThemeChange(ThemePreference.entries[it]) },
                )
            }
            Text(
                "System follows the device. The emulated screen keeps its own " +
                    "colours either way — those belong to the machine.",
                style = Trs80Theme.type.bodySmall,
                color = colors.muted,
            )
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

@Composable
private fun SettingsSection(label: String) {
    val colors = Trs80Theme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), style = Trs80Theme.type.kicker, color = colors.accentText)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(colors.hairline))
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(Trs80Theme.spacing.hairline)
            .background(Trs80Theme.colors.hairline)
    )
}
