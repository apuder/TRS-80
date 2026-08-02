/*
 * Copyright The TRS-80 App Authors
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

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.puder.trs80.shared.HardwareKeys
import org.puder.trs80.shared.androidKeyFor
import org.puder.trs80.shared.io.dispatchGamepadKeyEvent
import org.puder.trs80.shared.io.dispatchGamepadMotionEvent
import org.puder.trs80.shared.io.readPickedFile
import org.puder.trs80.shared.io.setFileChooser
import org.puder.trs80.shared.trs80AppView

/**
 * The app.
 *
 * There is nothing of the app in here: what it shows is the shared Compose UI,
 * the same one the iOS app shows, and this is the three things Android will only
 * hand to an activity — a window to draw in, a file picker, and input events.
 *
 * The emulator comes from [AndroidEmulatorCore], which lives in this module
 * because the JNI entry points are named after the class that declares them.
 * Everything above it takes the core as a parameter, so this is the only place
 * that names it.
 *
 * AppCompatActivity rather than ComponentActivity, for one reason: it overrides
 * [dispatchKeyEvent] publicly, and the one further up is restricted to
 * androidx's own code. Nothing else here wants anything AppCompat offers.
 */
class Trs80Activity : AppCompatActivity() {

    /**
     * Whether a machine is on screen and wants the real keyboard.
     *
     * Compose never sees these events -- they arrive at the activity, above it --
     * so the emulator screen turns this on while it is there and off on its way
     * out, and [dispatchKeyEvent] reads it.
     */
    private val hardwareKeys = object : HardwareKeys {
        override var enabled = false
    }

    /** Who wants the file being picked, for the moment between asking and getting. */
    private var awaitingFile: ((name: String, content: ByteArray) -> Unit)? = null

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val onPicked = awaitingFile
        awaitingFile = null
        if (uri == null || onPicked == null) {
            // Backed out of the picker, which is not a failure.
            return@registerForActivityResult
        }
        readPickedFile(this, uri)?.let { (name, content) -> onPicked(name, content) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(trs80AppView(this, AndroidEmulatorCore, hardwareKeys))
    }

    override fun onStart() {
        super.onStart()
        // Anything, because what is worth opening is a matter of what is inside
        // it: disk images and ROMs are handed out under a dozen different types
        // and often under none at all.
        setFileChooser { onPicked ->
            awaitingFile = onPicked
            openDocument.launch(arrayOf("*/*"))
        }
    }

    override fun onStop() {
        setFileChooser(null)
        super.onStop()
    }

    /**
     * Input events, in the order of who has a claim on them.
     *
     * A controller first, because its d-pad carries the same key codes a real
     * keyboard's arrow keys do and the two mean different things. Then the
     * machine, but only while one is on screen: everywhere else these belong to
     * the app, where the arrow keys move around a list.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (dispatchGamepadKeyEvent(event)) {
            return true
        }
        if (!hardwareKeys.enabled || event.repeatCount != 0) {
            return super.dispatchKeyEvent(event)
        }
        val key = androidKeyFor(event) ?: return super.dispatchKeyEvent(event)
        when (event.action) {
            KeyEvent.ACTION_DOWN -> AndroidEmulatorCore.keyDown(key.sym, key.key)
            KeyEvent.ACTION_UP -> AndroidEmulatorCore.keyUp(key.sym, key.key)
            else -> return super.dispatchKeyEvent(event)
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean =
        dispatchGamepadMotionEvent(event) || super.onGenericMotionEvent(event)
}
