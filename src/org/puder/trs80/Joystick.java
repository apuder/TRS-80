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

package org.puder.trs80;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * http://www.trs-80.com/wordpress/zaps-patches-pokes-tips/internals/#keyboard13
 */
public class Joystick extends View {

    private static final String KEY_LEFT  = "\u2190";
    private static final String KEY_RIGHT = "\u2192";
    private static final String KEY_UP    = "\u2191";
    private static final String KEY_DOWN  = "\u2193";

    private Paint               paint;
    private byte[]              memBuffer;
    private float               innerRadius;
    private float               arrowRadius;

    public Joystick(Context context, AttributeSet attrs) {
        super(context, attrs);

        innerRadius = pxFromDp(30);
        arrowRadius = pxFromDp(55);
        float arrowSize = pxFromDp(25);

        paint = new Paint();
        paint.setTypeface(TRS80Application.getTypefaceBold());
        paint.setTextSize(arrowSize);
        paint.setTextAlign(Align.CENTER);
        paint.setAntiAlias(true);

        memBuffer = TRS80Application.getHardware().getMemoryBuffer();
        this.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                allKeysUp();
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    return true;
                }

                float cx = getWidth() / 2.0f;
                float cy = getHeight() / 2.0f;
                double dx = event.getX() - cx;
                double dy = cy - event.getY();

                if (Math.sqrt(dx * dx + dy * dy) < innerRadius) {
                    return true;
                }

                double angle = computeAngle(dx, dy);
                final double slice = 360 / 8;
                final double start = slice / 2;
                if (angle < start) {
                    pressKeyRight();
                    return true;
                }
                if (angle < start + slice) {
                    pressKeyRight();
                    pressKeyUp();
                    return true;
                }
                if (angle < start + 2 * slice) {
                    pressKeyUp();
                    return true;
                }
                if (angle < start + 3 * slice) {
                    pressKeyUp();
                    pressKeyLeft();
                    return true;
                }
                if (angle < start + 4 * slice) {
                    pressKeyLeft();
                    return true;
                }
                if (angle < start + 5 * slice) {
                    pressKeyLeft();
                    pressKeyDown();
                    return true;
                }
                if (angle < start + 6 * slice) {
                    pressKeyDown();
                    return true;
                }
                if (angle < start + 7 * slice) {
                    pressKeyDown();
                    pressKeyRight();
                    return true;
                }
                pressKeyRight();
                return true;
            }
        });
    }

    private double computeAngle(double dx, double dy) {
        if (dx == 0) {
            return dy >= 0 ? 90 : 270;
        }
        double atan = Math.toDegrees(Math.atan(dy / dx));
        if (dx < 0 && dy > 0) {
            return 180 + atan;
        }
        if (dx < 0 && dy < 0) {
            return 180 + atan;
        }
        if (dx > 0 && dy < 0) {
            return 360 + atan;
        }
        return atan;
    }

    private static final int KEY_ADDRESS_LEFT  = 0x3840;
    private static final int KEY_MASK_LEFT     = 0x20;

    private static final int KEY_ADDRESS_RIGHT = 0x3840;
    private static final int KEY_MASK_RIGHT    = 0x40;

    private static final int KEY_ADDRESS_DOWN  = 0x3840;
    private static final int KEY_MASK_DOWN     = 16;

    private static final int KEY_ADDRESS_UP    = 0x3840;
    private static final int KEY_MASK_UP       = 8;

    private void allKeysUp() {
        memBuffer[KEY_ADDRESS_LEFT] &= ~KEY_MASK_LEFT;
        memBuffer[KEY_ADDRESS_RIGHT] &= ~KEY_MASK_RIGHT;
        memBuffer[KEY_ADDRESS_UP] &= ~KEY_MASK_UP;
        memBuffer[KEY_ADDRESS_DOWN] &= ~KEY_MASK_DOWN;
    }

    private void pressKeyDown() {
        memBuffer[KEY_ADDRESS_DOWN] |= KEY_MASK_DOWN;
    }

    private void pressKeyUp() {
        memBuffer[KEY_ADDRESS_UP] |= KEY_MASK_UP;
    }

    private void pressKeyLeft() {
        memBuffer[KEY_ADDRESS_LEFT] |= KEY_MASK_LEFT;
    }

    private void pressKeyRight() {
        memBuffer[KEY_ADDRESS_RIGHT] |= KEY_MASK_RIGHT;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int height = this.getHeight();
        int width = this.getWidth();
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        float radius = width / 2.0f - 5;
        paint.setAlpha(180);
        paint.setColor(Color.GRAY);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(5);
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setStrokeWidth(1);
        canvas.drawCircle(cx, cy, innerRadius, paint);

        cy += paint.descent();
        canvas.drawText(KEY_RIGHT, cx + arrowRadius, cy, paint);
        canvas.drawText(KEY_UP, cx, cy - arrowRadius, paint);
        canvas.drawText(KEY_LEFT, cx - arrowRadius, cy, paint);
        canvas.drawText(KEY_DOWN, cx, cy + arrowRadius, paint);
    }

    private float pxFromDp(float dp) {
        return dp * this.getContext().getResources().getDisplayMetrics().density;
    }
}
