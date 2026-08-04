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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.io.appDataDirectory
import org.puder.trs80.shared.localstore.RomManager
import org.puder.trs80.shared.storage.appSettings

/** Where the app's own files live inside the page's file system. */
private const val TRS80_DIRECTORY = "trs80"

/**
 * The web app: the same screens, on a canvas.
 *
 * The whole of the host, and it is short for the same reason the iOS one is --
 * everything drawn is [Trs80AppUi] from the shared module, and the machine is
 * the same C the other two run, fetched as WebAssembly. See [BrowserCore].
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val creator = FileManager.Creator(appDataDirectory() / TRS80_DIRECTORY)
    ConfigurationManager.init(creator, appSettings())
    RomManager.init(creator, appSettings())

    // The emulator is fetched, so the app draws nothing until it is here. The
    // other two platforms have theirs linked in and never wait; this is the one
    // place a browser's app starts differently from a device's.
    MainScope().launch {
        val emulator = runCatching { loadEmulator().await() }
            .onFailure { Log.e(TAG, "The emulator would not load: $it") }
            .getOrNull()
        ComposeViewport(viewportContainerId = "trs80") {
            Trs80AppUi(core = BrowserCore(emulator ?: return@ComposeViewport))
        }
    }
}

private const val TAG = "WasmApp"
