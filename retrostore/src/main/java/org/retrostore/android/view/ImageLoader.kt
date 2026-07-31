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

package org.retrostore.android.view

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutionException

private const val TAG = "ImageLoader"

/**
 * Loads images from a URL into an image view.
 *
 * The context is only held weakly, so requests made after it went away are silently dropped.
 */
class ImageLoader(context: Context) {
    private val contextRef = WeakReference(context)

    /** Loads the image at [url] into [view], scaled with a centre crop. */
    fun loadUrlIntoView(url: String, view: ImageView) {
        contextRef.get()?.let { Glide.with(it).load(url).centerCrop().into(view) }
    }

    /**
     * Loads the image at [url] as a bitmap of [width] x [height] pixels. The blocking Glide call
     * runs on [Dispatchers.IO], so this is safe to call from the main thread.
     *
     * @return The loaded bitmap.
     * @throws RuntimeException if the context went away.
     * @throws ExecutionException if the image could not be loaded.
     */
    suspend fun loadAsBitmapAsync(url: String, width: Int, height: Int): Bitmap {
        val context = contextRef.get() ?: throw RuntimeException("Context invalid.")

        return withContext(Dispatchers.IO) {
            try {
                Glide.with(context).asBitmap().load(url).submit(width, height).get()
            } catch (e: Exception) {
                if (e is InterruptedException || e is ExecutionException) {
                    Log.e(TAG, "Could not load image as bitmap.", e)
                }
                throw e
            }
        }
    }

    companion object {
        /** @return A new loader that loads images on [Dispatchers.IO], using [context]. */
        @JvmStatic
        fun get(context: Context): ImageLoader = ImageLoader(context)
    }
}
