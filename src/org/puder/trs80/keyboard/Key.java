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

package org.puder.trs80.keyboard;

import org.puder.trs80.Hardware;
import org.puder.trs80.R;
import org.puder.trs80.TRS80Application;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;

/**
 * The emulator features different keyboard layouts (original, compact, etc).
 * The XML layout resources can be found in res/layout/keyboard_*.xml. Class Key
 * implements the behavior of one key of the keyboard. Class Key is a custom
 * Android widget that is referenced from the aforementioned XML layout files.
 * Whenever the user 'clicks' on a key, class Key uses memory mapped IO to poke
 * a bit into the emulated RAM of the TRS-80. See the onTouch() method further
 * below. The custom XML attribute keyboard:id is used to cross-reference a key
 * map via the KeyboardManager (e.g., the Model 3 key map is stored in
 * keymap_model3.xml and loaded by KeyboardManager). This file contains the
 * memory addresses and bit mask for each key. The emulator runs in a separate
 * thread and will 'see' that a bit is flipped at a certain memory address and
 * will interpret this as a key-press. See the following link for the
 * memory-mapped IO address of the Model 3 keyboard:
 * 
 * http://www.trs-80.com/wordpress/zaps-patches-pokes-tips/internals/#keyboard13
 */
public class Key extends View {

    final static int        TK_0                = 0;
    final static int        TK_1                = 1;
    final static int        TK_2                = 2;
    final static int        TK_3                = 3;
    final static int        TK_4                = 4;
    final static int        TK_5                = 5;
    final static int        TK_6                = 6;
    final static int        TK_7                = 7;
    final static int        TK_8                = 8;
    final static int        TK_9                = 9;
    final static int        TK_A                = 10;
    final static int        TK_B                = 11;
    final static int        TK_C                = 12;
    final static int        TK_D                = 13;
    final static int        TK_E                = 14;
    final static int        TK_F                = 15;
    final static int        TK_G                = 16;
    final static int        TK_H                = 17;
    final static int        TK_I                = 18;
    final static int        TK_J                = 19;
    final static int        TK_K                = 20;
    final static int        TK_L                = 21;
    final static int        TK_M                = 22;
    final static int        TK_N                = 23;
    final static int        TK_O                = 24;
    final static int        TK_P                = 25;
    final static int        TK_Q                = 26;
    final static int        TK_R                = 27;
    final static int        TK_S                = 28;
    final static int        TK_T                = 29;
    final static int        TK_U                = 30;
    final static int        TK_V                = 31;
    final static int        TK_W                = 32;
    final static int        TK_X                = 33;
    final static int        TK_Y                = 34;
    final static int        TK_Z                = 35;
    final static int        TK_COMMA            = 36;
    final static int        TK_DOT              = 37;
    final static int        TK_SLASH            = 38;
    final static int        TK_SPACE            = 39;
    final static int        TK_ADD              = 40;
    final static int        TK_HASH             = 41;
    final static int        TK_BR_OPEN          = 42;
    final static int        TK_BR_CLOSE         = 43;
    final static int        TK_ASTERIX          = 44;
    final static int        TK_DOLLAR           = 45;
    final static int        TK_QUESTION         = 46;
    final static int        TK_LT               = 47;
    final static int        TK_GT               = 48;
    final static int        TK_EQUAL            = 49;
    final static int        TK_PERCENT          = 50;
    final static int        TK_APOS             = 51;
    final static int        TK_EXCLAMATION_MARK = 52;
    final static int        TK_AMP              = 53;
    final static int        TK_QUOT             = 54;
    final static int        TK_SEMICOLON        = 55;
    final static int        TK_ENTER            = 56;
    final static int        TK_CLEAR            = 57;
    final static int        TK_CLEAR_SHORT      = 58;
    final static int        TK_SHIFT_LEFT       = 59;
    final static int        TK_SHIFT_RIGHT      = 60;
    final static int        TK_COLON            = 61;
    final static int        TK_MINUS            = 62;
    final static int        TK_BREAK            = 63;
    final static int        TK_BREAK_SHORT      = 64;
    final static int        TK_UP               = 65;
    final static int        TK_AT               = 66;
    final static int        TK_LEFT             = 67;
    final static int        TK_RIGHT            = 68;
    final static int        TK_DOWN             = 69;
    final static int        TK_ALT              = 70;

    private int             idNormal;
    private int             idShifted;

    private String          labelNormal;
    private String          labelShifted;
    private boolean         isShifted;
    private boolean         isPressed;
    private boolean         isShiftKey;
    private boolean         isAltKey;

    private View            keyboardView1;
    private View            keyboardView2;

    private int             size;
    private Paint           paint;
    private RectF           rect;

    private KeyboardManager keyboard;

    private int             keyWidth;
    private int             keyHeight;
    private int             keyMargin;

    private int             posX                = -1;
    private int             posY                = -1;

