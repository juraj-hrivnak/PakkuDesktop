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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.FilesFilter
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesFloatingActions
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesFooterStatus
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesLinkList
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesPreviewFloatingActions
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesSelectionControls
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.filteredFilesLinks
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.rememberFilesExpandedFolders
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.rememberFilesLinkListSplitRatio
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.RecomposeProbe

@Composable
fun FilesTab(publish: (ModpackMsg) -> Unit, model: FilesTabModel) {
    RecomposeProbe("FilesTab")
    val filterFocusRequester = remember { FocusRequester() }
    val scanned = model.filesPreviewMode != FilesPreviewMode.None
    val links = remember(
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
        model.diskSnapshot,
        model.filesFilterText,
        model.filesFilterFolders,
        model.filesFilterChangeKinds,
    ) {
        model.filteredFilesLinks()
    }
    val lastClickedIndex = remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val (expandedFolders, onToggleFolderExpanded) = rememberFilesExpandedFolders(links)
    val splitRatio = rememberFilesLinkListSplitRatio(model.filesSplitRatio, publish)
    val selectableKeys = remember(links) {
        links.mapNotNull { it.selectionKey }.distinct()
    }
    val linkIndexByPathKey = remember(links) {
        links.withIndex().associate { (index, link) -> link.pathKey to index }
    }
    val focusedProjectUiKey = model.selectedProject?.uiKey()

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
                    text = "Fetch writes Pakku's files to disk. Sync reads disk changes into Pakku.",
                    color = JewelTheme.contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                )
            }
        }

        Divider(Orientation.Horizontal)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (model.lockFile?.get() != null) {
                FilesLinkList(
                    links = links,
                    listState = listState,
                    splitRatio = splitRatio,
                    onSplitRatioChange = { splitRatio.value = it },
                    selectedProjectKeys = model.selectedProjectKeys,
                    focusedProjectUiKey = focusedProjectUiKey,
                    filesPreviewMode = model.filesPreviewMode,
                    configFile = model.configFile?.get(),
                    expandedFolders = expandedFolders,
                    onToggleFolderExpanded = onToggleFolderExpanded,
                    linkIndexByPathKey = linkIndexByPathKey,
                    lastClickedIndex = lastClickedIndex,
                    publish = publish,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            FilesFloatingActions(publish, model)
            FilesPreviewFloatingActions(publish, model)
        }

        Divider(Orientation.Horizontal)
        FilesFooterStatus(model, links = links)
    }
}
