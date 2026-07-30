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

package org.puder.trs80.configuration

import okio.IOException
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.storage.AppStorage

/**
 * The configuration manager, creating it on first use.
 *
 * The manager itself is in `commonMain` and is handed its storage rather than
 * finding it, so something has to supply the Android store — this is that
 * something. It is deliberately lazy and still throws, because that is what the
 * screens calling it already expect: whoever gets here first pays for creating
 * the app's directory, and handles the failure by finishing.
 *
 * @throws IOException if the app's storage directory could not be created.
 */
@Throws(IOException::class)
fun ConfigurationManager.Companion.getOrInit(): ConfigurationManager =
    ConfigurationManager.init(FileManager.Creator.get(), AppStorage.get().settings)
