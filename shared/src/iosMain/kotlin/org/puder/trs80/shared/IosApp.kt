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
import org.puder.trs80.shared.configuration.EmulatorState
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.io.TRS80_DIRECTORY
import org.puder.trs80.shared.io.appDataDirectory
import org.puder.trs80.shared.io.appFileSystem
import org.puder.trs80.shared.io.pickFile
import org.puder.trs80.shared.localstore.RomManager
import org.puder.trs80.shared.navigation.Destination
import org.puder.trs80.shared.navigation.Trs80App
import org.puder.trs80.shared.navigation.rememberNavigator
import org.puder.trs80.shared.storage.appSettings
import org.puder.trs80.shared.ui.ConfigurationCard
import org.puder.trs80.shared.ui.LibraryActions
import org.puder.trs80.shared.ui.LibraryScreen
import org.puder.trs80.shared.ui.LibrarySort
import org.puder.trs80.shared.ui.asCatalogue
import org.puder.trs80.shared.ui.matching
import org.puder.trs80.shared.ui.matchingEntries
import org.puder.trs80.shared.ui.sortedFor
import org.puder.trs80.shared.ui.theme.ThemePreference
import org.puder.trs80.shared.ui.theme.Trs80Theme
import org.puder.trs80.shared.configuration.toDraft
import org.puder.trs80.shared.ui.EditConfigurationActions
import org.puder.trs80.shared.ui.EditConfigurationScreen
import org.puder.trs80.shared.ui.SettingsScreen
import androidx.compose.foundation.isSystemInDarkTheme
import org.puder.trs80.shared.navigation.Navigator
import org.puder.trs80.shared.ui.EmulatorScaffold
import org.puder.trs80.shared.ui.RetroStoreAppScreen
import org.puder.trs80.shared.ui.StoreState
import org.puder.trs80.shared.store.AppInstaller
import org.puder.trs80.shared.store.retroStore
import org.retrostore.client.common.proto.App
import androidx.compose.runtime.rememberCoroutineScope
import org.puder.trs80.shared.ui.encodePng
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
 * The iOS app: the library, and a machine when one is running.
 *
 * Still short of the Android app — there is no editor and no settings, and those
 * arrive as the rest of §7.2 lands. Everything it does show is shared code, so
 * Android will run the same screens when its own UI moves across.
 *
 * @param romPath a Model III ROM image and [diskPath] a disk, both in the app
 * bundle, seeded into the app's own storage on first run.
 */
