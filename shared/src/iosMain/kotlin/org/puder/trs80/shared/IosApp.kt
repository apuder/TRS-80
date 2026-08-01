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
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import org.puder.trs80.shared.ui.Catalog
import org.puder.trs80.shared.ui.CatalogEntry
import org.puder.trs80.shared.ui.DetailAction
import org.puder.trs80.shared.ui.DetailContent
import org.puder.trs80.shared.ui.DetailSheet
import org.jetbrains.compose.resources.stringResource
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.disk_many
import trs_80.shared.generated.resources.disk_one
import org.puder.trs80.shared.ui.mediaSummary
import org.puder.trs80.shared.ui.modelLabel
import org.puder.trs80.shared.store.modelOf
import org.puder.trs80.shared.ui.ConfigurationCard
import org.puder.trs80.shared.ui.LibraryActions
import org.puder.trs80.shared.ui.LibraryScreen
import org.puder.trs80.shared.ui.LibrarySort
import org.puder.trs80.shared.ui.asCatalog
import org.puder.trs80.shared.ui.matching
import org.puder.trs80.shared.ui.matchingEntries
import org.puder.trs80.shared.ui.sortedFor
import org.puder.trs80.shared.ui.theme.ThemePreference
import org.puder.trs80.shared.ui.theme.Trs80Theme
import org.puder.trs80.shared.configuration.toDraft
import org.puder.trs80.shared.ui.EditConfigurationActions
import org.puder.trs80.shared.ui.EditConfigurationScreen
import org.puder.trs80.shared.ui.RomSetupPanel
import org.puder.trs80.shared.ui.Roms
import org.puder.trs80.shared.ScreenColors
import org.puder.trs80.shared.ui.SettingsScreen
import androidx.compose.foundation.isSystemInDarkTheme
import org.puder.trs80.shared.navigation.Navigator
import org.puder.trs80.shared.ui.EmulatorScaffold
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

/** The glass behind the phosphor. The machine had one; it is not a choice. */
private val SCREEN_COLOR = Color(0xFF444444)

/**
 * The iOS app: the library, and a machine when one is running.
 *
 * Still short of the Android app — there is no editor and no settings, and those
 * arrive as the rest of §7.2 lands. Everything it does show is shared code, so
 * Android will run the same screens when its own UI moves across.
 *
 * @param diskPath a disk image in the app bundle, seeded into the app's own
 * storage on first run. The ROMs are no longer among them: the app downloads
 * those itself, as the Android app always has.
 */
