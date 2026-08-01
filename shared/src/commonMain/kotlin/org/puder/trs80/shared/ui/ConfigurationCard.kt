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

import androidx.compose.ui.graphics.ImageBitmap
import org.puder.trs80.shared.KeyboardLayout
import org.puder.trs80.shared.MODEL1
import org.puder.trs80.shared.MODEL3
import org.puder.trs80.shared.MODEL4
import org.puder.trs80.shared.MODEL4P
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.storage.StorageKeys

/** Shown where a configuration has no value for something. */
private const val NOT_SET = "---"

/**
 * One configuration, as the list draws it.
 *
 * A plain value computed from the domain, so the composable that draws it has
 * no opinions to test and this has no drawing to test. It is also what makes the
 * list previewable without an emulator, a store or a disk.
 */
data class ConfigurationCard(
    val id: Int,
    val name: String,
    val model: String,
    val diskCount: Int,
    val cassetteRewound: Boolean,
    val soundMuted: Boolean,
    val keyboardPortrait: String,
    val keyboardLandscape: String,
    /** Whether there is a session to resume, which is what offers Stop. */
    val hasSavedState: Boolean,
    /** Whether there is a TRS-Xray dump, which is what offers Share. */
    val hasXrayState: Boolean,
    val screenshot: ImageBitmap?,
    /** Whether the user made or edited this one; drives the CUSTOM mark. */
    val isCustom: Boolean = false,
    /** When it was last run, for ordering the library. */
    val lastUsed: Long = 0L,
)

/**
 * Reads every configuration into a card.
 *
 * Includes the screenshot, which means file reads and PNG decoding — do this off
 * the main thread. The Android app used an `AsyncTask` per card for exactly this
 * reason.
 */
fun ConfigurationManager.toCards(): List<ConfigurationCard> =
    (0 until configCount).map { position ->
        val configuration = getConfig(position)
        val state = runCatching { getEmulatorState(configuration.id) }.getOrNull()
        ConfigurationCard(
            id = configuration.id,
            name = configuration.name.orEmpty().ifEmpty { NOT_SET },
            model = modelLabel(configuration.model),
            diskCount = (0 until StorageKeys.DRIVE_COUNT)
                .count { !configuration.getDiskPath(it).isNullOrEmpty() },
            cassetteRewound = configuration.cassettePosition <= 0f,
            soundMuted = configuration.isSoundMuted,
            keyboardPortrait = keyboardLabel(configuration.keyboardLayoutPortrait),
            keyboardLandscape = keyboardLabel(configuration.keyboardLayoutLandscape),
            hasSavedState = state?.hasState() == true,
            hasXrayState = state?.hasXrayState() == true,
            screenshot = state?.readScreenshot()?.let(::decodeImage),
            isCustom = configuration.isCustom,
            lastUsed = configuration.lastUsed,
        )
    }

internal fun modelLabel(model: Int): String = when (model) {
    MODEL1 -> "Model I"
    MODEL3 -> "Model III"
    MODEL4 -> "Model 4"
    MODEL4P -> "Model 4P"
    else -> NOT_SET
}

internal fun keyboardLabel(layout: KeyboardLayout?): String = when (layout) {
    KeyboardLayout.KEYBOARD_LAYOUT_ORIGINAL -> "Orig"
    KeyboardLayout.KEYBOARD_LAYOUT_COMPACT -> "Comp"
    KeyboardLayout.KEYBOARD_LAYOUT_JOYSTICK -> "Joy"
    KeyboardLayout.KEYBOARD_GAME_CONTROLLER -> "Ctrl"
    KeyboardLayout.KEYBOARD_TILT -> "Tilt"
    KeyboardLayout.KEYBOARD_EXTERNAL -> "Ext"
    null -> NOT_SET
}
