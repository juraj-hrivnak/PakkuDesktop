/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import teksturepako.pakkuDesktop.app.git.*
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.button.ToolbarActionButton
import teksturepako.pakkuDesktop.app.ui.component.button.ToolbarSeparator
import teksturepako.pakkuDesktop.app.ui.component.button.ToolbarToggleButton
import teksturepako.pakkuDesktop.app.ui.component.context.GitFileRowContextMenu
import teksturepako.pakkuDesktop.app.ui.component.context.GitFolderRowContextMenu
import teksturepako.pakkuDesktop.app.ui.component.git.diff.DiffViewer
import teksturepako.pakkuDesktop.app.ui.component.modpack.git.GitFloatingActions
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FooterRow
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.FooterText
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.SelectionStatus
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeRow
import teksturepako.pakkuDesktop.app.ui.component.tree.StickyTreeFolderHeaders
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeFolderRow
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeItemRow
import teksturepako.pakkuDesktop.app.ui.driver.LocalGitChangelistPrep
import teksturepako.pakkuDesktop.app.ui.model.GitTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.elm.animatedDividerStyle
import java.nio.file.Path

@Composable
fun GitTab(
    publish: (ModpackMsg) -> Unit,
    model: GitTabModel,
    modpackRoot: Path?,
) {
    RecomposeProbe("GitTab")
    val gitState = model.git
    val diffContent = model.gitCurrentDiff
    val files = gitState.gitFiles
    val selectedCount = gitState.selectedFiles.size
    val grouped = model.groupUnversioned
    // Identity first; equal content is just as valid and must never leave the spinner up.
    val changelistPrep = LocalGitChangelistPrep.current
        ?.takeIf { it.grouped == grouped && (it.files === files || it.files == files) }
    // Empty shell while the driver builds the tree off the UI thread. Do not walk [files] here.
    val changelistSnapshot = changelistPrep?.snapshot
        ?: remember(grouped) { gitChangelistUiSnapshot(emptyList(), grouped) }

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
                gitState = gitState,
                busy = model.gitBusy,
                hasFolders = changelistSnapshot.folderIds.isNotEmpty(),
                grouped = grouped,
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
                            changelistSnapshot = changelistSnapshot,
                            treeReady = changelistPrep != null || files.isEmpty(),
                            grouped = grouped,
                            currentDiff = diffContent,
                            diffPendingPath = diffPendingPath,
                            modpackRoot = modpackRoot,
                            gitBusy = model.gitBusy,
                            filterText = model.gitFilterText,
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
            status = model.pendingGitOp?.status,
            publish = publish,
        )
    }
}

