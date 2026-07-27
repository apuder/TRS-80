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

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.TranslateAnimation
import android.widget.ViewAnimator

/**
 * Creates [Animation] objects for some of the most common animations, including a 3D flip
 * animation, [FlipAnimation]. Utility methods are provided for initiating fade-in-then-out and
 * flip animations.
 *
 * @author Ephraim A. Tekle
 */
object AnimationFactory {

    private const val DEFAULT_FLIP_TRANSITION_DURATION = 300L
    private const val DEFAULT_FADE_DURATION = 500L

    /**
     * The most typical flip view transitions: left-to-right and right-to-left. Used when creating
     * [FlipAnimation] animations.
     *
     * @author Ephraim A. Tekle
     */
    enum class FlipDirection {
        LEFT_RIGHT,
        RIGHT_LEFT,
        TOP_BOTTOM,
        BOTTOM_TOP;

        val startDegreeForFirstView: Float
            get() = 0f

        val startDegreeForSecondView: Float
            get() = when (this) {
                LEFT_RIGHT, TOP_BOTTOM -> -90f
                RIGHT_LEFT, BOTTOM_TOP -> 90f
            }

        val endDegreeForFirstView: Float
            get() = when (this) {
                LEFT_RIGHT, TOP_BOTTOM -> 90f
                RIGHT_LEFT, BOTTOM_TOP -> -90f
            }

        val endDegreeForSecondView: Float
            get() = 0f

        /** @return The direction that undoes this one. */
        val theOtherDirection: FlipDirection
            get() = when (this) {
                LEFT_RIGHT -> RIGHT_LEFT
                RIGHT_LEFT -> LEFT_RIGHT
                TOP_BOTTOM -> BOTTOM_TOP
                BOTTOM_TOP -> TOP_BOTTOM
            }

        /** Whether flipping in this direction rotates around the x-axis rather than the y-axis. */
        internal val rotationAxis: Int
            get() = when (this) {
                TOP_BOTTOM, BOTTOM_TOP -> FlipAnimation.ROTATION_X
                LEFT_RIGHT, RIGHT_LEFT -> FlipAnimation.ROTATION_Y
            }
    }

    /**
     * Creates a pair of [FlipAnimation]s that can be used for a 3D flip transition from
     * [fromView] to [toView]. A typical use case is with a [ViewAnimator] as an out and in
     * transition.
     *
     * NOTE: Avoid using this method. Instead, use [flipTransition].
     *
     * @param fromView the view to transition away from
     * @param toView the view to transition to
     * @param dir the flip direction
     * @param duration the transition duration in milliseconds
     * @param interpolator the interpolator to use (pass `null` to use an [AccelerateInterpolator])
     * @return The out animation followed by the in animation.
     */
    @JvmStatic
    fun flipAnimation(
            fromView: View,
            toView: View,
            dir: FlipDirection,
            duration: Long,
            interpolator: Interpolator?): Array<Animation> {
        val centerX = fromView.width / 2.0f
        val centerY = fromView.height / 2.0f

        val outFlip = FlipAnimation(
                dir.startDegreeForFirstView,
                dir.endDegreeForFirstView,
                centerX,
                centerY,
                FlipAnimation.SCALE_DEFAULT,
                FlipAnimation.ScaleUpDownEnum.SCALE_DOWN)
        outFlip.duration = duration
        outFlip.fillAfter = true
        outFlip.interpolator = interpolator ?: AccelerateInterpolator()
        outFlip.direction = dir.rotationAxis

        // The "to" view is measured with the same centre as the "from" view: when used with a
        // ViewFlipper its own layout is not established yet on first show.
        val inFlip = FlipAnimation(
                dir.startDegreeForSecondView,
                dir.endDegreeForSecondView,
                centerX,
                centerY,
                FlipAnimation.SCALE_DEFAULT,
                FlipAnimation.ScaleUpDownEnum.SCALE_UP)
        inFlip.duration = duration
        inFlip.fillAfter = true
        inFlip.interpolator = interpolator ?: AccelerateInterpolator()
        inFlip.startOffset = duration
        inFlip.direction = dir.rotationAxis

        return arrayOf<Animation>(
                AnimationSet(true).apply { addAnimation(outFlip) },
                AnimationSet(true).apply { addAnimation(inFlip) })
    }

