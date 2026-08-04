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
 * How big a picture the machine is asked to draw, at most.
 *
 * The core rasterizes at whatever cell size it is given, and the natural thing
 * is to give it the size the picture will be drawn at: then every glyph pixel
 * lands on a whole screen pixel and nothing is interpolated. That is what the
 * devices do, and what this is unlimited for there.
 *
 * A browser cannot afford it. The frame has to cross from the emulator's memory
 * into Kotlin's, one array at a time, and that copy costs about four
 * nanoseconds a byte -- fine for the 700KB a phone-sized picture needs, and
 * twenty-six milliseconds a frame for a MacBook's full screen, which is most of
 * a frame's budget spent moving bytes that were about to be scaled anyway.
 *
 * So on the web the machine draws smaller and the picture is scaled up. It
 * costs nothing to look at: what it draws is a one-bit character screen, the
 * drawing is nearest-neighbour (see EmulatorScreen), and the cell is divided by
 * a whole number so every emulated pixel becomes the same square block.
 */
expect val maxRasterPixels: Int
