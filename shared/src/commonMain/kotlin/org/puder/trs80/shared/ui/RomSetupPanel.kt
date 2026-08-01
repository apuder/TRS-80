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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.and_joiner
import trs_80.shared.generated.resources.getting_roms
import trs_80.shared.generated.resources.getting_roms_why
import trs_80.shared.generated.resources.not_now
import trs_80.shared.generated.resources.roms_failed
import trs_80.shared.generated.resources.roms_failed_detail
import trs_80.shared.generated.resources.try_again
import org.puder.trs80.shared.ui.theme.ModalPanel
import org.puder.trs80.shared.ui.theme.ProgressRing
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.TextAction
import org.puder.trs80.shared.ui.theme.Trs80Theme

/**
 * What the app is doing before it can run anything.
 *
 * Shown on first run while the ROM images come down. Without them no machine
 * will start, so this is not a background errand the user can be left to
 * discover by tapping a machine and getting a black screen — which is what
 * happened before this existed.
 *
 * @param missing the models still without a ROM once the attempt has finished;
 * empty while it is still going.
 */
@Composable
fun RomSetupPanel(
    downloading: Boolean,
    missing: List<RomStatus>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Trs80Theme.colors
    // Insistent while it is working -- there is nothing useful to do behind it
    // -- and dismissible once it has failed, so a user who would rather supply
    // their own file can get to the settings screen.
    ModalPanel(onDismiss = if (downloading) null else onDismiss) {
        if (downloading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(progress = null, size = 20.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(Res.string.getting_roms), style = Trs80Theme.type.title)
            }
            Spacer(Modifier.padding(top = 8.dp))
            Text(
                stringResource(Res.string.getting_roms_why),
                style = Trs80Theme.type.bodySmall,
                color = colors.muted,
            )
            return@ModalPanel
        }

        Text(stringResource(Res.string.roms_failed), style = Trs80Theme.type.title)
        Spacer(Modifier.padding(top = 8.dp))
        Text(
            stringResource(
                Res.string.roms_failed_detail,
                missing.joinToString(stringResource(Res.string.and_joiner)) { it.label },
            ),
            style = Trs80Theme.type.bodySmall,
            color = colors.muted,
        )
        Spacer(Modifier.padding(top = 18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextAction(stringResource(Res.string.not_now), onClick = onDismiss, color = colors.muted, padding = 0.dp)
            Spacer(Modifier.weight(1f))
            TextAction(stringResource(Res.string.try_again), onClick = onRetry, padding = 0.dp)
        }
    }
}
