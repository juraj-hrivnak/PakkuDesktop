/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.colorPalette
import org.jetbrains.jewel.ui.theme.treeStyle
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.button.ToolbarActionButton
import teksturepako.pakkuDesktop.app.ui.component.button.ToolbarSeparator
import teksturepako.pakkuDesktop.app.ui.component.modpack.git.GitFloatingActions
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FooterRow
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FooterText
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.SelectionStatus
import teksturepako.pakkuDesktop.app.ui.model.GitTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.elm.animatedColor
import teksturepako.pakkuDesktop.elm.animatedDividerStyle
import teksturepako.pakkuDesktop.app.git.GitFile
import teksturepako.pakkuDesktop.app.git.GitState
import teksturepako.pakkuDesktop.app.git.gitChangelistUiSnapshot
import teksturepako.pakkuDesktop.app.ui.component.git.diff.DiffViewer
import teksturepako.pakkuDesktop.app.git.DiffContent
import teksturepako.pakkuDesktop.app.git.GitChange
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeRow
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeFolderRow
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeItemRow
import teksturepako.pakkuDesktop.app.ui.component.tree.StickyTreeFolderHeaders
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeStatus
@Composable
fun GitTab(
    publish: (ModpackMsg) -> Unit,
    model: GitTabModel,
) {
    RecomposeProbe("GitTab")
    val gitState = model.git
    val diffContent = model.gitCurrentDiff
    val files = gitState.gitFiles
    val selectedCount = gitState.selectedFiles.size
    val changelistSnapshot = remember(files) { gitChangelistUiSnapshot(files) }

    val splitState = remember { SplitLayoutState(0.32f) }

    val diffPendingPath = model.gitDiffPendingFile?.path

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PakkuDesktopConstants.commonPaddingSize)
                .padding(top = PakkuDesktopConstants.commonPaddingSize, bottom = 2.dp),
        ) {
            ChangelistToolbar(
                filesEmpty = files.isEmpty(),
                selectedCount = selectedCount,
                totalCount = files.size,
                hasFolders = changelistSnapshot.folderIds.isNotEmpty(),
                publish = publish,
            )
        }

        Divider(Orientation.Horizontal)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            HorizontalSplitLayout(
                state = splitState,
                dividerStyle = animatedDividerStyle(),
                first = {
                    Box(Modifier.fillMaxSize()) {
                        SourceControlSidePanel(
                            gitState = gitState,
                            currentDiff = diffContent,
                            diffPendingPath = diffPendingPath,
                            publish = publish,
                            modifier = Modifier.fillMaxSize(),
                        )
                        GitFloatingActions(publish, model)
                    }
                },
                second = {
                    DiffViewer(
                        diffContent,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                modifier = Modifier.fillMaxSize(),
                firstPaneMinWidth = 240.dp,
                secondPaneMinWidth = 200.dp,
                draggableWidth = 16.dp,
            )
        }

        Divider(Orientation.Horizontal)

        GitFooterStatus(
            selectedCount = selectedCount,
            totalCount = files.size,
            pulling = model.wantsGitPull,
            pushing = model.wantsGitPush,
            committing = model.wantsGitCommit,
            publish = publish,
        )
    }
}

