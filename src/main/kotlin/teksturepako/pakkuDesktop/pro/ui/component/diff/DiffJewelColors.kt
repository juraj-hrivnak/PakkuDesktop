/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.theme.colorPalette
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffType

internal data class DiffLineStyle(
    val prefix: String,
    val lineBackground: Color,
    val prefixColor: Color,
)

/**
 * Row tint: biased toward mid-ramp swatches (typically more chromatic in IntelliJ palettes).
 * Dark UI uses a deeper-but-richer slice; light UI stays pastel but not as washed-out as the tail.
 */
private fun List<Color>.diffWash(isDark: Boolean): Color? {
    if (isEmpty()) return null
    val numer = if (isDark) 5 else 6
    val i = (lastIndex * numer / 10).coerceIn(0, lastIndex)
    return this[i]
}

/** +/- glyph: high-chroma end on dark UI; deeper saturated slice on light UI. */
private fun List<Color>.diffGlyph(isDark: Boolean): Color? {
    if (isEmpty()) return null
    val numer = if (isDark) 9 else 3
    val i = (lastIndex * numer / 10).coerceIn(0, lastIndex)
    return this[i]
}

private fun List<Color>.midOrNull(): Color? {
    if (isEmpty()) return null
    return this[lastIndex / 2]
}

/** More saturated blues toward mid–upper ramp. */
private fun List<Color>.blueTitleAccent(isDark: Boolean): Color? {
    if (isEmpty()) return null
    val numer = if (isDark) 6 else 5
    val i = (lastIndex * numer / 10).coerceIn(0, lastIndex)
    return this[i]
}

@Composable
@ReadOnlyComposable
internal fun diffLineStyle(type: DiffType): DiffLineStyle {
    val p = JewelTheme.colorPalette
    val surface = JewelTheme.globalColors.panelBackground
    val dark = JewelTheme.isDark
    val green = p.green
    val red = p.red

    return when (type) {
        DiffType.ADDED -> {
            val wash = green.diffWash(dark) ?: JewelTheme.contentColor
            val glyph = green.diffGlyph(dark) ?: JewelTheme.contentColor
            DiffLineStyle(
                prefix = "+",
                lineBackground = lerp(surface, wash, TINT_MIX),
                prefixColor = glyph,
            )
        }
        DiffType.DELETED -> {
            val wash = red.diffWash(dark) ?: JewelTheme.contentColor
            val glyph = red.diffGlyph(dark) ?: JewelTheme.contentColor
            DiffLineStyle(
                prefix = "-",
                lineBackground = lerp(surface, wash, TINT_MIX),
                prefixColor = glyph,
            )
        }
        DiffType.UNCHANGED -> DiffLineStyle(
            prefix = " ",
            lineBackground = Color.Transparent,
            prefixColor = JewelTheme.globalColors.text.info,
        )
    }
}

@Composable
@ReadOnlyComposable
internal fun diffHunkHeaderBackground(): Color {
    val g = JewelTheme.globalColors
    return lerp(g.panelBackground, g.borders.normal, HEADER_MIX)
}

@Composable
@ReadOnlyComposable
internal fun diffFileTitleColor(): Color {
    val blue = JewelTheme.colorPalette.blue
    val accent = blue.blueTitleAccent(JewelTheme.isDark)
        ?: blue.midOrNull()
        ?: JewelTheme.globalColors.outlines.focused
    return lerp(JewelTheme.contentColor, accent, TITLE_MIX)
}

@Composable
@ReadOnlyComposable
internal fun diffFileSubtitleColor(): Color = JewelTheme.globalColors.text.info

@Composable
@ReadOnlyComposable
internal fun diffHunkHeaderForeground(): Color = JewelTheme.contentColor

@Composable
@ReadOnlyComposable
internal fun diffGutterLabelColor(): Color = JewelTheme.globalColors.text.disabled

@Composable
@ReadOnlyComposable
internal fun diffEmptyHintColor(): Color = JewelTheme.globalColors.text.disabled

/** How strongly insert/delete palette colors mix into [GlobalColors.panelBackground]. */
private const val TINT_MIX = 0.62f

private const val HEADER_MIX = 0.32f
private const val TITLE_MIX = 0.48f
