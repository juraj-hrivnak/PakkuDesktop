/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.theme.colorPalette
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffType

internal data class DiffLineStyle(
    val prefix: String,
    val lineBackground: Color,
    val prefixColor: Color,
)

private fun List<Color>.midOrNull(): Color? {
    if (isEmpty()) return null
    return this[lastIndex / 2]
}

@Composable
@ReadOnlyComposable
internal fun diffLineStyle(type: DiffType): DiffLineStyle {
    val p = JewelTheme.colorPalette
    val surface = JewelTheme.globalColors.panelBackground
    val addedBase = p.green.midOrNull() ?: JewelTheme.contentColor
    val deletedBase = p.red.midOrNull() ?: JewelTheme.contentColor
    val tintAlpha = if (JewelTheme.isDark) 0.24f else 0.14f

    return when (type) {
        DiffType.ADDED -> DiffLineStyle(
            prefix = "+",
            lineBackground = addedBase.copy(alpha = tintAlpha),
            prefixColor = JewelTheme.contentColor,
        )
        DiffType.DELETED -> DiffLineStyle(
            prefix = "-",
            lineBackground = deletedBase.copy(alpha = tintAlpha),
            prefixColor = JewelTheme.contentColor,
        )
        DiffType.UNCHANGED -> DiffLineStyle(
            prefix = " ",
            lineBackground = surface,
            prefixColor = JewelTheme.contentColor,
        )
    }
}

@Composable
@ReadOnlyComposable
internal fun diffHunkHeaderBackground(): Color
{
    val p = JewelTheme.colorPalette
    val blueBase = p.blue.midOrNull() ?: JewelTheme.globalColors.panelBackground
    val tintAlpha = if (JewelTheme.isDark) 0.2f else 0.12f
    return blueBase.copy(alpha = tintAlpha)
}

@Composable
@ReadOnlyComposable
internal fun diffGutterBackground(): Color =
    JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f)

@Composable
@ReadOnlyComposable
internal fun diffSeparatorColor(): Color = JewelTheme.globalColors.borders.normal

@Composable
@ReadOnlyComposable
internal fun diffBorderColor(): Color = JewelTheme.globalColors.borders.normal

@Composable
@ReadOnlyComposable
internal fun diffFileTitleColor(): Color {
    return JewelTheme.contentColor
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
