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

package org.puder.trs80.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.retrostore.client.common.proto.App

/** What the store screens are showing, so they can say so rather than sitting blank. */
sealed interface StoreState {
    data object Loading : StoreState
    data class Failed(val message: String) : StoreState
    data class Loaded(val apps: List<App>) : StoreState
}

/**
 * The RetroStore catalogue.
 *
 * A port of `RetrostoreActivity`'s list: cover, title, description, author. The
 * covers come over the network, so they arrive after the rows do — which is why
 * a row draws its own placeholder rather than waiting for the image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroStoreScreen(
    state: StoreState,
    onOpen: (App) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("RetroStore") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { insets ->
        when (state) {
            is StoreState.Loading -> Centred(Modifier.padding(insets)) {
                CircularProgressIndicator()
            }

            is StoreState.Failed -> Centred(Modifier.padding(insets)) {
                Text("Could not reach the store.\n${state.message}")
            }

            is StoreState.Loaded -> LazyColumn(
                modifier = Modifier.padding(insets).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.apps, key = { it.id }) { app ->
                    StoreRow(app) { onOpen(app) }
                }
            }
        }
    }
}

@Composable
private fun StoreRow(app: App, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RemoteImage(
            url = app.screenshot_url.firstOrNull(),
            modifier = Modifier.size(96.dp).background(Color(0xFF222222)),
        )
        Column(Modifier.weight(1f)) {
            Text(app.name, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(
                app.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(app.author, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

/**
 * One app, with everything the store knows about it and a way to install it.
 *
 * A port of `AppDetailsPageActivity`. Installing is the only thing here that
 * changes anything, and it is deliberately one button that reports what
 * happened rather than a flow: it downloads disks and makes a configuration,
 * which either works or does not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroStoreAppScreen(
    app: App?,
    installing: Boolean,
    installed: Boolean,
    onInstall: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(app?.name.orEmpty()) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { insets ->
        if (app == null) {
            Centred(Modifier.padding(insets)) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(insets)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RemoteImage(
                url = app.screenshot_url.firstOrNull(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(Color(0xFF222222)),
            )
            Text(app.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                listOfNotNull(
                    app.author.takeIf { it.isNotEmpty() },
                    app.release_year.takeIf { it > 0 }?.toString(),
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(app.description, style = MaterialTheme.typography.bodyMedium)

            Button(
                onClick = onInstall,
                enabled = !installing && !installed,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(
                    when {
                        installed -> "Installed"
                        installing -> "Installing…"
                        else -> "Install"
                    }
                )
            }
        }
    }
}

@Composable
private fun Centred(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
