/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.SplitLayoutState
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.FilesFilter
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesDiffPane
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesDiffSide
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesFooterStatus
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesSelectionControls
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.SyncLinkedLazyLists
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.filteredFilesDiffPairs
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.animatedDividerStyle

@OptIn(FlowPreview::class)
@Composable
fun FilesTab(publish: (ModpackMsg) -> Unit, model: FilesTabModel) {
    val filterFocusRequester = remember { FocusRequester() }
    val splitState = remember { SplitLayoutState(model.filesSplitRatio.coerceIn(0.05f, 0.95f)) }
    val scanned = model.filesPreviewMode != FilesPreviewMode.None
    val pairs = remember(
        model.lockFile,
        model.configFile,
        model.filesPreviewMode,
        model.fetchDownloads,
        model.fetchOverrides,
        model.fetchCleanup,
        model.filesHashMismatchKeys,
        model.syncAdded,
        model.syncRemoved,
        model.syncUpdated,
        model.manualOverrides,
        model.filesFilterText,
        model.filesFilterFolders,
        model.filesFilterChangeKinds,
    ) {
        model.filteredFilesDiffPairs()
    }
    val lastClickedIndex = remember { mutableStateOf<Int?>(null) }
    val collapsedFolders = remember { mutableStateOf(emptySet<String>()) }
    val currentScroll = rememberLazyListState()
    val proposedScroll = rememberLazyListState()
    val selectableKeys = remember(pairs) {
        pairs.filter { it.selectable }.map { it.selectionKey }.distinct()
    }

    if (scanned) {
        SyncLinkedLazyLists(currentScroll, proposedScroll)
    }

    LaunchedEffect(splitState) {
        snapshotFlow { splitState.dividerPosition }
            .drop(1)
            .distinctUntilChanged()
            .debounce(200)
            .collect { ratio ->
                if (ratio in 0.05f..0.95f) {
                    publish(ModpackMsg.FilesSplitRatioChanged(ratio))
                }
            }
    }

    LaunchedEffect(model.filesSplitRatio) {
        if (kotlin.math.abs(splitState.dividerPosition - model.filesSplitRatio) > 0.01f) {
            splitState.dividerPosition = model.filesSplitRatio.coerceIn(0.05f, 0.95f)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PakkuDesktopConstants.commonPaddingSize)
                .padding(top = PakkuDesktopConstants.commonPaddingSize, bottom = 2.dp),
        ) {
            FilesFilter(
                publish = publish,
                model = model,
                focusRequester = filterFocusRequester,
            )
            if (scanned) {
                FilesSelectionControls(
                    publish = publish,
                    model = model,
                    lastClickedIndex = lastClickedIndex,
                    selectableKeys = selectableKeys,
                )
            } else {
                Text(
                    text = "Fetch or Sync to preview changes",
                    color = JewelTheme.contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                )
            }
        }

        Divider(Orientation.Horizontal)

        if (scanned) {
            HorizontalSplitLayout(
                state = splitState,
                dividerStyle = animatedDividerStyle(
                    color = JewelTheme.globalColors.borders.normal.copy(alpha = 0.22f),
                ),
                first = {
                    FilesDiffPane(
                        side = FilesDiffSide.Current,
                        publish = publish,
                        model = model,
                        pairs = pairs,
                        scrollState = currentScroll,
                        collapsedFolders = collapsedFolders,
                        lastClickedIndex = lastClickedIndex,
                        showScrollbar = false,
                        showSelection = true,
                        showHeader = true,
                    )
                },
                second = {
                    FilesDiffPane(
                        side = FilesDiffSide.Proposed,
                        publish = publish,
                        model = model,
                        pairs = pairs,
                        scrollState = proposedScroll,
                        collapsedFolders = collapsedFolders,
                        lastClickedIndex = lastClickedIndex,
                        showScrollbar = true,
                        showSelection = true,
                        showHeader = true,
                    )
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                firstPaneMinWidth = 220.dp,
                secondPaneMinWidth = 220.dp,
                draggableWidth = 16.dp,
            )
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                FilesDiffPane(
                    side = FilesDiffSide.Current,
                    publish = publish,
                    model = model,
                    pairs = pairs,
                    scrollState = currentScroll,
                    collapsedFolders = collapsedFolders,
                    lastClickedIndex = lastClickedIndex,
                    showScrollbar = true,
                    showSelection = false,
                    showHeader = false,
                )
            }
        }

        Divider(Orientation.Horizontal)
        FilesFooterStatus(model, fileCount = pairs.size)
    }
}
