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

import org.puder.trs80.keyboard.Key;
import org.puder.trs80.keyboard.KeyboardManager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class EmulatorActivity extends SherlockFragmentActivity {

    private Thread   cpuThread;
    private TextView logView;
    private int      orientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        orientation = getResources().getConfiguration().orientation;
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            getSupportActionBar().hide();
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        XTRS.setEmulatorActivity(this);
        TRS80Application.getHardware().computeFontDimensions(getWindow());
        KeyboardManager keyboard = new KeyboardManager();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Pause").setIcon(R.drawable.pause_icon)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ("Pause".equals(item.getTitle())) {
            pauseEmulator();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView0() {
        setContentView(R.layout.emulator);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup root = (ViewGroup) findViewById(R.id.keyboard_container);
        ViewGroup keyboard = (ViewGroup) inflater.inflate(R.layout.keyboard_original, null);

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
        logView = (TextView) findViewById(R.id.log);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final ViewGroup root = (ViewGroup) findViewById(R.id.keyboard_container);
        // if (android.os.Build.VERSION.SDK_INT >=
        // Build.VERSION_CODES.HONEYCOMB) {
        // root.setMotionEventSplittingEnabled(true);
        // }
        int keyboardType;
        switch (orientation) {
        case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
            keyboardType = TRS80Application.getCurrentConfiguration().getKeyboardLayoutLandscape();
            break;
        default:
            keyboardType = TRS80Application.getCurrentConfiguration().getKeyboardLayoutPortrait();
            break;
        }
        int layoutId = 0;
        switch (keyboardType) {
        case Configuration.KEYBOARD_LAYOUT_COMPACT:
            layoutId = R.layout.keyboard_compact;
            break;
        case Configuration.KEYBOARD_LAYOUT_ORIGINAL:
            layoutId = R.layout.keyboard_original;
            break;
        case Configuration.KEYBOARD_LAYOUT_GAMING_1:
            layoutId = R.layout.keyboard_gaming_1;
            break;
        case Configuration.KEYBOARD_LAYOUT_GAMING_2:
            layoutId = R.layout.keyboard_gaming_2;
            break;
        }
        inflater.inflate(layoutId, root);
        /*
         * The following code is a hack to work around a problem with the
         * keyboard layout in Android. The second keyboard should have
         * visibility GONE initially when the keyboard layout is inflated.
         * However, doing so messes up the layout of the second keyboard
         * (R.id.keyboard_view_2). This does not happen when visibility is
         * VISIBLE. So, to work around this issue, the initial visibility in the
         * keyboard layout is VISIBLE and we use a layout listener to make it
         * GONE after the layout has been computed and just before it will be
         * rendered.
         */
        ViewTreeObserver vto = root.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                View kb2 = root.findViewById(R.id.keyboard_view_2);
                if (kb2 != null) {
                    kb2.setVisibility(View.GONE);
                }
                ViewTreeObserver obs = root.getViewTreeObserver();
                obs.removeGlobalOnLayoutListener(this);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        pauseEmulator();
    }

    private void pauseEmulator() {
        takeScreenshot();
        setResult(Activity.RESULT_OK, getIntent());
    }

    private void takeScreenshot() {
        Screen screen = (Screen) findViewById(R.id.screen);
        TRS80Application.setScreenshot(screen.takeScreenshot());
    }

    public void log(final String msg) {
        logView.post(new Runnable() {

            @Override
            public void run() {
                logView.setText(msg);
            }
        });
    }
}
