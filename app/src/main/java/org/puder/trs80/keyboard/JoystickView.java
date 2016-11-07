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
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.puder.trs80.EmulatorActivity;

/**
 * http://www.trs-80.com/wordpress/zaps-patches-pokes-tips/internals/#keyboard13
 */
public class JoystickView extends View {

    private boolean           leftKeyPressed    = false;
    private boolean           rightKeyPressed   = false;
    private boolean           upKeyPressed      = false;
    private boolean           downKeyPressed    = false;

    private float             joystickX         = -1;
    private float             joystickY         = -1;

    private float             radiusButton;
    private float             radiusJoystick;
    private float             circleStrokeWidth = 10;
    private float             triangleSideLength;
    private Bitmap            joystickBitmap;
    private boolean           joystickIsPressed;

    protected Paint           paint;
    protected KeyboardManager keyboardManager;

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);

        EmulatorActivity emulator = (EmulatorActivity) context;
        this.keyboardManager = emulator.getKeyboardManager();

        paint = new Paint();
        paint.setAntiAlias(true);

        radiusButton = pxFromDp(40);
        triangleSideLength = pxFromDp(15);

        this.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    unpressAllKeys();
                    joystickX = -1;
                    joystickY = -1;
                    joystickIsPressed = false;
                    invalidate();
                    return true;
                }

                joystickIsPressed = true;
                joystickX = event.getX();
                joystickY = event.getY();
                invalidate();

                float cx = getWidth() / 2.0f;
                float cy = getHeight() / 2.0f;
                double dx = joystickX - cx;
                double dy = cy - joystickY;
                pressKeys(dx, dy);
                return true;
            }
        });
    }

    private void pressKeyDown() {
        keyboardManager.pressKeyDown();
    }

    private void pressKeyUp() {
        keyboardManager.pressKeyUp();
    }

    private void pressKeyLeft() {
        keyboardManager.pressKeyLeft();
    }

    private void pressKeyRight() {
        keyboardManager.pressKeyRight();
    }

    private void unpressKeyDown() {
        keyboardManager.unpressKeyDown();
    }

    private void unpressKeyUp() {
        keyboardManager.unpressKeyUp();
    }

    private void unpressKeyLeft() {
        keyboardManager.unpressKeyLeft();
    }

    private void unpressKeyRight() {
        keyboardManager.unpressKeyRight();
    }

    private float pxFromDp(float dp) {
        return dp * this.getContext().getResources().getDisplayMetrics().density;
    }

    private void unpressAllKeys() {
        if (leftKeyPressed) {
            leftKeyPressed = false;
            unpressKeyLeft();
        }
        if (rightKeyPressed) {
            rightKeyPressed = false;
            unpressKeyRight();
        }
        if (upKeyPressed) {
            upKeyPressed = false;
            unpressKeyUp();
        }
        if (downKeyPressed) {
            downKeyPressed = false;
            unpressKeyDown();
        }
    }

    private void pressKeys(double dx, double dy) {
        if (Math.sqrt(dx * dx + dy * dy) < radiusButton) {
            unpressAllKeys();
            return;
        }

        double angle = computeAngle(dx, dy);
        final double slice = 360 / 8;
        final double start = slice / 2;

        // Right key
        if (angle < start || angle >= start + 7 * slice) {
            if (!rightKeyPressed) {
                rightKeyPressed = true;
                pressKeyRight();
            }
            if (leftKeyPressed) {
                leftKeyPressed = false;
                unpressKeyLeft();
            }
            if (upKeyPressed) {
                upKeyPressed = false;
                unpressKeyUp();
            }
            if (downKeyPressed) {
                downKeyPressed = false;
                unpressKeyDown();
            }
            return;
        }

        // Right & up keys
        if (angle < start + slice) {
            if (!rightKeyPressed) {
                rightKeyPressed = true;
                pressKeyRight();
            }
            if (leftKeyPressed) {
                leftKeyPressed = false;
                unpressKeyLeft();
            }
            if (!upKeyPressed) {
                upKeyPressed = true;
                pressKeyUp();
            }
            if (downKeyPressed) {
                downKeyPressed = false;
                unpressKeyDown();
            }
            return;
        }

        // Up key
        if (angle < start + 2 * slice) {
            if (rightKeyPressed) {
                rightKeyPressed = false;
                unpressKeyRight();
            }
            if (leftKeyPressed) {
                leftKeyPressed = false;
                unpressKeyLeft();
            }
            if (!upKeyPressed) {
                upKeyPressed = true;
                pressKeyUp();
            }
            if (downKeyPressed) {
                downKeyPressed = false;
                unpressKeyDown();
            }
            return;
        }

        // Up & left keys
        if (angle < start + 3 * slice) {
            if (rightKeyPressed) {
                rightKeyPressed = false;
                unpressKeyRight();
            }
            if (!leftKeyPressed) {
                leftKeyPressed = true;
                pressKeyLeft();
            }
            if (!upKeyPressed) {
                upKeyPressed = true;
                pressKeyUp();
            }
            if (downKeyPressed) {
                downKeyPressed = false;
                unpressKeyDown();
            }
            return;
        }

        // Left key
        if (angle < start + 4 * slice) {
            if (rightKeyPressed) {
                rightKeyPressed = false;
                unpressKeyRight();
            }
            if (!leftKeyPressed) {
                leftKeyPressed = true;
                pressKeyLeft();
            }
            if (upKeyPressed) {
                upKeyPressed = false;
                unpressKeyUp();
            }
            if (downKeyPressed) {
                downKeyPressed = false;
                unpressKeyDown();
            }
            return;
        }

        // Left & down keys
        if (angle < start + 5 * slice) {
            if (rightKeyPressed) {
                rightKeyPressed = false;
                unpressKeyRight();
            }
            if (!leftKeyPressed) {
                leftKeyPressed = true;
                pressKeyLeft();
            }
            if (upKeyPressed) {
                upKeyPressed = false;
                unpressKeyUp();
            }
            if (!downKeyPressed) {
                downKeyPressed = true;
                pressKeyDown();
            }
            return;
        }

        // Down key
        if (angle < start + 6 * slice) {
            if (rightKeyPressed) {
                rightKeyPressed = false;
                unpressKeyRight();
            }
            if (leftKeyPressed) {
                leftKeyPressed = false;
                unpressKeyLeft();
            }
            if (upKeyPressed) {
                upKeyPressed = false;
                unpressKeyUp();
            }
            if (!downKeyPressed) {
                downKeyPressed = true;
                pressKeyDown();
            }
            return;
        }

        // Down & right keys
        if (angle < start + 7 * slice) {
            if (!rightKeyPressed) {
                rightKeyPressed = true;
                pressKeyRight();
            }
            if (leftKeyPressed) {
                leftKeyPressed = false;
                unpressKeyLeft();
            }
            if (upKeyPressed) {
                upKeyPressed = false;
                unpressKeyUp();
            }
            if (!downKeyPressed) {
                downKeyPressed = true;
                pressKeyDown();
            }
        }
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
        if (joystickX == -1) {
            joystickX = cx;
        }
        if (joystickY == -1) {
            joystickY = cy;
        }
        createJoystickBitmap();

        paint.setColor(joystickIsPressed ? Color.LTGRAY : Color.GRAY);
        paint.setAlpha(100);
        canvas.drawBitmap(joystickBitmap, 0, 0, paint);

        float x = joystickX - cx;
        float y = joystickY - cy;
        double dist = Math.sqrt(x * x + y * y);
        if (dist > radiusJoystick) {
            canvas.translate(cx, cy);
            double dx = joystickX - cx;
            double dy = cy - joystickY;
            canvas.rotate(-(float) computeAngle(dx, dy));
            canvas.drawCircle(radiusJoystick, 0, radiusButton, paint);
        } else {
            canvas.drawCircle(joystickX, joystickY, radiusButton, paint);
        }
    }

    private void createJoystickBitmap() {
        int height = this.getHeight();
        int width = this.getWidth();
        if (joystickBitmap != null && joystickBitmap.getWidth() == width
                && joystickBitmap.getHeight() == height) {
            return;
        }
        joystickBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(joystickBitmap);
        radiusJoystick = (width - circleStrokeWidth) / 2 - radiusButton;
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GRAY);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(circleStrokeWidth);
        canvas.drawCircle(cx, cy, radiusJoystick, paint);
        paint.setStrokeWidth(1);
        canvas.drawCircle(cx, cy, radiusButton, paint);

        paint.setStyle(Style.FILL_AND_STROKE);
        canvas.save();
        canvas.translate(cx, cy);
        Path path = new Path();
        path.reset();
        path.moveTo(radiusJoystick, -triangleSideLength / 2.0f);
        path.lineTo(radiusJoystick, triangleSideLength / 2.0f);
        path.lineTo(radiusJoystick + triangleSideLength * 0.667f, 0);
        path.close();
        for (int i = 0; i < 8; i++) {
            canvas.drawPath(path, paint);
            canvas.rotate(45);
        }
        canvas.restore();
    }
}
