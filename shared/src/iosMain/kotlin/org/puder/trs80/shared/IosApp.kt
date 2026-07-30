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

package org.puder.trs80.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import org.puder.trs80.shared.configuration.Configuration
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.io.TRS80_DIRECTORY
import org.puder.trs80.shared.io.appDataDirectory
import org.puder.trs80.shared.io.appFileSystem
import org.puder.trs80.shared.localstore.RomManager
import org.puder.trs80.shared.storage.StorageKeys
import org.puder.trs80.shared.storage.appSettings
import platform.UIKit.UIViewController

private const val TAG = "IosApp"

/** Green on dark, as the emulated machine's phosphor and glass. */
private val CHARACTER_COLOR = Color(0xFF77FB4D)
private val SCREEN_COLOR = Color(0xFF444444)

/**
 * The iOS entry point: a view controller showing the emulated screen.
 *
 * Still a spike — there is no configuration list, no settings and no input, all
 * of which belong to the port proper. What it does do is run the *real* domain:
 * the ROM and disk are installed through [RomManager] and [ConfigurationManager]
 * into the app's own directory, and the machine boots from what those hand back.
 * That is the point of it at this stage. It exercises NSUserDefaults behind
 * `appSettings()`, the sandbox's Documents directory behind `appDataDirectory()`,
 * and okio underneath both, on the device rather than in a test.
 *
 * @param romPath a Model III ROM image and [diskPath] a disk, both inside the
 * app bundle, which is read-only — hence the copy into the app's own directory
 * on first run.
 */
@OptIn(ExperimentalForeignApi::class)
fun EmulatorViewController(romPath: String, diskPath: String?): UIViewController {
    val source = IosEmulatorScreenSource()
    val configuration = installIfNeeded(romPath, diskPath)

    EmulatorCore.boot(
        model = configuration.model,
        romPath = requireNotNull(romPathFor(configuration.model)) {
            "No ROM stored for model ${configuration.model}."
        },
        diskPaths = configuration.diskPaths.filterNotNull(),
    )
    // trs80_run() blocks, so the CPU gets a thread of its own. Rasterizing
    // happens on whichever thread draws, never here: doing it on this one would
    // both steal time from the emulated machine and turn a torn read of video RAM
    // from a stale character into a half-drawn one.
    CoroutineScope(Dispatchers.Default).launch { EmulatorCore.run() }

    return ComposeUIViewController {
        EmulatorScreen(
            source = source,
            characterColor = CHARACTER_COLOR,
            screenColor = SCREEN_COLOR,
        )
    }
}

/**
 * Installs the bundled ROM and disk on first run, and returns the configuration
 * to boot.
 *
 * Idempotent, because it is the app's whole start-up path: on later runs the
 * store already has the configuration and the files are already on disk, so this
 * finds them rather than copying them again.
 */
private fun installIfNeeded(romPath: String, diskPath: String?): Configuration {
    val settings = appSettings()
    val creator = FileManager.Creator(appDataDirectory() / TRS80_DIRECTORY)
    val manager = ConfigurationManager.init(creator, settings)
    RomManager.init(creator, settings)

    if (manager.configCount > 0) {
        return manager.getConfig(0).also { Log.i(TAG, "Using existing configuration ${it.id}.") }
    }

    val rom = requireNotNull(appFileSystem.read(romPath.toPath()) { readByteArray() })
    RomManager.get().addRom(MODEL3, "model3.rom", rom)

    val disks = diskPath?.let {
        listOf(
            ConfigurationManager.ConfigMedia(
                filename = it.toPath().name,
                data = appFileSystem.read(it.toPath()) { readByteArray() },
            )
        )
    }.orEmpty()

    return requireNotNull(
        manager.addNewConfiguration(MODEL3, "Model III", disks, cassette = null)
    ) { "Could not install the bundled configuration." }
}

/** @return the stored path of the ROM for [model], or null if there is none. */
private fun romPathFor(model: Int): String? =
    appSettings().getStringOrNull(StorageKeys.romKey(model))
