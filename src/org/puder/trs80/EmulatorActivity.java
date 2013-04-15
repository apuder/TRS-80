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

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.ViewGroup;

public class EmulatorActivity extends Activity {

    private Thread cpuThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TRS80Application.getHardware().computeFontDimensions(getWindow());
        Keyboard keyboard = new Keyboard();
        TRS80Application.setKeyboard(keyboard);
        initView();
    }

    @Override
    public void onResume() {
        super.onResume();
        cpuThread = new Thread(new Runnable() {

            @Override
            public void run() {
                XTRS.run();
            }
        });
        XTRS.setRunning(true);
        cpuThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        boolean retry = true;
        XTRS.setRunning(false);
        while (retry) {
            try {
                cpuThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }

    }

    private void initView0() {
        setContentView(R.layout.emulator);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup root = (ViewGroup) findViewById(R.id.keyboard_container);
        ViewGroup keyboard = (ViewGroup) inflater.inflate(R.layout.keyboard_default, null);

        Key key_0 = (Key) keyboard.findViewById(R.id.key_0);
        ((ViewGroup) key_0.getParent()).removeView(key_0);
        key_0.setPosition(100, 100);
        root.addView(key_0);

        Key key_1 = (Key) keyboard.findViewById(R.id.key_1);
        ((ViewGroup) key_1.getParent()).removeView(key_1);
        key_1.setPosition(200, 200);
        root.addView(key_1);
    }

    private void initView() {
        setContentView(R.layout.emulator);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup root = (ViewGroup) findViewById(R.id.keyboard_container);
        inflater.inflate(R.layout.keyboard_default, root);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        Screen screen = (Screen) findViewById(R.id.screen);
        TRS80Application.setScreenshot(screen.takeScreenshot());
        super.onBackPressed();
    }
}
