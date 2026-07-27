/*
 * Copyright 2017, Sascha Haeberling
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
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val TAG = "ImageLoader"

/**
 * Loads images from a URL into an image view.
 *
 * The context is only held weakly, so requests made after it went away are silently dropped.
 */
class ImageLoader(context: Context, private val executor: Executor) {
    private val contextRef = WeakReference(context)

    /** Loads the image at [url] into [view], scaled with a centre crop. */
    fun loadUrlIntoView(url: String, view: ImageView) {
        contextRef.get()?.let { Glide.with(it).load(url).centerCrop().into(view) }
    }

    /**
     * Asynchronously loads the image at [url] as a bitmap of [width] x [height] pixels.
     *
     * @return A future with the bitmap, which fails if the context went away or the image could
     * not be loaded.
     */
    fun loadAsBitmapAsync(url: String, width: Int, height: Int): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        val context = contextRef.get()
        if (context == null) {
            future.setException(RuntimeException("Context invalid."))
            return future
        }

        executor.execute {
            try {
                future.set(Glide.with(context).asBitmap().load(url).submit(width, height).get())
            } catch (e: Exception) {
                when (e) {
                    is InterruptedException, is ExecutionException -> {
                        Log.e(TAG, "Could not load image as bitmap.", e)
                        future.setException(e)
                    }
                    else -> throw e
                }
            }
        }
        return future
    }

    companion object {
        /** @return A new loader that loads images on a cached thread pool, using [context]. */
        @JvmStatic
        fun get(context: Context): ImageLoader =
                ImageLoader(context, Executors.newCachedThreadPool())
    }
}
