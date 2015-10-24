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

import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class GameController {

    enum Action {
        LEFT_DOWN, TOP_DOWN, RIGHT_DOWN, BOTTOM_DOWN, CENTER_DOWN, LEFT_UP, TOP_UP, RIGHT_UP, BOTTOM_UP, CENTER_UP
    }


    private GameControllerListener listener;

    private boolean leftKeyPressed  = false;
    private boolean rightKeyPressed = false;
    private boolean upKeyPressed    = false;
    private boolean downKeyPressed  = false;


    public GameController(GameControllerListener listener) {
        this.listener = listener;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!isDpadDevice(event)) {
            return false;
        }
        if (event.getRepeatCount() != 0) {
            return false;
        }
        int action = event.getAction();
        Action key = null;
        switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
            key = action == KeyEvent.ACTION_DOWN ? Action.LEFT_DOWN : Action.LEFT_UP;
            break;
        case KeyEvent.KEYCODE_DPAD_UP:
            key = action == KeyEvent.ACTION_DOWN ? Action.TOP_DOWN : Action.TOP_UP;
            break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            key = action == KeyEvent.ACTION_DOWN ? Action.RIGHT_DOWN : Action.RIGHT_UP;
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
            key = action == KeyEvent.ACTION_DOWN ? Action.BOTTOM_DOWN : Action.BOTTOM_UP;
            break;
        case KeyEvent.KEYCODE_DPAD_CENTER:
            key = action == KeyEvent.ACTION_DOWN ? Action.CENTER_DOWN : Action.CENTER_UP;
            break;

        }
        if (key == null) {
            return false;
        }
        listener.onGameControllerAction(key);
        return true;
    }

    private boolean isDpadDevice(InputEvent event) {
        return (event.getSource() & InputDevice.SOURCE_DPAD) != InputDevice.SOURCE_DPAD;
    }

    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                && event.getAction() == MotionEvent.ACTION_MOVE) {

            final int historySize = event.getHistorySize();
            for (int i = 0; i < historySize; i++) {
                processJoystickInput(event, i);
            }

            processJoystickInput(event, -1);
            return true;
        }
        return false;
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device, int axis,
            int historyPos) {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());

        if (range != null) {
            final float flat = range.getFlat();
            final float value = historyPos < 0 ? event.getAxisValue(axis)
                    : event.getHistoricalAxisValue(axis, historyPos);
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }

    private void processJoystickInput(MotionEvent event, int historyPos) {
        InputDevice mInputDevice = event.getDevice();

        float x = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_X, historyPos);
        if (x == 0) {
            x = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_HAT_X, historyPos);
        }
        if (x == 0) {
            x = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Z, historyPos);
        }

        float y = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Y, historyPos);
        if (y == 0) {
            y = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_HAT_Y, historyPos);
        }
        if (y == 0) {
            y = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_RZ, historyPos);
        }

        pressKeys(x, -y);
    }

    private void pressKeys(float dx, float dy) {
        if (dx == 0 && dy == 0) {
            unpressAllKeys();
            return;
        }

        float angle = computeAngle(dx, dy);
        final float slice = 360 / 8;
        final float start = slice / 2;

        // Right key
        if (angle < start || angle >= start + 7 * slice) {
            if (!rightKeyPressed) {
                rightKeyPressed = true;
                listener.onGameControllerAction(Action.RIGHT_DOWN);
            }
            if (leftKeyPressed) {
                leftKeyPressed = false;
                listener.onGameControllerAction(Action.LEFT_UP);
            }
            if (upKeyPressed) {
                upKeyPressed = false;
                listener.onGameControllerAction(Action.TOP_UP);
            }
            if (downKeyPressed) {
                downKeyPressed = false;
                listener.onGameControllerAction(Action.BOTTOM_UP);
            }
            return;
        }

        // Right & up keys
        if (angle < start + slice) {
            if (!rightKeyPressed) {
                rightKeyPressed = true;
                listener.onGameControllerAction(Action.RIGHT_DOWN);
            }
            if (leftKeyPressed) {
                leftKeyPressed = false;
                listener.onGameControllerAction(Action.LEFT_UP);
            }
            if (!upKeyPressed) {
                upKeyPressed = true;
                listener.onGameControllerAction(Action.TOP_DOWN);
            }
            if (downKeyPressed) {
                downKeyPressed = false;
                listener.onGameControllerAction(Action.BOTTOM_UP);
            }
            return;
        }

        // Up key
        if (angle < start + 2 * slice) {
            if (rightKeyPressed) {
                rightKeyPressed = false;
                listener.onGameControllerAction(Action.RIGHT_UP);
            }
            if (leftKeyPressed) {
                leftKeyPressed = false;
                listener.onGameControllerAction(Action.LEFT_UP);
            }
            if (!upKeyPressed) {
                upKeyPressed = true;
                listener.onGameControllerAction(Action.TOP_DOWN);
            }
            if (downKeyPressed) {
                downKeyPressed = false;
                listener.onGameControllerAction(Action.BOTTOM_UP);
            }
            return;
        }

        // Up & left keys
        if (angle < start + 3 * slice) {
            if (rightKeyPressed) {
                rightKeyPressed = false;
                listener.onGameControllerAction(Action.RIGHT_UP);
            }
            if (!leftKeyPressed) {
                leftKeyPressed = true;
                listener.onGameControllerAction(Action.LEFT_DOWN);
            }
            if (!upKeyPressed) {
                upKeyPressed = true;
                listener.onGameControllerAction(Action.TOP_DOWN);
            }
            if (downKeyPressed) {
                downKeyPressed = false;
                listener.onGameControllerAction(Action.BOTTOM_UP);
            }
            return;
        }

        // Left key
        if (angle < start + 4 * slice) {
            if (rightKeyPressed) {
                rightKeyPressed = false;
                listener.onGameControllerAction(Action.RIGHT_UP);
            }
            if (!leftKeyPressed) {
                leftKeyPressed = true;
                listener.onGameControllerAction(Action.LEFT_DOWN);
            }
            if (upKeyPressed) {
                upKeyPressed = false;
                listener.onGameControllerAction(Action.TOP_UP);
            }
            if (downKeyPressed) {
                downKeyPressed = false;
                listener.onGameControllerAction(Action.BOTTOM_UP);
            }
            return;
        }

        // Left & down keys
        if (angle < start + 5 * slice) {
            if (rightKeyPressed) {
                rightKeyPressed = false;
                listener.onGameControllerAction(Action.RIGHT_UP);
            }
            if (!leftKeyPressed) {
                leftKeyPressed = true;
                listener.onGameControllerAction(Action.LEFT_DOWN);
            }
            if (upKeyPressed) {
                upKeyPressed = false;
                listener.onGameControllerAction(Action.TOP_UP);
            }
            if (!downKeyPressed) {
                downKeyPressed = true;
                listener.onGameControllerAction(Action.BOTTOM_DOWN);
            }
            return;
        }

        // Down key
        if (angle < start + 6 * slice) {
            if (rightKeyPressed) {
                rightKeyPressed = false;
                listener.onGameControllerAction(Action.RIGHT_UP);
            }
            if (leftKeyPressed) {
                leftKeyPressed = false;
                listener.onGameControllerAction(Action.LEFT_UP);
            }
            if (upKeyPressed) {
                upKeyPressed = false;
                listener.onGameControllerAction(Action.TOP_UP);
            }
            if (!downKeyPressed) {
                downKeyPressed = true;
                listener.onGameControllerAction(Action.BOTTOM_DOWN);
            }
            return;
        }

        // Down & right keys
        if (angle < start + 7 * slice) {
            if (!rightKeyPressed) {
                rightKeyPressed = true;
                listener.onGameControllerAction(Action.RIGHT_DOWN);
            }
            if (leftKeyPressed) {
                leftKeyPressed = false;
                listener.onGameControllerAction(Action.LEFT_UP);
            }
            if (upKeyPressed) {
                upKeyPressed = false;
                listener.onGameControllerAction(Action.TOP_UP);
            }
            if (!downKeyPressed) {
                downKeyPressed = true;
                listener.onGameControllerAction(Action.BOTTOM_DOWN);
            }
        }
    }

    private void unpressAllKeys() {
        if (leftKeyPressed) {
            leftKeyPressed = false;
            listener.onGameControllerAction(Action.LEFT_UP);
        }
        if (rightKeyPressed) {
            rightKeyPressed = false;
            listener.onGameControllerAction(Action.RIGHT_UP);
        }
        if (upKeyPressed) {
            upKeyPressed = false;
            listener.onGameControllerAction(Action.TOP_UP);
        }
        if (downKeyPressed) {
            downKeyPressed = false;
            listener.onGameControllerAction(Action.BOTTOM_UP);
        }
    }

    private float computeAngle(float dx, float dy) {
        if (dx == 0) {
            return dy >= 0 ? 90 : 270;
        }
        if (dy == 0) {
            return dx >= 0 ? 0 : 180;
        }
        float atan = (float) Math.toDegrees(Math.atan(dy / dx));
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

}
