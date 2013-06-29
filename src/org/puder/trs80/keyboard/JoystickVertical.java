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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * http://www.trs-80.com/wordpress/zaps-patches-pokes-tips/internals/#keyboard13
 */
public class JoystickVertical extends Joystick {

    private static final float BORDER_WIDTH = 3;

    private float              width, height;
    private RectF              upperSemiCircle;
    private RectF              lowerSemiCircle;
    private float              y1, y2;
    private float              keyUpX;
    private float              keyUpY;
    private float              keyDownX;
    private float              keyDownY;

    public JoystickVertical(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                allKeysUp();
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    return true;
                }

                int d = getHeight() / 3;
                float y = event.getY();

                if (y < d) {
                    pressKeyUp();
                    return true;
                }
                if (y < 2 * d) {
                    return true;
                }
                pressKeyDown();
                return true;
            }
        });
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (upperSemiCircle == null) {
            height = this.getHeight() - BORDER_WIDTH;
            width = this.getWidth() - BORDER_WIDTH;
            upperSemiCircle = new RectF(BORDER_WIDTH, BORDER_WIDTH, width, width);
            lowerSemiCircle = new RectF(BORDER_WIDTH, height - width, width, height);
            y1 = (upperSemiCircle.bottom - upperSemiCircle.top) / 2 + upperSemiCircle.top;
            y2 = (lowerSemiCircle.bottom - lowerSemiCircle.top) / 2 + lowerSemiCircle.top;
            keyUpY = y1 + paint.descent();
            keyUpX = this.getWidth() / 2;
            keyDownY = y2 + paint.descent();
            keyDownX = keyUpX;
        }
        paint.setAlpha(180);
        paint.setColor(Color.GRAY);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(BORDER_WIDTH);
        paint.setAntiAlias(true);

        canvas.drawArc(upperSemiCircle, 180, 180, false, paint);
        canvas.drawArc(lowerSemiCircle, 360, 180, false, paint);
        canvas.drawLine(BORDER_WIDTH, y1, BORDER_WIDTH, y2, paint);
        canvas.drawLine(width, y1, width, y2, paint);

        paint.setColor(Color.WHITE);
        paint.setStyle(Style.FILL);
        paint.setStrokeWidth(1);
        paint.setTextAlign(Align.CENTER);

        canvas.drawText(KEY_UP, keyUpX, keyUpY, paint);
        canvas.drawText(KEY_DOWN, keyDownX, keyDownY, paint);
    }
}
