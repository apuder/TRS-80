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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.puder.trs80.shared.ui.theme.DestructiveButton
import org.puder.trs80.shared.ui.theme.ModalPanel
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.TextAction
import org.puder.trs80.shared.ui.theme.Trs80Theme
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.cancel
import trs_80.shared.generated.resources.delete
import trs_80.shared.generated.resources.delete_consequence
import trs_80.shared.generated.resources.delete_question
import trs_80.shared.generated.resources.this_entry

/**
 * Asks before deleting a machine.
 *
 * Drawn over whatever asked for it rather than in a platform dialog, so it is
 * the app's own type and palette and behaves the same on both platforms. The
 * scrim swallows taps, which is what stops the screen behind it being operated
 * blind.
 *
 * Shared by the editor and the library's overflow, because the same question
 * deserves the same words: what goes with the machine is what makes this worth
 * asking at all, and it should not be phrased two ways.
 */
@Composable
internal fun ConfirmDelete(name: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val colors = Trs80Theme.colors
    ModalPanel(onDismiss = onCancel) {
        Text(
            stringResource(
                Res.string.delete_question,
                name.ifEmpty { stringResource(Res.string.this_entry) },
            ),
            style = Trs80Theme.type.title,
        )
        Spacer(Modifier.padding(top = 8.dp))
        Text(
            stringResource(Res.string.delete_consequence),
            style = Trs80Theme.type.bodySmall,
            color = colors.muted,
        )
        Spacer(Modifier.padding(top = 18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextAction(
                stringResource(Res.string.cancel),
                onClick = onCancel,
                color = colors.muted,
                padding = 0.dp,
            )
            Spacer(Modifier.weight(1f))
            DestructiveButton(
                stringResource(Res.string.delete),
                onClick = onConfirm,
                filled = true,
                icon = null,
            )
        }
    }
}
