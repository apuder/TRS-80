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

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Base class for the app's activities. Adds a simple alert dialog helper and window inset
 * handling shared by all of them.
 */
open class BaseActivity : AppCompatActivity() {

    /**
     * Insets the activity's content below the system bars, and below the action bar where one is
     * shown.
     *
     * Apps targeting SDK 35+ are laid out edge to edge, so the system no longer reserves space
     * for the status and navigation bars. AppCompat positions the action bar itself but leaves
     * the content view spanning the whole window, which puts content underneath both bars.
     */
    protected fun applyContentInsets() {
        val content = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(content)
    }

    public override fun onStop() {
        super.onStop()
        AlertDialogUtil.dismissDialog(this)
    }

    /**
     * Shows an alert with an OK button and, when [buttonCallback] is given, a second button
     * labelled [buttonText] that runs it.
     */
    @JvmOverloads
    protected fun showDialog(
        titleId: Int,
        iconId: Int,
        message: String,
        buttonText: String? = null,
        buttonCallback: Runnable? = null
    ) {
        val builder = AlertDialogUtil.createAlertDialog(this, titleId, iconId, message).apply {
            setPositiveButton(R.string.alert_dialog_ok) { _, _ ->
                AlertDialogUtil.dismissDialog(this@BaseActivity)
            }
            if (buttonCallback != null) {
                setNegativeButton(buttonText) { _, _ ->
                    AlertDialogUtil.dismissDialog(this@BaseActivity)
                    buttonCallback.run()
                }
            }
        }
        AlertDialogUtil.showDialog(this, builder)
    }

    /** Shows an alert with an OK button whose body is the string resource [messageId]. */
    protected fun showDialog(titleId: Int, iconId: Int, messageId: Int) =
        showDialog(titleId, iconId, getString(messageId))
}
