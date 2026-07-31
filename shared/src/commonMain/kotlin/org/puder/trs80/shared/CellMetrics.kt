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

package org.puder.trs80.shared

/**
 * The geometry of the emulated screen: its size in character cells, and the size that one
 * cell is drawn at.
 *
 * The cell size depends on the display the emulator was laid out for, so this is only known
 * once the host has measured its screen.
 *
 * @property columns the number of character cells per row.
 * @property rows the number of rows of character cells.
 * @property cellWidth the width of one character cell, in pixels.
 * @property cellHeight the height of one character cell, in pixels.
 */
data class CellMetrics(
        val columns: Int,
        val rows: Int,
        val cellWidth: Int,
        val cellHeight: Int)
