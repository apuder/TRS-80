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

import com.russhwolf.settings.Settings

/**
 * The one key-value store everything shared reads and writes.
 *
 * SharedPreferences on Android and NSUserDefaults on iOS, behind
 * multiplatform-settings' one interface — which is what let the existing Android
 * data stay readable when storage moved, and is why no format changed.
 *
 * The domain classes take a [Settings] in their constructors rather than calling
 * this, so they stay testable against an in-memory store. This is for the hosts,
 * which have to get the real one from somewhere.
 */
expect fun appSettings(): Settings