    public Key(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setBackgroundResource(R.drawable.key_background);

        Hardware h = TRS80Application.getHardware();
        keyWidth = h.getKeyWidth();
        keyHeight = h.getKeyHeight();
        keyMargin = h.getKeyMargin();

        keyboard = TRS80Application.getKeyboardManager();
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Keyboard, 0, 0);
        idNormal = ta.getInt(R.styleable.Keyboard_id, -1);
        idShifted = ta.getInt(R.styleable.Keyboard_idShifted, -1);
        if (idNormal == TK_ALT) {
            labelNormal = "Alt";
        } else {
            KeyMap keyMap = keyboard.getKeyMap(idNormal);
            labelNormal = keyMap.label;
        }
        size = ta.getInteger(R.styleable.Keyboard_size, 1);
        ta.recycle();

        if (idShifted != -1) {
            KeyMap keyMap = keyboard.getKeyMap(idShifted);
            labelShifted = keyMap.label;
            keyboard.addShiftableKey(this);
        }
        isShiftKey = idNormal == TK_SHIFT_LEFT || idNormal == TK_SHIFT_RIGHT;
        if (isShiftKey) {
            labelShifted = labelNormal;
            keyboard.addShiftableKey(this);
        }
        isShifted = false;
        isPressed = false;
        isAltKey = idNormal == TK_ALT;
        paint = new Paint();
        paint.setTypeface(TRS80Application.getTypeface());
        paint.setAntiAlias(true);
        float textSizeScale = labelNormal.length() > 1 ? 0.4f : 0.6f;
        paint.setTextSize(keyHeight * textSizeScale);

        rect = new RectF();
        this.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                if (action == MotionEvent.ACTION_DOWN) {
                    isPressed = true;
                    invalidate();
                }
                if (action == MotionEvent.ACTION_UP) {
                    isPressed = false;
                    invalidate();
                }
                if (isAltKey) {
                    if (action == MotionEvent.ACTION_UP) {
                        switchKeyboard();
                    }
                    return true;
                }
                if (action == MotionEvent.ACTION_DOWN) {
                    if (isShifted && idShifted != -1) {
                        keyboard.keyDown(idShifted);
                    } else {
                        keyboard.keyDown(idNormal);
                    }
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (isShiftKey) {
                        if (!isShifted) {
                            keyboard.shiftKeys();
                        } else {
                            keyboard.unshiftKeys();
                        }
                    } else {
                        if (isShifted && idShifted != -1) {
                            keyboard.keyUp(idShifted);
                        } else {
                            keyboard.keyUp(idNormal);
                        }
                        keyboard.unshiftKeys();
                    }
                }
                return true;
            }
        });
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (isShifted) {
            paint.setColor(Color.WHITE);
            paint.setAlpha(70);
            paint.setStyle(Style.FILL);
            canvas.drawRoundRect(rect, 10, 10, paint);
        }

        if (isPressed) {
            paint.setColor(Color.WHITE);
            paint.setAlpha(95);
            paint.setStyle(Style.FILL);
            canvas.drawRoundRect(rect, 10, 10, paint);
        }

        paint.setColor(Color.GRAY);
        paint.setAlpha(130);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(3);
        canvas.drawRoundRect(rect, 10, 10, paint);

        paint.setColor(Color.WHITE);
        paint.setAlpha(110);
        paint.setStyle(Style.FILL);
        paint.setStrokeWidth(1);
        paint.setTextAlign(Align.CENTER);
        int xPos = (int) (rect.right / 2);
        int yPos = (int) ((rect.bottom / 2) - ((paint.descent() + paint.ascent()) / 2));
        canvas.drawText(isShifted ? labelShifted : labelNormal, xPos, yPos, paint);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (isAltKey) {
            // We can only do this here to ensure that this view has already
            // been
            // added to the view hierarchy
            keyboardView1 = this.getRootView().findViewById(R.id.keyboard_view_1);
            keyboardView2 = this.getRootView().findViewById(R.id.keyboard_view_2);
        }

        MarginLayoutParams params = (MarginLayoutParams) this.getLayoutParams();
        if (posX != -1 && posY != -1) {
            params.setMargins(posX, posY, 0, 0);
        } else {
            params.setMargins(keyMargin, keyMargin, keyMargin, keyMargin);
        }
        this.setLayoutParams(params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        if ((widthMeasureSpec & MeasureSpec.EXACTLY) != 0) {
            // Layout specifies specific size. Use that.
            width = widthMeasureSpec & ~MeasureSpec.EXACTLY;
        } else {
            width = keyWidth * size;
        }
        if ((heightMeasureSpec & MeasureSpec.EXACTLY) != 0) {
            // Layout specifies specific size. Use that.
            height = heightMeasureSpec & ~MeasureSpec.EXACTLY;
        } else {
            height = keyHeight;
        }
        setMeasuredDimension(width | MeasureSpec.EXACTLY, height | MeasureSpec.EXACTLY);
        rect.set(1, 1, width - 1, height - 1);
    }

    public void setPosition(int x, int y) {
        posX = x;
        posY = y;
    }

    public void shift() {
        if (!isShifted) {
            isShifted = true;
            invalidate();
        }
    }

    public void unshift() {
        if (isShifted) {
            isShifted = false;
            invalidate();
        }
    }

    public void switchKeyboard() {
        if (keyboardView1.getVisibility() == View.GONE) {
            keyboardView1.setVisibility(View.VISIBLE);
            keyboardView2.setVisibility(View.GONE);
        } else {
            keyboardView1.setVisibility(View.GONE);
            keyboardView2.setVisibility(View.VISIBLE);
        }
    }
}
