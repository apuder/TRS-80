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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.Trs80Theme
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.fire
import kotlin.math.hypot

/** How tall the pads stand, which is about what the on-screen keyboard takes. */
private val PAD_HEIGHT = 210.dp

/**
 * The knob, which is also the dead zone: inside it nothing is pressed.
 *
 * Smaller than the ring by enough to leave the ring readable — a knob that
 * nearly fills its ring gives no sense of how far it has been pushed.
 */
private val KNOB_RADIUS = 28.dp

/**
 * A stick and a fire button, for programs written for a joystick.
 *
 * The stick sits on the right where a thumb reaches it, and everything left of
 * it fires — the same division the Android app made, and for the same reason:
 * during play neither is looked at, so the fire target should be as large as
 * the screen can spare.
 */
@Composable
fun JoystickPad(sender: KeySender, modifier: Modifier = Modifier) {
    val inset = Trs80Theme.spacing.gap
    Box(modifier.fillMaxWidth().height(PAD_HEIGHT)) {
        FirePad(sender, Modifier.fillMaxSize())
        Stick(
            sender,
            Modifier
                .align(Alignment.BottomEnd)
                .padding(inset)
                .size(PAD_HEIGHT - inset * 2),
        )
    }
}

/**
 * Fire, and nothing else.
 *
 * What the tilt layout leaves on screen: the machine is steered by moving the
 * device, so the only thing a finger still has to do is shoot.
 */
@Composable
fun FirePad(sender: KeySender, modifier: Modifier = Modifier) {
    val colors = Trs80Theme.colors
    var down by remember { mutableStateOf(false) }
    Box(
        modifier.pointerInput(sender) {
            awaitEachGesture {
                var change = awaitFirstDown()
                down = true
                sender.press(KEY_FIRE)
                while (change.pressed) {
                    change = awaitPointerEvent().changes.firstOrNull() ?: break
                }
                down = false
                sender.release(KEY_FIRE)
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(Res.string.fire),
            style = Trs80Theme.type.kicker,
            color = colors.muted.copy(alpha = if (down) 0.9f else 0.4f),
        )
    }
}

/** The tilt layout's surface: the whole width, and only fire on it. */
@Composable
fun TiltPad(sender: KeySender, modifier: Modifier = Modifier) {
    FirePad(sender, modifier.fillMaxWidth().height(PAD_HEIGHT))
}

/** The stick: a ring, and a knob that follows the finger as far as the ring. */
@Composable
private fun Stick(sender: KeySender, modifier: Modifier = Modifier) {
    val colors = Trs80Theme.colors
    val directions = remember(sender) { DirectionKeys(sender) }
    val density = LocalDensity.current
    val knobPx = with(density) { KNOB_RADIUS.toPx() }
    val hairlinePx = with(density) { Trs80Theme.spacing.hairline.toPx() }
    var knob by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier.pointerInput(sender) {
            awaitEachGesture {
                val centre = Offset(size.width / 2f, size.height / 2f)
                var change = awaitFirstDown()
                while (change.pressed) {
                    knob = change.position
                    // Screen y grows downwards, and a stick's does not.
                    directions.set(
                        Direction.of(
                            change.position.x - centre.x,
                            centre.y - change.position.y,
                            knobPx,
                        )
                    )
                    change = awaitPointerEvent().changes.firstOrNull() ?: break
                }
                knob = null
                directions.releaseAll()
            }
        }
    ) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val ring = size.width / 2f - knobPx
        drawCircle(
            color = colors.text.copy(alpha = 0.18f),
            radius = ring,
            center = centre,
            style = Stroke(width = hairlinePx),
        )
        val at = knob
        val position = if (at == null) {
            centre
        } else {
            val dx = at.x - centre.x
            val dy = at.y - centre.y
            val distance = hypot(dx, dy)
            // Past the ring the knob stops, but keeps the finger's direction.
            if (distance > ring) {
                Offset(centre.x + dx / distance * ring, centre.y + dy / distance * ring)
            } else {
                at
            }
        }
        drawCircle(
            color = colors.accent.copy(alpha = if (at == null) 0.5f else 0.85f),
            radius = knobPx,
            center = position,
        )
    }
}

private fun DirectionKeys.set(direction: Direction) =
    set(direction.left, direction.right, direction.up, direction.down)
