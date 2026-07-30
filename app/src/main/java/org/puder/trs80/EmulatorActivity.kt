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

package org.puder.trs80

import org.puder.trs80.configuration.getOrInit
import org.puder.trs80.configuration.saveState
import org.puder.trs80.configuration.saveScreenshot
import org.puder.trs80.configuration.loadState
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration as AndroidConfiguration
import android.graphics.Point
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import org.puder.trs80.cast.CastMessageSender
import org.puder.trs80.cast.RemoteCastScreen
import org.puder.trs80.shared.configuration.Configuration
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.configuration.EmulatorState
import org.puder.trs80.shared.KeyboardLayout
import org.puder.trs80.storage.AppStorage
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.keyboard.KeyboardManager
import java.io.IOException

private const val TAG = "EmulatorActivity"

// Action menu item ids.
private const val MENU_OPTION_PAUSE = 0
private const val MENU_OPTION_REWIND = 1
private const val MENU_OPTION_RESET = 2
private const val MENU_OPTION_PASTE = 3
private const val MENU_OPTION_SOUND_ON = 4
private const val MENU_OPTION_SOUND_OFF = 5
private const val MENU_OPTION_TUTORIAL = 6
private const val MENU_OPTION_HELP = 7


/** Only the configuration by this name knows the commands the tutorial types. */
private const val CONFIGURATION_TUTORIAL_NAME = "TRS-80 Tutorial"

/** Carriage return, the TRS-80's line terminator. */
private const val TRS_NEWLINE = '\r'

/**
 * How the accelerometer's device axes map onto screen axes, per display rotation:
 * `negateX, negateY, xSrc, ySrc`.
 */
private val AXIS_SWAP = arrayOf(
    intArrayOf(1, -1, 0, 1),  // ROTATION_0
    intArrayOf(-1, -1, 1, 0), // ROTATION_90
    intArrayOf(-1, 1, 0, 1),  // ROTATION_180
    intArrayOf(1, 1, 1, 0)    // ROTATION_270
)

/** How far the device has to be tilted, in m/s^2, before a cursor key is pressed. */
private const val ACCELEROMETER_PRESS_THRESHOLD = 1.0f

/** How far it has to be tilted back before that key is released again. */
private const val ACCELEROMETER_UNPRESS_THRESHOLD = 0.3f

/**
 * Hosts a running emulator: the emulated screen, the on-screen keyboard, and the threads that
 * drive them.
 *
 * ## Two-phase startup
 *
 * The size of the emulated display depends on the size of the window, which is only known once
 * the window has been laid out. [onCreate] therefore first sets `R.layout.emulator_measure`, a
 * layout consisting of a single full-size view, and waits for its global layout pass
 * ([onGlobalLayout]). The measured rectangle then sizes the font and the keys, and only after
 * that does the real `R.layout.emulator` go up.
 */
