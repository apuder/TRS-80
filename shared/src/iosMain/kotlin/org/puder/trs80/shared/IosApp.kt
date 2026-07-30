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
import platform.UIKit.UIViewController

/** Green on dark, as the emulated machine's phosphor and glass. */
private val CHARACTER_COLOR = Color(0xFF77FB4D)
private val SCREEN_COLOR = Color(0xFF444444)

/**
 * The iOS entry point: a view controller showing the emulated screen.
 *
 * This is the whole iOS host for now. It boots a machine, runs the CPU, and draws
 * what the core rasterizes — enough to show the emulator working on iOS, and not
 * yet an app. The configuration list, settings and input all belong to the
 * redesign.
 *
 * @param romPath a Model III ROM image, and [diskPath] a disk to boot from.
 */
@OptIn(ExperimentalForeignApi::class)
fun EmulatorViewController(romPath: String, diskPath: String?): UIViewController {
    val source = IosEmulatorScreenSource()

    EmulatorCore.boot(
        model = 3,
        romPath = romPath,
        diskPaths = listOfNotNull(diskPath),
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
