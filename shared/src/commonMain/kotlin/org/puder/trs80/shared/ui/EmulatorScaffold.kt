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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.puder.trs80.shared.ui.theme.Hairline
import org.puder.trs80.shared.ui.theme.StrokeIcon
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.Trs80Icon
import org.puder.trs80.shared.ui.theme.Trs80Theme

/**
 * The chrome around a running machine: a way out, a name, and the controls.
 *
 * The Android app has this as an action bar and has always needed it, since a
 * machine fills the screen and there is otherwise nothing to press. iOS needs it
 * more: there is no system Back at all, so without this the emulator is a place
 * the app can go and never leave.
 *
 * Drawn in the app's own palette. It was a Material scaffold with default
 * colors, and those follow a MaterialTheme this app never sets — so the bar
 * stayed light while the rest of the app went dark.
 */
@Composable
fun EmulatorScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    keyboard: (@Composable () -> Unit)? = null,
    /** What the machine can be asked to do while it runs; null offers nothing. */
    machine: MachineActions? = null,
    screen: @Composable () -> Unit,
) {
    val colors = Trs80Theme.colors
    var controlsOpen by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(colors.ground)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.fillMaxWidth().padding(end = Trs80Theme.spacing.screenEdge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StrokeIcon(Trs80Icon.ChevronLeft, color = colors.accentText, onClick = onBack)
                Text(
                    title,
                    style = Trs80Theme.type.wordmark,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (machine != null) {
                    Spacer(Modifier.width(Trs80Theme.spacing.gap))
                    MachineControlsButton(onClick = { controlsOpen = true })
                }
            }
            Hairline()

            // The picture takes what the keyboard leaves, on the machine's own
            // dark surround rather than the app's ground: what sits around a
            // screen belongs to the screen. The emulated display scales to
            // whatever it is given, so this needs no arithmetic -- the core is
            // told the size and rasterizes to it.
            Box(
                Modifier.weight(1f).fillMaxWidth().background(colors.crt),
                contentAlignment = Alignment.Center,
            ) {
                screen()
            }
            keyboard?.invoke()
        }

        // At the root, so the scrim covers the machine and not just the bar.
        if (controlsOpen && machine != null) {
            MachinePanel(machine, onDismiss = { controlsOpen = false })
        }
    }
}
