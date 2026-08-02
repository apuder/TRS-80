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

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.Trs80Theme

/** How long a message stays up before it goes on its own. */
private const val TOAST_MILLIS = 2600L

private const val FADE_MILLIS = 180

/**
 * A short message that says what just happened and then leaves.
 *
 * Written here rather than taken from the platform. Android has `Toast` and iOS
 * has nothing of the kind, so a shared screen cannot ask for one; and the app
 * draws in its own register everywhere else, which a system toast would break
 * on the one platform that has them.
 *
 * Nothing is interactive: it reports, it does not ask. Anything worth a decision
 * gets a panel.
 */
@Composable
fun Toast(message: String?, onDismissed: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Trs80Theme.colors
    var showing by remember(message) { mutableStateOf(message != null) }

    LaunchedEffect(message) {
        if (message == null) {
            return@LaunchedEffect
        }
        showing = true
        delay(TOAST_MILLIS)
        showing = false
        // Let the fade finish before the caller forgets the text, or it would
        // vanish rather than fade.
        delay(FADE_MILLIS.toLong())
        onDismissed()
    }

    // Laid out over everything but hit-testing nothing: a message that swallows
    // a tap on whatever it happens to cover is worse than one that is missed.
    Box(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        AnimatedVisibility(
            visible = showing && message != null,
            enter = fadeIn(tween(FADE_MILLIS)),
            exit = fadeOut(tween(FADE_MILLIS)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp),
        ) {
            Box(
                Modifier
                    .padding(horizontal = 24.dp)
                    .background(colors.ground)
                    .border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    message.orEmpty(),
                    style = Trs80Theme.type.bodySmall,
                    color = colors.text,
                )
            }
        }
    }
}
