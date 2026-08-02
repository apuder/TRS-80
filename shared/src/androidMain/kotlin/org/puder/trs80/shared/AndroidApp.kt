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

package org.puder.trs80.shared

import android.content.Context
import android.view.KeyEvent
import android.view.View
import androidx.compose.ui.platform.ComposeView
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.io.TRS80_DIRECTORY
import org.puder.trs80.shared.io.appDataDirectory
import org.puder.trs80.shared.io.initAppDataDirectory
import org.puder.trs80.shared.io.initPlatformIo
import org.puder.trs80.shared.localstore.RomManager
import org.puder.trs80.shared.storage.initAppSettings
import org.puder.trs80.shared.ui.trs80KeyForCharacter

/**
 * The Android host: what an activity needs to put the app on screen.
 *
 * The counterpart of `Trs80ViewController` on iOS, and as small for the same
 * reason — everything on screen is [Trs80AppUi], which is shared. What is here
 * is a Context to reach the platform through, a View to draw into, and the
 * hardware keyboard.
 */

/**
 * Everything the shared code needs before anything can ask it for something.
 *
 * Called once from `Application.onCreate`. Handing a context over is the price
 * of Android's storage, clipboard and intents all being reachable only through
 * one; the shared code has none, so this is where that gap is bridged.
 *
 * The directory is the one the app has always used, so a machine installed by
 * any previous version is found rather than a fresh empty library.
 */
fun initSharedApp(context: Context) {
    initAppDataDirectory(context)
    initPlatformIo(context)
    val settings = initAppSettings(context)
    val creator = FileManager.Creator(appDataDirectory() / TRS80_DIRECTORY)
    ConfigurationManager.init(creator, settings)
    RomManager.init(creator, settings)
}

/**
 * The whole app as a [View], for an activity to make its content.
 *
 * A View rather than a `setContent` call, so the app module needs no Compose
 * compiler of its own: it hosts a tree it does not compile, exactly as the iOS
 * app hosts a view controller it does not build.
 */
fun trs80AppView(
    context: Context,
    core: EmulatorCore,
    hardwareKeys: HardwareKeys? = null,
): View = ComposeView(context).apply {
    setContent { Trs80AppUi(core = core, hardwareKeys = hardwareKeys) }
}

/**
 * What a real keyboard's [event] means to the machine, or null if it means
 * nothing.
 *
 * The named keys first, because they have no character of their own; everything
 * else goes by the character the event produced, which already has the layout
 * and the shift key applied. Backspace moves the cursor left, as it did on the
 * machine.
 */
fun androidKeyFor(event: KeyEvent): KeyMap? = when (event.keyCode) {
    KeyEvent.KEYCODE_ENTER -> KeyboardMapping.byName("key_ENTER")
    KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_DPAD_LEFT -> KeyboardMapping.byName("key_LEFT")
    KeyEvent.KEYCODE_DPAD_RIGHT -> KeyboardMapping.byName("key_RIGHT")
    KeyEvent.KEYCODE_DPAD_UP -> KeyboardMapping.byName("key_UP")
    KeyEvent.KEYCODE_DPAD_DOWN -> KeyboardMapping.byName("key_DOWN")
    KeyEvent.KEYCODE_ESCAPE -> KeyboardMapping.byName("key_BREAK")
    // Ctrl-B and Ctrl-C, the only modified keys the machine knows.
    KeyEvent.KEYCODE_B -> if (event.isCtrlPressed) {
        KeyboardMapping.byName("key_BREAK")
    } else {
        characterKey(event)
    }

    KeyEvent.KEYCODE_C -> if (event.isCtrlPressed) {
        KeyboardMapping.byName("key_CLEAR")
    } else {
        characterKey(event)
    }

    else -> characterKey(event)
}

private fun characterKey(event: KeyEvent): KeyMap? =
    event.unicodeChar.takeIf { it != 0 }?.toChar()?.let(::trs80KeyForCharacter)
