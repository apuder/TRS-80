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
package org.puder.trs80.shared.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

/**
 * In memory, and gone with the tab -- deliberately, for now.
 *
 * localStorage is right here and would remember a machine perfectly. The
 * trouble is that it would remember it *alone*: the disk images that make a
 * machine a machine live in the file system, and the browser's is in memory
 * until something persists it. Remembering the configuration and losing its
 * disk gives a library full of machines that boot to `Cass?`, which is worse
 * than a library that starts empty, because it looks like the app is broken
 * rather than new.
 *
 * So the two agree, and both forget: the store is localStorage, which is where
 * this wants to be, and it is emptied on the way in. When the file system
 * persists -- OPFS, probably, or the images in IndexedDB -- deleting the clear()
 * below is the whole of what makes the library remember again.
 */
private val settings: Settings by lazy {
    // Once, on the way in. Emptying it per call would wipe what the call before
    // had just written -- this is asked for from half a dozen places.
    StorageSettings().also { it.clear() }
}

actual fun appSettings(): Settings = settings
