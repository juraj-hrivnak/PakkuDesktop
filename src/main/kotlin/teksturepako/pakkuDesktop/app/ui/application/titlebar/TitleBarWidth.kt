/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.titlebar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import dev.nucleusframework.core.runtime.LinuxDesktopEnvironment
import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.window.utils.linux.LinuxButtonLayout
import dev.nucleusframework.window.utils.linux.rememberLinuxButtonLayout
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.application.window.LocalWindowState
import teksturepako.pakkuDesktop.app.ui.driver.LocalSystemDensity

/** Base title bar height at any UI scale (Nucleus Jewel default; density scales it). */
const val TITLE_BAR_BASE_HEIGHT_DP = 40f

/** One logical dp below the title bar (border row in [MainTitleBar]). */
const val TITLE_BAR_BORDER_HEIGHT_DP = 1f

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
    // WindowState dp is system density. Above 100% UI scale, LocalDensity is larger, so the
    // same dp number is wider than the window and Nucleus places the caption buttons past the frame.
    val systemDensity = LocalSystemDensity.current?.density ?: LocalDensity.current.density
    val layoutWidth = titleBarLayoutWidth(windowWidth, systemDensity, LocalDensity.current.density)
    val tier = titleBarWidthTier(layoutWidth)
    return remember(layoutWidth, linuxLayout) {
        val startClusterMaxWidth = titleBarStartClusterMaxWidth(layoutWidth, linuxLayout)
        TitleBarLayout(
            tier = tier,
            startClusterMaxWidth = startClusterMaxWidth,
            startSlotMaxWidth = (startClusterMaxWidth - PakkuDesktopConstants.railWidth).coerceAtLeast(0.dp),
        )
    }
}

/**
 * [systemWindowWidth] is [androidx.compose.ui.window.WindowState] width (system density).
 * Returns that width in the density actually used to lay out the title bar.
 */
fun titleBarLayoutWidth(systemWindowWidth: Dp, systemDensity: Float, layoutDensity: Float): Dp {
    if (systemDensity <= 0f || layoutDensity <= 0f || systemDensity == layoutDensity) return systemWindowWidth
    return systemWindowWidth * (systemDensity / layoutDensity)
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
