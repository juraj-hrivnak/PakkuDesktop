/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * Folder headers drawn over a [androidx.compose.foundation.lazy.LazyColumn] of [rows].
 * Place in the same [Box] as the list, aligned to its top: the list layout never changes,
 * so scrolling does not shift content when headers appear or disappear.
 */
@Composable
fun StickyTreeFolderHeaders(
    listState: LazyListState,
    rows: List<PathTreeRow<*>>,
    modifier: Modifier = Modifier,
    headerContent: @Composable (PathTreeRow.Folder) -> Unit,
) {
    val slotHeightPx = with(LocalDensity.current) { TreeRowMinHeight.roundToPx() }
    val index = remember(rows) { StickyFolderIndex(rows) }
    val headers by remember(listState, index, slotHeightPx) {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo.map { VisibleTreeRow(it.index, it.offset, it.size) }
            index.headers(visible, slotHeightPx)
        }
    }
    if (headers.isEmpty()) return
    // Parents are composed last so a pushed-out child slides under its parent.
    val drawOrder = headers.asReversed()
    Layout(
        modifier = modifier.fillMaxWidth().clipToBounds(),
        content = {
            drawOrder.forEach { header ->
                key(header.folder.fullPath) {
                    Box(Modifier.background(JewelTheme.globalColors.panelBackground)) {
                        headerContent(header.folder)
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints.fixed(constraints.maxWidth, slotHeightPx)) }
        val height = drawOrder.maxOf { it.top + slotHeightPx }.coerceIn(0, constraints.maxHeight)
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { i, placeable -> placeable.place(0, drawOrder[i].top) }
        }
    }
}
