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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KEY_SPACING = 3.dp
private val KEY_CORNER = 6.dp

private val KEY_FACE = Color(0x33FFFFFF)
private val KEY_FACE_PRESSED = Color(0x99FFFFFF)
private val KEY_FACE_LATCHED = Color(0x66FFFFFF)
private val KEY_LABEL = Color.White

/**
 * The on-screen keyboard.
 *
 * A port of the Android key grid, and faithful to how it *behaves* — the same
 * keys in the same places, the same latching shift, the same Alt that swaps the
 * compact keyboard's two pages. Not faithful to how it looks, deliberately: the
 * UI spec condemns the current styling outright, the labels at 43% opacity fail
 * contrast, and drawing it properly is the redesign's job (§9). What is here is
 * legible and plain, and is meant to be replaced.
 *
 * @param keyHeight how tall one key is. The width follows from the widest row,
 * so the whole keyboard fits regardless of how many keys a row holds.
 */
@Composable
fun Keyboard(
    state: KeyboardState,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 44.dp,
) {
    val page = state.definition.pages.getOrNull(state.page) ?: return
    // Rows differ in how many key-widths they hold -- the space bar alone is ten
    // -- so every row is laid out against the widest, and the keyboard is as wide
    // as its widest row rather than each row filling the screen on its own.
    val widest = page.rows.maxOfOrNull { row -> row.sumOf { it.size } } ?: return

    Column(
        modifier = modifier.fillMaxWidth().padding(KEY_SPACING),
        verticalArrangement = Arrangement.spacedBy(KEY_SPACING),
    ) {
        for (row in page.rows) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KEY_SPACING),
            ) {
                val units = row.sumOf { it.size }
                // Rows narrower than the widest are centred, as they were.
                if (units < widest) {
                    Box(Modifier.weight((widest - units) / 2f))
                }
                for (key in row) {
                    KeyFace(
                        state = state,
                        key = key,
                        modifier = Modifier.weight(key.size.toFloat()).height(keyHeight),
                    )
                }
                if (units < widest) {
                    Box(Modifier.weight((widest - units) / 2f))
                }
            }
        }
    }
}

@Composable
private fun KeyFace(state: KeyboardState, key: KeyboardKey, modifier: Modifier) {
    val isPressed = key.name in state.pressed
    val isLatched = state.isShift(key) && state.latchedShift == key.name
    val face = when {
        isPressed -> KEY_FACE_PRESSED
        isLatched -> KEY_FACE_LATCHED
        else -> KEY_FACE
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KEY_CORNER))
            .background(face)
            // Not clickable(): a key has to send its down the moment it is
            // touched and its up when the finger lifts, because that is what the
            // emulated machine reads. A click arrives only after both, by which
            // time a game has missed the keypress entirely.
            .pointerInput(key.name) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    state.press(key)
                    waitForUpOrCancellation()
                    state.release(key)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = state.labelFor(key),
            color = KEY_LABEL,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
