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

import org.puder.trs80.shared.Log

private const val TAG = "PlatformIo"

private fun openWindow(url: String) { js("window.open(url, '_blank')") }

/**
 * Not yet: taking a file wants an `<input type=file>` in the page and a trip
 * through its change event, which is a piece of DOM this module does not have
 * yet. Nothing calls it until there is a machine to give a disk to.
 */
actual fun pickFile(onPicked: (name: String, content: ByteArray) -> Unit) {
    Log.w(TAG, "Picking a file is not implemented in the browser yet.")
}

/**
 * Null, and it cannot be otherwise here.
 *
 * The browser's clipboard read is asynchronous and permissioned, and this is a
 * synchronous call. Paste in a page comes from a paste *event* instead -- the
 * user's own Ctrl-V -- which is a different shape entirely and belongs to
 * whatever draws the machine.
 */
actual fun clipboardText(): String? = null

actual fun openUrl(url: String) = openWindow(url)

/** No share sheet without a user gesture; the link goes to a new tab instead. */
actual fun shareText(text: String) {
    Log.w(TAG, "Sharing is not implemented in the browser yet.")
}

/** A web app has no store listing to send anyone to. */
actual val storeListingUrl: String? = null
