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
 * The page's own localStorage.
 *
 * Per origin and per browser, like every other setting a site keeps, with the
 * caveat that clearing site data clears the machines with it. It used to be
 * emptied on the way in, because remembering a machine whose disk images had
 * gone gave a library full of entries that booted to `Cass?` -- but the disks
 * are kept now too, in the same store, so the two agree again and both
 * remember. See BrowserFileSystem.
 */
actual fun appSettings(): Settings = StorageSettings()
