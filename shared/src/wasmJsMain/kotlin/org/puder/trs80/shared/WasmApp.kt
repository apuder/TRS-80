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
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.io.appDataDirectory
import org.puder.trs80.shared.localstore.RomManager
import org.puder.trs80.shared.storage.appSettings
import org.puder.trs80.shared.ui.DiskImageSpec

/** Where the app's own files live inside the page's file system. */
private const val TRS80_DIRECTORY = "trs80"

/**
 * The web app: the same screens, on a canvas.
 *
 * The whole of the host, and it is short for the same reason the iOS one is --
 * everything drawn is [Trs80AppUi] from the shared module. What is missing here
 * is not screens but the machine: see [BrowserCore].
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val creator = FileManager.Creator(appDataDirectory() / TRS80_DIRECTORY)
    ConfigurationManager.init(creator, appSettings())
    RomManager.init(creator, appSettings())

    ComposeViewport(viewportContainerId = "trs80") {
        Trs80AppUi(core = BrowserCore)
    }
}

/**
 * A machine that does nothing, so that everything around one can be worked on.
 *
 * The emulator is C. A browser takes C only through Emscripten, as WebAssembly
 * with a JavaScript boundary either side, and that is a piece of work with its
 * own decisions in it -- where the run loop lives, how the framebuffer crosses
 * into Kotlin, what happens to the audio. None of that has to be answered to
 * find out whether the library, the catalog, the editor and the settings work
 * in a page, and this is what lets that question be asked first.
 *
 * Every member is the honest do-nothing: booting says it worked, the screen is
 * blank, and nothing is stored. When the real core lands this file is what it
 * replaces.
 */
private object BrowserCore : EmulatorCore {

    override val isExpandedMode = false

    override val screenBuffer = object : ScreenBuffer {
        override fun get(index: Int): Byte = ' '.code.toByte()
    }

    override val pixelBuffer = object : ScreenBuffer {
        override fun get(index: Int): Byte = 0
    }

    override var pixelWidth = 0
        private set

    override var pixelHeight = 0
        private set

    override val romCellWidth = 8
    override val romCellHeight = 12

    override fun setCellSize(width: Int, height: Int) {
        pixelWidth = width * SCREEN_COLUMNS
        pixelHeight = height * SCREEN_ROWS
    }

    /** Nothing ever changes, so nothing is ever drawn. */
    override fun render(): Boolean = false

    override fun invalidateRender() = Unit

    override fun copyPixelsInto(destination: ByteArray) = destination.fill(0)

    override fun boot(
        model: Int,
        romPath: String,
        diskPaths: List<String?>,
        cassettePath: String?,
        entryAddress: Int,
    ): Boolean {
        Log.i(TAG, "Pretending to boot model $model; there is no core in the browser yet.")
        return true
    }

    override fun run() = Unit
    override fun stop() = Unit
    override fun reset() = Unit
    override fun saveState(path: String) = Unit
    override fun loadState(path: String) = Unit
    override fun setSoundMuted(muted: Boolean) = Unit
    override fun paste(text: String) = Unit
    override fun rewindCassette() = Unit
    override fun cassettePosition() = 0f
    override fun keyDown(sym: Int, key: Int) = Unit
    override fun keyUp(sym: Int, key: Int) = Unit
    override fun createBlankDisk(path: String, spec: DiskImageSpec) = false
}

private const val TAG = "BrowserCore"
