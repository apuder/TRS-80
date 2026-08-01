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

package org.puder.trs80.shared

import android.content.Context
import android.content.pm.PackageManager

private var version: String? = null

/**
 * Reads the version out of the package, once, at start-up.
 *
 * Android keeps it on the `PackageManager` rather than anywhere the shared code
 * can reach, so the app hands it over — the same arrangement as the data
 * directory and the settings store.
 */
fun initAppVersion(context: Context) {
    version = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = info.longVersionCode
        "${info.versionName} ($code)"
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

actual fun appVersion(): String = version ?: "unknown"
