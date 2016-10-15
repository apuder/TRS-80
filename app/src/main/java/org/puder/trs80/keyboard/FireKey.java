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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.puder.trs80.EmulatorActivity;

public class FireKey extends View {

    private KeyboardManager keyboard;

    public FireKey(Context context, AttributeSet attrs) {
        super(context, attrs);

        EmulatorActivity emulator = (EmulatorActivity) context;
        this.keyboard = emulator.getKeyboardManager();

        this.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                if (action == MotionEvent.ACTION_DOWN) {
                    keyboard.pressKeySpace();
                }
                if (action == MotionEvent.ACTION_UP) {
                    keyboard.unpressKeySpace();
                }
                return true;
            }
        });
    }
}
