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

import org.puder.trs80.keyboard.KeyboardManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class EmulatorActivity extends SherlockFragmentActivity implements SensorEventListener,
        OnKeyListener {

    // Action Menu
    private static final int   MENU_OPTION_PAUSE     = 0;
    private static final int   MENU_OPTION_RESET     = 1;
    private static final int   MENU_OPTION_SOUND_ON  = 2;
    private static final int   MENU_OPTION_SOUND_OFF = 3;

    private Thread             cpuThread;
    private TextView           logView;
    private int                orientation;
    private MenuItem           muteMenuItem;
    private MenuItem           unmuteMenuItem;
    private SensorManager      sensorManager         = null;
    private Sensor             sensorAccelerometer;
    private KeyboardManager    keyboardManager;
    private int                rotation;
    private OrientationChanged orientationManager;

    class OrientationChanged extends OrientationEventListener {

        public OrientationChanged(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (getKeyboardType() == Configuration.KEYBOARD_TILT) {
                disable();
                // Lock screen orientation for tilt interface
                lockOrientation();
            }
        }

        @SuppressLint("NewApi")
        private void lockOrientation() {
            Log.d("TRS80", "Locking screen orientation");
            Display display = getWindowManager().getDefaultDisplay();
            rotation = display.getRotation();
            int height;
            int width;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
                height = display.getHeight();
                width = display.getWidth();
            } else {
                Point size = new Point();
                display.getSize(size);
                height = size.y;
                width = size.x;
            }
            switch (rotation) {
            case Surface.ROTATION_90:
                if (width > height) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(9/* reversePortait */);
                }
                break;
            case Surface.ROTATION_180:
                if (height > width) {
                    setRequestedOrientation(9/* reversePortait */);
                } else {
                    setRequestedOrientation(8/* reverseLandscape */);
                }
                break;
            case Surface.ROTATION_270:
                if (width > height) {
                    setRequestedOrientation(8/* reverseLandscape */);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                break;
            default:
                if (height > width) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TRS80Application.getCurrentConfiguration() == null) {
            /*
             * We got killed by Android and then re-launched. The only thing we
             * can do is exit.
             */
            finish();
            return;
        }

        orientation = getResources().getConfiguration().orientation;
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            getSupportActionBar().hide();
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        XTRS.setEmulatorActivity(this);
        Hardware hardware = TRS80Application.getHardware();
        hardware.computeFontDimensions(getWindow());
        keyboardManager = new KeyboardManager();
        TRS80Application.setKeyboardManager(keyboardManager);
        XTRS.flushAudioQueue();
        initView();

        orientationManager = new OrientationChanged(this);
        orientationManager.enable();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getKeyboardType() == Configuration.KEYBOARD_TILT) {
            startAccelerometer();
        }
        cpuThread = new Thread(new Runnable() {

            @Override
            public void run() {
                XTRS.run();
            }
        });
        XTRS.setRunning(true);
        cpuThread.setPriority(Thread.MAX_PRIORITY);
        cpuThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        orientationManager.disable();
        if (getKeyboardType() == Configuration.KEYBOARD_TILT) {
            stopAccelerometer();
        }
        boolean retry = true;
        XTRS.setRunning(false);
        while (retry) {
            try {
                cpuThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
        XTRS.closeAudio();
        XTRS.flushAudioQueue();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_OPTION_PAUSE, Menu.NONE, this.getString(R.string.menu_pause))
                .setIcon(R.drawable.pause_icon).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENU_OPTION_RESET, Menu.NONE, this.getString(R.string.menu_reset))
                .setIcon(R.drawable.reset_icon).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        if (TRS80Application.getCurrentConfiguration().muteSound()) {
            // Mute sound permanently and don't show mute/unmute icons
            XTRS.setSoundMuted(true);
        } else {
            muteMenuItem = menu.add(Menu.NONE, MENU_OPTION_SOUND_OFF, Menu.NONE,
                    this.getString(R.string.menu_sound_off));
            muteMenuItem.setIcon(R.drawable.sound_off_icon).setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_IF_ROOM);
            unmuteMenuItem = menu.add(Menu.NONE, MENU_OPTION_SOUND_ON, Menu.NONE,
                    this.getString(R.string.menu_sound_on));
            unmuteMenuItem.setIcon(R.drawable.sound_on_icon).setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_IF_ROOM);
            updateMuteSoundIcons();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_OPTION_PAUSE:
            pauseEmulator();
            return true;
        case MENU_OPTION_RESET:
            XTRS.reset();
            return true;
        case MENU_OPTION_SOUND_ON:
            XTRS.setSoundMuted(true);
            XTRS.flushAudioQueue();
            updateMuteSoundIcons();
            return true;
        case MENU_OPTION_SOUND_OFF:
            XTRS.setSoundMuted(false);
            XTRS.flushAudioQueue();
            updateMuteSoundIcons();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            return keyboardManager.keyDown(event);
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            return keyboardManager.keyUp(event);
        }
        return false;
    }

    public void onScreenRotationClick(View view) {
        view.setVisibility(View.GONE);
        stopAccelerometer();
        orientationManager.disable();
        keyboardManager.allCursorKeysUp();
        keyboardManager.unpressKeySpace();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void startAccelerometer() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopAccelerometer() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void initView() {
        setContentView(R.layout.emulator);
        View top = this.findViewById(R.id.emulator);
        top.setFocusable(true);
        top.setFocusableInTouchMode(true);
        top.setOnKeyListener(this);
        top.requestFocus();
        logView = (TextView) findViewById(R.id.log);
        int keyboardType = getKeyboardType();
        if (keyboardType == Configuration.KEYBOARD_EXTERNAL) {
            return;
        }
        final ViewGroup root = (ViewGroup) findViewById(R.id.keyboard_container);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // if (android.os.Build.VERSION.SDK_INT >=
        // Build.VERSION_CODES.HONEYCOMB) {
        // root.setMotionEventSplittingEnabled(true);
        // }
        int layoutId = 0;
        switch (getKeyboardType()) {
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
        case Configuration.KEYBOARD_TILT:
            layoutId = R.layout.keyboard_tilt;
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
        pauseEmulator();
    }

    private int getKeyboardType() {
        if (getResources().getConfiguration().keyboard != android.content.res.Configuration.KEYBOARD_NOKEYS) {
            return Configuration.KEYBOARD_EXTERNAL;
        }

        int keyboardType;
        switch (orientation) {
        case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
            keyboardType = TRS80Application.getCurrentConfiguration().getKeyboardLayoutLandscape();
            break;
        default:
            keyboardType = TRS80Application.getCurrentConfiguration().getKeyboardLayoutPortrait();
            break;
        }
        return keyboardType;
    }

    private void pauseEmulator() {
        takeScreenshot();
        setResult(Activity.RESULT_OK, getIntent());
        finish();
    }

    private void takeScreenshot() {
        Screen screen = (Screen) findViewById(R.id.screen);
        TRS80Application.setScreenshot(screen.takeScreenshot());
    }

    private void updateMuteSoundIcons() {
        if (XTRS.isSoundMuted()) {
            muteMenuItem.setVisible(true);
            unmuteMenuItem.setVisible(false);
        } else {
            muteMenuItem.setVisible(false);
            unmuteMenuItem.setVisible(true);
        }
    }

    public void log(final String msg) {
        logView.post(new Runnable() {

            @Override
            public void run() {
                logView.setText(msg);
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * negateX, negateY, xSrc, ySrc
     */
    final static int[][] axisSwap                = { { 1, -1, 0, 1 }, // ROTATION_0
            { -1, -1, 1, 0 }, // ROTATION_90
            { -1, 1, 0, 1 }, // ROTATION_180
            { 1, 1, 1, 0 }                      };     // ROTATION_270

    final static float   ACCELEROMETER_THRESHOLD = 1.5f;

    @Override
    public void onSensorChanged(SensorEvent event) {
        int[] as = axisSwap[rotation];
        float xValue = (float) as[0 /* negateX */] * event.values[as[2/* xSrc */]];
        float yValue = (float) as[1 /* negateY */] * event.values[as[3 /* ySrc */]];
        keyboardManager.allCursorKeysUp();
        if (xValue < -ACCELEROMETER_THRESHOLD) {
            keyboardManager.pressKeyRight();
        } else if (xValue > ACCELEROMETER_THRESHOLD) {
            keyboardManager.pressKeyLeft();
        }
        if (yValue < -ACCELEROMETER_THRESHOLD) {
            keyboardManager.pressKeyDown();
        } else if (yValue > ACCELEROMETER_THRESHOLD) {
            keyboardManager.pressKeyUp();
        }
    }
}
