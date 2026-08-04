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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.puder.trs80.shared.ScreenColors
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.configuration.EmulatorState
import org.puder.trs80.shared.configuration.systemState
import org.puder.trs80.shared.configuration.toDraft
import org.puder.trs80.shared.io.COMMUNITY_URL
import org.puder.trs80.shared.io.GamepadInput
import org.puder.trs80.shared.io.MotionInput
import org.puder.trs80.shared.io.appFileSystem
import org.puder.trs80.shared.io.clipboardText
import org.puder.trs80.shared.io.openUrl
import org.puder.trs80.shared.io.pickFile
import org.puder.trs80.shared.io.shareText
import org.puder.trs80.shared.io.shareUrl
import org.puder.trs80.shared.io.storeListingUrl
import org.puder.trs80.shared.localstore.RomManager
import org.puder.trs80.shared.navigation.Destination
import org.puder.trs80.shared.navigation.Navigator
import org.puder.trs80.shared.navigation.Trs80App
import org.puder.trs80.shared.navigation.rememberNavigator
import org.puder.trs80.shared.storage.ExperimentalFeatures
import org.puder.trs80.shared.storage.TapRun
import org.puder.trs80.shared.storage.appSettings
import org.puder.trs80.shared.store.AppInstaller
import org.puder.trs80.shared.store.modelOf
import org.puder.trs80.shared.store.retroStore
import org.puder.trs80.shared.ui.Catalog
import org.puder.trs80.shared.ui.CatalogEntry
import org.puder.trs80.shared.ui.ConfigurationCard
import org.puder.trs80.shared.ui.DetailAction
import org.puder.trs80.shared.ui.DetailContent
import org.puder.trs80.shared.ui.DetailSheet
import org.puder.trs80.shared.ui.DirectionKeys
import org.puder.trs80.shared.ui.DiskCreation
import org.puder.trs80.shared.ui.DiskImageSpec
import org.puder.trs80.shared.ui.EditConfigurationActions
import org.puder.trs80.shared.ui.EditConfigurationScreen
import org.puder.trs80.shared.ui.EmulatorScaffold
import org.puder.trs80.shared.ui.EntryDetail
import org.puder.trs80.shared.ui.FirstRunPane
import org.puder.trs80.shared.ui.KEY_FIRE
import org.puder.trs80.shared.ui.KeySender
import org.puder.trs80.shared.ui.Keyboard
import org.puder.trs80.shared.ui.KeyboardState
import org.puder.trs80.shared.ui.LibraryActions
import org.puder.trs80.shared.ui.LibraryScreen
import org.puder.trs80.shared.ui.LibrarySort
import org.puder.trs80.shared.ui.MachineActions
import org.puder.trs80.shared.ui.MachineKeyboard
import org.puder.trs80.shared.ui.ORIGINAL_KEYBOARD
import org.puder.trs80.shared.ui.PaneContent
import org.puder.trs80.shared.ui.ResumePane
import org.puder.trs80.shared.ui.RomSetupPanel
import org.puder.trs80.shared.ui.Roms
import org.puder.trs80.shared.ui.ScreensViewer
import org.puder.trs80.shared.ui.SettingsScreen
import org.puder.trs80.shared.ui.StoreState
import org.puder.trs80.shared.ui.TUTORIAL_APP_ID
import org.puder.trs80.shared.ui.TutorialPanel
import org.puder.trs80.shared.ui.tutorialSteps
import org.puder.trs80.shared.ui.tutorialReadyWait
import org.puder.trs80.shared.ui.tutorialSettle
import org.puder.trs80.shared.ui.typeCommand
import kotlinx.coroutines.delay
import org.puder.trs80.shared.ui.awaitPrompt
import org.puder.trs80.shared.ui.awaitReady
import org.puder.trs80.shared.ui.DOS_PROMPT
import org.puder.trs80.shared.ui.ENTER_KEY
import org.puder.trs80.shared.ui.BASIC_PROMPT
import org.puder.trs80.shared.ui.asWritten
import org.puder.trs80.shared.ui.Toast
import org.puder.trs80.shared.ui.asCatalog
import org.puder.trs80.shared.ui.encodePng
import org.puder.trs80.shared.ui.isLandscape
import org.puder.trs80.shared.ui.isWideLayout
import org.puder.trs80.shared.ui.keyboardFor
import org.puder.trs80.shared.ui.matching
import org.puder.trs80.shared.ui.matchingEntries
import org.puder.trs80.shared.ui.mediaSummary
import org.puder.trs80.shared.ui.modelLabel
import org.puder.trs80.shared.ui.paneContentFor
import org.puder.trs80.shared.ui.sortedFor
import org.puder.trs80.shared.ui.theme.ThemePreference
import org.puder.trs80.shared.ui.theme.Trs80Theme
import org.puder.trs80.shared.ui.toCards
import org.retrostore.client.common.proto.App
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.disk_many
import trs_80.shared.generated.resources.disk_one
import trs_80.shared.generated.resources.experimental_unlocked
import trs_80.shared.generated.resources.share_app_message
import trs_80.shared.generated.resources.share_failed
import trs_80.shared.generated.resources.share_no_state
import trs_80.shared.generated.resources.share_token
import trs_80.shared.generated.resources.sharing_state
import trs_80.shared.generated.resources.untitled