    /**
     * Flips to the next of the [ViewAnimator]'s subviews. If the currently visible view is the
     * last one, the flip direction is reversed for this transition.
     *
     * @param viewAnimator the animator to flip
     * @param dir the direction of the flip
     * @param duration the transition duration in milliseconds
     */
    @JvmStatic
    @JvmOverloads
    fun flipTransition(
            viewAnimator: ViewAnimator,
            dir: FlipDirection,
            duration: Long = DEFAULT_FLIP_TRANSITION_DURATION) {
        val fromView = viewAnimator.currentView
        val currentIndex = viewAnimator.displayedChild
        val nextIndex = (currentIndex + 1) % viewAnimator.childCount
        val toView = viewAnimator.getChildAt(nextIndex)

        val animations = flipAnimation(
                fromView,
                toView,
                if (nextIndex < currentIndex) dir.theOtherDirection else dir,
                duration,
                null)

        viewAnimator.outAnimation = animations[0]
        viewAnimator.inAnimation = animations[1]
        viewAnimator.showNext()
    }

    /**
     * @param duration the animation duration in milliseconds
     * @param interpolator the interpolator to use (pass `null` to use an [AccelerateInterpolator])
     * @return A slide transition animation that enters a view from the left.
     */
    @JvmStatic
    fun inFromLeftAnimation(duration: Long, interpolator: Interpolator?): Animation =
            slide(-1.0f, 0.0f, 0.0f, 0.0f, duration, interpolator)

    /**
     * @param duration the animation duration in milliseconds
     * @param interpolator the interpolator to use (pass `null` to use an [AccelerateInterpolator])
     * @return A slide transition animation that hides a view by sliding it to the right.
     */
    @JvmStatic
    fun outToRightAnimation(duration: Long, interpolator: Interpolator?): Animation =
            slide(0.0f, 1.0f, 0.0f, 0.0f, duration, interpolator)

    /**
     * @param duration the animation duration in milliseconds
     * @param interpolator the interpolator to use (pass `null` to use an [AccelerateInterpolator])
     * @return A slide transition animation that enters a view from the right.
     */
    @JvmStatic
    fun inFromRightAnimation(duration: Long, interpolator: Interpolator?): Animation =
            slide(1.0f, 0.0f, 0.0f, 0.0f, duration, interpolator)

    /**
     * @param duration the animation duration in milliseconds
     * @param interpolator the interpolator to use (pass `null` to use an [AccelerateInterpolator])
     * @return A slide transition animation that hides a view by sliding it to the left.
     */
    @JvmStatic
    fun outToLeftAnimation(duration: Long, interpolator: Interpolator?): Animation =
            slide(0.0f, -1.0f, 0.0f, 0.0f, duration, interpolator)

    /**
     * @param duration the animation duration in milliseconds
     * @param interpolator the interpolator to use (pass `null` to use an [AccelerateInterpolator])
     * @return A slide transition animation that enters a view from the top.
     */
    @JvmStatic
    fun inFromTopAnimation(duration: Long, interpolator: Interpolator?): Animation =
            slide(0.0f, 0.0f, -1.0f, 0.0f, duration, interpolator)

    /**
     * @param duration the animation duration in milliseconds
     * @param interpolator the interpolator to use (pass `null` to use an [AccelerateInterpolator])
     * @return A slide transition animation that hides a view by sliding it to the top.
     */
    @JvmStatic
    fun outToTopAnimation(duration: Long, interpolator: Interpolator?): Animation =
            slide(0.0f, 0.0f, 0.0f, -1.0f, duration, interpolator)

    /**
     * @param duration the animation duration in milliseconds
     * @param delay how long to wait before starting the animation, in milliseconds
     * @return A fade animation that fades the subject in by changing alpha from 0 to 1.
     * @see fadeInAnimation
     */
    @JvmStatic
    fun fadeInAnimation(duration: Long, delay: Long): Animation =
            AlphaAnimation(0f, 1f).also {
                it.interpolator = DecelerateInterpolator()
                it.duration = duration
                it.startOffset = delay
            }

