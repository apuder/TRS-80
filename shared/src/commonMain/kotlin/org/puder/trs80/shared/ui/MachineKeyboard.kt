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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.puder.trs80.shared.KeyboardLayout

/**
 * Whatever the chosen layout puts on screen.
 *
 * Four of the five put something there; the game controller puts nothing,
 * because everything it needs is in the player's hands already and a strip of
 * unused screen would only take room from the picture.
 */
@Composable
fun MachineKeyboard(
    layout: KeyboardLayout?,
    grid: KeyboardState,
    sender: KeySender,
    modifier: Modifier = Modifier,
    overlay: Boolean = false,
    keyHeight: Dp = 44.dp,
) {
    when (layout) {
        // The pads are already mostly holes -- a ring, a knob and a word -- so
        // they need nothing doing to them to sit over the picture.
        KeyboardLayout.KEYBOARD_LAYOUT_JOYSTICK -> JoystickPad(sender, modifier)
        KeyboardLayout.KEYBOARD_TILT -> TiltPad(sender, modifier)
        KeyboardLayout.KEYBOARD_GAME_CONTROLLER -> Unit
        else -> Keyboard(grid, modifier, keyHeight = keyHeight, overlay = overlay)
    }
}
