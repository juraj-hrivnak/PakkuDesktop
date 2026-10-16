/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Divider
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.FilesFilter
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesFloatingActions
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesFooterStatus
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesLinkList
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesPreviewFloatingActions
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesSelectionControls
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FilesLink
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.rememberFilesLinkListSplitRatio
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeSnapshot
import teksturepako.pakkuDesktop.app.ui.driver.LocalFilesPanePrep
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.RecomposeProbe

@Composable
fun FilesTab(publish: (ModpackMsg) -> Unit, model: FilesTabModel) {
    RecomposeProbe("FilesTab")
    val filterFocusRequester = remember { FocusRequester() }
    val scanned = model.filesPreviewMode != FilesPreviewMode.None
    val prep = LocalFilesPanePrep.current
    val links = prep?.links ?: emptyList()
    val lastClickedIndex = remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val emptyTree = remember { PathTreeSnapshot.of(emptyList<FilesLink>()) { it.path } }
    val tree = prep?.tree ?: emptyTree
    val expandedFolders = remember(tree.folderIds, model.filesCollapsedFolders) {
        tree.folderIds - model.filesCollapsedFolders
    }
    val splitRatio = rememberFilesLinkListSplitRatio(model.filesSplitRatio, publish)
    val selectableKeys = prep?.selectableKeys ?: emptyList()
    val linkIndexByPathKey = prep?.linkIndexByPathKey ?: emptyMap()
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
            FilesSelectionControls(
                publish = publish,
                model = model,
                lastClickedIndex = lastClickedIndex,
                selectableKeys = selectableKeys,
                hasFolders = tree.folderIds.isNotEmpty(),
                linksEmpty = links.isEmpty(),
                folderIds = tree.folderIds,
                showSelection = scanned,
            )
        }

        Divider(Orientation.Horizontal)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (model.lockFile?.get() != null) {
                FilesLinkList(
                    links = links,
                    preparedTree = tree,
                    listState = listState,
                    splitRatio = splitRatio,
                    onSplitRatioChange = { splitRatio.value = it },
                    selectedProjectKeys = model.selectedProjectKeys,
                    focusedProjectUiKey = focusedProjectUiKey,
                    filesPreviewMode = model.filesPreviewMode,
                    configFile = model.configFile?.get(),
                    expandedFolders = expandedFolders,
                    existingDiskFolders = model.diskSnapshot.existingFolders,
                    linkIndexByPathKey = linkIndexByPathKey,
                    lastClickedIndex = lastClickedIndex,
                    publish = publish,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (model.lockFile?.get() != null && prep == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            FilesFloatingActions(publish, model)
            FilesPreviewFloatingActions(publish, model)
        }

        Divider(Orientation.Horizontal)
        FilesFooterStatus(
            publish = publish,
            model = model,
            links = links,
            selectableKeys = selectableKeys,
            lastClickedIndex = lastClickedIndex,
        )
    }
}
