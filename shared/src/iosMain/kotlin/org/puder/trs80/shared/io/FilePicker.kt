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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.puder.trs80.shared.Log
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject
import platform.posix.memcpy

private const val TAG = "FilePicker"

/**
 * The delegate the presented picker calls back on.
 *
 * UIKit holds its delegate weakly, so without somewhere to keep this it would
 * be collected the moment the call returns and the picker would report to
 * nobody. One at a time is enough: only one picker can be on screen.
 */
private var pending: PickerDelegate? = null

/**
 * Asks the user for a file, and hands back its name and contents.
 *
 * Opened `asCopy`, so what arrives is a copy this app already owns rather than a
 * reference into someone else's document that would need its security scope
 * managed. [onPicked] is not called if the user backs out.
 */
actual fun pickFile(onPicked: (name: String, content: ByteArray) -> Unit) {
    val host = topViewController()
    if (host == null) {
        Log.e(TAG, "No view controller to present the picker from.")
        return
    }
    val delegate = PickerDelegate(onPicked)
    pending = delegate
    val picker = UIDocumentPickerViewController(
        forOpeningContentTypes = listOf(UTTypeItem),
        asCopy = true,
    )
    picker.delegate = delegate
    host.presentViewController(picker, animated = true, completion = null)
}

private class PickerDelegate(
    private val onPicked: (String, ByteArray) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        pending = null
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        val data = NSData.dataWithContentsOfURL(url)
        if (data == null) {
            Log.e(TAG, "Could not read the file the user picked.")
            return
        }
        onPicked(url.lastPathComponent ?: "picked", data.toByteArray())
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        pending = null
    }
}

/** Whatever is frontmost, which is what a modal has to be presented from. */
private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) {
        return ByteArray(0)
    }
    val out = ByteArray(size)
    out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}
