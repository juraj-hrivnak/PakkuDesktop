/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

enum class FilesDiffSide { Current, Proposed }

@Composable
fun FilesDiffPane(
    side: FilesDiffSide,
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    pairs: List<FilesDiffPair>,
    scrollState: LazyListState,
    collapsedFolders: MutableState<Set<String>>,
    lastClickedIndex: MutableState<Int?>,
    showScrollbar: Boolean,
    showSelection: Boolean,
    showHeader: Boolean,
) {
    Column(Modifier.fillMaxSize()) {
        if (showHeader) {
            FilesDiffPaneHeader(side, model)
            Divider(Orientation.Horizontal)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            FilesDiffList(
                side = side,
                publish = publish,
                model = model,
                pairs = pairs,
                scrollState = scrollState,
                collapsedFolders = collapsedFolders,
                lastClickedIndex = lastClickedIndex,
                showScrollbar = showScrollbar,
                showSelection = showSelection,
                allowDrop = side == FilesDiffSide.Current,
            )
            when (side) {
                FilesDiffSide.Current -> FilesFloatingActions(publish, model)
                FilesDiffSide.Proposed -> FilesPreviewFloatingActions(publish, model)
            }
        }
    }
}

@Composable
private fun FilesDiffPaneHeader(side: FilesDiffSide, model: ModpackModel) {
    val title = when (side) {
        FilesDiffSide.Current -> "Current"
        FilesDiffSide.Proposed -> "New"
    }
    val subtitle = when (model.filesPreviewMode) {
        FilesPreviewMode.Fetch -> if (side == FilesDiffSide.Proposed) "After fetch" else null
        FilesPreviewMode.Sync -> if (side == FilesDiffSide.Proposed) "After sync" else null
        FilesPreviewMode.None -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = JewelTheme.contentColor.copy(alpha = 0.5f),
            )
        }
    }
}
