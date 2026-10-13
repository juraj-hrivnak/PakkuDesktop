/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.ui.component.ContextMenuRepresentation

/** Right-click menu using Jewel styling ([ContextMenuRepresentation]). */
@Composable
fun PkUiContextMenuArea(
    items: () -> List<ContextMenuItem>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val state = remember { ContextMenuState() }
    CompositionLocalProvider(LocalContextMenuRepresentation provides ContextMenuRepresentation) {
        ContextMenuArea(
            items = items,
            state = state,
            enabled = enabled,
        ) {
            Box(modifier.fillMaxWidth()) { content() }
        }
    }
}