@Composable
private fun SourceControlSidePanel(
    gitState: GitState,
    changelistSnapshot: GitChangelistUiSnapshot,
    treeReady: Boolean,
    grouped: Boolean,
    currentDiff: DiffContent?,
    diffPendingPath: String?,
    modpackRoot: Path?,
    gitBusy: Boolean,
    filterText: String,
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
) {
    val files = gitState.gitFiles

    val selectedPaths = remember(gitState.selectedFiles, grouped) {
        gitState.selectedFiles.map { gitTreePath(it, grouped) }.toSet()
    }
    val allRows = remember(changelistSnapshot, gitState.expandedFolderPaths, selectedPaths) {
        changelistSnapshot.flatRows(gitState.expandedFolderPaths, selectedPaths)
    }
    val flatRows = remember(allRows, filterText) {
        if (filterText.isBlank()) allRows else filterChangelistRows(allRows, filterText)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Changes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            val textFieldState = rememberTextFieldState(filterText)
            LaunchedEffect(textFieldState.text) {
                val text = textFieldState.text.toString()
                if (text != filterText) {
                    publish(ModpackMsg.GitFilterTextChanged(text))
                }
            }
            TextField(
                textFieldState,
                Modifier
                    .weight(1f)
                    .height(28.dp),
                placeholder = {
                    Text("Search files…", color = JewelTheme.contentColor.copy(alpha = 0.5f))
                },
            )
        }

        Divider(Orientation.Horizontal)

        if (!treeReady) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (files.isEmpty()) {
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
                if (flatRows.isEmpty() && filterText.isNotBlank()) {
                    Text(
                        text = "No files match \"$filterText\"",
                        modifier = Modifier
                            .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 18.dp)
                            .fillMaxWidth(),
                        style = JewelTheme.defaultTextStyle,
                        color = JewelTheme.contentColor.copy(alpha = 0.55f),
                    )
                } else {
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
                                    modpackRoot = modpackRoot,
                                    gitBusy = gitBusy,
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
                            modpackRoot = modpackRoot,
                            gitBusy = gitBusy,
                            publish = publish,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter [rows] so that only items whose path contains [query] (case-insensitive) remain,
 * along with the folder rows that are ancestors of at least one matching item.
 */
internal fun filterChangelistRows(rows: List<PathTreeRow<GitFile>>, query: String): List<PathTreeRow<GitFile>> {
    val q = query.lowercase()
    // Find all matching file paths
    val matchingPaths = rows.filterIsInstance<PathTreeRow.Item<GitFile>>()
        .filter { it.path.lowercase().contains(q) }
        .mapTo(HashSet()) { it.path }

    if (matchingPaths.isEmpty()) return emptyList()

    // Collect folder paths that are ancestors of at least one matching file
    val keepFolders = HashSet<String>()
    for (path in matchingPaths) {
        val parts = path.split('/').filter { it.isNotEmpty() }
        var acc = parts[0]
        keepFolders.add(acc)
        for (i in 1 until parts.lastIndex) {
            acc = "$acc/${parts[i]}"
            keepFolders.add(acc)
        }
    }

    return rows.filter { row ->
        when (row) {
            is PathTreeRow.Item -> row.path in matchingPaths
            is PathTreeRow.Folder -> row.fullPath in keepFolders
        }
    }
}

@Composable
private fun ChangelistToolbar(
    gitState: GitState,
    busy: Boolean,
    hasFolders: Boolean,
    grouped: Boolean,
    publish: (ModpackMsg) -> Unit,
) {
    val filesEmpty = gitState.gitFiles.isEmpty()
    val selectedCount = gitState.selectedFiles.size
    val totalCount = gitState.gitFiles.size
    val hasTracked = gitState.gitFiles.any { it.status !is GitChange.Untracked }
    val selectedUnversioned = gitState.selectedFiles
        .filter { it.status is GitChange.Untracked }
        .map { it.path }
        .sorted()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarActionButton(
            key = AllIconsKeys.Actions.Refresh,
            label = "Refresh",
            enabled = !busy,
            onClick = { publish(ModpackMsg.GitOpRequested(GitOp.Refresh)) },
        )
        ToolbarActionButton(
            key = AllIconsKeys.Actions.Rollback,
            label = "Rollback…",
            enabled = !busy && hasTracked,
            onClick = { publish(ModpackMsg.ShowGitDialog(GitDialog.Rollback)) },
        )
        ToolbarActionButton(
            key = AllIconsKeys.Vcs.ShelveSilent,
            label = "Stash changes…",
            enabled = !busy && !filesEmpty,
            onClick = { publish(ModpackMsg.ShowGitDialog(GitDialog.Stash)) },
        )
        ToolbarActionButton(
            key = AllIconsKeys.Vcs.Ignore_file,
            label = "Add selected unversioned files to .gitignore",
            enabled = !busy && selectedUnversioned.isNotEmpty(),
            onClick = { publish(ModpackMsg.GitOpRequested(GitOp.Ignore(selectedUnversioned))) },
        )
        ToolbarSeparator()
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
        ToolbarSeparator()
        ToolbarToggleButton(
            key = AllIconsKeys.Vcs.ShowUnversionedFiles,
            label = if (grouped) "Unversioned files: separate group" else "Unversioned files: mixed with changes",
            value = grouped,
            onValueChange = { publish(ModpackMsg.GitGroupUnversionedChanged(it)) },
        )
    }
}

@Composable
private fun GitFooterStatus(
    selectedCount: Int,
    totalCount: Int,
    status: String?,
    publish: (ModpackMsg) -> Unit,
) {
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
    modpackRoot: Path?,
    gitBusy: Boolean,
    publish: (ModpackMsg) -> Unit,
) {
    when (row) {
        is PathTreeRow.Folder -> {
            val group = GitChangeGroup.ofFolder(row.fullPath)
            GitFolderRowContextMenu(
                folderPath = row.fullPath,
                expanded = row.expanded,
                publish = publish,
                modifier = Modifier.fillMaxWidth(),
            ) {
            TreeFolderRow(
                modifier = Modifier.fillMaxWidth(),
                row = row,
                onToggleExpanded = {
                    publish(ModpackMsg.GitChangelistFolderExpansionToggled(row.fullPath))
                },
                onToggleSelected = {
                    publish(ModpackMsg.GitFolderSelectionToggled(row.fullPath))
                },
                label = group?.label ?: row.displayName,
                icon = when (group) {
                    GitChangeGroup.Changes -> AllIconsKeys.Vcs.Changelist
                    GitChangeGroup.Unversioned -> AllIconsKeys.Vcs.ShowUnversionedFiles
                    null -> AllIconsKeys.Nodes.Folder
                },
            ) {
                if (group != null) {
                    Text(
                        if (row.subtreeItemCount == 1) "1 file" else "${row.subtreeItemCount} files",
                        color = JewelTheme.contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            }
        }
        is PathTreeRow.Item -> {
            val file = row.item
            val isViewed =
                sameGitPath(file.path, diffPendingPath) ||
                    sameGitPath(file.path, currentDiff?.newPath) ||
                    sameGitPath(file.path, currentDiff?.oldPath)
            val pathLabel = file.path.substringAfterLast('/').ifEmpty { file.path }
            GitFileRowContextMenu(
                file = file,
                publish = publish,
                busy = gitBusy,
                modpackRoot = modpackRoot,
                modifier = Modifier.fillMaxWidth(),
            ) {
            TreeItemRow(
                modifier = Modifier.fillMaxWidth(),
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
}

/** Normalize repo-relative paths from JGit vs unified diff headers (slashes, `./`). */
private fun gitPathKey(repoRelative: String): String =
    repoRelative.replace('\\', '/').trim().removePrefix("./")

private fun sameGitPath(a: String, b: String?): Boolean =
    b != null && gitPathKey(a) == gitPathKey(b)
