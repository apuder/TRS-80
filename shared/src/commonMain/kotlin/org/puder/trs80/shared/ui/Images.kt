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

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes an encoded image -- in practice the PNG screenshot saved with a
 * configuration's emulator state.
 *
 * @return the image, or null if the bytes are not one. A screenshot that cannot
 * be read is a card without a picture, not a crash: it is a cache of something
 * the emulator can produce again.
 */
expect fun decodeImage(bytes: ByteArray): ImageBitmap?

/**
 * Encodes [image] as a PNG, for the screenshot stored with a configuration.
 *
 * @return the bytes, or null if the image could not be encoded.
 */
expect fun encodePng(image: ImageBitmap): ByteArray?
