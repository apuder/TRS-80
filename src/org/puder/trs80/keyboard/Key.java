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
 * below. The XML layout file contains the memory address and bit mask via
 * custom XML attributes keyboard:address and keyboard:mask. The emulator runs
 * in a separate thread and will 'see' that a bit is flipped at a certain memory
 * address and will interpret this as a key-press. See the following link for
 * the memory-mapped IO address of the Model 3 keyboard:
 * 
 * http://www.trs-80.com/wordpress/zaps-patches-pokes-tips/internals/#keyboard13
 */
public class Key extends View {

    private String          label;
    private String          labelShifted;
    private boolean         isShifted;
    private boolean         isPressed;
    private boolean         isShiftKey;
    private boolean         isAltKey;

    private View            keyboardView1;
    private View            keyboardView2;

    private int             address;
    private byte            mask;
    private int             address2;
    private byte            mask2;
    private int             size;
    private Paint           paint;
    private byte[]          memBuffer;
    private RectF           rect;

    private KeyboardManager keyboard;

    private int             keyWidth;
    private int             keyHeight;
    private int             keyMargin;

    private int             posX = -1;
    private int             posY = -1;

    public Key(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setBackgroundResource(R.drawable.key_background);

        Hardware h = TRS80Application.getHardware();
        keyWidth = h.getKeyWidth();
        keyHeight = h.getKeyHeight();
        keyMargin = h.getKeyMargin();

        keyboard = TRS80Application.getKeyboard();
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Keyboard, 0, 0);
        label = ta.getString(R.styleable.Keyboard_label);
        address = ta.getInteger(R.styleable.Keyboard_address, -1);
        address2 = ta.getInteger(R.styleable.Keyboard_address2, -1);
        mask = (byte) ta.getInteger(R.styleable.Keyboard_mask, -1);
        mask2 = (byte) ta.getInteger(R.styleable.Keyboard_mask2, -1);
        size = ta.getInteger(R.styleable.Keyboard_size, 1);
        ta.recycle();

        int idx = label.indexOf("|");
        if (idx > 0) {
            labelShifted = label.substring(0, idx);
            label = label.substring(idx + 1);
            keyboard.addShiftableKey(this);
        }
        isShiftKey = label.equals("SHIFT");
        if (isShiftKey) {
            labelShifted = label;
            keyboard.addShiftableKey(this);
        }
        isShifted = false;
        isPressed = false;
        isAltKey = label.equals("Alt");
        paint = new Paint();
        paint.setTypeface(TRS80Application.getTypeface());
        paint.setAntiAlias(true);
        float textSizeScale = label.length() > 1 ? 0.4f : 0.6f;
        paint.setTextSize(keyHeight * textSizeScale);

        memBuffer = TRS80Application.getHardware().getMemoryBuffer();
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
                    memBuffer[address] |= mask;
                    if (address2 != -1) {
                        memBuffer[address2] |= mask2;
                    }
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (isShiftKey) {
                        if (!isShifted) {
                            keyboard.shiftKeys();
                        } else {
                            memBuffer[address] &= ~mask;
                            keyboard.unshiftKeys();
                        }
                    } else {
                        memBuffer[address] &= ~mask;
                        if (address2 != -1) {
                            memBuffer[address2] &= ~mask2;
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
        canvas.drawText(isShifted ? labelShifted : label, xPos, yPos, paint);
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
            if (isShiftKey) {
                memBuffer[address] &= ~mask;
            }
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
