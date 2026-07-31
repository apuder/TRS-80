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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * What the list can be asked to do. One per button on a card, plus adding.
 *
 * Passed in rather than performed here, because every one of them either
 * navigates or changes the domain, and this draws.
 *
 * Null means the host cannot do it, and the button is not drawn at all. That is
 * for the sake of the port: the screens arrive one at a time, and a button that
 * is visible but does nothing is worse than one that is not there yet.
 */
data class ConfigurationListActions(
    val onRun: ((Int) -> Unit)? = null,
    val onEdit: ((Int) -> Unit)? = null,
    val onDelete: ((Int) -> Unit)? = null,
    val onStop: ((Int) -> Unit)? = null,
    val onShare: ((Int) -> Unit)? = null,
    val onAdd: (() -> Unit)? = null,
    val onOpenStore: (() -> Unit)? = null,
)

/**
 * The list of configurations: the app's home.
 *
 * A port of the RecyclerView of flip cards, and faithful to it in what a card
 * holds and how it behaves — a card shows its name and last screenshot, and
 * tapping it turns it over to reveal the details and the actions.
 *
 * What is deliberately *not* carried across is the flip itself. It was a 3D
 * rotation built on Android view animation, with no multiplatform equivalent
 * and no future: the UI spec replaces the flip card outright. Turning the card
 * over is a crossfade here, which keeps the behaviour that exists — two faces,
 * one tap between them — without inventing the design that replaces it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationListScreen(
    cards: List<ConfigurationCard>,
    actions: ConfigurationListActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("TRS-80") },
                actions = {
                    actions.onOpenStore?.let { open ->
                        TextButton(onClick = open) { Text("Store") }
                    }
                },
            )
        },
        floatingActionButton = {
            actions.onAdd?.let { add ->
                ExtendedFloatingActionButton(
                    onClick = add,
                    content = { Text("New configuration") },
                )
            }
        },
    ) { insets ->
        if (cards.isEmpty()) {
            EmptyList(Modifier.padding(insets))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(insets).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(cards, key = { it.id }) { card ->
                ConfigurationCardView(card, actions)
            }
        }
    }
}

@Composable
private fun EmptyList(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "No configurations yet.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** One card, showing either its face or its back. */
@Composable
private fun ConfigurationCardView(
    card: ConfigurationCard,
    actions: ConfigurationListActions,
) {
    var showingBack by remember(card.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showingBack = !showingBack },
    ) {
        Crossfade(targetState = showingBack, label = "card") { back ->
            if (back) {
                CardBack(card, actions)
            } else {
                CardFront(card)
            }
        }
    }
}

@Composable
private fun CardFront(card: ConfigurationCard) {
    Column(Modifier.padding(16.dp)) {
        Text(
            card.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                // The emulated screen is 64x16 cells of 1:3, so 4:3 overall.
                .aspectRatio(4f / 3f)
                .background(Color(0xFF444444)),
            contentAlignment = Alignment.Center,
        ) {
            val screenshot = card.screenshot
            if (screenshot != null) {
                Image(
                    bitmap = screenshot,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    // Pixels, not photographs.
                    filterQuality = FilterQuality.None,
                )
            } else {
                Text("Not run yet", color = Color(0xFF9E9E9E))
            }
        }
    }
}

@Composable
private fun CardBack(card: ConfigurationCard, actions: ConfigurationListActions) {
    Column(Modifier.padding(16.dp)) {
        Text(
            card.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(Modifier.padding(top = 12.dp)) {
            Detail("Model", card.model)
            Detail("Disks", card.diskCount.toString())
            Detail("Cassette", if (card.cassetteRewound) "Rewound" else "Not rewound")
            Detail("Sound", if (card.soundMuted) "Disabled" else "Enabled")
            Detail("Keyboard", "${card.keyboardPortrait} / ${card.keyboardLandscape}")
        }
        Row(
            Modifier.padding(top = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            actions.onRun?.let { CardButton("Run", card.id, it) }
            actions.onEdit?.let { CardButton("Edit", card.id, it) }
            if (card.hasSavedState) {
                actions.onStop?.let { CardButton("Stop", card.id, it) }
            }
            if (card.hasXrayState) {
                actions.onShare?.let { CardButton("Share", card.id, it) }
            }
            actions.onDelete?.let { CardButton("Delete", card.id, it) }
        }
    }
}

@Composable
private fun CardButton(label: String, id: Int, action: (Int) -> Unit) {
    TextButton(onClick = { action(id) }) { Text(label) }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
