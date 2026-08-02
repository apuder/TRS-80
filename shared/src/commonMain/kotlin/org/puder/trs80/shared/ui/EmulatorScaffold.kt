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
    val landscape = isLandscape()

    Box(modifier.fillMaxSize().background(colors.ground)) {
        if (landscape) {
            LandscapeMachine(
                title = title,
                onBack = onBack,
                hasControls = machine != null,
                onControls = { controlsOpen = true },
                keyboard = keyboard,
                screen = screen,
            )
        } else {
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

                // What surrounds the picture is the app's ground, the same as the
                // library's. The machine's own glass sits close enough to the colour
                // the emulated screen draws itself that the two ran together and the
                // picture had no edge. The display scales to whatever it is given,
                // so this needs no arithmetic -- the core is told the size and
                // rasterizes to it.
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    screen()
                }
                keyboard?.invoke()
            }
        }

        // At the root, so the scrim covers the machine and not just the bar.
        if (controlsOpen && machine != null) {
            MachinePanel(machine, onDismiss = { controlsOpen = false })
        }
    }
}

/**
 * The machine turned sideways: picture everywhere, everything else on top of it.
 *
 * Stacked rather than stacked *up*. A TRS-80's picture is about two and a half
 * times as wide as it is tall, so turning the phone is what finally lets it fill
 * the width -- and then a keyboard laid out beneath it would take more than half
 * the height and shrink the picture back to less than it had in portrait. The
 * Android app reached the same conclusion and drew the keyboard over the screen;
 * this does the same, with the keys as outlines so the picture reads through
 * them.
 *
 * The bar goes with it. Android hides its action bar outright in landscape, but
 * iOS has no system Back, so the two controls that matter stay as glyphs in the
 * corners, dimmed to what they are: a way out and a menu, not part of the
 * machine.
 */
@Composable
private fun LandscapeMachine(
    title: String,
    onBack: () -> Unit,
    hasControls: Boolean,
    onControls: () -> Unit,
    keyboard: (@Composable () -> Unit)?,
    screen: @Composable () -> Unit,
) {
    val colors = Trs80Theme.colors
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            screen()
        }

        Row(
            Modifier.fillMaxWidth().align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StrokeIcon(
                Trs80Icon.ChevronLeft,
                color = colors.accentText.copy(alpha = OVERLAY_CONTROL_ALPHA),
                onClick = onBack,
            )
            Spacer(Modifier.weight(1f))
            if (hasControls) {
                MachineControlsButton(
                    onClick = onControls,
                    tint = colors.text.copy(alpha = OVERLAY_CONTROL_ALPHA),
                    modifier = Modifier.padding(end = Trs80Theme.spacing.screenEdge),
                )
            }
        }

        keyboard?.let {
            Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter)) { it() }
        }
    }
}

/**
 * How far the two chrome glyphs fade when they lie on the picture.
 *
 * Faint enough not to read as part of what the machine is drawing, solid enough
 * to find without hunting. Android's own overlaid control sits at 0.4, but
 * Android has a system Back behind it and iOS does not — the chevron here is the
 * only way out of a running machine, so it is not something to hide.
 */
private const val OVERLAY_CONTROL_ALPHA = 0.7f
