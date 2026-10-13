/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.titlebar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import dev.nucleusframework.core.runtime.LinuxDesktopEnvironment
import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.window.utils.linux.LinuxButtonLayout
import dev.nucleusframework.window.utils.linux.rememberLinuxButtonLayout
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.application.window.LocalWindowState

/** Decorative chrome (alpha chip) only at full width; MC line uses the same cutoff. */
enum class TitleBarWidthTier {
    Comfortable,
    Compact,
}

val LocalTitleBarWidthTier = compositionLocalOf { TitleBarWidthTier.Comfortable }

/** Max width for menu rows beside the brand icon (start cluster minus rail). */
val LocalTitleBarStartSlotMaxWidth = compositionLocalOf { Dp.Unspecified }

/** Max width for brand icon + menus as one start-aligned cluster. */
val LocalTitleBarStartClusterMaxWidth = compositionLocalOf { Dp.Unspecified }

@Composable
fun rememberTitleBarLayout(windowWidth: Dp = LocalWindowState.current.size.width): TitleBarLayout {
    val linuxLayout = if (Platform.Current == Platform.Linux) rememberLinuxButtonLayout() else null
    val tier = titleBarWidthTier(windowWidth)
    return remember(windowWidth, linuxLayout) {
        val startClusterMaxWidth = titleBarStartClusterMaxWidth(windowWidth, linuxLayout)
        TitleBarLayout(
            tier = tier,
            startClusterMaxWidth = startClusterMaxWidth,
            startSlotMaxWidth = (startClusterMaxWidth - PakkuDesktopConstants.railWidth).coerceAtLeast(0.dp),
        )
    }
}

data class TitleBarLayout(
    val tier: TitleBarWidthTier,
    val startClusterMaxWidth: Dp,
    val startSlotMaxWidth: Dp,
)

fun titleBarWidthTier(windowWidth: Dp): TitleBarWidthTier =
    if (windowWidth < 880.dp) TitleBarWidthTier.Compact else TitleBarWidthTier.Comfortable

fun titleBarLabelMaxWidth(slotMaxWidth: Dp, fraction: Float = 0.38f, floor: Dp = 48.dp): Dp {
    if (slotMaxWidth.isUnspecified) return 200.dp
    return (slotMaxWidth * fraction).coerceAtLeast(floor.coerceAtMost(slotMaxWidth))
}

fun titleBarStartClusterMaxWidth(windowWidth: Dp, linuxLayout: LinuxButtonLayout?): Dp {
    val controls = windowControlsWidth(linuxLayout)
    val endActions = END_PADDING + TOOLBAR_ICON_SLOT + TOOLBAR_ICON_SLOT + ALPHA_CHIP_WIDTH
    val macLeading = if (Platform.Current == Platform.MacOS) MAC_TRAFFIC_LIGHTS_RESERVE else 0.dp
    val reserved = controls + endActions + macLeading + TITLEBAR_SLOP
    return (windowWidth - reserved).coerceAtLeast(0.dp)
}

private fun windowControlsWidth(linuxLayout: LinuxButtonLayout?): Dp = when (Platform.Current) {
    Platform.Linux -> {
        val isKde = LinuxDesktopEnvironment.Current == LinuxDesktopEnvironment.KDE
        val button = if (isKde) 28.dp else 40.dp
        val count = linuxLayout?.buttons?.size ?: 3
        button * count + if (isKde) 4.dp else 0.dp
    }
    Platform.Windows -> 138.dp
    else -> 0.dp
}

private val TOOLBAR_ICON_SLOT = 36.dp
private val ALPHA_CHIP_WIDTH = 76.dp
private val END_PADDING = 16.dp
private val TITLEBAR_SLOP = 8.dp
private val MAC_TRAFFIC_LIGHTS_RESERVE = 80.dp