private const val TAG = "Trs80App"

/** The glass behind the phosphor. The machine had one; it is not a choice. */
private val SCREEN_COLOR = Color(0xFF444444)

/**
 * Something that takes the hardware keyboard while a machine is on screen.
 *
 * Neither platform can capture keys from inside Compose. iOS sees them in
 * UIKit, above the Compose view; Android sees them in `dispatchKeyEvent`, above
 * the Compose view. So the host owns the capture and this is the switch the
 * emulator screen turns on while it is there and off on its way out.
 */
interface HardwareKeys {
    var enabled: Boolean
}

/**
 * The app: the library, a machine when one is running, the editor and settings.
 *
 * All of it, for both platforms. A host supplies three things -- somewhere to
 * draw, an [EmulatorCore] and, if it can capture them, the hardware keys -- and
 * everything else from here down is the same code on iOS and Android. That is
 * the point of the exercise: two apps that are the same app.
 *
 * The core is a parameter rather than something this reaches for because
 * Android's implementation lives in the app module, where the JNI entry points
 * are; nothing shared can name it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Trs80AppUi(core: EmulatorCore, hardwareKeys: HardwareKeys? = null) {
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
    val experimental = remember { ExperimentalFeatures(appSettings()) }
    // Read into state so turning one on repaints what offers it, rather than
    // waiting for whatever happens to recompose next.
    var unlocked by remember { mutableStateOf(experimental.isUnlocked) }
    var shareEnabled by remember { mutableStateOf(experimental.isShareEnabled) }
    val taps = remember { TapRun() }
    var message by remember { mutableStateOf<String?>(null) }
    val unlockedMessage = stringResource(Res.string.experimental_unlocked)
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
                library = {
                    Library(
                        navigator = navigator,
                        catalog = catalog,
                        shareEnabled = shareEnabled,
                        onMessage = { message = it },
                    )
                },
                emulator = {
                    // What the screen's own back control does. Registered per
                    // destination rather than once around the app, because
                    // leaving is not the same act on every screen -- see the
                    // editor, which has something to undo on the way out.
                    BackHandler { navigator.goBack() }
                    RunningMachine(
                        core = core,
                        configurationId = it.configurationId,
                        hardwareKeys = hardwareKeys,
                        onBack = { navigator.goBack() },
                    )
                },
                settings = {
                    BackHandler { navigator.goBack() }
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
                        experimentalUnlocked = unlocked,
                        shareEnabled = shareEnabled,
                        onShareEnabledChange = {
                            experimental.setShareEnabled(it)
                            shareEnabled = experimental.isShareEnabled
                        },
                        onCommunity = { openUrl(COMMUNITY_URL) },
                        // Absent while there is no listing: see storeListingUrl.
                        onRate = storeListingUrl?.let { url -> { openUrl(url) } },
                        onShareApp = {
                            scope.launch {
                                shareText(getString(Res.string.share_app_message, shareUrl))
                            }
                        },
                        onVersionTap = {
                            // Only the tap that gets there says so. Carrying on
                            // past ten, or coming back to a section already
                            // open, announces nothing.
                            if (taps.tap(currentTimeMillis()) && !unlocked) {
                                experimental.unlock()
                                unlocked = true
                                message = unlockedMessage
                            }
                        },
                    )
                },
                editConfiguration = { Editor(core, it.configurationId, it.isNew, navigator) },
            )

            // Over whatever is on screen: nothing the app offers works until
            // these are here.
            val settingUp = roms.busy || (roms.attempted && roms.missing.isNotEmpty())
            Toast(message, onDismissed = { message = null })

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
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Editor(
    core: EmulatorCore,
    configurationId: Int,
    isNew: Boolean,
    navigator: Navigator,
) {
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

    /**
     * Leaves without saving, deleting a machine that only exists because the
     * editor was opened to make one.
     *
     * Both the screen's back control and the system's back go through here. If
     * the system's went straight to the navigator instead, backing out of a new
     * machine would leave it in the library -- unnamed, unconfigured, and made
     * by a press that meant "never mind".
     */
    fun leave() {
        if (isNew) {
            manager.deleteConfigWithId(configurationId)
        }
        navigator.goBack()
    }
    BackHandler { leave() }

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
            onBack = { leave() },
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
            onCreateDisk = { spec -> createBlankDisk(core, manager, configurationId, spec) },
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
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Library(
    navigator: Navigator,
    catalog: Catalog,
    shareEnabled: Boolean,
    onMessage: (String) -> Unit,
) {
    var cards by remember { mutableStateOf(emptyList<ConfigurationCard>()) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(LibrarySort.LastUsed) }
    var expanded by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(emptySet<String>()) }
    var failed by remember { mutableStateOf(emptySet<String>()) }
    // The id, not the entry: an entry is a snapshot of what the catalog looked
    // like when it was tapped, so holding one leaves the sheet offering to
    // download something that has since arrived.
    var selectedId by remember { mutableStateOf<String?>(null) }
    // Which of the selected entry's screens is open, if any. Held here rather
    // than in the pane, because the picture wants the window and the pane is
    // half of it.
    var viewingScreen by remember(selectedId) { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    // Back takes off one layer at a time -- the picture, then the entry -- and
    // the library itself is the bottom, where back leaves the app because there
    // is nothing under it. Two layers rather than one: a screen is opened from
    // an entry, so closing the entry as well would drop the user two steps back
    // from one press.
    BackHandler(enabled = viewingScreen != null || selectedId != null) {
        if (viewingScreen != null) {
            viewingScreen = null
        } else {
            selectedId = null
        }
    }

    suspend fun reload() {
        cards = withContext(Dispatchers.Default) { ConfigurationManager.get().toCards() }
    }

    /** Fetches [app] and, if [thenRun], starts the machine it becomes. */
    fun install(app: App, thenRun: Boolean) {
        installing = installing + app.id
        failed = failed - app.id
        scope.launch {
            val configuration = withContext(Dispatchers.Default) {
                runCatching { AppInstaller(ConfigurationManager.get()).install(app) }
                    .onFailure { Log.e(TAG, "Could not install ${app.name}.", it) }
                    .getOrNull()
            }
            installing = installing - app.id
            if (configuration == null) {
                // Said rather than swallowed: a Play that quietly does nothing
                // cannot be told from a tap that missed.
                failed = failed + app.id
                return@launch
            }
            reload()
            if (thenRun) {
                selectedId = null
                navigator.goTo(Destination.Emulator(configuration.id))
            }
        }
    }

    /**
     * Plays a catalog entry, fetching it first if this is the first time.
     *
     * Always the entry's clean machine, never one the user has since changed:
     * those are listed separately and started by name, so that "play this
     * program" keeps meaning the program as the catalog has it.
     */
    fun playEntry(entry: CatalogEntry) {
        if (entry.installing) {
            return
        }
        val clean = entry.cleanId
        if (clean != null) {
            selectedId = null
            navigator.goTo(Destination.Emulator(clean))
            return
        }
        (catalog.state as? StoreState.Loaded)?.apps?.firstOrNull { it.id == entry.id }
            ?.let { install(it, thenRun = true) }
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

    // Machines installed before the app recorded where they came from have to be
    // matched on their name once, or they would each show up as a program the
    // user has never played. Only unambiguous names: a name two catalog programs
    // share is left alone rather than guessed at.
    LaunchedEffect(listed) {
        if (listed.isEmpty()) {
            return@LaunchedEffect
        }
        val byName = listed.groupBy { it.name.trim().lowercase() }
            .filterValues { it.size == 1 }
            .mapValues { (_, apps) -> apps.single().id }
        val adopted = withContext(Dispatchers.Default) {
            ConfigurationManager.get().adoptStoreIds(byName)
        }
        if (adopted) {
            reload()
        }
    }

    val entries = listed.asCatalog(cards, installing)
        .map { it.copy(failed = it.id in failed) }

    val selectedEntry = selectedId?.let { id -> entries.firstOrNull { it.id == id } }
    val selectedApp = listed.firstOrNull { it.id == selectedEntry?.id }
    // Wide enough to hold the entry beside the list rather than over it. Asked
    // once here so the sheet and the pane can never both be on screen.
    val wide = isWideLayout()
    val sorted = cards.matching(query).sortedFor(sort)

    Box(Modifier.fillMaxSize()) {
    LibraryScreen(
        yours = sorted,
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
            onOpenEntry = { selectedId = it.id },
            onOpenSettings = { navigator.goTo(Destination.Settings) },
            onRefresh = { scope.launch { catalog.refresh() } },
            onEdit = { navigator.goTo(Destination.EditConfiguration(it, isNew = false)) },
            onDuplicate = { id ->
                scope.launch {
                    withContext(Dispatchers.Default) { ConfigurationManager.get().duplicate(id) }
                    reload()
                }
            },
            onDelete = { id ->
                scope.launch {
                    withContext(Dispatchers.Default) {
                        ConfigurationManager.get().deleteConfigWithId(id)
                    }
                    reload()
                }
            },
            onAdd = {
                val fresh = ConfigurationManager.get().newConfiguration()
                navigator.goTo(Destination.EditConfiguration(fresh.id, isNew = true))
            },
            onPlayEntry = ::playEntry,
            onStop = { id ->
                scope.launch {
                    withContext(Dispatchers.Default) { stopMachine(id) }
                    reload()
                }
            },
            // Offered only while the flag is on, so the row is absent rather
            // than present-and-refusing for everyone who has not asked for it.
            onShare = if (!shareEnabled) null else { id -> shareState(id, scope, onMessage) },
        ),
        selectedId = selectedId.takeIf { wide },
        pane = if (!wide) {
            null
        } else {
            {
                when (val holding = paneContentFor(selectedId, sorted)) {
                    is PaneContent.Entry ->
                        if (selectedEntry != null && selectedApp != null) {
                            Detail(
                                entry = selectedEntry,
                                app = selectedApp,
                                onPlay = { playEntry(selectedEntry) },
                                onRun = {
                                    selectedId = null
                                    navigator.goTo(Destination.Emulator(it))
                                },
                                onDismiss = { selectedId = null },
                                asPane = true,
                                onOpenScreen = { viewingScreen = it },
                            )
                        } else {
                            // The catalog was reloaded out from under the
                            // selection; the pane still has to hold something.
                            FirstRunPane(entries.size)
                        }

                    is PaneContent.Resume -> ResumePane(holding.card) {
                        navigator.goTo(Destination.Emulator(it))
                    }

                    PaneContent.FirstRun -> FirstRunPane(entries.size)
                }
            }
        },
    )

        if (!wide && selectedEntry != null && selectedApp != null) {
            Detail(
                entry = selectedEntry,
                app = selectedApp,
                onPlay = { playEntry(selectedEntry) },
                onRun = { selectedId = null; navigator.goTo(Destination.Emulator(it)) },
                onDismiss = { selectedId = null },
            )
        }

        // Over the list and the pane both. On a phone the sheet draws its own,
        // because there a sheet already is the window.
        val screens = selectedApp?.screenshot_url.orEmpty()
        viewingScreen?.takeIf { screens.isNotEmpty() }?.let { start ->
            ScreensViewer(
                urls = screens,
                startIndex = start,
                onDismiss = { viewingScreen = null },
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
    onPlay: () -> Unit,
    onRun: (Int) -> Unit,
    onDismiss: () -> Unit,
    asPane: Boolean = false,
    /** Opens the screens viewer, which only the window is big enough to draw. */
    onOpenScreen: (Int) -> Unit = {},
) {
    // The clean machine describes the program; failing that, the version the
    // user reached for most recently. Either is a copy of the same media.
    val describes = entry.cleanId ?: entry.versions.firstOrNull()?.id
    val disks = remember(describes) {
        describes?.let { id ->
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

    val detail = DetailContent(
        title = app.name,
        author = app.author,
        year = app.release_year,
        coverUrl = app.screenshot_url.firstOrNull(),
        screenshotUrls = app.screenshot_url,
        description = app.description,
        machine = modelLabel(modelOf(app.ext_trs80?.model)),
        media = media,
        source = "RetroStore",
    )
    val action = when {
        entry.installing -> DetailAction.Downloading
        entry.failed -> DetailAction.Failed
        else -> DetailAction.Play
    }

    if (!asPane) {
        DetailSheet(
            content = detail,
            action = action,
            onPrimary = onPlay,
            onDismiss = onDismiss,
            versions = entry.versions,
            onPlayVersion = onRun,
        )
        return
    }

    // The same entry, standing in a pane. No scrim, no sheet to dismiss: the
    // list is beside it and stays live, which is the whole point of the layout.
    //
    // The viewer is not drawn here, unlike in the sheet. A sheet already covers
    // the window, so one opened inside it covers the window too; a pane is half
    // of one, and a full-screen picture confined to half the screen with the
    // list still sitting beside it is not a viewer.
    EntryDetail(
        content = detail,
        action = action,
        onPrimary = onPlay,
        onOpenScreen = onOpenScreen,
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        twoColumn = true,
        versions = entry.versions,
        onPlayVersion = onRun,
    )
}

/** The size of a file on disk, or zero if it has gone. */
private fun sizeOf(path: String): Long =
    runCatching { appFileSystem.metadata(path.toPath()).size ?: 0L }.getOrDefault(0L)

/**
 * Writes a blank disk image into a machine's own folder.
 *
 * The folder rather than anywhere the user picks, for the same reason a chosen
 * disk is copied into it: a configuration has to point at something that stays,
 * and the machine's folder is what gets carried along when it is duplicated and
 * removed when it is deleted.
 *
 * Refuses to write over an existing file. The alternative is a machine quietly
 * losing a disk it was still using, and the drive it is in would not even be
 * this one.
 */
private fun createBlankDisk(
    core: EmulatorCore,
    manager: ConfigurationManager,
    configurationId: Int,
    spec: DiskImageSpec,
): DiskCreation {
    val filename = spec.filename ?: return DiskCreation.Failed
    if (manager.hasMedia(configurationId, filename)) {
        return DiskCreation.NameTaken
    }
    val path = manager.mediaPath(configurationId, filename) ?: return DiskCreation.Failed
    if (!core.createBlankDisk(path, spec)) {
        runCatching { appFileSystem.delete(path.toPath()) }
        return DiskCreation.Failed
    }
    return DiskCreation.Created(path)
}

/**
 * A machine, booted for as long as this is on screen.
 *
 * Boots on the way in and stops on the way out, so going back to the list stops
 * the CPU rather than leaving it running behind the list.
 */
@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun RunningMachine(
    core: EmulatorCore,
    configurationId: Int,
    hardwareKeys: HardwareKeys?,
    onBack: () -> Unit,
) {
    val source = remember(configurationId) { CoreScreenSource(core) }
    // Held so leaving can write to it. The session is put away on the way out
    // rather than in onDispose, because the list reloads the moment the back
    // stack pops -- writing afterwards means it reads the previous screenshot.
    var emulatorState by remember(configurationId) { mutableStateOf<EmulatorState?>(null) }
    // The machine's own phosphor, which until now was a constant: the color
    // was being stored and edited and then quietly ignored at the point it
    // mattered.
    // Muting here lasts for this session only; the configuration's own setting
    // is what it starts from and the editor is where it is changed for good.
    var soundMuted by remember(configurationId) {
        mutableStateOf(
            ConfigurationManager.get().getConfigById(configurationId)?.isSoundMuted == true
        )
    }
    // Which tutorial step is on screen, or null when there is no tour running.
    // Session-long state like the rest of this: leaving the machine ends it.
    var tutorialStep by remember(configurationId) { mutableStateOf<Int?>(null) }
    var typing by remember(configurationId) { mutableStateOf(false) }
    val steps = tutorialSteps()

    val characterColor = remember(configurationId) {
        val argb = ConfigurationManager.get().getConfigById(configurationId)
            ?.characterColorAsRGB
            ?: ScreenColors.GREEN
        Color(argb)
    }

    LaunchedEffect(typing, tutorialStep) {
        val at = tutorialStep
        if (!typing || at == null) {
            return@LaunchedEffect
        }
        val step = steps[at]
        // The machine's own typing, which is what the Paste control uses. No
        // key is pressed and held from out here, so none can be left down.
        val type: suspend (String) -> Unit = { text -> core.paste(text) }
        // Nothing is typed until the machine is sitting where this command
        // belongs. It may still be booting -- in which case it is asking for the
        // time, and that gets answered -- or printing what the last one did.
        //
        // The first command is the one that has to worry about where the machine
        // was left. Opening this machine resumes the session it was last in, and
        // that can be anywhere: in BASIC, in a game, halfway through a listing.
        // None of those become the DOS prompt by being waited at, so the tour
        // gives the machine a few seconds and then power-cycles it -- the same
        // thing the menu's own Tutorial entry does -- rather than standing there
        // for twenty-five seconds and quietly giving up.
        val screen = { core.screenBuffer }
        val answerBoot: suspend () -> Unit = { type(ENTER_KEY) }
        var ready = if (at == 0) {
            awaitReady(step.awaits, screen, answerBoot, timeoutMillis = tutorialReadyWait)
        } else {
            awaitReady(step.awaits, screen, answerBoot)
        }
        if (!ready && at == 0) {
            Log.i(TAG, "Restarting the machine: it was not where the tour begins.")
            core.reset()
            core.rewindCassette()
            ready = awaitReady(step.awaits, screen, answerBoot)
        }
        if (!ready) {
            Log.e(TAG, "The machine never reached ${step.awaits} for \"${step.asWritten()}\".")
            typing = false
            tutorialStep = null
            return@LaunchedEffect
        }
        // Nothing held over from the step before: a key the machine still
        // thinks is down turns the next command into gibberish, and it only
        // takes one to have been lost.
        core.releaseAllKeys()
        typeCommand(step.command, type)
        // The next panel arrives when the machine is idle again, which is what
        // "the command has finished" looks like from out here. Waiting a fixed
        // time instead would cover the output of a slow one and interrupt a
        // fast one.
        val next = (at + 1).takeIf { it < steps.size }
        val settled = awaitPrompt(
            next?.let { steps[it].awaits } ?: listOf(DOS_PROMPT, BASIC_PROMPT),
            { core.screenBuffer },
        )
        // A moment alone with what just happened. The panel is what the user
        // reads next and it covers the screen, so it waits until there has been
        // time to see the listing, or the reply, that the step was for.
        if (settled && next != null) {
            delay(tutorialSettle)
        }
        typing = false
        tutorialStep = if (settled) next else null
    }

    // The machine takes the hardware keyboard for as long as it is on screen.
    DisposableEffect(hardwareKeys) {
        hardwareKeys?.enabled = true
        onDispose { hardwareKeys?.enabled = false }
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
            core.boot(
                model = configuration.model,
                romPath = rom,
                // Not filtered: the core takes one path per drive, and dropping
                // the empty ones would shift every later disk down a drive.
                diskPaths = configuration.diskPaths,
            )
            // Pick the session up where it was left, if there is one.
            if (state?.hasState() == true) {
                core.loadState(state.stateFilePath)
            }
            // Nothing is being held down. A saved session remembers which keys
            // were, so one put away mid-keystroke comes back with that key still
            // pressed and the machine repeating it -- which no amount of tapping
            // from the outside will stop, because the machine is busy.
            core.releaseAllKeys()
        }
        // A thread of its very own, not Dispatchers.Default. trs80_run() does not
        // return until the machine is stopped, so on a shared pool it permanently
        // occupies one of a handful of threads -- on Darwin that pool is a global
        // dispatch queue, and taking a worker out of it for the life of the app
        // breaks things far away from here.
        val cpu = newSingleThreadContext("trs80-cpu")
        CoroutineScope(cpu).launch { core.run() }
        emulatorState = state
        onDispose {
            core.stop()
            cpu.close()
        }
    }

    // What the machine's sound actually does. The user's setting, except while
    // the tour is running: it types a CSAVE, and a cassette write is a minute of
    // square wave straight out of the speaker -- nobody chose to hear that by
    // starting a tutorial. The setting is left alone rather than switched, so
    // the machine goes back to whatever it was on when the last step is done.
    //
    // This is also where the configuration's own mute first reaches the core,
    // which used to wait for somebody to touch the control.
    val touring = tutorialStep != null
    LaunchedEffect(soundMuted, touring) {
        core.setSoundMuted(soundMuted || touring)
    }

    val configuration = remember(configurationId) {
        ConfigurationManager.get().getConfigById(configurationId)
    }
    // Started rather than offered: this machine is a tutorial, and somebody who
    // opened it came for the tour. Cancelling leaves them at the DOS prompt with
    // the keyboard, and the panel's own entry starts it again.
    LaunchedEffect(configurationId, configuration) {
        if (configuration?.storeId == TUTORIAL_APP_ID && tutorialStep == null) {
            tutorialStep = 0
        }
    }

    val landscape = isLandscape()
    // Turning the phone can change which controls the machine offers: landscape
    // has a layout of its own and falls back to the portrait one when the user
    // has not chosen it, which is what the Android app does and what finally
    // gives the editor's landscape setting something to do.
    val layout = if (landscape) {
        configuration?.keyboardLayoutLandscape ?: configuration?.keyboardLayoutPortrait
    } else {
        configuration?.keyboardLayoutPortrait
    }
    // Falling back to the full keyboard: a machine with no way to type at it is
    // not much use.
    val definition = remember(layout) { keyboardFor(layout) ?: ORIGINAL_KEYBOARD }
    val keyboard = remember(definition) {
        KeyboardState(
            definition = definition,
            onKeyDown = { core.keyDown(it.sym, it.key) },
            onKeyUp = { core.keyUp(it.sym, it.key) },
        )
    }
    val sender = remember(configurationId) {
        KeySender(
            onKeyDown = { core.keyDown(it.sym, it.key) },
            onKeyUp = { core.keyUp(it.sym, it.key) },
        )
    }

    // The two layouts steered by something other than a finger on glass. Both
    // are stopped on the way out: an accelerometer left running costs battery
    // for a machine nobody is looking at.
    DisposableEffect(layout, sender) {
        val directions = DirectionKeys(sender)
        val motion = if (layout == KeyboardLayout.KEYBOARD_TILT) {
            MotionInput { directions.set(it.left, it.right, it.up, it.down) }.also { it.start() }
        } else {
            null
        }
        val gamepad = if (layout == KeyboardLayout.KEYBOARD_GAME_CONTROLLER) {
            GamepadInput(
                onDirection = { directions.set(it.left, it.right, it.up, it.down) },
                onFire = { if (it) sender.press(KEY_FIRE) else sender.release(KEY_FIRE) },
            ).also { it.start() }
        } else {
            null
        }
        onDispose {
            motion?.stop()
            gamepad?.stop()
            directions.releaseAll()
        }
    }

    EmulatorScaffold(
        title = configuration?.name?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.untitled),
        onBack = {
            // Stop, then write. The CPU thread tests a flag rather than being
            // interrupted, so this keeps the same small race between the last
            // instruction and the snapshot that the Android app has always had.
            core.stop()
            emulatorState?.let { state ->
                core.saveState(state.stateFilePath)
                source.snapshot(characterColor, SCREEN_COLOR)
                    ?.let(::encodePng)
                    ?.let(state::writeScreenshot)
            }
            onBack()
        },
        machine = MachineActions(
            onReset = {
                core.reset()
                if (configuration?.storeId == TUTORIAL_APP_ID) {
                    typing = false
                    tutorialStep = 0
                }
            },
            onRewindCassette = { core.rewindCassette() },
            onPaste = {
                // The machine ends a line with a carriage return, which is what
                // its own keyboard would have sent.
                clipboardText()?.replace('\n', '\r')?.also(core::paste) != null
            },
            soundMuted = soundMuted,
            // Just the setting; the effect above is what tells the core, so
            // that there is one place deciding whether the machine is audible.
            onSoundMutedChange = { soundMuted = it },
            onTutorial = if (configuration?.storeId != TUTORIAL_APP_ID) {
                null
            } else {
                {
                    // From the top, as the old app did: the tour's first command
                    // is a directory listing, and it reads the tape later on.
                    core.reset()
                    core.rewindCassette()
                    tutorialStep = 0
                    typing = false
                }
            },
        ),
        keyboard = if (tutorialStep != null) {
            {}
        } else {
            {
            MachineKeyboard(
                layout,
                keyboard,
                sender,
                // Sideways the keys lie on the picture, so they are drawn as
                // outlines and stand a little shorter -- height is what is
                // scarce, and the picture is what the height is for.
                overlay = landscape,
                keyHeight = if (landscape) 38.dp else 44.dp,
            )
            }
        },
    ) {
        EmulatorScreen(
            source = source,
            characterColor = characterColor,
            screenColor = SCREEN_COLOR,
        )
    }

    // Between steps only: while a command is being typed there is nothing to
    // read and everything to watch.
    tutorialStep?.takeIf { !typing }?.let { at ->
        TutorialPanel(
            step = steps[at],
            number = at + 1,
            total = steps.size,
            onNext = { typing = true },
            onCancel = { tutorialStep = null; typing = false },
        )
    }
}

/** @return the path of the ROM for [model], or null if there is none. */
private fun romPathFor(model: Int): String? = RomManager.get().romPath(model)

/**
 * Throws away a machine's paused session.
 *
 * What Android's Stop does, and the same two steps: the saved state goes, and
 * the tape is wound back. The machine, its disks and its settings stay — this
 * ends a session, it does not remove anything the user set up.
 */
private fun stopMachine(configurationId: Int) {
    val manager = ConfigurationManager.get()
    runCatching { manager.getEmulatorState(configurationId).deleteSavedState() }
        .onFailure { Log.e(TAG, "Could not clear the saved state.", it) }
    manager.getConfigById(configurationId)?.cassettePosition = 0f
}

/**
 * Uploads a machine's TRS-Xray state to the store and reports the token.
 *
 * Experimental, and reachable only once the user has turned it on. The token is
 * the whole result — it is what someone else types in to fetch the state — so
 * it is said rather than logged.
 */
private fun shareState(
    configurationId: Int,
    scope: CoroutineScope,
    onMessage: (String) -> Unit,
) {
    scope.launch {
        val state = withContext(Dispatchers.Default) {
            runCatching {
                ConfigurationManager.get().getEmulatorState(configurationId)
                    .systemState(ConfigurationManager.get().getConfigById(configurationId)?.model ?: 0)
            }.onFailure { Log.e(TAG, "Cannot read the machine's state.", it) }.getOrNull()
        }
        if (state == null) {
            onMessage(getString(Res.string.share_no_state))
            return@launch
        }
        onMessage(getString(Res.string.sharing_state))
        val token = runCatching { retroStore.uploadState(state) }
            .onFailure { Log.e(TAG, "Could not upload the state.", it) }
            .getOrNull()
        onMessage(
            if (token == null) {
                getString(Res.string.share_failed)
            } else {
                getString(Res.string.share_token, token.toString())
            }
        )
    }
}