class EmulatorActivity : BaseActivity(), SensorEventListener, GameControllerListener,
    OnGlobalLayoutListener {

    private lateinit var currentConfiguration: Configuration
    private lateinit var emulatorState: EmulatorState
    private lateinit var currentHardware: Hardware
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var gameController: GameController

    /** The window, in pixels, that the emulated display and the keyboard have to fit into. */
    private lateinit var windowRect: Rect

    private var cpuThread: Thread? = null
    private var renderThread: RenderThread? = null

    private var currentSurfaceHolder: SurfaceHolder? = null

    private var orientation = 0
    private var rotation = 0
    private var isCasting = false
    private var isGeneratingFont = false
    private var isStopped = false

    /** Landscape hides the system and action bars, so no insets are wanted. */
    private var isFullscreen = false

    private var pasteMenuItem: MenuItem? = null
    private var muteMenuItem: MenuItem? = null
    private var unmuteMenuItem: MenuItem? = null

    private var sensorManager: SensorManager? = null
    private var sensorAccelerometer: Sensor? = null

    /** Renders the font in the background; see the class documentation of the two-phase startup. */
    private var taskSetup: AsyncTask<Rect, Void, Void>? = null

    /** Whether the emulator's sound output is muted. Assigning also mutes the emulator core. */
    private var soundMuted = false
        set(muted) {
            field = muted
            XTRS.setSoundMuted(muted)
        }

    // Cursor keys currently held down by tilting the device.
    private var leftKeyPressed = false
    private var rightKeyPressed = false
    private var upKeyPressed = false
    private var downKeyPressed = false

    /**
     * Translates key presses into emulator key events. The on-screen key views take it from
     * here, so it exists for as long as the activity does.
     */
    lateinit var keyboardManager: KeyboardManager
        private set

    /** The width of an on-screen keyboard key, in pixels. */
    val keyWidth: Int
        get() = currentHardware.keyWidth

    /** The height of an on-screen keyboard key, in pixels. */
    val keyHeight: Int
        get() = currentHardware.keyHeight

    /** The margin around an on-screen keyboard key, in pixels. */
    val keyMargin: Int
        get() = currentHardware.keyMargin

    /** The width of the emulated display, in pixels. */
    val screenWidth: Int
        get() = currentHardware.screenWidth

    /** The height of the emulated display, in pixels. */
    val screenHeight: Int
        get() = currentHardware.screenHeight

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TRS80Application.setCrashedFlag(false)

        val id = intent.getIntExtra(EXTRA_CONFIGURATION_ID, -1)
        if (id == -1) {
            // We got killed by Android and then re-launched. The only thing we can do is exit.
            TRS80Application.setCrashedFlag(true)
            finish()
            return
        }

        initEmulator(savedInstanceState, id)

        isGeneratingFont = true
        isStopped = false
        setContentView(R.layout.emulator_measure)
        // Applied before the window is measured: the measured rect is what sizes the emulated
        // display, so it has to exclude the system and action bars. In fullscreen there are no
        // bars to avoid, and insetting would both shrink the picture and clip its bottom rows.
        if (!isFullscreen) {
            applyContentInsets()
        }
        findViewById<View>(R.id.emulator_measure).viewTreeObserver.addOnGlobalLayoutListener(this)

        AlertDialogUtil.showHint(
            this, R.string.hint_emulator, R.string.menu_tutorial, Runnable { showTutorial() }
        )
    }

    /** Measures the window and starts the emulator, once and only once. */
    override fun onGlobalLayout() {
        removeGlobalLayoutListener()
        setupEmulator()
    }

    public override fun onRestart() {
        super.onRestart()
        if (TRS80Application.hasCrashed()) {
            return
        }
        isStopped = false
        if (taskSetup == null && !isGeneratingFont) {
            startEmulation()
        }
    }

    override fun onStop() {
        super.onStop()
        if (TRS80Application.hasCrashed()) {
            return
        }
        isStopped = true
        removeGlobalLayoutListener()
        RemoteCastScreen.get().endSession()
        if (keyboardType == KeyboardLayout.KEYBOARD_TILT) {
            stopAccelerometer()
        }
        taskSetup?.cancel(true)
        taskSetup = null
        Log.d(TAG, "Stopping CPU thread...")
        stopCPUThread()
        Log.d(TAG, "Stopping render thread...")
        stopRenderThread()
        Log.d(TAG, "Done.")
        XTRS.emulatorActivity = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Loads the configuration, sets up the hardware and the window, and boots the native
     * emulator. Does nothing beyond logging if the configuration cannot be loaded.
     */
    private fun initEmulator(savedInstanceState: Bundle?, id: Int) {
        try {
            emulatorState = EmulatorState.forConfigId(id, FileManager.Creator.get())
            val configManager = ConfigurationManager.getOrInit()
            val config = configManager.getConfigById(id)
            if (config == null) {
                Log.e(TAG, "Configuration not found.")
                return
            }
            currentConfiguration = config
        } catch (e: IOException) {
            Log.e(TAG, "Cannot create emulator state.", e)
            return
        }
        isCasting = CastMessageSender.get().isReadyToSend
        currentHardware = Hardware(currentConfiguration)

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        keyboardManager = KeyboardManager()
        gameController = GameController(this)

        orientation = resources.configuration.orientation
        isFullscreen = orientation == AndroidConfiguration.ORIENTATION_LANDSCAPE &&
                !isCasting && !isInMultiWindowMode
        if (isFullscreen) {
            supportActionBar?.hide()
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (savedInstanceState == null) {
            if (XTRS.init(currentConfiguration, emulatorState) != 0) {
                finish()
                return
            }
            RemoteCastScreen.get().sendConfiguration(currentConfiguration)
            emulatorState.loadState()
        }
    }

    private fun removeGlobalLayoutListener() {
        // The measuring view is gone once initRootView() has swapped in the real layout.
        val root = findViewById<View>(R.id.emulator_measure)
        if (root == null || !root.viewTreeObserver.isAlive) {
            return
        }
        root.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    private fun setupEmulator() {
        val root = findViewById<View>(R.id.emulator_measure)
        windowRect = Rect(0, 0, root.width, root.height)
        initRootView()
        startEmulation()
    }

    private fun initRootView() {
        setContentView(R.layout.emulator)
        val root = findViewById<View>(R.id.emulator)
        if (isFullscreen) {
            // The root CoordinatorLayout declares fitsSystemWindows, so it still offsets its
            // children by the status bar inset even though the bar is hidden. Swallow the insets
            // so the picture starts at the top edge.
            ViewCompat.setOnApplyWindowInsetsListener(root) { _, _ -> WindowInsetsCompat.CONSUMED }
        } else {
            applyContentInsets()
        }
        root.isFocusable = true
        root.isFocusableInTouchMode = true
        root.requestFocus()

        val visibleId = if (CastMessageSender.get().isReadyToSend) R.id.cast_icon else R.id.screen
        findViewById<View>(visibleId).visibility = View.VISIBLE

        initKeyboardView()
    }

    private fun initKeyboardView() {
        stopAccelerometer()
        val container = findViewById<ViewGroup>(R.id.keyboard_container)
        container.removeAllViews()
        val layout = keyboardType
        showKeyboardHint(layout)

        val switchKeyboard = container.rootView.findViewById<View>(R.id.switch_keyboard)
        val layoutId = when (layout) {
            KeyboardLayout.KEYBOARD_LAYOUT_COMPACT -> R.layout.keyboard_compact
            KeyboardLayout.KEYBOARD_LAYOUT_ORIGINAL -> R.layout.keyboard_original
            KeyboardLayout.KEYBOARD_LAYOUT_JOYSTICK -> R.layout.keyboard_joystick
            KeyboardLayout.KEYBOARD_TILT -> R.layout.keyboard_tilt
            // These two are driven by hardware, so there is no on-screen keyboard to show.
            KeyboardLayout.KEYBOARD_GAME_CONTROLLER, KeyboardLayout.KEYBOARD_EXTERNAL -> {
                switchKeyboard.visibility = View.GONE
                return
            }
        }
        switchKeyboard.visibility = View.VISIBLE
        currentHardware.computeKeyDimensions(windowRect, layout)
        layoutInflater.inflate(layoutId, container, true)

        /*
         * The following code is a hack to work around a problem with the keyboard layout in
         * Android. The second keyboard should have visibility GONE initially when the keyboard
         * layout is inflated. However, doing so messes up the layout of the second keyboard
         * (R.id.keyboard_view_2). This does not happen when visibility is VISIBLE. So, to work
         * around this issue, the initial visibility in the keyboard layout is VISIBLE and we use
         * a layout listener to make it GONE after the layout has been computed and just before it
         * will be rendered.
         */
        container.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val secondKeyboard = container.findViewById<View>(R.id.keyboard_view_2)
                if (secondKeyboard != null) {
                    secondKeyboard.visibility = View.GONE
                }
                container.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        container.requestLayout()
        if (layout == KeyboardLayout.KEYBOARD_TILT) {
            startAccelerometer()
        }
    }

    private fun startEmulation() {
        XTRS.emulatorActivity = this

        updateMenuIcons()

        RemoteCastScreen.get().startSession()
        if (keyboardType == KeyboardLayout.KEYBOARD_TILT) {
            startAccelerometer()
        }

        isGeneratingFont = true

        // AsyncTask is deprecated, but Hardware.generateFont() takes one to poll for
        // cancellation, so it stays until that API is reworked.
        taskSetup = object : AsyncTask<Rect, Void, Void>() {
            override fun doInBackground(vararg rects: Rect): Void? {
                currentHardware.computeScreenDimensions(rects[0])
                return null
            }

            override fun onPostExecute(result: Void?) {
                isGeneratingFont = false
                if (isStopped) {
                    return
                }
                findViewById<View>(R.id.spinner).visibility = View.GONE
                /*
                 * The following requestLayout is necessary because sometimes layouting
                 * R.layout.emulator that is set in initRootView() finishes before the TRS screen
                 * dimensions have been computed in doInBackground() of this AsyncTask.
                 */
                findViewById<View>(R.id.screen).requestLayout()
                createRenderThread()
                renderThread?.let {
                    it.setHardwareSpecs(currentHardware)
                    it.surfaceHolder = currentSurfaceHolder
                    it.start()
                }
                startCPUThread()
            }
        }.execute(windowRect)
    }

    private fun createRenderThread() {
        if (renderThread != null) {
            return
        }
        renderThread = RenderThread(isCasting).also {
            it.priority = Thread.MAX_PRIORITY
            it.setHardwareSpecs(currentHardware)
            it.surfaceHolder = currentSurfaceHolder
        }
    }

    private fun stopRenderThread() {
        val thread = renderThread
        if (thread != null && thread.isAlive) {
            // The frame loop runs on a looper now, which interrupt() does not
            // break out of; quit() stops the looper as well as the loop flag.
            thread.quit()
            var retry = true
            while (retry) {
                try {
                    thread.join()
                    retry = false
                } catch (e: InterruptedException) {
                    // Keep waiting: the render thread has to be gone before the surface is.
                }
            }
            takeScreenshot()
        }
        renderThread = null
    }

    private fun startCPUThread() {
        val thread = Thread { XTRS.run() }
        cpuThread = thread
        XTRS.setRunning(true)
        thread.priority = Thread.MAX_PRIORITY
        thread.start()
    }

    private fun stopCPUThread() {
        val thread = cpuThread ?: return
        XTRS.setRunning(false)
        var retry = true
        while (retry) {
            try {
                thread.join()
                retry = false
            } catch (e: InterruptedException) {
                // Keep waiting: the state may only be saved once the CPU has stopped.
            }
        }
        cpuThread = null
        currentConfiguration.cassettePosition = XTRS.getCassettePosition()
        emulatorState.saveState()
    }

    private fun takeScreenshot() {
        val thread = renderThread ?: return
        if (currentHardware.screenWidth <= 0 || currentHardware.screenHeight <= 0) {
            return
        }
        val id = currentConfiguration.id
        emulatorState.saveScreenshot(thread.takeScreenshot(currentHardware))
        ScreenshotEvents.notifyScreenshotTaken(id)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_OPTION_PAUSE, Menu.NONE, getString(R.string.menu_pause))
            .setIcon(R.drawable.pause_icon)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.add(Menu.NONE, MENU_OPTION_RESET, Menu.NONE, getString(R.string.menu_reset))
            .setIcon(R.drawable.reset_icon)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(Menu.NONE, MENU_OPTION_REWIND, Menu.NONE, getString(R.string.menu_rewind))
            .setIcon(R.drawable.rewind_icon)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        pasteMenuItem = menu.add(
            Menu.NONE, MENU_OPTION_PASTE, Menu.NONE, getString(R.string.menu_paste)
        ).apply {
            setIcon(R.drawable.paste_icon)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        if (currentConfiguration.isSoundMuted) {
            // Mute sound permanently and don't show mute/unmute icons.
            soundMuted = true
        } else {
            muteMenuItem = menu.add(
                Menu.NONE, MENU_OPTION_SOUND_OFF, Menu.NONE, getString(R.string.menu_sound_off)
            ).apply {
                setIcon(R.drawable.sound_off_icon)
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
            unmuteMenuItem = menu.add(
                Menu.NONE, MENU_OPTION_SOUND_ON, Menu.NONE, getString(R.string.menu_sound_on)
            ).apply {
                setIcon(R.drawable.sound_on_icon)
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }
        menu.add(Menu.NONE, MENU_OPTION_TUTORIAL, Menu.NONE, getString(R.string.menu_tutorial))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, MENU_OPTION_HELP, Menu.NONE, getString(R.string.menu_help))
            .setIcon(R.drawable.help_icon_white)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        updateMenuIcons()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            // Pausing is just leaving: onStop() saves the machine state and a screenshot.
            android.R.id.home, MENU_OPTION_PAUSE -> finish()

            MENU_OPTION_REWIND -> {
                Snackbar.make(
                    findViewById<View>(R.id.emulator),
                    R.string.rewinding_cassette,
                    Snackbar.LENGTH_SHORT
                ).show()
                XTRS.rewindCassette()
            }

            MENU_OPTION_RESET -> XTRS.reset()

            MENU_OPTION_PASTE -> paste()

            MENU_OPTION_SOUND_ON -> {
                soundMuted = true
                updateMenuIcons()
            }

            MENU_OPTION_SOUND_OFF -> {
                soundMuted = false
                updateMenuIcons()
            }

            MENU_OPTION_TUTORIAL -> showTutorial()

            MENU_OPTION_HELP -> showDialog(R.string.help_title_emulator, -1, R.string.help_emulator)

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun updateMenuIcons() {
        val mute = muteMenuItem
        val unmute = unmuteMenuItem
        if (mute != null && unmute != null) {
            mute.isVisible = soundMuted
            unmute.isVisible = !soundMuted
        }
        val text = clipboardText()
        pasteMenuItem?.isEnabled = text != null && text != ""
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (gameController.dispatchKeyEvent(event)) {
            return true
        }
        var consumed = false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            consumed = keyboardManager.keyDown(event)
        }
        if (event.action == KeyEvent.ACTION_UP && event.repeatCount == 0) {
            consumed = keyboardManager.keyUp(event)
        }
        return consumed || super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean =
        gameController.dispatchGenericMotionEvent(event)

    override fun onGameControllerAction(action: GameController.Action) {
        when (action) {
            GameController.Action.LEFT_DOWN -> keyboardManager.pressKeyLeft()
            GameController.Action.LEFT_UP -> keyboardManager.unpressKeyLeft()
            GameController.Action.TOP_DOWN -> keyboardManager.pressKeyUp()
            GameController.Action.TOP_UP -> keyboardManager.unpressKeyUp()
            GameController.Action.RIGHT_DOWN -> keyboardManager.pressKeyRight()
            GameController.Action.RIGHT_UP -> keyboardManager.unpressKeyRight()
            GameController.Action.BOTTOM_DOWN -> keyboardManager.pressKeyDown()
            GameController.Action.BOTTOM_UP -> keyboardManager.unpressKeyDown()
            GameController.Action.CENTER_DOWN -> keyboardManager.pressKeySpace()
            GameController.Action.CENTER_UP -> keyboardManager.unpressKeySpace()
        }
    }

    /** Bound to the keyboard switch button by `res/layout/emulator.xml`. */
    fun onKeyboardSwitchClicked(view: View) {
        val keyboardTypes = resources.getStringArray(R.array.conf_keyboard_type)
        val builder = AlertDialogUtil.createAlertDialog(this)
        builder.setSingleChoiceItems(keyboardTypes, keyboardType.id) { _, which ->
            AlertDialogUtil.dismissDialog(this)
            val layout = KeyboardLayout.fromId(which) ?: return@setSingleChoiceItems
            when (orientation) {
                AndroidConfiguration.ORIENTATION_LANDSCAPE ->
                    currentConfiguration.setKeyboardLayoutLandscape(layout)

                else -> currentConfiguration.setKeyboardLayoutPortrait(layout)
            }
            initKeyboardView()
        }
        AlertDialogUtil.showDialog(this, builder)
    }

    /**
     * The keyboard to show in the current orientation. An attached hardware keyboard wins over
     * anything configured; a landscape configuration falls back to the portrait one.
     */
    private val keyboardType: KeyboardLayout
        get() {
            if (resources.configuration.keyboard != AndroidConfiguration.KEYBOARD_NOKEYS) {
                return KeyboardLayout.KEYBOARD_EXTERNAL
            }
            val landscape =
                if (orientation == AndroidConfiguration.ORIENTATION_LANDSCAPE) {
                    currentConfiguration.keyboardLayoutLandscape
                } else {
                    null
                }
            return landscape
                ?: currentConfiguration.keyboardLayoutPortrait
                ?: KeyboardLayout.KEYBOARD_LAYOUT_ORIGINAL
        }

    private fun showKeyboardHint(layout: KeyboardLayout) {
        val messageId = when (layout) {
            KeyboardLayout.KEYBOARD_LAYOUT_JOYSTICK -> R.string.hint_keyboard_joystick
            KeyboardLayout.KEYBOARD_TILT -> R.string.hint_keyboard_tilt
            KeyboardLayout.KEYBOARD_EXTERNAL -> R.string.hint_keyboard_external
            else -> return
        }
        AlertDialogUtil.showHint(this, messageId)
    }

    private fun startAccelerometer() {
        lockOrientation()
        val manager = sensorManager ?: (getSystemService(Context.SENSOR_SERVICE) as SensorManager)
            .also {
                sensorManager = it
                sensorAccelerometer = it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            }
        manager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun stopAccelerometer() {
        unlockScreenOrientation()
        sensorManager?.unregisterListener(this)
    }

    /**
     * Pins the activity to the orientation it is currently displayed in, so that tilting the
     * device to steer does not also rotate the screen.
     */
    @Suppress("DEPRECATION") // The Display size APIs have no replacement below API 30.
    private fun lockOrientation() {
        val display = windowManager.defaultDisplay
        rotation = display.rotation
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        setRequestedOrientation(
            when (rotation) {
                Surface.ROTATION_90 ->
                    if (width > height) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT

                Surface.ROTATION_180 ->
                    if (height > width) ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    else ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE

                Surface.ROTATION_270 ->
                    if (width > height) ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                else ->
                    if (height > width) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        )
    }

    private fun unlockScreenOrientation() =
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR)

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        val axes = AXIS_SWAP[rotation]
        val xValue = axes[0 /* negateX */] * event.values[axes[2 /* xSrc */]]
        val yValue = axes[1 /* negateY */] * event.values[axes[3 /* ySrc */]]
        if (xValue < -ACCELEROMETER_PRESS_THRESHOLD && !rightKeyPressed) {
            rightKeyPressed = true
            keyboardManager.pressKeyRight()
        }
        if (xValue > -ACCELEROMETER_UNPRESS_THRESHOLD && rightKeyPressed) {
            rightKeyPressed = false
            keyboardManager.unpressKeyRight()
        }
        if (xValue > ACCELEROMETER_PRESS_THRESHOLD && !leftKeyPressed) {
            leftKeyPressed = true
            keyboardManager.pressKeyLeft()
        }
        if (xValue < ACCELEROMETER_UNPRESS_THRESHOLD && leftKeyPressed) {
            leftKeyPressed = false
            keyboardManager.unpressKeyLeft()
        }
        if (yValue < -ACCELEROMETER_PRESS_THRESHOLD && !downKeyPressed) {
            downKeyPressed = true
            keyboardManager.pressKeyDown()
        }
        if (yValue > -ACCELEROMETER_UNPRESS_THRESHOLD && downKeyPressed) {
            downKeyPressed = false
            keyboardManager.unpressKeyDown()
        }
        if (yValue > ACCELEROMETER_PRESS_THRESHOLD && !upKeyPressed) {
            upKeyPressed = true
            keyboardManager.pressKeyUp()
        }
        if (yValue < ACCELEROMETER_UNPRESS_THRESHOLD && upKeyPressed) {
            upKeyPressed = false
            keyboardManager.unpressKeyUp()
        }
    }

    /** @return The text of the first clipboard item, or null if there is none. */
    private fun clipboardText(): CharSequence? =
        if (clipboardManager.hasPrimaryClip()) {
            clipboardManager.primaryClip?.getItemAt(0)?.text
        } else {
            null
        }

    private fun paste() {
        val text = clipboardText() ?: return
        XTRS.paste(text.toString().replace('\n', TRS_NEWLINE))
    }

    private fun showTutorial() {
        val name = currentConfiguration.name
        if (name != null && name != CONFIGURATION_TUTORIAL_NAME) {
            showDialog(R.string.help_title_emulator, -1, R.string.tutorial_prereq)
            return
        }
        Tutorial(keyboardManager, findViewById(R.id.emulator)).show()
    }

    /**
     * Adopts the surface the emulated screen is drawn into. Called by [Screen] as its surface
     * comes and goes, with null once it is gone.
     */
    fun setSurfaceHolder(holder: SurfaceHolder?) {
        currentSurfaceHolder = holder
        val thread = renderThread
        if (thread != null) {
            thread.surfaceHolder = holder
        } else {
            stopRenderThread()
        }
    }

    /**
     * Upcall from the emulator, reporting that it hit an unsupported code path. Brings the app
     * down: the emulated machine cannot carry on from here.
     */
    fun notImplemented(msg: String) {
        TRS80Application.setCrashedFlag(true)
        throw RuntimeException(msg)
    }

    companion object {
        /** Intent extra naming the configuration to run, as an int. */
        const val EXTRA_CONFIGURATION_ID = "conf_id"
    }
}
