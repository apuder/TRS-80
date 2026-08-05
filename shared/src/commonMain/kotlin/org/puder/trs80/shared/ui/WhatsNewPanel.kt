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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.puder.trs80.shared.ui.theme.Hairline
import org.puder.trs80.shared.ui.theme.ModalPanel
import org.puder.trs80.shared.ui.theme.SectionKicker
import org.puder.trs80.shared.ui.theme.TextAction
import org.puder.trs80.shared.ui.theme.Trs80Theme
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.ok
import trs_80.shared.generated.resources.whats_new
import trs_80.shared.generated.resources.whats_new_body
import trs_80.shared.generated.resources.whats_new_title

/**
 * A word to somebody whose app has just changed under them.
 *
 * Shown once, and only to a user who had the previous version: what raises it
 * is machines arriving through the legacy import, which is something a new
 * install never does. Someone meeting the app for the first time has no old
 * version to be told about.
 *
 * Dismissed rather than answered — there is nothing to decide, so the scrim
 * closes it as readily as the button does, and it is gone for good either way.
 */
@Composable
internal fun WhatsNewPanel(onDismiss: () -> Unit) {
    val colors = Trs80Theme.colors
    ModalPanel(onDismiss = onDismiss) {
        SectionKicker(stringResource(Res.string.whats_new))
        Text(
            stringResource(Res.string.whats_new_title),
            style = Trs80Theme.type.title,
            color = colors.accentText,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.padding(top = 10.dp))
        Text(
            stringResource(Res.string.whats_new_body),
            style = Trs80Theme.type.body,
            color = colors.muted,
        )
        Spacer(Modifier.padding(top = 18.dp))
        Hairline()
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            TextAction(stringResource(Res.string.ok), onClick = onDismiss, padding = 0.dp)
        }
    }
}
