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

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView

/** The package resources are looked up in when the hint markup embeds a drawable. */
private const val DRAWABLE_PACKAGE = "org.puder.trs80"

/** How much larger than the surrounding text an inlined drawable is drawn. */
private const val INLINE_DRAWABLE_SCALE = 1.2f

/**
 * Creates and tracks the app's alert dialogs, at most one per [Context].
 */
object AlertDialogUtil {

    private val dialogs = mutableMapOf<Context, Dialog>()

    /**
     * Dismisses the dialog currently shown for [context], if any, and returns a builder for a
     * fresh one.
     */
    @JvmStatic
    fun createAlertDialog(context: Context): AlertDialog.Builder {
        dismissDialog(context)
        return AlertDialog.Builder(context)
    }

    /**
     * Returns a builder for a dialog showing [message] as HTML, with the given title and, unless
     * [iconId] is -1, the given icon.
     */
    @JvmStatic
    fun createAlertDialog(
        context: Context,
        titleId: Int,
        iconId: Int,
        message: CharSequence
    ): AlertDialog.Builder = createAlertDialog(context).apply {
        setTitle(titleId)
        if (iconId != -1) {
            setIcon(iconId)
        }
        setView(inflateMessageView(context, message))
    }

    /** Returns a builder for a dialog showing the string resource [messageId] as HTML. */
    @JvmStatic
    fun createAlertDialog(
        context: Context,
        titleId: Int,
        iconId: Int,
        messageId: Int
    ): AlertDialog.Builder = createAlertDialog(context, titleId, iconId, context.getText(messageId))

    /** Shows the dialog built by [builder], replacing any dialog already shown for [context]. */
    @JvmStatic
    fun showDialog(context: Context, builder: AlertDialog.Builder) {
        dismissDialog(context)
        dialogs[context] = builder.create().apply { show() }
    }

    /** Dismisses and forgets the dialog currently shown for [context], if any. */
    @JvmStatic
    fun dismissDialog(context: Context) {
        dialogs.remove(context)?.dismiss()
    }

    /**
     * Shows the hint with the given string resource id, unless it has been shown before.
     *
     * @param buttonTitle string resource for an extra button, only used when [buttonCallback] is
     * given.
     * @param buttonCallback run when the user taps the extra button.
     * @return whether the hint was shown.
     */
    @JvmStatic
    @JvmOverloads
    fun showHint(
        context: Context,
        hintId: Int,
        buttonTitle: Int = 0,
        buttonCallback: Runnable? = null
    ): Boolean {
        val key = "conf_new_hint_id$hintId"
        val sharedPrefs =
            context.getSharedPreferences(SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE)
        if (!sharedPrefs.getBoolean(key, true)) {
            return false
        }
        sharedPrefs.edit().putBoolean(key, false).apply()

        val builder = createAlertDialog(context, R.string.hint_title, -1, hintId).apply {
            setPositiveButton(R.string.hint_ok) { _, _ -> dismissDialog(context) }
            if (buttonCallback != null) {
                setNegativeButton(context.getText(buttonTitle)) { _, _ ->
                    dismissDialog(context)
                    buttonCallback.run()
                }
            }
        }
        showDialog(context, builder)
        return true
    }

    /**
     * Inflates the dialog body and renders [message] into it as HTML, resolving `<img>` sources
     * against the app's drawables.
     */
    @Suppress("DEPRECATION") // Html.fromHtml and Resources.getDrawable predate the flag/theme APIs.
    private fun inflateMessageView(context: Context, message: CharSequence): View =
        LayoutInflater.from(context).inflate(R.layout.alert_dialog, null, false).also { view ->
            val textView = view.findViewById<TextView>(R.id.alert_text)
            val iconSize = (textView.textSize * INLINE_DRAWABLE_SCALE).toInt()
            val imageGetter = Html.ImageGetter { source ->
                val name = source.replace("/", "")
                val id = context.resources.getIdentifier(name, "drawable", DRAWABLE_PACKAGE)
                context.resources.getDrawable(id).apply { setBounds(0, 0, iconSize, iconSize) }
            }
            textView.text = Html.fromHtml(message.toString(), imageGetter, null)
            textView.movementMethod = LinkMovementMethod.getInstance()
        }
}
