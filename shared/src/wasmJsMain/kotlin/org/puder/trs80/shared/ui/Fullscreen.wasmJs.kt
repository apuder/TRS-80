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
 * Nothing to hide: a page has no system bars, and the browser's own chrome is
 * not ours to take. Full screen in a browser is a user gesture away -- the
 * Fullscreen API needs one -- so it belongs to a control, not to a screen
 * saying it would like the room.
 */
@Composable
actual fun Fullscreen(fullscreen: Boolean) = Unit

@Composable
actual fun SystemBarContents(light: Boolean) = Unit