@OptIn(ExperimentalForeignApi::class)
fun Trs80ViewController(diskPath: String?): UIViewController {
    installIfNeeded(diskPath)

    val capture = KeyCapture()
    val compose = ComposeUIViewController {
        val navigator = rememberNavigator()
        val catalog = remember { Catalog() }
        val roms = remember { Roms(RomManager.get()) }
        var setupDismissed by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        // The machine cannot start without these, so they are fetched before
        // anything asks for one. Failures leave the settings screen offering
        // another go rather than stopping the app.
        LaunchedEffect(roms) { roms.downloadMissing() }
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
            Box(Modifier.fillMaxSize()) {
            Trs80App(
                navigator = navigator,
                library = { Library(navigator, catalog) },
                emulator = {
                    RunningMachine(
                        it.configurationId,
                        capture = capture,
                        onBack = { navigator.goBack() },
                    )
                },
                settings = {
                    SettingsScreen(
                        theme = theme,
                        onThemeChange = {
                            theme = it
                            it.storeIn(appSettings())
                        },
                        onBack = { navigator.goBack() },
                        roms = roms.statuses,
                        romsDownloading = roms.downloading,
                        onDownloadRoms = { scope.launch { roms.downloadMissing() } },
                        onChooseRom = { model ->
                            pickFile { name, content -> roms.useFile(model, name, content) }
                        },
                        onRedownloadRom = { model ->
                            scope.launch { roms.redownload(model) }
                        },
                    )
                },
                editConfiguration = { Editor(it.configurationId, it.isNew, navigator) },
            )

            // Over whatever is on screen: nothing the app offers works until
            // these are here.
            val settingUp = roms.busy || (roms.attempted && roms.missing.isNotEmpty())
            if (settingUp && !setupDismissed) {
                RomSetupPanel(
                    downloading = roms.busy,
                    missing = roms.missing,
                    onRetry = { scope.launch { roms.downloadMissing() } },
                    onDismiss = { setupDismissed = true },
                )
            }
            }
        }
    }

    // A real keyboard has to be taken in UIKit, not in Compose; see
    // KeyForwardingController.
    return KeyForwardingController(
        content = compose,
        capture = capture,
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
 * The library: the user's machines and the store's catalog on one screen.
 *
 * Both halves are loaded here rather than inside the screen, so the screen stays
 * a drawing of what it is handed — which is what keeps the sorting and filtering
 * testable on their own, without a display.
 */
@Composable
private fun Library(navigator: Navigator, catalog: Catalog) {
    var cards by remember { mutableStateOf(emptyList<ConfigurationCard>()) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(LibrarySort.LastUsed) }
    var expanded by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(emptySet<String>()) }
    var selected by remember { mutableStateOf<CatalogEntry?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        cards = withContext(Dispatchers.Default) { ConfigurationManager.get().toCards() }
    }

    fun install(app: App) {
        installing = installing + app.id
        scope.launch {
            val configuration = withContext(Dispatchers.Default) {
                runCatching { AppInstaller(ConfigurationManager.get()).install(app) }
                    .onFailure { Log.e(TAG, "Could not install ${app.name}.", it) }
                    .getOrNull()
            }
            installing = installing - app.id
            if (configuration != null) {
                reload()
            }
        }
    }

    // Re-read on every return, so a machine that has just run moves to the top
    // and shows what it was last doing.
    LaunchedEffect(navigator.current) {
        if (navigator.current is Destination.Library) {
            reload()
        }
    }
    LaunchedEffect(catalog) { catalog.loadOnce() }

    val listed = (catalog.state as? StoreState.Loaded)?.apps.orEmpty()
    val entries = listed.asCatalog(cards, installing)

    val selectedEntry = selected
    Box(Modifier.fillMaxSize()) {
    LibraryScreen(
        yours = cards.matching(query).sortedFor(sort),
        catalog = entries.matchingEntries(query),
        catalogState = catalog.state,
        refreshing = catalog.refreshing,
        query = query,
        sort = sort,
        expanded = expanded,
        onQueryChange = { query = it },
        onSortChange = { sort = it },
        onExpandedChange = { expanded = it },
        actions = LibraryActions(
            onRun = { navigator.goTo(Destination.Emulator(it)) },
            onOpenEntry = { selected = it },
            onOpenSettings = { navigator.goTo(Destination.Settings) },
            onRefresh = { scope.launch { catalog.refresh() } },
            onEdit = { navigator.goTo(Destination.EditConfiguration(it, isNew = false)) },
            onAdd = {
                val fresh = ConfigurationManager.get().newConfiguration()
                navigator.goTo(Destination.EditConfiguration(fresh.id, isNew = true))
            },
            onInstall = { entry ->
                listed.firstOrNull { it.id == entry.id }
                    ?.let(::install)
            },
        ),
    )

        val selectedApp = listed.firstOrNull { it.id == selectedEntry?.id }
        if (selectedEntry != null && selectedApp != null) {
            Detail(
                entry = selectedEntry,
                app = selectedApp,
                installing = selectedEntry.id in installing,
                onInstall = { install(selectedApp) },
                onRun = { navigator.goTo(Destination.Emulator(it)) },
                onCopied = { scope.launch { reload() } },
                onDismiss = { selected = null },
            )
        }
    }
}

/**
 * The sheet for one catalog entry.
 *
 * The record is filled in from what is actually on the device: the store hands
 * over a program's media only by sending all of it, so the size of something
 * not yet downloaded is not a thing this app can know without downloading it.
 */
