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

package org.puder.trs80.shared.storage

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/** The one preferences file everything shared is stored in. */
const val STORE_NAME = "trs80_store"

private var store: Settings? = null

/**
 * Opens the shared store. Called once from `TRS80Application.onCreate`.
 *
 * Android needs a `Context` to open a preferences file and the shared code has
 * none, so the app hands it over — the same arrangement as
 * `initAppDataDirectory`.
 */
fun initAppSettings(context: Context): Settings =
    SharedPreferencesSettings(
        context.applicationContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)
    ).also { store = it }

actual fun appSettings(): Settings =
    checkNotNull(store) { "Must call initAppSettings() first." }
