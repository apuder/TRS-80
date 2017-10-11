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

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Optional;

import org.greenrobot.eventbus.EventBus;
import org.puder.trs80.cast.CastMessageSender;
import org.puder.trs80.cast.RemoteCastScreen;
import org.puder.trs80.configuration.Configuration;
import org.puder.trs80.configuration.ConfigurationManager;
import org.puder.trs80.configuration.EmulatorState;
import org.puder.trs80.configuration.KeyboardLayout;
import org.puder.trs80.io.FileManager;
import org.puder.trs80.keyboard.KeyboardManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.puder.trs80.configuration.KeyboardLayout.KEYBOARD_EXTERNAL;
import static org.puder.trs80.configuration.KeyboardLayout.KEYBOARD_GAME_CONTROLLER;
import static org.puder.trs80.configuration.KeyboardLayout.KEYBOARD_TILT;

public class EmulatorActivity extends BaseActivity implements SensorEventListener,
        GameControllerListener, OnGlobalLayoutListener {

    public static final String  EXTRA_CONFIGURATION_ID = "conf_id";
    private static final String TAG = "EmulatorActivity";

    // Action Menu
    private static final int   MENU_OPTION_PAUSE     = 0;
    private static final int   MENU_OPTION_REWIND    = 1;
    private static final int   MENU_OPTION_RESET     = 2;
    private static final int   MENU_OPTION_PASTE     = 3;
    private static final int   MENU_OPTION_SOUND_ON  = 4;
    private static final int   MENU_OPTION_SOUND_OFF = 5;
    private static final int   MENU_OPTION_TUTORIAL  = 6;
    private static final int   MENU_OPTION_HELP      = 7;

    private static final String CONFIGURATION_TUTORIAL_NAME = "TRS-80 Tutorial";

    private Configuration      currentConfiguration;
    private EmulatorState      emulatorState;
    private Hardware           currentHardware;
    private Thread             cpuThread;
    private RenderThread       renderThread;
    private TextView           logView;
    private int                orientation;
    private boolean            soundMuted            = false;
    private MenuItem           pasteMenuItem         = null;
    private MenuItem           muteMenuItem          = null;
    private MenuItem           unmuteMenuItem        = null;
    private SensorManager      sensorManager         = null;
    private Sensor             sensorAccelerometer;
    private KeyboardManager    keyboardManager;
    private ViewGroup          keyboardContainer     = null;
    private GameController     gameController;
    private int                rotation;
    private ClipboardManager   clipboardManager;
    private AsyncTask          taskSetup;
    private Rect               windowRect;
    private boolean            isCasting;
    private SurfaceHolder      surfaceHolder;
    private boolean            isGeneratingFont;
    private boolean            isStopped;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TRS80Application.setCrashedFlag(false);

        Intent intent = getIntent();
        int id = intent.getIntExtra(EXTRA_CONFIGURATION_ID, -1);

        if (id == -1) {
            /*
             * We got killed by Android and then re-launched. The only thing we
             * can do is exit.
             */
            TRS80Application.setCrashedFlag(true);
            finish();
            return;
        }

        init(savedInstanceState, id);

        /*
         * We first set a dummy layout that only consists of one empty view. We set
         * a layout listener on this view in order to measure the screen dimensions.
         * This seems to be the only workable way to properly do this with N's split-
         * screen mode.
         */
        isGeneratingFont = true;
        isStopped = false;
        setContentView(R.layout.emulator_measure);
        final View root = findViewById(R.id.emulator_measure);
        root.getViewTreeObserver().addOnGlobalLayoutListener(this);

        AlertDialogUtil.showHint(this, R.string.hint_emulator, R.string.menu_tutorial,
                new Runnable() {
                    @Override
                    public void run() {
                        showTutorial();
                    }
                });
    }

    @Override
    public void onGlobalLayout() {
        removeGlobalLayoutListener();
        setupEmulator();
    }

    private void removeGlobalLayoutListener() {
        final View root = findViewById(R.id.emulator_measure);
        if (root == null || !root.getViewTreeObserver().isAlive()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        } else {
            //noinspection deprecation
            root.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    }

    private void init(Bundle savedInstanceState, int id) {
        try {
            emulatorState = EmulatorState.forConfigId(id, FileManager.Creator.get(getResources()));
            ConfigurationManager configManager = ConfigurationManager.get(getApplicationContext());
            Optional<Configuration> configOpt = configManager.getConfigById(id);
            if (!configOpt.isPresent()) {
                Log.e(TAG, "Configuration not found.");
                return;
            }
            currentConfiguration = configOpt.get();
        } catch (IOException e) {
            Log.e(TAG, "Cannot create emulator state.", e);
            return;
        }
        isCasting = CastMessageSender.get().isReadyToSend();
        currentHardware = new Hardware(currentConfiguration);

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        keyboardManager = new KeyboardManager();
        gameController = new GameController(this);

        boolean isInMultiWindowMode = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isInMultiWindowMode = isInMultiWindowMode();
        }

        orientation = getResources().getConfiguration().orientation;
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                && !isCasting && !isInMultiWindowMode) {
            getSupportActionBar().hide();
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState == null) {
            int err = XTRS.init(currentConfiguration, emulatorState);
            if (err != 0) {
                //showError(err);
                finish();
                return;
            }
            RemoteCastScreen.get().sendConfiguration(currentConfiguration);
            emulatorState.loadState();
        }
    }

    private void setupEmulator() {
        windowRect = new Rect();
        View root = findViewById(R.id.emulator_measure);
        windowRect.top = 0;
        windowRect.left = 0;
        windowRect.right = root.getWidth();
        windowRect.bottom = root.getHeight();
        initRootView();
        startEmulation();
    }

    private void startEmulation() {
        XTRS.setEmulatorActivity(this);

        updateMenuIcons();

        RemoteCastScreen.get().startSession();
        if (getKeyboardType() == KEYBOARD_TILT) {
            startAccelerometer();
        }

        isGeneratingFont = true;

        taskSetup = new AsyncTask<Rect, Void, Void>() {
            @Override
            protected Void doInBackground(Rect... rects) {
                currentHardware.generateFont(rects[0], this);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                isGeneratingFont = false;
                if (isStopped) {
                    return;
                }
                findViewById(R.id.spinner).setVisibility(View.GONE);
                /*
                 * The following requestLayout is necessary because sometimes layouting
                 * R.layout.emulator that is set in initRootView() finishes before the
                 * TRS screen dimensions have been computed in doInBackground() of this
                 * AsyncTask.
                 */
                findViewById(R.id.screen).requestLayout();
                createRenderThread();
                renderThread.setHardwareSpecs(currentHardware);
                renderThread.setSurfaceHolder(surfaceHolder);
                renderThread.start();
                startCPUThread();
            }
        }.execute(windowRect);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (TRS80Application.hasCrashed()) {
            return;
        }
        isStopped = false;
        if (taskSetup == null && !isGeneratingFont) {
            startEmulation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (TRS80Application.hasCrashed()) {
            return;
        }
        isStopped = true;
        removeGlobalLayoutListener();
        RemoteCastScreen.get().endSession();
        if (getKeyboardType() == KEYBOARD_TILT) {
            stopAccelerometer();
        }
        if (taskSetup != null) {
            taskSetup.cancel(true);
            taskSetup = null;
        }
        Log.d(TAG, "Stopping CPU thread...");
        stopCPUThread();
        Log.d(TAG, "Taking screenshot...");
        takeScreenshot();
        Log.d(TAG, "Stopping render thread...");
        stopRenderThread();
        Log.d(TAG, "Done.");
        XTRS.setEmulatorActivity(null);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_PAUSE, Menu.NONE,
                        this.getString(R.string.menu_pause)).setIcon(R.drawable.pause_icon),
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_RESET, Menu.NONE,
                        this.getString(R.string.menu_reset)).setIcon(R.drawable.reset_icon),
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_REWIND, Menu.NONE,
                        this.getString(R.string.menu_rewind)).setIcon(R.drawable.rewind_icon),
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        pasteMenuItem = menu.add(Menu.NONE, MENU_OPTION_PASTE, Menu.NONE,
                this.getString(R.string.menu_paste));
        MenuItemCompat.setShowAsAction(pasteMenuItem.setIcon(R.drawable.paste_icon),
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        if (currentConfiguration.isSoundMuted()) {
            // Mute sound permanently and don't show mute/unmute icons
            setSoundMuted(true);
        } else {
            muteMenuItem = menu.add(Menu.NONE, MENU_OPTION_SOUND_OFF, Menu.NONE,
                    this.getString(R.string.menu_sound_off));
            MenuItemCompat.setShowAsAction(muteMenuItem.setIcon(R.drawable.sound_off_icon),
                    MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
            unmuteMenuItem = menu.add(Menu.NONE, MENU_OPTION_SOUND_ON, Menu.NONE,
                    this.getString(R.string.menu_sound_on));
            MenuItemCompat.setShowAsAction(unmuteMenuItem.setIcon(R.drawable.sound_on_icon),
                    MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        }
        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_TUTORIAL, Menu.NONE,
                        this.getString(R.string.menu_tutorial)),
                MenuItemCompat.SHOW_AS_ACTION_NEVER);
        MenuItemCompat
                .setShowAsAction(
                        menu.add(Menu.NONE, MENU_OPTION_HELP, Menu.NONE,
                                this.getString(R.string.menu_help)).setIcon(
                                R.drawable.help_icon_white), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        updateMenuIcons();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
        case MENU_OPTION_PAUSE:
            finish();
            return true;
        case MENU_OPTION_REWIND:
            View root = findViewById(R.id.emulator);
            Snackbar.make(root, R.string.rewinding_cassette, Snackbar.LENGTH_SHORT).show();
            XTRS.rewindCassette();
            return true;
        case MENU_OPTION_RESET:
            XTRS.reset();
            return true;
        case MENU_OPTION_PASTE:
            paste();
            return true;
        case MENU_OPTION_SOUND_ON:
            setSoundMuted(true);
            updateMenuIcons();
            return true;
        case MENU_OPTION_SOUND_OFF:
            setSoundMuted(false);
            updateMenuIcons();
            return true;
        case MENU_OPTION_TUTORIAL:
            showTutorial();
            return true;
        case MENU_OPTION_HELP:
            showDialog(R.string.help_title_emulator, -1, R.string.help_emulator);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (gameController.dispatchKeyEvent(event)) {
            return true;
        }
        boolean consumed = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            consumed = keyboardManager.keyDown(event);
        }
        if (event.getAction() == KeyEvent.ACTION_UP && event.getRepeatCount() == 0) {
            consumed = keyboardManager.keyUp(event);
        }
        return consumed || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return gameController.dispatchGenericMotionEvent(event);
    }

    @Override
    public void onGameControllerAction(GameController.Action action) {
        switch (action) {
        case LEFT_DOWN:
            keyboardManager.pressKeyLeft();
            break;
        case LEFT_UP:
            keyboardManager.unpressKeyLeft();
            break;
        case TOP_DOWN:
            keyboardManager.pressKeyUp();
            break;
        case TOP_UP:
            keyboardManager.unpressKeyUp();
            break;
        case RIGHT_DOWN:
            keyboardManager.pressKeyRight();
            break;
        case RIGHT_UP:
            keyboardManager.unpressKeyRight();
            break;
        case BOTTOM_DOWN:
            keyboardManager.pressKeyDown();
            break;
        case BOTTOM_UP:
            keyboardManager.unpressKeyDown();
            break;
        case CENTER_DOWN:
            keyboardManager.pressKeySpace();
            break;
        case CENTER_UP:
            keyboardManager.unpressKeySpace();
            break;
        }
    }

    public void onKeyboardSwitchClicked(View view) {
        List<String> keyboardTypes = Arrays.asList(getResources().getStringArray(
                R.array.conf_keyboard_type));
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this);
        builder.setSingleChoiceItems(keyboardTypes.toArray(new String[keyboardTypes.size()]),
                getKeyboardType().id, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialogUtil.dismissDialog(EmulatorActivity.this);
                        Optional<KeyboardLayout> layout = KeyboardLayout.fromId(which);
                        if (!layout.isPresent()) {
                            return;
                        }

                        switch (orientation) {
                        case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                            currentConfiguration.setKeyboardLayoutLandscape(layout.get());
                            break;
                        default:
                            currentConfiguration.setKeyboardLayoutPortrait(layout.get());
                            break;
                        }
                        initKeyboardView();
                    }
                });
        AlertDialogUtil.showDialog(this, builder);
    }

    private void createRenderThread() {
        if (renderThread != null) {
            return;
        }
        renderThread = new RenderThread(isCasting);
        renderThread.setPriority(Thread.MAX_PRIORITY);
        renderThread.setHardwareSpecs(currentHardware);
        renderThread.setSurfaceHolder(surfaceHolder);
    }

    private void stopRenderThread() {
        boolean retry = true;
        if (renderThread != null && renderThread.isAlive()) {
            renderThread.setRunning(false);
            renderThread.interrupt();
            while (retry) {
                try {
                    renderThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                }
            }
        }
        renderThread = null;
    }

    private void startCPUThread() {
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

    private void stopCPUThread() {
        if (cpuThread == null) {
            return;
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
        cpuThread = null;
        currentConfiguration.setCassettePosition(XTRS.getCassettePosition());
        emulatorState.saveState();
    }

    private void startAccelerometer() {
        lockOrientation();
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopAccelerometer() {
        unlockScreenOrientation();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void initRootView() {
        setContentView(R.layout.emulator);
        View top = this.findViewById(R.id.emulator);
        top.setFocusable(true);
        top.setFocusableInTouchMode(true);
        top.requestFocus();
        logView = (TextView) findViewById(R.id.log);

        if (CastMessageSender.get().isReadyToSend()) {
            ImageView castIcon = (ImageView) findViewById(R.id.cast_icon);
            castIcon.setVisibility(View.VISIBLE);
        } else {
            Screen screen = (Screen) findViewById(R.id.screen);
            screen.setVisibility(View.VISIBLE);
        }

        initKeyboardView();
    }

    private void initKeyboardView() {
        stopAccelerometer();
        keyboardContainer = (ViewGroup) findViewById(R.id.keyboard_container);
        keyboardContainer.removeAllViews();
        final KeyboardLayout keyboardType = getKeyboardType();
        showKeyboardHint(keyboardType);
        if (keyboardType == KEYBOARD_GAME_CONTROLLER
                || keyboardType == KEYBOARD_EXTERNAL) {
            keyboardContainer.getRootView().findViewById(R.id.switch_keyboard)
                    .setVisibility(View.GONE);
            return;
        }
        keyboardContainer.getRootView().findViewById(R.id.switch_keyboard)
                .setVisibility(View.VISIBLE);
        // if (android.os.Build.VERSION.SDK_INT >=
        // Build.VERSION_CODES.HONEYCOMB) {
        // root.setMotionEventSplittingEnabled(true);
        // }
        currentHardware.computeKeyDimensions(windowRect, getKeyboardType());

        int layoutId = 0;
        switch (keyboardType) {
        case KEYBOARD_LAYOUT_COMPACT:
            layoutId = R.layout.keyboard_compact;
            break;
        case KEYBOARD_LAYOUT_ORIGINAL:
            layoutId = R.layout.keyboard_original;
            break;
        case KEYBOARD_LAYOUT_JOYSTICK:
            layoutId = R.layout.keyboard_joystick;
            break;
        case KEYBOARD_TILT:
            layoutId = R.layout.keyboard_tilt;
            break;
        }
        getLayoutInflater().inflate(layoutId, keyboardContainer, true);

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
        ViewTreeObserver vto = keyboardContainer.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                View kb2 = keyboardContainer.findViewById(R.id.keyboard_view_2);
                if (kb2 != null) {
                    kb2.setVisibility(View.GONE);
                }
                ViewTreeObserver obs = keyboardContainer.getViewTreeObserver();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    obs.removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
                    obs.removeGlobalOnLayoutListener(this);
                }
            }
        });
        keyboardContainer.requestLayout();
        if (keyboardType == KEYBOARD_TILT) {
            startAccelerometer();
        }
    }

    private void lockOrientation() {
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
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                }
                break;
            case Surface.ROTATION_180:
                if (height > width) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
                break;
            case Surface.ROTATION_270:
                if (width > height) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
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

    private void unlockScreenOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    private void showKeyboardHint(KeyboardLayout keyboardType) {
        int messageId = -1;
        switch (keyboardType) {
        case KEYBOARD_LAYOUT_JOYSTICK:
            messageId = R.string.hint_keyboard_joystick;
            break;
        case KEYBOARD_TILT:
            messageId = R.string.hint_keyboard_tilt;
            break;
        case KEYBOARD_EXTERNAL:
            messageId = R.string.hint_keyboard_external;
            break;
        }
        if (messageId == -1) {
            return;
        }
        AlertDialogUtil.showHint(this, messageId);
    }

    private void paste() {
        if (!clipboardManager.hasPrimaryClip()) {
            return;
        }
        ClipData clip = clipboardManager.getPrimaryClip();
        ClipData.Item item = clip.getItemAt(0);
        CharSequence text = item.getText();
        if (text != null) {
            XTRS.paste(text.toString().replace('\n', '\015'));
        }
    }

    private void showTutorial() {
        Optional<String> name = currentConfiguration.getName();
        if (name.isPresent() && !CONFIGURATION_TUTORIAL_NAME.equals(name.get())) {
            showDialog(R.string.help_title_emulator, -1, R.string.tutorial_prereq);
            return;
        }
        new Tutorial(keyboardManager, findViewById(R.id.emulator)).show();
    }

    private KeyboardLayout getKeyboardType() {
        final android.content.res.Configuration conf = getResources().getConfiguration();
        if (conf.keyboard != android.content.res.Configuration.KEYBOARD_NOKEYS) {
            return KEYBOARD_EXTERNAL;
        }


        Optional<KeyboardLayout> landscape = currentConfiguration.getKeyboardLayoutLandscape();
        Optional<KeyboardLayout> portrait = currentConfiguration.getKeyboardLayoutPortrait();

        switch (orientation) {
            case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                if (landscape.isPresent()) {
                    return landscape.get();
                }
            default:
                if (portrait.isPresent()) {
                    return portrait.get();
                }
        }
        return KeyboardLayout.KEYBOARD_LAYOUT_ORIGINAL;
    }

    private void setSoundMuted(boolean muted) {
        this.soundMuted = muted;
        XTRS.setSoundMuted(muted);
    }

    private void takeScreenshot() {
        int width = currentHardware.getScreenWidth();
        int height = currentHardware.getScreenHeight();
        if (renderThread != null && width > 0 && height > 0) {
            int id = currentConfiguration.getId();
            Bitmap screenshot = renderThread.takeScreenshot(currentHardware);
            emulatorState.saveScreenshot(screenshot);
            EventBus.getDefault().post(new ScreenshotTakenEvent(id));
        }
    }

    private void updateMenuIcons() {
        if (muteMenuItem != null && unmuteMenuItem != null) {
            if (soundMuted) {
                muteMenuItem.setVisible(true);
                unmuteMenuItem.setVisible(false);
            } else {
                muteMenuItem.setVisible(false);
                unmuteMenuItem.setVisible(true);
            }
        }

        if (pasteMenuItem != null) {
            boolean hasClip = false;
            if (clipboardManager.hasPrimaryClip()) {
                ClipData clip = clipboardManager.getPrimaryClip();
                ClipData.Item item = clip.getItemAt(0);
                CharSequence text = item.getText();
                hasClip = (text != null) && !text.equals("");
            }
            pasteMenuItem.setEnabled(hasClip);
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
    final static int[][] axisSwap                        = { { 1, -1, 0, 1 }, // ROTATION_0
            { -1, -1, 1, 0 }, // ROTATION_90
            { -1, 1, 0, 1 }, // ROTATION_180
            { 1, 1, 1, 0 }                              };      // ROTATION_270

    final static float   ACCELEROMETER_PRESS_THRESHOLD   = 1.0f;
    final static float   ACCELEROMETER_UNPRESS_THRESHOLD = 0.3f;

    private boolean      leftKeyPressed                  = false;
    private boolean      rightKeyPressed                 = false;
    private boolean      upKeyPressed                    = false;
    private boolean      downKeyPressed                  = false;


    @Override
    public void onSensorChanged(SensorEvent event) {
        int[] as = axisSwap[rotation];
        float xValue = as[0 /* negateX */] * event.values[as[2/* xSrc */]];
        float yValue = as[1 /* negateY */] * event.values[as[3 /* ySrc */]];
        if (xValue < -ACCELEROMETER_PRESS_THRESHOLD && !rightKeyPressed) {
            rightKeyPressed = true;
            keyboardManager.pressKeyRight();
        }
        if (xValue > -ACCELEROMETER_UNPRESS_THRESHOLD && rightKeyPressed) {
            rightKeyPressed = false;
            keyboardManager.unpressKeyRight();
        }
        if (xValue > ACCELEROMETER_PRESS_THRESHOLD && !leftKeyPressed) {
            leftKeyPressed = true;
            keyboardManager.pressKeyLeft();
        }
        if (xValue < ACCELEROMETER_UNPRESS_THRESHOLD && leftKeyPressed) {
            leftKeyPressed = false;
            keyboardManager.unpressKeyLeft();
        }
        if (yValue < -ACCELEROMETER_PRESS_THRESHOLD && !downKeyPressed) {
            downKeyPressed = true;
            keyboardManager.pressKeyDown();
        }
        if (yValue > -ACCELEROMETER_UNPRESS_THRESHOLD && downKeyPressed) {
            downKeyPressed = false;
            keyboardManager.unpressKeyDown();
        }
        if (yValue > ACCELEROMETER_PRESS_THRESHOLD && !upKeyPressed) {
            upKeyPressed = true;
            keyboardManager.pressKeyUp();
        }
        if (yValue < ACCELEROMETER_UNPRESS_THRESHOLD && upKeyPressed) {
            upKeyPressed = false;
            keyboardManager.unpressKeyUp();
        }
    }

    public void notImplemented(String msg) {
        TRS80Application.setCrashedFlag(true);
        Crashlytics.setString("NOT_IMPLEMENTED", msg);
        Crashlytics.setInt("MODEL", currentConfiguration.getModel());
        Crashlytics.setString("NAME", currentConfiguration.getName().or("-"));
        Optional<String> path = currentConfiguration.getDiskPath(0);
        Crashlytics.setString("DISK_0", path.or("-"));
        throw new RuntimeException();
    }

    public int getKeyWidth() {
        return currentHardware.getKeyWidth();
    }

    public int getKeyHeight() {
        return currentHardware.getKeyHeight();
    }

    public int getKeyMargin() {
        return currentHardware.getKeyMargin();
    }

    public KeyboardManager getKeyboardManager() {
        return keyboardManager;
    }

    public int getScreenWidth() {
        return currentHardware.getScreenWidth();
    }

    public int getScreenHeight() {
        return currentHardware.getScreenHeight();
    }

    public void setSurfaceHolder(SurfaceHolder holder) {
        surfaceHolder = holder;
        if (renderThread != null) {
            renderThread.setSurfaceHolder(holder);
        }
    }
}
