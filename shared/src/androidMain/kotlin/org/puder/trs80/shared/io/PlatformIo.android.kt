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

package org.puder.trs80.shared.io

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import org.puder.trs80.shared.Log

private const val TAG = "PlatformIo"

private var appContext: Context? = null

/**
 * Hands the shared code a context, for the four small things that need one.
 *
 * Called once from `TRS80Application.onCreate`, alongside [initAppDataDirectory]
 * and the others. The application context, not an activity's: what is kept here
 * outlives every screen.
 */
fun initPlatformIo(context: Context) {
    appContext = context.applicationContext
}

/** The application context, or null with a complaint if nobody handed one over. */
internal fun androidContext(): Context? =
    appContext ?: null.also { Log.e(TAG, "Must call initPlatformIo() first.") }

/**
 * How the host activity offers a file picker.
 *
 * Android's picker is an activity result, which only an activity can register
 * for, so unlike the rest of this file it cannot be answered from a context
 * alone. The host registers one for as long as it is on screen; before it does,
 * and after it goes, asking for a file does nothing rather than crashing.
 */
fun interface FileChooser {
    fun choose(onPicked: (name: String, content: ByteArray) -> Unit)
}

private var fileChooser: FileChooser? = null

/** Offers, or withdraws, the host's file picker. */
fun setFileChooser(chooser: FileChooser?) {
    fileChooser = chooser
}

actual fun pickFile(onPicked: (name: String, content: ByteArray) -> Unit) {
    val chooser = fileChooser
    if (chooser == null) {
        Log.e(TAG, "Nothing on screen can present a file picker.")
        return
    }
    chooser.choose(onPicked)
}

/**
 * Reads a document the user picked, as a name and its bytes.
 *
 * Whole, into memory, which is what the shared code takes: these are disk
 * images and ROMs, and the largest of them is a few hundred kilobytes.
 *
 * @return null if the document could not be read, which includes the user
 * having picked something this app has no permission for.
 */
fun readPickedFile(context: Context, uri: Uri): Pair<String, ByteArray>? {
    val resolver = context.contentResolver
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: uri.lastPathSegment
        ?: return null
    val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }
        .onFailure { Log.e(TAG, "Could not read $uri.", it) }
        .getOrNull()
        ?: return null
    return name to bytes
}

actual fun clipboardText(): String? {
    val context = androidContext() ?: return null
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = clipboard?.primaryClip?.takeIf { it.itemCount > 0 } ?: return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

actual fun openUrl(url: String) {
    val context = androidContext() ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        // Started from the application context, which belongs to no task of its
        // own; without this the system refuses to start it at all.
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { Log.e(TAG, "Nothing here opens $url.", it) }
}

actual fun shareText(text: String) {
    val context = androidContext() ?: return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(chooser) }
        .onFailure { Log.e(TAG, "Could not offer anything to share with.", it) }
}

/**
 * This app's Play Store page.
 *
 * Built from the package the app is actually running as, rather than written
 * out: the store id is the package name, and the two must not be able to
 * disagree — a debug build with a suffixed id would otherwise offer to rate the
 * release.
 */
actual val storeListingUrl: String?
    get() = androidContext()?.let { "https://play.google.com/store/apps/details?id=${it.packageName}" }
