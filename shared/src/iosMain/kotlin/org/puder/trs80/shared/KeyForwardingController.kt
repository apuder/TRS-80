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

import kotlinx.cinterop.ExperimentalForeignApi
import org.puder.trs80.shared.KeyMap
import org.puder.trs80.shared.KeyboardMapping
import org.puder.trs80.shared.ui.trs80KeyForCharacter
import platform.UIKit.UIKey
import platform.UIKit.UIKeyboardHIDUsage
import platform.UIKit.UIKeyboardHIDUsageKeyboardDeleteOrBackspace
import platform.UIKit.UIKeyboardHIDUsageKeyboardDownArrow
import platform.UIKit.UIKeyboardHIDUsageKeyboardEscape
import platform.UIKit.UIKeyboardHIDUsageKeyboardLeftArrow
import platform.UIKit.UIKeyboardHIDUsageKeyboardReturnOrEnter
import platform.UIKit.UIKeyboardHIDUsageKeyboardRightArrow
import platform.UIKit.UIKeyboardHIDUsageKeyboardUpArrow
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

/**
 * Wraps a view controller so that a real keyboard reaches the emulated machine.
 *
 * This exists because Compose's `onKeyEvent` never fires here. Focus works —
 * the node reports itself focused — but iOS only sends key presses to the first
 * responder, and Compose's view becomes first responder when a text field takes
 * focus. An emulator screen has no text field, so nothing ever asks for the
 * keyboard and nothing ever arrives.
 *
 * So the presses are taken one level up, in UIKit, which is also where Android
 * takes them: `EmulatorActivity.dispatchKeyEvent`, not the views inside it.
 */
@OptIn(ExperimentalForeignApi::class)
class KeyForwardingController(
    private val content: UIViewController,
    private val onKeyDown: (KeyMap) -> Unit,
    private val onKeyUp: (KeyMap) -> Unit,
) : UIViewController(nibName = null, bundle = null) {

    override fun viewDidLoad() {
        super.viewDidLoad()
        addChildViewController(content)
        content.view.setFrame(view.bounds)
        view.addSubview(content.view)
        content.didMoveToParentViewController(this)
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        content.view.setFrame(view.bounds)
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        // Nothing else wants to be first responder on this screen, so this can
        // simply take it and keep it.
        becomeFirstResponder()
    }

    override fun canBecomeFirstResponder(): Boolean = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        if (!forward(presses, onKeyDown)) {
            super.pressesBegan(presses, withEvent)
        }
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        if (!forward(presses, onKeyUp)) {
            super.pressesEnded(presses, withEvent)
        }
    }

    /** @return whether anything in [presses] meant something to the machine. */
    private fun forward(presses: Set<*>, send: (KeyMap) -> Unit): Boolean {
        var handled = false
        for (press in presses) {
            val key = (press as? UIPress)?.key ?: continue
            val entry = entryFor(key) ?: continue
            send(entry)
            handled = true
        }
        return handled
    }
}

/** @return what a physical key means to the machine, or null if nothing. */
@OptIn(ExperimentalForeignApi::class)
private fun entryFor(key: UIKey): KeyMap? {
    when (key.keyCode) {
        UIKeyboardHIDUsageKeyboardReturnOrEnter -> return KeyboardMapping.byName("key_ENTER")
        // Backspace moves the cursor left, as it did on the machine.
        UIKeyboardHIDUsageKeyboardDeleteOrBackspace,
        UIKeyboardHIDUsageKeyboardLeftArrow -> return KeyboardMapping.byName("key_LEFT")
        UIKeyboardHIDUsageKeyboardRightArrow -> return KeyboardMapping.byName("key_RIGHT")
        UIKeyboardHIDUsageKeyboardUpArrow -> return KeyboardMapping.byName("key_UP")
        UIKeyboardHIDUsageKeyboardDownArrow -> return KeyboardMapping.byName("key_DOWN")
        UIKeyboardHIDUsageKeyboardEscape -> return KeyboardMapping.byName("key_BREAK")
    }
    // Otherwise go by the character produced, which has the layout and shift
    // already applied -- the same rule the Android mapping uses.
    val character = key.characters.firstOrNull() ?: return null
    return trs80KeyForCharacter(character)
}
