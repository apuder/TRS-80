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
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

private const val TAG = "AppLinks"

/**
 * This app's page on the App Store, once it has one.
 *
 * Empty until the app ships: an iOS listing has a numeric id assigned when the
 * record is created, and there is no way to work one out in advance. While it is
 * empty the app offers no Rate, because a Rate that opens something else is a
 * lie, and Share sends people to the project's own page instead.
 *
 * Fill this in and both follow.
 */
private const val APP_STORE_ID = ""

actual val storeListingUrl: String?
    get() = APP_STORE_ID.takeIf { it.isNotEmpty() }?.let { "https://apps.apple.com/app/id$it" }

actual fun openUrl(url: String) {
    val target = NSURL.URLWithString(url)
    if (target == null) {
        Log.e(TAG, "Not a URL: $url")
        return
    }
    UIApplication.sharedApplication.openURL(target)
}

/**
 * Offers [text] to whatever the user shares with.
 *
 * The system sheet rather than anything of the app's own: what is on it is the
 * user's business, and it changes with what they have installed.
 */
actual fun shareText(text: String) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController
    if (root == null) {
        Log.e(TAG, "Nothing on screen to present the share sheet from.")
        return
    }
    val sheet = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null,
    )
    // An iPad refuses to present this without somewhere to point the popover at,
    // and the property is on the presentation controller the sheet is given
    // rather than on the sheet itself.
    sheet.popoverPresentationController?.sourceView = root.view
    root.presentViewController(sheet, animated = true, completion = null)
}