@OptIn(ExperimentalForeignApi::class)
fun Trs80ViewController(romPath: String, diskPath: String?): UIViewController {
    installIfNeeded(romPath, diskPath)

    val compose = ComposeUIViewController {
        val navigator = rememberNavigator()
        // The choice is read once and held here, so changing it repaints the
        // whole app rather than only the screen that changed it.
        var theme by remember { mutableStateOf(ThemePreference.from(appSettings())) }
        Trs80Theme(
            dark = when (theme) {
                ThemePreference.Light -> false
                ThemePreference.Dark -> true
                ThemePreference.System -> isSystemInDarkTheme()
            },
        ) {
            Trs80App(
                navigator = navigator,
                library = { Library(navigator) },
                emulator = {
                    RunningMachine(it.configurationId, onBack = { navigator.goBack() })
                },
                retroStoreApp = { destination ->
                    StoreApp(destination.appId, onBack = { navigator.goBack() })
                },
                settings = {
                    SettingsScreen(
                        theme = theme,
                        onThemeChange = {
                            theme = it
                            it.storeIn(appSettings())
                        },
                        onBack = { navigator.goBack() },
                    )
                },
                editConfiguration = { Editor(it.configurationId, it.isNew, navigator) },
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
 * The configuration editor.
 *
 * The draft is read once, on the way in, and the screen edits that copy — so
 * leaving without saving leaves the stored configuration as it was. Save is the
 * only thing that writes.
 *
 * A machine created by the library's + is already persisted by the time it gets
 * here, because that is the only way it can have an id; deleting it on the way
 * back out is what makes Back behave as though it was never created.
 */
@Composable
private fun Editor(configurationId: Int, isNew: Boolean, navigator: Navigator) {
    val manager = ConfigurationManager.get()
    val original = remember(configurationId) {
        manager.getConfigById(configurationId)?.toDraft()
    }
    if (original == null) {
        // The configuration went away underneath us; there is nothing to edit.
        LaunchedEffect(configurationId) { navigator.goBack() }
        return
    }
    var draft by remember(configurationId) { mutableStateOf(original) }
    // The configuration's own model belongs on the control even if its ROM has
    // gone missing, or the editor would show nothing selected.
    val models = remember(original.model) {
        (RomManager.get().modelsToOffer() + original.model).distinct().sorted()
    }

    EditConfigurationScreen(
        draft = draft,
        original = original,
        models = models,
        onChange = { draft = it },
        actions = EditConfigurationActions(
            onSave = {
                manager.persistDraft(draft)
                navigator.goBack()
            },
            onBack = {
                if (isNew) {
                    manager.deleteConfigWithId(configurationId)
                }
                navigator.goBack()
            },
            onChooseDisk = { drive ->
                pickFile { name, content ->
                    manager.storeMedia(configurationId, name, content)
                        ?.let { draft = draft.withDiskIn(drive, it) }
                }
            },
            onChooseCassette = {
                pickFile { name, content ->
                    manager.storeMedia(configurationId, name, content)
                        ?.let { draft = draft.copy(cassettePath = it) }
                }
            },
            onRevert = { draft = original },
            onDelete = {
                manager.deleteConfigWithId(configurationId)
                navigator.goBack()
            },
        ),
    )
}

/**
 * The library: the user's machines and the store's catalogue on one screen.
 *
 * Both halves are loaded here rather than inside the screen, so the screen stays
 * a drawing of what it is handed — which is what keeps the sorting and filtering
 * testable on their own, without a display.
 */
@Composable
private fun Library(navigator: Navigator) {
    var cards by remember { mutableStateOf(emptyList<ConfigurationCard>()) }
    var apps by remember { mutableStateOf<StoreState>(StoreState.Loading) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(LibrarySort.LastUsed) }
    var expanded by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(emptySet<String>()) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        cards = withContext(Dispatchers.Default) { ConfigurationManager.get().toCards() }
    }

    // Re-read on every return, so a machine that has just run moves to the top
    // and shows what it was last doing.
    LaunchedEffect(navigator.current) {
        if (navigator.current is Destination.Library) {
            reload()
        }
    }
    LaunchedEffect(Unit) {
        apps = try {
            StoreState.Loaded(withContext(Dispatchers.Default) { retroStore.fetchApps(0, 100) })
        } catch (e: Exception) {
            Log.e(TAG, "Could not fetch the store catalogue.", e)
            StoreState.Failed(e.message.orEmpty())
        }
    }

    LibraryScreen(
        yours = cards.matching(query).sortedFor(sort),
        catalogue = ((apps as? StoreState.Loaded)?.apps.orEmpty())
            .asCatalogue(cards, installing)
            .matchingEntries(query),
        catalogueState = apps,
        query = query,
        sort = sort,
        expanded = expanded,
        onQueryChange = { query = it },
        onSortChange = { sort = it },
        onExpandedChange = { expanded = it },
        actions = LibraryActions(
            onRun = { navigator.goTo(Destination.Emulator(it)) },
            onOpenEntry = { navigator.goTo(Destination.RetroStoreApp(it.id)) },
            onOpenSettings = { navigator.goTo(Destination.Settings) },
            onEdit = { navigator.goTo(Destination.EditConfiguration(it, isNew = false)) },
            onAdd = {
                val fresh = ConfigurationManager.get().newConfiguration()
                navigator.goTo(Destination.EditConfiguration(fresh.id, isNew = true))
            },
            onInstall = { entry ->
                installing = installing + entry.id
                scope.launch {
                    val installed = withContext(Dispatchers.Default) {
                        runCatching {
                            retroStore.getApp(entry.id)
                                ?.let { AppInstaller(ConfigurationManager.get()).install(it) }
                        }.onFailure { Log.e(TAG, "Could not install ${entry.title}.", it) }
                            .getOrNull()
                    }
                    installing = installing - entry.id
                    if (installed != null) {
                        reload()
                    }
                }
            },
        ),
    )
}

/** One app from the store, and installing it. */
@Composable
private fun StoreApp(appId: String, onBack: () -> Unit) {
    var app by remember(appId) { mutableStateOf<App?>(null) }
    var installing by remember(appId) { mutableStateOf(false) }
    var installed by remember(appId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(appId) {
        app = try {
            withContext(Dispatchers.Default) { retroStore.getApp(appId) }
        } catch (e: Exception) {
            Log.e(TAG, "Could not fetch app $appId.", e)
            null
        }
    }

    RetroStoreAppScreen(
        app = app,
        installing = installing,
        installed = installed,
        onInstall = {
            val toInstall = app ?: return@RetroStoreAppScreen
            installing = true
            scope.launch {
                val configuration = withContext(Dispatchers.Default) {
                    runCatching { AppInstaller(ConfigurationManager.get()).install(toInstall) }
                        .onFailure { Log.e(TAG, "Could not install ${toInstall.name}.", it) }
                        .getOrNull()
                }
                installing = false
                installed = configuration != null
            }
        },
        onBack = onBack,
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
    // Held so leaving can write to it. The session is put away on the way out
    // rather than in onDispose, because the list reloads the moment the back
    // stack pops -- writing afterwards means it reads the previous screenshot.
    var emulatorState by remember(configurationId) { mutableStateOf<EmulatorState?>(null) }

    DisposableEffect(configurationId) {
        val configuration = ConfigurationManager.get().getConfigById(configurationId)
        val rom = configuration?.let { romPathFor(it.model) }
        // Recorded on the way in: the library orders by it, and a session that
        // crashes still counts as the thing the user was last doing.
        configuration?.markUsed()
        val state = runCatching {
            ConfigurationManager.get().getEmulatorState(configurationId)
        }.getOrNull()
        if (configuration == null || rom == null) {
            Log.e(TAG, "Cannot run configuration $configurationId: no configuration or no ROM.")
        } else {
            EmulatorCore.boot(
                model = configuration.model,
                romPath = rom,
                diskPaths = configuration.diskPaths.filterNotNull(),
            )
            // Pick the session up where it was left, if there is one.
            if (state?.hasState() == true) {
                EmulatorCore.loadState(state.stateFilePath)
            }
        }
        // A thread of its very own, not Dispatchers.Default. trs80_run() does not
        // return until the machine is stopped, so on a shared pool it permanently
        // occupies one of a handful of threads -- on Darwin that pool is a global
        // dispatch queue, and taking a worker out of it for the life of the app
        // breaks things far away from here.
        val cpu = newSingleThreadContext("trs80-cpu")
        CoroutineScope(cpu).launch { EmulatorCore.run() }
        emulatorState = state
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
        onBack = {
            // Stop, then write. The CPU thread tests a flag rather than being
            // interrupted, so this keeps the same small race between the last
            // instruction and the snapshot that the Android app has always had.
            EmulatorCore.stop()
            emulatorState?.let { state ->
                EmulatorCore.saveState(state.stateFilePath)
                source.snapshot(CHARACTER_COLOR, SCREEN_COLOR)
                    ?.let(::encodePng)
                    ?.let(state::writeScreenshot)
            }
            onBack()
        },
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
