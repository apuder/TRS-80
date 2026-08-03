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

/**
 * Nothing to do.
 *
 * iOS already hands the app the whole screen and draws its status bar over it,
 * and the safe-area insets the layout honours are the same either way. What
 * could be hidden is the home indicator, which is a hairline the machine's
 * picture is not fighting for.
 */
@Composable
actual fun Fullscreen(fullscreen: Boolean) = Unit
