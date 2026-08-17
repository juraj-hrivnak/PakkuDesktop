/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.ui.component.Tooltip

/** Kept for call-site compatibility; Jewel positions via its own placement metrics. */
enum class TooltipPosition {
    TOP, BOTTOM, START, END
}

/**
 * Thin wrapper around Jewel [Tooltip] — do not reimplement popup/hover timing here.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PkUiTooltip(
    tooltip: @Composable () -> Unit,
    @Suppress("UNUSED_PARAMETER") position: TooltipPosition = TooltipPosition.END,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Tooltip(
        tooltip = tooltip,
        modifier = modifier,
        content = content,
    )
}
