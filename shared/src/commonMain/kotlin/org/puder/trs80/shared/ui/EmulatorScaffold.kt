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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The chrome around a running machine: a way out, and a name.
 *
 * The Android app has this as an action bar and has always needed it, since a
 * machine fills the screen and there is otherwise nothing to press. iOS needs it
 * more: there is no system Back at all, so without this the emulator is a place
 * the app can go and never leave.
 *
 * Deliberately plain. What belongs here — the reset and sound controls, the
 * keyboards, and what any of it does in landscape, where the picture leaves no
 * room — is the redesign's business (§9), and inventing it now would be
 * inventing the thing the designer is meant to decide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    screen: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    // A labelled button rather than a chevron: the icon pack is
                    // another dependency, and this is chrome the redesign
                    // replaces anyway.
                    TextButton(onClick = onBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { insets ->
        Box(Modifier.padding(insets).fillMaxSize()) {
            screen()
        }
    }
}
