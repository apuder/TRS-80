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

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

/**
 * The app's own defaults.
 *
 * Nothing to initialize, unlike Android: NSUserDefaults is there from the start,
 * and the standard suite is already private to the app.
 *
 * One instance, held for the life of the process. NSUserDefaultsSettings
 * registers with NSUserDefaults to observe changes, so a fresh one per call
 * leaves observers behind whose target Kotlin/Native then collects — after
 * which the next write reaches a deallocated object and Objective-C aborts with
 * a null selector, on whatever thread happened to touch it.
 */
private val settings: Settings by lazy {
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
}

actual fun appSettings(): Settings = settings
