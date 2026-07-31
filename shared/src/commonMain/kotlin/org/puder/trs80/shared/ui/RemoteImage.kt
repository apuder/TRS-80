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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.puder.trs80.shared.Log
import org.puder.trs80.shared.io.httpGetBytes

private const val TAG = "RemoteImage"

/**
 * The images fetched so far, so scrolling a list back does not fetch again.
 *
 * Deliberately small and deliberately never evicted from: the store's catalogue
 * is a few dozen covers, and the alternative is a cache with a policy nobody has
 * measured. If it ever holds enough to matter, that is the point to give it one.
 */
private val cache = mutableMapOf<String, ImageBitmap>()

/**
 * An image fetched from [url], showing [placeholder] until it arrives.
 *
 * Android has Glide for this and the shared code has nothing, so this is the
 * small amount of it that a list of covers actually needs: fetch once, decode
 * off the main thread, keep it, and never fail loudly — a missing cover is a
 * blank box, not an error, because it is decoration.
 */
@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {},
) {
    var image by remember(url) { mutableStateOf(url?.let(cache::get)) }

    LaunchedEffect(url) {
        if (url == null || image != null) {
            return@LaunchedEffect
        }
        image = withContext(Dispatchers.Default) {
            try {
                decodeImage(httpGetBytes(url))?.also { cache[url] = it }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load $url: $e")
                null
            }
        }
    }

    val loaded = image
    if (loaded == null) {
        Box(modifier) { placeholder() }
    } else {
        Image(
            bitmap = loaded,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}
