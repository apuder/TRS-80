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

package org.puder.trs80.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import org.puder.trs80.EmulatorActivity
import org.puder.trs80.R
import org.puder.trs80.TypefaceCache
import kotlin.math.min

/**
 * A single key of the on-screen TRS-80 keyboard.
 *
 * Keys are inflated from the keyboard layouts, which supply the key id, the shifted key id and
 * the width of the key through the `Keyboard` styleable. The hosting context must be an
 * [EmulatorActivity], since the key takes its dimensions and its [KeyboardManager] from it.
 */
@SuppressLint("ClickableViewAccessibility", "CustomViewStyleable")
class Key @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0) : View(context, attrs, defStyle) {

    private val keyboard: KeyboardManager
    private val keyWidth: Int
    private val keyHeight: Int
    private val keyMargin: Int

    private val idNormal: Int
    private val idShifted: Int
    private val size: Int
    private val labelNormal: String
    private val labelShifted: String
    private val isShiftKey: Boolean
    private val isAltKey: Boolean

    private val paint = Paint()
    private val rect = RectF()

    private var isShifted = false
    private var isKeyPressed = false
    private var posX = -1
    private var posY = -1

    /** The two halves of the compact keyboard, resolved on first use, as the original did. */
    private val keyboardView1: View by lazy(LazyThreadSafetyMode.NONE) {
        rootView.findViewById(R.id.keyboard_view_1)
    }
    private val keyboardView2: View by lazy(LazyThreadSafetyMode.NONE) {
        rootView.findViewById(R.id.keyboard_view_2)
    }

    init {
        setBackgroundResource(R.drawable.key_background)

        val emulator = context as EmulatorActivity
        keyboard = emulator.keyboardManager
        keyWidth = emulator.keyWidth
        keyHeight = emulator.keyHeight
        keyMargin = emulator.keyMargin

        val ta = context.obtainStyledAttributes(attrs, R.styleable.Keyboard, 0, 0)
        idNormal = ta.getInt(R.styleable.Keyboard_id, -1)
        idShifted = ta.getInt(R.styleable.Keyboard_idShifted, -1)
        labelNormal = if (idNormal == TK_ALT) "Alt" else keyboard.getKeyMap(idNormal).label
        size = ta.getInteger(R.styleable.Keyboard_size, 1)
        ta.recycle()

        isShiftKey = idNormal == TK_SHIFT_LEFT || idNormal == TK_SHIFT_RIGHT
        isAltKey = idNormal == TK_ALT

        var shiftedLabel: String? = null
        if (idShifted != -1) {
            shiftedLabel = keyboard.getKeyMap(idShifted).label
            keyboard.addShiftableKey(this)
        }
        if (isShiftKey) {
            // A shift key keeps its own label when shifted.
            shiftedLabel = labelNormal
            keyboard.addShiftableKey(this)
        }
        labelShifted = shiftedLabel ?: labelNormal

        paint.typeface = TypefaceCache.getTypeface(FONT_PATH, context)
        paint.isAntiAlias = true
        paint.textSize = keyHeight * (if (labelNormal.length > 1) 0.4f else 0.6f)

        setOnTouchListener { _, event ->
            handleTouch(event.action and MotionEvent.ACTION_MASK)
            true
        }
    }

    /** Places the key at an absolute offset instead of using the uniform key margin. */
    fun setPosition(x: Int, y: Int) {
        posX = x
        posY = y
    }

    /** Switches the key to its shifted label, if it is not already showing it. */
    fun shift() {
        if (!isShifted) {
            isShifted = true
            invalidate()
        }
    }

    /** Switches the key back to its normal label, if it is not already showing it. */
    fun unshift() {
        if (isShifted) {
            isShifted = false
            invalidate()
        }
    }

    /** Toggles between the two halves of the compact keyboard layout. */
    fun switchKeyboard() {
        if (keyboardView1.visibility == View.GONE) {
            keyboardView1.visibility = View.VISIBLE
            keyboardView2.visibility = View.GONE
        } else {
            keyboardView1.visibility = View.GONE
            keyboardView2.visibility = View.VISIBLE
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (isShifted) {
            fillRoundRect(canvas, SHIFTED_ALPHA)
        }
        if (isKeyPressed) {
            fillRoundRect(canvas, PRESSED_ALPHA)
        }

        paint.color = Color.GRAY
        paint.alpha = 130
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)

        paint.color = resources.getColor(R.color.key_color, null)
        paint.alpha = 110
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 1f
        paint.textAlign = Paint.Align.CENTER
        // Both coordinates are deliberately truncated to whole pixels.
        val xPos = (rect.right / 2).toInt().toFloat()
        val yPos = (rect.bottom / 2 - (paint.descent() + paint.ascent()) / 2).toInt().toFloat()
        canvas.drawText(if (isShifted) labelShifted else labelNormal, xPos, yPos, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = keyWidth * size
        val desiredHeight = keyHeight

        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> min(desiredWidth, MeasureSpec.getSize(widthMeasureSpec))
            else -> desiredWidth
        }
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> min(desiredHeight, MeasureSpec.getSize(heightMeasureSpec))
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
        rect.set(1f, 1f, (width - 1).toFloat(), (height - 1).toFloat())

        val params = layoutParams as ViewGroup.MarginLayoutParams
        if (posX != -1 && posY != -1) {
            params.setMargins(posX, posY, 0, 0)
        } else {
            params.setMargins(keyMargin, keyMargin, keyMargin, keyMargin)
        }
        layoutParams = params
    }

