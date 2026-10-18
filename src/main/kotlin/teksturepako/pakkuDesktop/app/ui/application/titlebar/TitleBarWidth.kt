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
import teksturepako.pakkuDesktop.app.ui.application.window.LocalWindowState
import teksturepako.pakkuDesktop.app.ui.driver.LocalSystemDensity

/** Base title bar height at any UI scale (Nucleus Jewel default; density scales it). */
const val TITLE_BAR_BASE_HEIGHT_DP = 40f

/** One logical dp below the title bar (border row in [MainTitleBar]). */
const val TITLE_BAR_BORDER_HEIGHT_DP = 1f

/** Decorative chrome (alpha chip, MC loader line) only at full width; MC line uses the same cutoff. */
enum class TitleBarWidthTier {
    Comfortable,
    Compact,
}

val LocalTitleBarWidthTier = compositionLocalOf { TitleBarWidthTier.Comfortable }

/**
 * Max width for menu rows beside the brand icon. Provided by the start cluster from the
 * width actually remaining after the end-side content measured itself — never from estimates.
 */
val LocalTitleBarStartSlotMaxWidth = compositionLocalOf { Dp.Unspecified }

@Composable
fun rememberTitleBarWidthTier(windowWidth: Dp = LocalWindowState.current.size.width): TitleBarWidthTier {
    // WindowState dp is system density. Above 100% UI scale, LocalDensity is larger, so the
    // same dp number is wider than the window; convert before comparing against the 880dp tier.
    val systemDensity = LocalSystemDensity.current?.density ?: LocalDensity.current.density
    val layoutWidth = titleBarLayoutWidth(windowWidth, systemDensity, LocalDensity.current.density)
    return remember(layoutWidth) { titleBarWidthTier(layoutWidth) }
}

/**
 * [systemWindowWidth] is [androidx.compose.ui.window.WindowState] width (system density).
 * Returns that width in the density actually used to lay out the title bar.
 */
fun titleBarLayoutWidth(systemWindowWidth: Dp, systemDensity: Float, layoutDensity: Float): Dp {
    if (systemDensity <= 0f || layoutDensity <= 0f || systemDensity == layoutDensity) return systemWindowWidth
    return systemWindowWidth * (systemDensity / layoutDensity)
}

fun titleBarWidthTier(windowWidth: Dp): TitleBarWidthTier =
    if (windowWidth < 880.dp) TitleBarWidthTier.Compact else TitleBarWidthTier.Comfortable

fun titleBarLabelMaxWidth(slotMaxWidth: Dp, fraction: Float = 0.38f, floor: Dp = 48.dp): Dp {
    if (slotMaxWidth.isUnspecified) return 200.dp
    return (slotMaxWidth * fraction).coerceAtLeast(floor.coerceAtMost(slotMaxWidth))
}
