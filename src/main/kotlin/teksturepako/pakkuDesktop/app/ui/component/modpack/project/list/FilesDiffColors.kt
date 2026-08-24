/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.styling.BadgeStyle
import org.jetbrains.jewel.ui.theme.badgeStyle
import org.jetbrains.jewel.ui.theme.colorPalette
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind

fun FilesChangeKind.actionLabel(): String = when (this) {
    FilesChangeKind.Fetch -> "Download"
    FilesChangeKind.Override -> "Sync"
    FilesChangeKind.Cleanup -> "Delete"
    FilesChangeKind.Added -> "Add"
    FilesChangeKind.Updated, FilesChangeKind.HashMismatch -> "Update"
    FilesChangeKind.Removed -> "Remove"
}

@Composable
@ReadOnlyComposable
fun FilesChangeKind.actionBadgeStyle(): BadgeStyle = when (this) {
    FilesChangeKind.Fetch, FilesChangeKind.Added, FilesChangeKind.Override ->
        JewelTheme.badgeStyle.green
    FilesChangeKind.Updated, FilesChangeKind.HashMismatch ->
        JewelTheme.badgeStyle.blueSecondary
    FilesChangeKind.Cleanup, FilesChangeKind.Removed ->
        JewelTheme.badgeStyle.graySecondary
}

@Composable
@ReadOnlyComposable
fun FilesChangeKind.statusColor(): Color {
    val p = JewelTheme.colorPalette
    val fallback = JewelTheme.contentColor
    fun pick(ramps: List<Color>, fallbackColor: Color): Color =
        ramps.getOrNull((ramps.lastIndex * 5 / 10).coerceIn(0, ramps.lastIndex)) ?: fallbackColor
    return when (this) {
        FilesChangeKind.Fetch, FilesChangeKind.Added, FilesChangeKind.Override ->
            pick(p.green, PakkuDesktopConstants.green)
        FilesChangeKind.Updated, FilesChangeKind.HashMismatch ->
            pick(p.blue, PakkuDesktopConstants.highlightColor)
        FilesChangeKind.Cleanup, FilesChangeKind.Removed ->
            pick(p.red, PakkuDesktopConstants.coral)
    }
}

@Composable
@ReadOnlyComposable
fun FilesChangeKind.lineBackground(): Color {
    val tintAlpha = if (JewelTheme.isDark) 0.24f else 0.14f
    return statusColor().copy(alpha = tintAlpha)
}