@Composable
private fun SourceControlSidePanel(
    gitState: GitState,
    currentDiff: DiffContent?,
    diffPendingPath: String?,
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
) {
    val files = gitState.gitFiles

    val changelistSnapshot = remember(files) { gitChangelistUiSnapshot(files) }
    val selectedPaths = remember(gitState.selectedFiles) {
        gitState.selectedFiles.map { it.path }.toSet()
    }
    val flatRows = remember(changelistSnapshot, gitState.expandedFolderPaths, selectedPaths) {
        changelistSnapshot.flatRows(gitState.expandedFolderPaths, selectedPaths)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Changes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Divider(Orientation.Horizontal)

        if (files.isEmpty()) {
            Text(
                text = "No local changes",
                modifier = Modifier
                    .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 18.dp)
                    .fillMaxWidth(),
                style = JewelTheme.defaultTextStyle,
                color = JewelTheme.contentColor.copy(alpha = 0.55f),
            )
            Text(
                text = "Edits to tracked files and new files will appear in this list.",
                modifier = Modifier
                    .padding(horizontal = PakkuDesktopConstants.commonPaddingSize)
                    .fillMaxWidth(),
                style = JewelTheme.defaultTextStyle,
                color = JewelTheme.contentColor.copy(alpha = 0.45f),
            )
            Spacer(Modifier.weight(1f))
        } else {
            val listState = rememberLazyListState()
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clipToBounds(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                ) {
                    flatRows.forEach { row ->
                        val key = when (row) {
                            is PathTreeRow.Folder -> "d:${row.fullPath}"
                            is PathTreeRow.Item -> "f:${row.path}"
                        }
                        val contentType = when (row) {
                            is PathTreeRow.Folder -> "changelist_folder"
                            is PathTreeRow.Item -> "changelist_file"
                        }
                        item(key = key, contentType = contentType) {
                            GitChangesListRow(
                                row = row,
                                currentDiff = currentDiff,
                                diffPendingPath = diffPendingPath,
                                publish = publish,
                            )
                        }
                    }
                }
                StickyTreeFolderHeaders(listState, flatRows) { folderRow ->
                    GitChangesListRow(
                        row = folderRow,
                        currentDiff = currentDiff,
                        diffPendingPath = diffPendingPath,
                        publish = publish,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangelistToolbar(
    filesEmpty: Boolean,
    selectedCount: Int,
    totalCount: Int,
    hasFolders: Boolean,
    publish: (ModpackMsg) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarActionButton(
            key = AllIconsKeys.Actions.Selectall,
            label = "Select all",
            enabled = !filesEmpty && selectedCount < totalCount,
            onClick = { publish(ModpackMsg.GitSelectAllChangedFiles) },
        )
        ToolbarActionButton(
            key = AllIconsKeys.Actions.Unselectall,
            label = "Unselect all",
            enabled = selectedCount > 0,
            onClick = { publish(ModpackMsg.GitClearChangedFileSelection) },
        )
        ToolbarSeparator()
        ToolbarActionButton(
            key = AllIconsKeys.Actions.Expandall,
            label = "Expand all",
            enabled = !filesEmpty && hasFolders,
            onClick = { publish(ModpackMsg.GitChangelistExpandAllFolders) },
        )
        ToolbarActionButton(
            key = AllIconsKeys.Actions.Collapseall,
            label = "Collapse all",
            enabled = !filesEmpty && hasFolders,
            onClick = { publish(ModpackMsg.GitChangelistCollapseAllFolders) },
        )
    }
}

@Composable
private fun GitFooterStatus(
    selectedCount: Int,
    totalCount: Int,
    pulling: Boolean,
    pushing: Boolean,
    committing: Boolean,
    publish: (ModpackMsg) -> Unit,
) {
    val status = when {
        committing -> "Committing…"
        pulling -> "Pulling…"
        pushing -> "Pushing…"
        else -> null
    }

    FooterRow {
        FooterText(if (totalCount == 1) "1 file" else "$totalCount files")
        if (status != null) FooterText(status)
        Spacer(Modifier.weight(1f))
        SelectionStatus(
            selectedCount = selectedCount,
            totalCount = totalCount,
            canSelectAll = selectedCount < totalCount,
            onSelectAll = { publish(ModpackMsg.GitSelectAllChangedFiles) },
            onUnselectAll = { publish(ModpackMsg.GitClearChangedFileSelection) },
        )
    }
}

@Composable
private fun GitChangesListRow(
    row: PathTreeRow<GitFile>,
    currentDiff: DiffContent?,
    diffPendingPath: String?,
    publish: (ModpackMsg) -> Unit,
) {
    when (row) {
        is PathTreeRow.Folder ->
            TreeFolderRow(
                row = row,
                onToggleExpanded = {
                    publish(ModpackMsg.GitChangelistFolderExpansionToggled(row.fullPath))
                },
                onToggleSelected = {
                    publish(ModpackMsg.GitFolderSelectionToggled(row.fullPath))
                },
            )
        is PathTreeRow.Item -> {
            val file = row.item
            val isViewed =
                sameGitPath(file.path, diffPendingPath) ||
                    sameGitPath(file.path, currentDiff?.newPath) ||
                    sameGitPath(file.path, currentDiff?.oldPath)
            val pathLabel = file.path.substringAfterLast('/').ifEmpty { file.path }
            TreeItemRow(
                depth = row.depth,
                label = pathLabel,
                status = file.status.toTreeStatus(),
                checked = row.selected,
                onCheckedChange = { publish(ModpackMsg.GitFileSelectionToggled(file)) },
                highlighted = isViewed,
                onClick = { publish(ModpackMsg.GitDiffFileSelected(file)) },
                interactionKey = file.path,
            )
        }
    }
}

/** Normalize repo-relative paths from JGit vs unified diff headers (slashes, `./`). */
private fun gitPathKey(repoRelative: String): String =
    repoRelative.replace('\\', '/').trim().removePrefix("./")

private fun sameGitPath(a: String, b: String?): Boolean =
    b != null && gitPathKey(a) == gitPathKey(b)

private fun GitChange.toTreeStatus(): TreeStatus = when (this) {
    is GitChange.Added -> TreeStatus.Added
    is GitChange.Modified -> TreeStatus.Modified
    is GitChange.Deleted -> TreeStatus.Deleted
    is GitChange.Untracked -> TreeStatus.Untracked
}
