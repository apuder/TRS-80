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

package org.puder.trs80.shared.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * How the back stack is written down when the app is put away.
 *
 * Spelled out rather than derived, because Kotlin/Native has no reflection to
 * derive it from: on iOS a serializer that is not registered here is simply not
 * found, at runtime, when the app is being restored — which is the least
 * convenient moment. Adding a [Destination] means adding a line here.
 */
private val savedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Destination.Library::class, Destination.Library.serializer())
            subclass(Destination.Emulator::class, Destination.Emulator.serializer())
            subclass(Destination.EditConfiguration::class, Destination.EditConfiguration.serializer())
            subclass(Destination.Settings::class, Destination.Settings.serializer())
            subclass(Destination.RetroStoreApp::class, Destination.RetroStoreApp.serializer())
            subclass(Destination.CreateDisk::class, Destination.CreateDisk.serializer())
        }
    }
}

/**
 * A [Navigator] whose stack survives the app being put away and brought back.
 *
 * The stack itself comes from Navigation 3, which saves and restores it; the
 * [Navigator] around it is what gives it the app's own rules — one-shot results,
 * and ignoring a request to go where it already is.
 */
@Composable
fun rememberNavigator(root: Destination = Destination.Library): Navigator {
    val backStack = rememberNavBackStack(savedStateConfiguration, root)
    @Suppress("UNCHECKED_CAST")
    return remember(backStack) { Navigator(backStack as MutableList<Destination>) }
}

/**
 * The whole app: whatever the navigator's stack says should be on screen.
 *
 * The library and the emulator exist so far. The rest arrive one at a
 * time (§7.2) and each is a line here — which is the point of having done the
 * navigation first.
 */
@Composable
fun Trs80App(
    navigator: Navigator,
    library: @Composable () -> Unit,
    emulator: @Composable (Destination.Emulator) -> Unit,
    retroStoreApp: (@Composable (Destination.RetroStoreApp) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = navigator.backStack,
        onBack = { navigator.goBack() },
        modifier = modifier,
        entryProvider = entryProvider {
            entry<Destination.Library> { library() }
            entry<Destination.Emulator> { emulator(it) }
            // Every destination needs an entry or NavDisplay has nothing to draw
            // for it, so a host that cannot show one says so rather than the app
            // navigating into a blank screen.
            entry<Destination.RetroStoreApp> {
                retroStoreApp?.invoke(it) ?: Missing("RetroStore app")
            }
        },
    )
}

/** Stands in for a destination this host has no screen for yet. */
@Composable
private fun Missing(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name is not available here yet.")
    }
}
