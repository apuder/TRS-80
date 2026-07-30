/*
 * Copyright 2025, Arno Puder
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

import android.content.Context
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

private var filesDirectory: Path? = null

/**
 * Hands the shared code the app's private directory.
 *
 * Called once from `TRS80Application.onCreate`. It exists because Android's
 * files directory is only reachable through a `Context`, and the domain code
 * that needs it has none — this is the one place that gap is bridged, rather
 * than every class holding a context of its own as they used to.
 */
fun initAppDataDirectory(context: Context) {
    filesDirectory = context.applicationContext.filesDir.toOkioPath()
}

actual fun appDataDirectory(): Path =
    checkNotNull(filesDirectory) { "Must call initAppDataDirectory() first." }

actual val appFileSystem: FileSystem get() = FileSystem.SYSTEM
