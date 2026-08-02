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
import androidx.compose.ui.platform.LocalWindowInfo

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
