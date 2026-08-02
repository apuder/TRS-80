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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * Whether the window is wider than it is tall.
 *
 * The window rather than the device: a phone that has been turned and a tablet
 * running the app in half the screen pose the same question, which is how much
 * height there is to spend. Asking the platform which way up it is would answer
 * a different one.
 */
@Composable
fun isLandscape(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    return size.width > size.height
}

/**
 * The narrowest window the library will put two panes in.
 *
 * Derived from what the layout needs rather than from a list of devices: the
 * list column is a fixed 380dp, and a detail pane narrower than about 400 is
 * worse than the sheet it replaces -- the record table alone wants 236. Add the
 * margins and 840 is where a second pane starts being an improvement. It is
 * also the conventional "expanded" width, which is not a coincidence: everyone
 * who has worked this out has landed near the same number.
 */
private val WIDE_ENOUGH = 840.dp

/**
 * And the shortest.
 *
 * This is the one that keeps phones out. A large phone turned sideways is the
 * widest-aspect thing this app runs on -- wider than any tablet -- and the least
 * suited to two panes, because it is barely 440dp tall. Height is what a pane
 * actually needs: it stacks a cover, a title, the actions, a description and a
 * row of screens.
 */
private val TALL_ENOUGH = 600.dp

/**
 * Whether there is room to show the list and one entry side by side.
 *
 * Measured, not proportioned. Aspect ratio answers a question about shape and
 * this is a question about quantity — the two come apart badly at both ends of
 * the range this has to cover. A phone in landscape is the widest thing here
 * (2.17) and must stay single-column; a foldable's inner screen is nearly
 * square (0.90 unfolded) and is the best case for two panes there is. Any ratio
 * that admits the tablet admits the phone.
 */
@Composable
fun isWideLayout(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val width = with(density) { size.width.toDp() }
    val height = with(density) { size.height.toDp() }
    return width >= WIDE_ENOUGH && height >= TALL_ENOUGH
}
