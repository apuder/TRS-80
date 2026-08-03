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

package org.puder.trs80.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.puder.trs80.shared.ui.SystemBarContents
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import trs_80.shared.generated.resources.Archivo_Regular
import trs_80.shared.generated.resources.JetBrainsMono_Regular
import trs_80.shared.generated.resources.JetBrainsMono_SemiBold
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.SpaceMono_Bold
import trs_80.shared.generated.resources.SpaceMono_Regular
import trs_80.shared.generated.resources.VT323_Regular

/**
 * The colors of the app, in one register.
 *
 * Straight from the visual spec. The two registers differ only in ground,
 * hairline and accent step — everything else about a screen is the same in
 * both, which is why this is one shape with two instances rather than two
 * themes.
 */
@Immutable
data class Trs80Colors(
    /** The page behind everything. */
    val ground: Color,
    /** Body text and titles. */
    val text: Color,
    /** The accent, used for strokes and marks — never as a fill behind text. */
    val accent: Color,
    /** The accent as text, which needs more contrast than the stroke does. */
    val accentText: Color,
    /** Input fields, which sit slightly proud of the ground. */
    val field: Color,
    /** Hairlines and dividers. */
    val hairline: Color,
    /** Secondary text: author lines, counts, captions. */
    val muted: Color,
    /** Destructive actions. Deliberately a purer red than [accent]. */
    val danger: Color,
    /** Inside a CRT plate — the glass, not the shell. */
    val crt: Color,
    /** The phosphor, for text drawn inside a plate. */
    val phosphor: Color,
    val isDark: Boolean,
)

val LightColors = Trs80Colors(
    ground = Color(0xFFF6F6F4),
    text = Color(0xFF16181C),
    accent = Color(0xFFC8442E),
    accentText = Color(0xFFA83725),
    field = Color(0xFFFFFFFF),
    hairline = Color(0xFF16181C).copy(alpha = 0.13f),
    muted = Color(0xFF16181C).copy(alpha = 0.58f),
    danger = Color(0xFFD22B1E),
    crt = Color(0xFF2E302D),
    phosphor = Color(0xFF5CE15C),
    isDark = false,
)

val DarkColors = Trs80Colors(
    ground = Color(0xFF1A1917),
    text = Color(0xFFEFEAE1),
    accent = Color(0xFFE2705A),
    accentText = Color(0xFFEA8B76),
    field = Color(0xFFEFEAE1).copy(alpha = 0.06f),
    hairline = Color(0xFFEFEAE1).copy(alpha = 0.14f),
    muted = Color(0xFFEFEAE1).copy(alpha = 0.58f),
    danger = Color(0xFFFF6C5C),
    crt = Color(0xFF2E302D),
    phosphor = Color(0xFF5CE15C),
    isDark = true,
)

/**
 * The four faces, each with one job.
 *
 * The spec is strict about this and it is worth keeping strict: VT323 belongs
 * inside the CRT and nowhere else. The moment it appears in the shell, the
 * shell starts pretending to be the machine.
 */
@Immutable
data class Trs80Typography(
    /** Titles, plate captions, screen titles. */
    val title: TextStyle,
    /** The same face, smaller, for rows. */
    val titleSmall: TextStyle,
    /** Author lines, body copy, search, table values. */
    val body: TextStyle,
    /** The same, smaller. */
    val bodySmall: TextStyle,
    /** Section kickers, state chips, key caps. Always upper case, always tracked. */
    val kicker: TextStyle,
    /** Smaller kicker, for badges on plates. */
    val kickerSmall: TextStyle,
    /** Inside the CRT only. */
    val screen: TextStyle,
    /** The app's name in the top bar. */
    val wordmark: TextStyle,
)

/** Spacing, so screens agree without every one of them inventing numbers. */
@Immutable
data class Trs80Spacing(
    val hairline: Dp = 1.dp,
    /** The mat around a plate's glass. */
    val mat: Dp = 3.dp,
    val tight: Dp = 4.dp,
    val small: Dp = 7.dp,
    val gap: Dp = 11.dp,
    /** The screen's own left and right margin. */
    val screenEdge: Dp = 18.dp,
    /** A plate's glass. */
    val plateHeight: Dp = 104.dp,
    /** Catalog row artwork. */
    val rowArt: Dp = 56.dp,
)

val LocalTrs80Colors: ProvidableCompositionLocal<Trs80Colors> =
    staticCompositionLocalOf { LightColors }
val LocalTrs80Typography: ProvidableCompositionLocal<Trs80Typography> =
    staticCompositionLocalOf { error("No typography; wrap the content in Trs80Theme.") }
val LocalTrs80Spacing: ProvidableCompositionLocal<Trs80Spacing> =
    staticCompositionLocalOf { Trs80Spacing() }

/** The design system. Everything the app draws sits inside one of these. */
object Trs80Theme {
    val colors: Trs80Colors
        @Composable get() = LocalTrs80Colors.current
    val type: Trs80Typography
        @Composable get() = LocalTrs80Typography.current
    val spacing: Trs80Spacing
        @Composable get() = LocalTrs80Spacing.current
}

/**
 * Builds the type scale. Composable because the faces are resources, and a
 * resource is only reachable from a composition.
 */
@Composable
/**
 * The type scale.
 *
 * A step up from the sizes in the visual spec: those are px in a phone mock
 * viewed on a desktop, and they read small on a device held at arm's length.
 * The smallest style carries the most weight here — it sets the model name on
 * every plate and every label in a segmented control.
 */
private fun rememberTypography(): Trs80Typography {
    val spaceMono = FontFamily(
        Font(Res.font.SpaceMono_Regular, FontWeight.Normal),
        Font(Res.font.SpaceMono_Bold, FontWeight.Bold),
    )
    val archivo = FontFamily(Font(Res.font.Archivo_Regular, FontWeight.Normal))
    val jetBrains = FontFamily(
        Font(Res.font.JetBrainsMono_Regular, FontWeight.Normal),
        Font(Res.font.JetBrainsMono_SemiBold, FontWeight.SemiBold),
    )
    val vt323 = FontFamily(Font(Res.font.VT323_Regular, FontWeight.Normal))

    return Trs80Typography(
        title = TextStyle(
            fontFamily = spaceMono,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.02).em,
        ),
        titleSmall = TextStyle(
            fontFamily = spaceMono,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.02).em,
        ),
        body = TextStyle(fontFamily = archivo, fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = archivo, fontSize = 13.sp, lineHeight = 18.sp),
        kicker = TextStyle(
            fontFamily = jetBrains,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.14.em,
        ),
        kickerSmall = TextStyle(
            fontFamily = jetBrains,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.1.em,
        ),
        screen = TextStyle(fontFamily = vt323, fontSize = 21.sp, lineHeight = 21.sp),
        wordmark = TextStyle(fontFamily = jetBrains, fontSize = 17.sp, lineHeight = 17.sp),
    )
}

/**
 * Wraps content in the app's look.
 *
 * @param dark which register to use, defaulting to the system's.
 */
@Composable
fun Trs80Theme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Whatever is behind the system bars is this app's ground, so this app is
    // the only thing that knows what colour their contents have to be.
    SystemBarContents(light = dark)

    CompositionLocalProvider(
        LocalTrs80Colors provides if (dark) DarkColors else LightColors,
        LocalTrs80Typography provides rememberTypography(),
        LocalTrs80Spacing provides Trs80Spacing(),
        content = content,
    )
}
