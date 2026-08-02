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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.puder.trs80.shared.ui.theme.MinimumTouchTarget
import org.puder.trs80.shared.ui.theme.SectionKicker
import org.puder.trs80.shared.ui.theme.StrokeIcon
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.Trs80Icon
import org.puder.trs80.shared.ui.theme.Trs80Theme
import org.puder.trs80.shared.ui.theme.scanlines
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.first_run_note
import trs_80.shared.generated.resources.never_run
import trs_80.shared.generated.resources.pane_hint
import trs_80.shared.generated.resources.resume
import trs_80.shared.generated.resources.saved_session
import trs_80.shared.generated.resources.start_anywhere
import trs_80.shared.generated.resources.where_you_left_off

/** How wide the pane lets its text run before it stops being readable. */
private val PANE_TEXT = 460.dp

/**
 * The machine last run, offered back at the size the pane can afford.
 *
 * What anyone with any history at all opened the app for, so it gets the whole
 * pane rather than a line in a list. It says what it actually knows — the
 * machine, its model and whether there is a session waiting — and not how long
 * ago or how far in, because the app records a timestamp and a flag and nothing
 * that would make either of those true.
 */
@Composable
fun ResumePane(card: ConfigurationCard, onRun: (Int) -> Unit) {
    val colors = Trs80Theme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Trs80Theme.spacing.screenEdge),
    ) {
        SectionKicker(stringResource(Res.string.where_you_left_off))
        Box(
            Modifier
                .fillMaxWidth()
                .height(Trs80Theme.spacing.plateHeight * 2)
                .border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.2f))
                .padding(Trs80Theme.spacing.mat)
                .background(colors.crt)
                .scanlines(),
        ) {
            val screenshot = card.screenshot
            if (screenshot != null) {
                Image(
                    bitmap = screenshot,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            card.name,
            style = Trs80Theme.type.title,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            listOf(
                card.model.uppercase(),
                if (card.hasSavedState) {
                    stringResource(Res.string.saved_session)
                } else {
                    stringResource(Res.string.never_run)
                },
            ).joinToString(" · "),
            style = Trs80Theme.type.kickerSmall,
            color = colors.muted,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .widthIn(max = PANE_TEXT)
                .border(Trs80Theme.spacing.hairline, colors.accent)
                .background(colors.accent.copy(alpha = 0.08f))
                .heightIn(min = MinimumTouchTarget)
                .clickable { onRun(card.id) }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StrokeIcon(Trs80Icon.Play, color = colors.accentText, size = 22.dp)
            Spacer(Modifier.padding(start = 12.dp))
            Text(
                stringResource(Res.string.resume),
                style = Trs80Theme.type.kicker,
                color = colors.accentText,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(Res.string.pane_hint),
            style = Trs80Theme.type.bodySmall,
            color = colors.muted,
            modifier = Modifier.widthIn(max = PANE_TEXT),
        )
    }
}

/**
 * What the pane says to someone who has never played anything.
 *
 * There is nothing to resume and nothing selected, and half a screen of empty
 * is worse than half a screen that explains itself once. Just the sentence: a
 * mock ROM banner over it only said a model this user has not chosen, in a
 * phosphor green that belongs on the emulated screen and nowhere else.
 */
@Composable
fun FirstRunPane(catalogSize: Int) {
    val colors = Trs80Theme.colors
    Box(
        Modifier.fillMaxSize().padding(Trs80Theme.spacing.screenEdge),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = PANE_TEXT),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(Res.string.start_anywhere),
                style = Trs80Theme.type.title,
                color = colors.text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.first_run_note, catalogSize),
                style = Trs80Theme.type.bodySmall,
                color = colors.muted,
            )
        }
    }
}
