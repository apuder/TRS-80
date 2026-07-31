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

import okio.Path.Companion.toPath

/**
 * Turning the path of a file into something safe to write down, and back again.
 *
 * Configurations name their disks, cassettes and ROMs by path, and those paths
 * used to be stored absolute. That is fine on Android, where `filesDir` never
 * moves. On iOS it is a bug: the app's data container is a UUID that changes
 * when the app is reinstalled, and Apple's guidance is not to persist paths into
 * it. The symptom is a configuration that survives with every value intact and
 * every file missing.
 *
 * So anything inside the app's own directory is stored relative to it and made
 * absolute again on the way out.
 */

/** @return what should be written down for a file at [absolute]. */
fun toStoredPath(absolute: String): String {
    val base = appDataDirectory().toString()
    return when {
        absolute.startsWith("$base/") -> absolute.removePrefix("$base/")
        else -> absolute
    }
}

/**
 * @return the absolute path of a stored one.
 *
 * A path that is already absolute is left alone: that is what every
 * configuration written before this change holds, and on Android those still
 * resolve. They are rewritten as relative the next time they are set.
 */
fun resolveStoredPath(stored: String): String =
    if (stored.startsWith("/")) stored else (appDataDirectory() / stored.toPath()).toString()
