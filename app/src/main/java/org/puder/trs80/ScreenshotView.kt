/*
 * Copyright 2012-2013, Arno Puder
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

package org.puder.trs80

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.ceil

/** Height-to-width ratio used while there is no screenshot to take the ratio from. */
private const val ASPECT_RATIO = 0.75f

/**
 * Shows the screenshot of a configuration, sized to the aspect ratio of the image. Until a
 * screenshot exists, it shows a "start the emulator" placeholder instead.
 *
 * Inflated from `res/layout/configuration_item.xml`, so the `(Context, AttributeSet)`
 * constructor has to remain available to the framework.
 */
class ScreenshotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val screenshot = ImageView(context)
    private var hasScreenshot = false

    init {
        addView(screenshot)
    }

    /** Shows [img], or falls back to the placeholder when it is `null`. */
    fun setScreenshotBitmap(img: Bitmap?) {
        if (img != null) {
            screenshot.setImageBitmap(img)
        }
        hasScreenshot = img != null
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int
        if (hasScreenshot) {
            val drawable = screenshot.drawable
            // Kept as a float multiply followed by a float divide, matching the original
            // rounding to the pixel.
            height = ceil(
                (width * drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth.toFloat())
                    .toDouble()
            ).toInt()
        } else {
            height = ceil((width * ASPECT_RATIO).toDouble()).toInt()
            val cached = placeholder
            if (width != 0 && (cached == null || cached.width != width)) {
                placeholder = createPlaceholder(width, height)
            }
            val bitmap = placeholder
            if (bitmap != null) {
                screenshot.setImageBitmap(bitmap)
            }
        }
        setMeasuredDimension(width, height)
        val mode = MeasureSpec.EXACTLY
        measureChildren(
            MeasureSpec.makeMeasureSpec(width, mode),
            MeasureSpec.makeMeasureSpec(height, mode)
        )
    }

    /** Draws the "start the emulator" icon centred on a black background. */
    private fun createPlaceholder(width: Int, height: Int): Bitmap {
        val icon = BitmapFactory.decodeResource(resources, R.drawable.start_emulator_icon)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        Canvas(bitmap).apply {
            drawColor(Color.BLACK)
            drawBitmap(
                icon,
                ((width - icon.width) / 2).toFloat(),
                ((height - icon.height) / 2).toFloat(),
                null
            )
        }
        return bitmap
    }

    companion object {
        /**
         * The placeholder, shared by every instance: they all measure to the same width, so
         * it only ever has to be drawn once.
         */
        private var placeholder: Bitmap? = null
    }
}
