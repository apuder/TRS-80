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
 * Asks for the whole screen while [fullscreen] is true, and gives it back after.
 *
 * For the machine held sideways, where height is what is scarce: the picture is
 * as tall as the window allows, so a status bar across the top is picture the
 * user does not get. Everywhere else the bars stay, because everywhere else is
 * a list and a list is not worth hiding a clock for.
 *
 * A composable rather than a call, so that leaving the screen puts the bars back
 * without anyone remembering to.
 */
@Composable
expect fun Fullscreen(fullscreen: Boolean)