@Composable
private fun Detail(
    entry: CatalogEntry,
    app: App,
    installing: Boolean,
    onInstall: () -> Unit,
    onRun: (Int) -> Unit,
    onCopied: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val installedId = entry.installedId
    val disks = remember(installedId) {
        installedId?.let { id ->
            ConfigurationManager.get().getConfigById(id)?.diskPaths.orEmpty().filterNotNull()
        }.orEmpty()
    }
    val media = mediaSummary(
        disks = when {
            disks.isEmpty() -> ""
            disks.size == 1 -> stringResource(Res.string.disk_one)
            else -> stringResource(Res.string.disk_many, disks.size)
        },
        totalBytes = disks.sumOf { sizeOf(it) },
    )

    DetailSheet(
        content = DetailContent(
            title = app.name,
            author = app.author,
            year = app.release_year,
            coverUrl = app.screenshot_url.firstOrNull(),
            screenshotUrls = app.screenshot_url,
            description = app.description,
            machine = modelLabel(modelOf(app.ext_trs80?.model)),
            media = media,
            source = "RetroStore",
        ),
        action = when {
            installing -> DetailAction.Downloading
            entry.installed -> DetailAction.Play
            else -> DetailAction.Download
        },
        onPrimary = {
            val id = entry.installedId
            when {
                installing -> Unit
                id != null -> { onDismiss(); onRun(id) }
                else -> onInstall()
            }
        },
        onCopy = {
            val id = entry.installedId ?: return@DetailSheet
            scope.launch {
                withContext(Dispatchers.Default) { ConfigurationManager.get().duplicate(id) }
                onCopied()
                onDismiss()
            }
        },
        onDismiss = onDismiss,
    )
}

/** The size of a file on disk, or zero if it has gone. */
private fun sizeOf(path: String): Long =
    runCatching { appFileSystem.metadata(path.toPath()).size ?: 0L }.getOrDefault(0L)

/**
 * A machine, booted for as long as this is on screen.
 *
 * Boots on the way in and stops on the way out, so going back to the list stops
 * the CPU rather than leaving it running behind the list.
 */
@OptIn(ExperimentalForeignApi::class, DelicateCoroutinesApi::class)
@Composable
private fun RunningMachine(configurationId: Int, capture: KeyCapture, onBack: () -> Unit) {
    val source = remember(configurationId) { IosEmulatorScreenSource() }
    // Held so leaving can write to it. The session is put away on the way out
    // rather than in onDispose, because the list reloads the moment the back
    // stack pops -- writing afterwards means it reads the previous screenshot.
    var emulatorState by remember(configurationId) { mutableStateOf<EmulatorState?>(null) }
    // The machine's own phosphor, which until now was a constant: the color
    // was being stored and edited and then quietly ignored at the point it
    // mattered.
    val characterColor = remember(configurationId) {
        val argb = ConfigurationManager.get().getConfigById(configurationId)
            ?.characterColorAsRGB
            ?: ScreenColors.GREEN
        Color(argb)
    }

    // The machine takes the hardware keyboard for as long as it is on screen.
    DisposableEffect(capture) {
        capture.enabled = true
        onDispose { capture.enabled = false }
    }

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
                // Not filtered: the core takes one path per drive, and dropping
                // the empty ones would shift every later disk down a drive.
                diskPaths = configuration.diskPaths,
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
                source.snapshot(characterColor, SCREEN_COLOR)
                    ?.let(::encodePng)
                    ?.let(state::writeScreenshot)
            }
            onBack()
        },
        keyboard = { Keyboard(keyboard) },
    ) {
        EmulatorScreen(
            source = source,
            characterColor = characterColor,
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
private fun installIfNeeded(diskPath: String?) {
    val settings = appSettings()
    val creator = FileManager.Creator(appDataDirectory() / TRS80_DIRECTORY)
    val manager = ConfigurationManager.init(creator, settings)
    RomManager.init(creator, settings)

    if (manager.configCount > 0) {
        Log.i(TAG, "${manager.configCount} configuration(s) already installed.")
        return
    }

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
