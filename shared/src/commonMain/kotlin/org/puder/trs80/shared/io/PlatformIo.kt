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

/**
 * The things the app asks of the device it is running on.
 *
 * Small, and each of them is the same request on both platforms with a
 * different system behind it: pick a file, read the clipboard, open a link,
 * offer something to share. They are gathered here rather than passed in as
 * lambdas because there is exactly one right answer per platform and nothing
 * would ever want to supply another.
 */

/**
 * Asks the user for a file, and hands back its name and contents.
 *
 * What arrives is a copy this app owns, not a reference into someone else's
 * document. [onPicked] is not called if the user backs out.
 */
expect fun pickFile(onPicked: (name: String, content: ByteArray) -> Unit)

/** @return what is on the clipboard, if it is text. */
expect fun clipboardText(): String?

/** Hands [url] to the system, which decides what opens it. */
expect fun openUrl(url: String)

/**
 * Offers [text] to whatever the user shares with.
 *
 * The system's own sheet rather than anything of the app's: what is on it is
 * the user's business, and it changes with what they have installed.
 */
expect fun shareText(text: String)

/**
 * This app's page in the platform's store, or null while it has none.
 *
 * Null is not a failure to handle -- it is what decides whether Rate is offered
 * at all, because a Rate that opens something else is a lie.
 */
expect val storeListingUrl: String?

/** Where the app's community lives; the same place for both platforms. */
const val COMMUNITY_URL = "https://retrostore.org/community"

/** Where to send someone when there is no store listing to send them to. */
private const val PROJECT_URL = "https://retrostore.org"

/** What Share puts in the message: the store if there is one, the project if not. */
val shareUrl: String get() = storeListingUrl ?: PROJECT_URL
