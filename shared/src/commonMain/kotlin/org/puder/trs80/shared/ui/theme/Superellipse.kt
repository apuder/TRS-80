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

package org.puder.trs80.shared.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * How square the curve is: 2 is an ellipse, 4 is the icon shape, and the corner
 * keeps tightening from there towards a rectangle.
 *
 * Four is where a phone's icon mask sits, and it is the point of the shape --
 * the corner is round without the straight edges arriving suddenly, so a row of
 * these reads as one column of pictures rather than as a row of stickers.
 */
private const val ICON_EXPONENT = 4f

/**
 * How many points the outline is drawn with.
 *
 * The curve has no exact form in line and arc segments, so it is sampled.
 * Ninety-six is past the point where more of them change a pixel at the size
 * these are drawn.
 */
private const val SAMPLES = 96

/**
 * A superellipse: a rectangle whose corners are rounded by an equation rather
 * than by an arc.
 *
 * |x/a|^n + |y/b|^n = 1. What that buys over a rounded rectangle is that there
 * is no join: an arc meets a straight edge at a point where the curvature jumps,
 * and at icon sizes the eye finds it. Here the curvature falls away smoothly, so
 * the shape looks drawn rather than constructed.
 */
data class SuperellipseShape(private val exponent: Float = ICON_EXPONENT) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        for (step in 0 until SAMPLES) {
            val angle = step.toFloat() / SAMPLES * 2f * PI.toFloat()
            val point = superellipsePoint(angle, size, exponent)
            if (step == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * A point on the superellipse filling [size], at [angle] radians.
 *
 * The signed powers are what make it a Lamé curve rather than an ellipse: each
 * axis is taken to 2/n, so at n = 2 this is a circle and above it the curve
 * pushes out towards the corners.
 */
internal fun superellipsePoint(angle: Float, size: Size, exponent: Float): Offset {
    val halfWidth = size.width / 2f
    val halfHeight = size.height / 2f
    val cosine = cos(angle)
    val sine = sin(angle)
    val power = 2f / exponent
    return Offset(
        x = halfWidth + halfWidth * sign(cosine) * abs(cosine).pow(power),
        y = halfHeight + halfHeight * sign(sine) * abs(sine).pow(power),
    )
}

/** The shape a piece of cover art is cut to. */
val CoverShape: Shape = SuperellipseShape()