    /**
     * Turns a masked [MotionEvent] action into emulator key events. Note that a normal key sends
     * its key-down on touch-down but a shift key toggles on touch-up, since it has to latch.
     */
    private fun handleTouch(action: Int) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                isKeyPressed = true
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                isKeyPressed = false
                invalidate()
            }
        }

        if (isAltKey) {
            if (action == MotionEvent.ACTION_UP) {
                switchKeyboard()
            }
            return
        }

        if (action == MotionEvent.ACTION_DOWN && !isShiftKey) {
            keyboard.keyDown(activeId())
        }

        if (action != MotionEvent.ACTION_UP) {
            return
        }

        if (isShiftKey) {
            if (isShifted) {
                keyboard.unshiftKeys()
                keyboard.keyUp(idNormal)
            } else {
                keyboard.shiftKeys(idNormal)
                keyboard.keyDown(idNormal)
            }
            return
        }

        keyboard.keyUp(activeId())
        // Releasing any other key also releases a latched shift key.
        when (val shiftKey = keyboard.pressedShiftKey) {
            TK_SHIFT_LEFT, TK_SHIFT_RIGHT -> {
                keyboard.unshiftKeys()
                keyboard.keyUp(shiftKey)
            }
        }
    }

    /** @return The key id this key currently stands for, honouring the shift state. */
    private fun activeId(): Int = if (isShifted && idShifted != -1) idShifted else idNormal

    private fun fillRoundRect(canvas: Canvas, alpha: Int) {
        paint.color = Color.WHITE
        paint.alpha = alpha
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
    }

    companion object {
        const val TK_NONE = -1
        const val TK_0 = 0
        const val TK_1 = 1
        const val TK_2 = 2
        const val TK_3 = 3
        const val TK_4 = 4
        const val TK_5 = 5
        const val TK_6 = 6
        const val TK_7 = 7
        const val TK_8 = 8
        const val TK_9 = 9
        const val TK_A = 10
        const val TK_B = 11
        const val TK_C = 12
        const val TK_D = 13
        const val TK_E = 14
        const val TK_F = 15
        const val TK_G = 16
        const val TK_H = 17
        const val TK_I = 18
        const val TK_J = 19
        const val TK_K = 20
        const val TK_L = 21
        const val TK_M = 22
        const val TK_N = 23
        const val TK_O = 24
        const val TK_P = 25
        const val TK_Q = 26
        const val TK_R = 27
        const val TK_S = 28
        const val TK_T = 29
        const val TK_U = 30
        const val TK_V = 31
        const val TK_W = 32
        const val TK_X = 33
        const val TK_Y = 34
        const val TK_Z = 35
        const val TK_COMMA = 36
        const val TK_DOT = 37
        const val TK_SLASH = 38
        const val TK_SPACE = 39
        const val TK_ADD = 40
        const val TK_HASH = 41
        const val TK_BR_OPEN = 42
        const val TK_BR_CLOSE = 43
        const val TK_ASTERIX = 44
        const val TK_DOLLAR = 45
        const val TK_QUESTION = 46
        const val TK_LT = 47
        const val TK_GT = 48
        const val TK_EQUAL = 49
        const val TK_PERCENT = 50
        const val TK_APOS = 51
        const val TK_EXCLAMATION_MARK = 52
        const val TK_AMP = 53
        const val TK_QUOT = 54
        const val TK_SEMICOLON = 55
        const val TK_ENTER = 56
        const val TK_CLEAR = 57
        const val TK_CLEAR_SHORT = 58
        const val TK_SHIFT_LEFT = 59
        const val TK_SHIFT_RIGHT = 60
        const val TK_COLON = 61
        const val TK_MINUS = 62
        const val TK_BREAK = 63
        const val TK_BREAK_SHORT = 64
        const val TK_UP = 65
        const val TK_AT = 66
        const val TK_LEFT = 67
        const val TK_RIGHT = 68
        const val TK_DOWN = 69
        const val TK_ALT = 70

        private const val FONT_PATH = "fonts/DejaVuSansMono.ttf"
        private const val CORNER_RADIUS = 10f
        private const val SHIFTED_ALPHA = 70
        private const val PRESSED_ALPHA = 95
    }
}
