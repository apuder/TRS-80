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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.io.TRS80_DIRECTORY
import org.puder.trs80.shared.io.appDataDirectory
import org.puder.trs80.shared.io.appFileSystem
import org.puder.trs80.shared.localstore.RomManager
import org.puder.trs80.shared.navigation.Destination
import org.puder.trs80.shared.navigation.Trs80App
import org.puder.trs80.shared.navigation.rememberNavigator
import org.puder.trs80.shared.storage.appSettings
import org.puder.trs80.shared.ui.ConfigurationCard
import org.puder.trs80.shared.ui.ConfigurationListActions
import org.puder.trs80.shared.ui.ConfigurationListScreen
import org.puder.trs80.shared.ui.EmulatorScaffold
import org.puder.trs80.shared.ui.Keyboard
import org.puder.trs80.shared.ui.KeyboardState
import org.puder.trs80.shared.ui.ORIGINAL_KEYBOARD
import org.puder.trs80.shared.ui.keyboardFor
import org.puder.trs80.shared.ui.toCards
import platform.UIKit.UIViewController

private const val TAG = "IosApp"

/** Green on dark, as the emulated machine's phosphor and glass. */
private val CHARACTER_COLOR = Color(0xFF77FB4D)
private val SCREEN_COLOR = Color(0xFF444444)

/**
 * The iOS app: the configuration list, and a machine when one is running.
 *
 * Still short of the Android app — there is no editor, no settings and no
 * RetroStore, and those arrive as the rest of §7.2 lands. What it is no longer
 * is a hard-coded jump into the emulator: it runs the shared domain, draws the
 * shared list and navigates with the shared navigator, so everything it shows is
 * code Android will run too.
 *
 * @param romPath a Model III ROM image and [diskPath] a disk, both in the app
 * bundle, seeded into the app's own storage on first run.
 */
@OptIn(ExperimentalForeignApi::class)
fun Trs80ViewController(romPath: String, diskPath: String?): UIViewController {
    installIfNeeded(romPath, diskPath)

    val compose = ComposeUIViewController {
        val navigator = rememberNavigator()
        var cards by remember { mutableStateOf(emptyList<ConfigurationCard>()) }

        // Reading the cards means reading files and decoding PNGs, so it happens
        // off the main thread. Keyed on the destination, so coming back from a
        // machine picks up its new screenshot and saved state.
        LaunchedEffect(navigator.current) {
            if (navigator.current is Destination.ConfigurationList) {
                cards = withContext(Dispatchers.Default) { ConfigurationManager.get().toCards() }
            }
        }

        MaterialTheme(colorScheme = darkColorScheme()) {
            Trs80App(
                navigator = navigator,
                configurationList = {
                    ConfigurationListScreen(
                        cards = cards,
                        // Edit, share, delete and add need screens and
                        // confirmations that do not exist yet, so the list does
                        // not offer them rather than offering nothing.
                        actions = ConfigurationListActions(
                            onRun = { navigator.goTo(Destination.Emulator(it)) },
                        ),
                    )
                },
                emulator = { RunningMachine(it.configurationId, onBack = { navigator.goBack() }) },
            )
        }
    }

    // A real keyboard has to be taken in UIKit, not in Compose; see
    // KeyForwardingController.
    return KeyForwardingController(
        content = compose,
        onKeyDown = { EmulatorCore.keyDown(it.sym, it.key) },
        onKeyUp = { EmulatorCore.keyUp(it.sym, it.key) },
    )
}

/**
 * A machine, booted for as long as this is on screen.
 *
 * Boots on the way in and stops on the way out, so going back to the list stops
 * the CPU rather than leaving it running behind the list.
 */
@OptIn(ExperimentalForeignApi::class, DelicateCoroutinesApi::class)
@Composable
private fun RunningMachine(configurationId: Int, onBack: () -> Unit) {
    val source = remember(configurationId) { IosEmulatorScreenSource() }

    DisposableEffect(configurationId) {
        val configuration = ConfigurationManager.get().getConfigById(configurationId)
        val rom = configuration?.let { romPathFor(it.model) }
        if (configuration == null || rom == null) {
            Log.e(TAG, "Cannot run configuration $configurationId: no configuration or no ROM.")
        } else {
            EmulatorCore.boot(
                model = configuration.model,
                romPath = rom,
                diskPaths = configuration.diskPaths.filterNotNull(),
            )
        }
        // A thread of its very own, not Dispatchers.Default. trs80_run() does not
        // return until the machine is stopped, so on a shared pool it permanently
        // occupies one of a handful of threads -- on Darwin that pool is a global
        // dispatch queue, and taking a worker out of it for the life of the app
        // breaks things far away from here.
        val cpu = newSingleThreadContext("trs80-cpu")
        CoroutineScope(cpu).launch { EmulatorCore.run() }
        onDispose {
            EmulatorCore.stop()
            cpu.close()
        }
    }

    val configuration = remember(configurationId) {
        ConfigurationManager.get().getConfigById(configurationId)
    }
    // The layout the configuration asks for, falling back to the full keyboard.
    // A machine with no way to type at it is not much use.
    val definition = remember(configurationId) {
        keyboardFor(configuration?.keyboardLayoutPortrait) ?: ORIGINAL_KEYBOARD
    }
    val keyboard = remember(definition) {
        KeyboardState(
            definition = definition,
            onKeyDown = { EmulatorCore.keyDown(it.sym, it.key) },
            onKeyUp = { EmulatorCore.keyUp(it.sym, it.key) },
        )
    }

    EmulatorScaffold(
        title = configuration?.name.orEmpty(),
        onBack = onBack,
        keyboard = { Keyboard(keyboard) },
    ) {
        EmulatorScreen(
            source = source,
            characterColor = CHARACTER_COLOR,
            screenColor = SCREEN_COLOR,
        )
    }
}

/**
 * Seeds the bundled ROM and disk on first run.
 *
 * Idempotent: on later runs the store already has the configuration and the
 * files are already there, so this finds them rather than copying again.
 */
private fun installIfNeeded(romPath: String, diskPath: String?) {
    val settings = appSettings()
    val creator = FileManager.Creator(appDataDirectory() / TRS80_DIRECTORY)
    val manager = ConfigurationManager.init(creator, settings)
    RomManager.init(creator, settings)

    if (manager.configCount > 0) {
        Log.i(TAG, "${manager.configCount} configuration(s) already installed.")
        return
    }

    val rom = appFileSystem.read(romPath.toPath()) { readByteArray() }
    RomManager.get().addRom(MODEL3, "model3.rom", rom)

    val disks = diskPath?.let {
        listOf(
            ConfigurationManager.ConfigMedia(
                filename = it.toPath().name,
                data = appFileSystem.read(it.toPath()) { readByteArray() },
            )
        )
    }.orEmpty()

    manager.addNewConfiguration(MODEL3, "Model III", disks, cassette = null)
        ?: Log.e(TAG, "Could not install the bundled configuration.")
}

/** @return the path of the ROM for [model], or null if there is none. */
private fun romPathFor(model: Int): String? = RomManager.get().romPath(model)
