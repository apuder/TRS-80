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
public class JoystickHorizontal extends Joystick {

    private static final float BORDER_WIDTH = 3;

    private float              width, height;
    private RectF              leftSemiCircle;
    private RectF              rightSemiCircle;
    private float              x1, x2;
    private float              keyLeftX;
    private float              keyLeftY;
    private float              keyRightX;
    private float              keyRightY;

    public JoystickHorizontal(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                allKeysUp();
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    return true;
                }

                int d = getWidth() / 3;
                float x = event.getX();

                if (x < d) {
                    pressKeyLeft();
                    return true;
                }
                if (x < 2 * d) {
                    return true;
                }
                pressKeyRight();
                return true;
            }
        });
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (leftSemiCircle == null) {
            height = this.getHeight() - BORDER_WIDTH;
            width = this.getWidth() - BORDER_WIDTH;
            leftSemiCircle = new RectF(BORDER_WIDTH, BORDER_WIDTH, height, height);
            rightSemiCircle = new RectF(width - height, BORDER_WIDTH, width, height);
            x1 = (leftSemiCircle.right - leftSemiCircle.left) / 2 + leftSemiCircle.left;
            x2 = (rightSemiCircle.right - rightSemiCircle.left) / 2 + rightSemiCircle.left;
            keyLeftX = x1;
            keyLeftY = this.getHeight() / 2 + paint.descent();
            keyRightX = x2;
            keyRightY = keyLeftY;
        }
        paint.setAlpha(180);
        paint.setColor(Color.GRAY);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(BORDER_WIDTH);
        paint.setAntiAlias(true);

        canvas.drawArc(leftSemiCircle, 90, 180, false, paint);
        canvas.drawArc(rightSemiCircle, 270, 180, false, paint);
        canvas.drawLine(x1, BORDER_WIDTH, x2, BORDER_WIDTH, paint);
        canvas.drawLine(x1, height, x2, height, paint);

        paint.setColor(Color.WHITE);
        paint.setStyle(Style.FILL);
        paint.setStrokeWidth(1);
        paint.setTextAlign(Align.CENTER);

        canvas.drawText(KEY_LEFT, keyLeftX, keyLeftY, paint);
        canvas.drawText(KEY_RIGHT, keyRightX, keyRightY, paint);
    }
}
