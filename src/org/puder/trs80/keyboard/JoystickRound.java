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
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * http://www.trs-80.com/wordpress/zaps-patches-pokes-tips/internals/#keyboard13
 */
public class JoystickRound extends Joystick {

    private float innerRadius;
    private float arrowRadius;

    public JoystickRound(Context context, AttributeSet attrs) {
        super(context, attrs);

        innerRadius = pxFromDp(30);
        arrowRadius = pxFromDp(55);

        this.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                allKeysUp();
                int action = event.getAction() & MotionEvent.ACTION_MASK;
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
}
