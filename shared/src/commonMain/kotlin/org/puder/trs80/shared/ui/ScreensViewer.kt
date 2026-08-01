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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.puder.trs80.shared.ui.theme.StrokeIcon
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.Trs80Icon
import org.puder.trs80.shared.ui.theme.Trs80Theme

/**
 * The screens, one at a time and as large as they will go.
 *
 * Swiped rather than scrolled: these are a handful of pictures of the same
 * machine, so what the reader wants is the next one whole, not a continuous
 * strip. Kept on black rather than the app's ground — at this size the picture
 * is the whole screen, and anything else framing it is a distraction.
 */
@Composable
fun ScreensViewer(
    urls: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (urls.isEmpty()) {
        return
    }
    val pager = rememberPagerState(
        initialPage = startIndex.coerceIn(0, urls.lastIndex),
        pageCount = { urls.size },
    )
    Box(
        modifier
            .fillMaxSize()
            // The library's own ground, so the viewer belongs to the app it
            // opened from and follows the theme with it. Not black, and not the
            // machine's glass either: both sit at a fixed brightness, and in a
            // dark theme the glass reads brighter than anything around it.
            .background(Trs80Theme.colors.ground)
            // Anywhere off the picture closes it, which is what a viewer opened
            // by tapping a picture should do.
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // No frame and no matting: the picture is given the whole
                // screen and keeps its own shape. Imposing one here was how the
                // first attempt letterboxed a 512x192 screen inside a 4:3 box.
                RemoteImage(
                    url = urls[page],
                    modifier = Modifier.fillMaxSize().padding(Trs80Theme.spacing.gap),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Trs80Theme.spacing.screenEdge),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                StrokeIcon(Trs80Icon.Close, color = Color.White, onClick = onDismiss)
            }
        }

        if (urls.size > 1) {
            Text(
                "${pager.currentPage + 1} / ${urls.size}",
                style = Trs80Theme.type.kickerSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = 18.dp),
            )
        }
    }
}