    /**
     * @param duration the animation duration in milliseconds
     * @param delay how long to wait before starting the animation, in milliseconds
     * @return A fade animation that fades the subject out by changing alpha from 1 to 0.
     * @see fadeOutAnimation
     */
    @JvmStatic
    fun fadeOutAnimation(duration: Long, delay: Long): Animation =
            AlphaAnimation(1f, 0f).also {
                it.interpolator = AccelerateInterpolator()
                it.startOffset = delay
                it.duration = duration
            }

    /**
     * @param duration ignored; the animation always runs for [DEFAULT_FADE_DURATION].
     * @param view the view to be faded in
     * @return A fade animation that sets the visibility of the view at the start and end of the
     * animation.
     */
    @JvmStatic
    fun fadeInAnimation(duration: Long, view: View): Animation =
            fadeInAnimation(DEFAULT_FADE_DURATION, 0L).also {
                it.setAnimationListener(
                        visibilityListener(view, atStart = View.GONE, atEnd = View.VISIBLE))
            }

    /**
     * @param duration ignored; the animation always runs for [DEFAULT_FADE_DURATION].
     * @param view the view to be faded out
     * @return A fade animation that sets the visibility of the view at the start and end of the
     * animation.
     */
    @JvmStatic
    fun fadeOutAnimation(duration: Long, view: View): Animation =
            fadeOutAnimation(DEFAULT_FADE_DURATION, 0L).also {
                it.setAnimationListener(
                        visibilityListener(view, atStart = View.VISIBLE, atEnd = View.GONE))
            }

    /**
     * @param duration the animation duration in milliseconds
     * @param delay how long to wait after fading in the subject and before starting the fade out
     * @return A fade-in animation followed by a fade-out animation.
     */
    @JvmStatic
    fun fadeInThenOutAnimation(duration: Long, delay: Long): Array<Animation> =
            arrayOf(
                    fadeInAnimation(duration, 0L),
                    fadeOutAnimation(duration, duration + delay))

    /**
     * Fades the view out. The animation starts right away.
     *
     * @param v the view to be faded out
     */
    @JvmStatic
    fun fadeOut(v: View?) {
        v?.let { it.startAnimation(fadeOutAnimation(DEFAULT_FADE_DURATION, it)) }
    }

    /**
     * Fades the view in. The animation starts right away.
     *
     * @param v the view to be faded in
     */
    @JvmStatic
    fun fadeIn(v: View?) {
        v?.let { it.startAnimation(fadeInAnimation(DEFAULT_FADE_DURATION, it)) }
    }

    /**
     * Fades the view in, waits the specified amount of time, then fades the view out.
     *
     * @param v the view to be faded in then out
     * @param delay how long the view stays visible
     */
    @JvmStatic
    fun fadeInThenOut(v: View?, delay: Long) {
        if (v == null) {
            return
        }
        v.visibility = View.VISIBLE
        val fadeInOut = fadeInThenOutAnimation(DEFAULT_FADE_DURATION, delay)
        val animation = AnimationSet(true)
        animation.addAnimation(fadeInOut[0])
        animation.addAnimation(fadeInOut[1])
        animation.setAnimationListener(
                visibilityListener(v, atStart = View.VISIBLE, atEnd = View.GONE))
        v.startAnimation(animation)
    }

    /** Builds a translate animation between positions relative to the parent. */
    private fun slide(
            fromX: Float,
            toX: Float,
            fromY: Float,
            toY: Float,
            duration: Long,
            interpolator: Interpolator?): Animation =
            TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, fromX,
                    Animation.RELATIVE_TO_PARENT, toX,
                    Animation.RELATIVE_TO_PARENT, fromY,
                    Animation.RELATIVE_TO_PARENT, toY).also {
                it.duration = duration
                it.interpolator = interpolator ?: AccelerateInterpolator()
            }

    /** A listener that forces [view] into a defined visibility at both ends of the animation. */
    private fun visibilityListener(view: View, atStart: Int, atEnd: Int) =
            object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    view.visibility = atStart
                }

                override fun onAnimationEnd(animation: Animation?) {
                    view.visibility = atEnd
                }

                override fun onAnimationRepeat(animation: Animation?) = Unit
            }
}
