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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun Fullscreen(fullscreen: Boolean) {
    val view = LocalView.current
    val window = (view.context.activity())?.window ?: return
    DisposableEffect(window, fullscreen) {
        val bars = WindowCompat.getInsetsController(window, view)
        if (fullscreen) {
            // Swiping from an edge brings them back for a moment and then they
            // go again: the machine takes most of its input from the same edges
            // the bars live on, and a bar that stays after one stray swipe would
            // sit on the keyboard for the rest of the session.
            bars.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            bars.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            bars.show(WindowInsetsCompat.Type.systemBars())
        }
        // Whatever happens, the app does not leave the phone without its bars.
        onDispose { bars.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

@Composable
actual fun SystemBarContents(light: Boolean) {
    val view = LocalView.current
    val window = (view.context.activity())?.window ?: return
    // Restored on the way out, not merely set on the way in. The themes nest --
    // the machine puts a dark one inside the app's -- and the outer one does not
    // recompose when the inner one goes, so without this the app came back from
    // a machine wearing the machine's white clock on its light ground.
    DisposableEffect(window, light) {
        val bars = WindowCompat.getInsetsController(window, view)
        // The properties are named for the *bar*, not for what is drawn in it:
        // "light status bars" means a light background, so dark icons.
        val was = bars.isAppearanceLightStatusBars
        bars.isAppearanceLightStatusBars = !light
        bars.isAppearanceLightNavigationBars = !light
        onDispose {
            bars.isAppearanceLightStatusBars = was
            bars.isAppearanceLightNavigationBars = was
        }
    }
}

/** The activity a composable is drawn in, through however many wrappers. */
private fun Context.activity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}
