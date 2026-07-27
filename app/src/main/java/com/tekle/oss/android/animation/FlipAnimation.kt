/**
 * Copyright (c) 2012 Ephraim Tekle genzeb@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  @author Ephraim A. Tekle
 *
 */
package com.tekle.oss.android.animation

import android.graphics.Camera
import android.view.animation.Animation
import android.view.animation.Transformation

/**
 * Extends [Animation] to support a 3D flip view transition animation. Two instances of this class
 * are required: one for the "from" view and another for the "to" view.
 *
 * NOTE: use [AnimationFactory] to use this class.
 *
 * @param fromDegrees the start angle in degrees for a rotation along the y-axis, i.e. in-and-out
 * of the screen, i.e. 3D flip. This should really be a multiple of 90 degrees.
 * @param toDegrees the end angle in degrees for a rotation along the y-axis, i.e. in-and-out of
 * the screen, i.e. 3D flip. This should really be a multiple of 90 degrees.
 * @param centerX the x-axis value of the center of rotation
 * @param centerY the y-axis value of the center of rotation
 * @param scale to get a 3D effect, the transition views need to be zoomed (scaled). This value
 * must be between (0,1) or else the default scale [SCALE_DEFAULT] is used.
 * @param scaleType the flip view transition is broken down into two: the zoom-out of the "from"
 * view and the zoom-in of the "to" view. This parameter determines which is being done. See
 * [ScaleUpDownEnum].
 *
 * @author Ephraim A. Tekle
 */
class FlipAnimation(
        private val fromDegrees: Float,
        private val toDegrees: Float,
        private val centerX: Float,
        private val centerY: Float,
        scale: Float,
        scaleType: ScaleUpDownEnum?) : Animation() {

    private val scale = if (scale <= 0 || scale >= 1) SCALE_DEFAULT else scale
    private val scaleType = scaleType ?: ScaleUpDownEnum.SCALE_CYCLE

    /** The axis to rotate around, either [ROTATION_X] or [ROTATION_Y]. */
    var direction = ROTATION_Y

    private lateinit var camera: Camera

    override fun initialize(width: Int, height: Int, parentWidth: Int, parentHeight: Int) {
        super.initialize(width, height, parentWidth, parentHeight)
        camera = Camera()
    }

    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        val degrees = fromDegrees + (toDegrees - fromDegrees) * interpolatedTime
        val matrix = t.matrix

        camera.save()
        if (direction == ROTATION_X) {
            camera.rotateX(degrees)
        } else {
            camera.rotateY(degrees)
        }
        camera.getMatrix(matrix)
        camera.restore()

        matrix.preTranslate(-centerX, -centerY)
        matrix.postTranslate(centerX, centerY)

        val currentScale = scaleType.getScale(scale, interpolatedTime)
        matrix.preScale(currentScale, currentScale, centerX, centerY)
    }

    /**
     * Determines the zoom (or scale) behavior of a [FlipAnimation].
     *
     * @author Ephraim A. Tekle
     */
    enum class ScaleUpDownEnum {
        /** The view is scaled up from the scale value until it is at 100% zoom (i.e. no zoom). */
        SCALE_UP,

        /** The view is scaled down from no zoom (100%) until it is at the specified zoom level. */
        SCALE_DOWN,

        /** The view cycles through a zoom down and then a zoom up. */
        SCALE_CYCLE,

        /** No zoom effect is applied. */
        SCALE_NONE;

        /**
         * The intermittent zoom level given the current or desired maximum zoom level for the
         * specified iteration.
         *
         * @param max the maximum desired or current zoom level
         * @param iter the iteration (from 0..1).
         * @return the current zoom level
         */
        fun getScale(max: Float, iter: Float): Float = when (this) {
            SCALE_UP -> max + (1 - max) * iter
            SCALE_DOWN -> 1 - (1 - max) * iter
            SCALE_CYCLE -> if (iter > 0.5) {
                max + (1 - max) * (iter - 0.5f) * 2
            } else {
                1 - (1 - max) * (iter * 2)
            }

            SCALE_NONE -> 1f
        }
    }

    companion object {
        const val ROTATION_X = 0
        const val ROTATION_Y = 1

        /**
         * How much to scale up/down. The default scale of 75% of full size seems optimal based on
         * testing. Feel free to experiment away, however.
         */
        const val SCALE_DEFAULT = 0.75f
    }
}
