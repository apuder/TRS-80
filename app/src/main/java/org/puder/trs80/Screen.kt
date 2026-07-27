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
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * The surface the emulated TRS-80 screen is drawn into by `RenderThread`. It owns no
 * rendering logic itself; it only hands its holder to the hosting [EmulatorActivity], which
 * forwards it to the render thread.
 *
 * Inflated from `res/layout/emulator.xml`, so the `(Context, AttributeSet)` constructor has
 * to remain available to the framework.
 */
class Screen @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        publishSurfaceHolder(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        publishSurfaceHolder(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        publishSurfaceHolder(null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val emulator = context as EmulatorActivity
        setMeasuredDimension(
            emulator.screenWidth or MeasureSpec.EXACTLY,
            emulator.screenHeight or MeasureSpec.EXACTLY
        )
    }

    private fun publishSurfaceHolder(holder: SurfaceHolder?) {
        (context as EmulatorActivity).setSurfaceHolder(holder)
    }
}
