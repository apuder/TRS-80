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
 * Read access to the emulated machine's video RAM: one byte per screen cell, in row-major
 * order.
 *
 * The memory behind this is owned by the emulator core, which writes to it as the emulated
 * machine runs and without any synchronization with its readers. A reader that compares a
 * cell against something and then stores it must therefore read it only once, or it risks
 * comparing one value and storing another.
 */
interface ScreenBuffer {

    /** The byte in the cell at [index], counting left to right, then top to bottom. */
    operator fun get(index: Int): Byte
}
