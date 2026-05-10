/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.component.HorizontalBar
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pro.git.GitState
import teksturepako.pakkuDesktop.pro.ui.component.diff.DiffViewer
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitChange
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile

/** Custom changelist tree (no Jewel LazyTree): indent → chevron → checkbox → status → label. */
private val GitChangesIndentPerLevel = 14.dp
private val GitChangesChevronSlot = 18.dp
private val GitChangesChevronGap = 4.dp
private val GitChangesCheckSlot = 26.dp
private val GitChangesStatusSlot = 20.dp
private val GitChangesRowMinHeight = 28.dp

@Composable
fun GitTab(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    val gitState = model.git
    val diffContent = model.gitCurrentDiff

    val splitState = remember { SplitLayoutState(0.32f) }

    Column(Modifier.fillMaxSize()) {
        HorizontalSplitLayout(
            state = splitState,
            first = {
                SourceControlSidePanel(
                    gitState = gitState,
                    currentDiff = diffContent,
                    publish = publish,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            second = {
                DiffViewer(
                    diffContent,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            firstPaneMinWidth = 260.dp,
            secondPaneMinWidth = 200.dp,
            draggableWidth = 16.dp,
        )
    }
}

@Composable
private fun SourceControlSidePanel(
    gitState: GitState,
    currentDiff: DiffContent?,
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
) {
    val files = gitState.gitFiles
    val selectedCount = gitState.selectedFiles.size

    val structureKey = remember(files) { gitChangesStructureKey(files) }
    val dirRoot = remember(structureKey) { buildChangesDirRoot(files) }
    val folderIds = remember(structureKey) { folderIdsForGitFiles(files) }
    val subtreeByFolder = remember(structureKey) { buildSubtreeFileIndex(files) }

    var openFolders by remember(structureKey) { mutableStateOf(folderIds.toSet()) }
    LaunchedEffect(structureKey) {
        openFolders = folderIds.toSet()
    }

    val flatRows = remember(dirRoot, openFolders) { visibleGitChangesRows(dirRoot, openFolders) }

    val toggleFolder: (String) -> Unit = { path ->
        openFolders = if (path in openFolders) openFolders - path else openFolders + path
    }

    Column(modifier) {
        HorizontalBar {
            Text(
                text = "SOURCE CONTROL",
                modifier = Modifier.padding(4.dp),
                style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp),
                color = JewelTheme.contentColor.copy(alpha = 0.55f),
            )
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    enabled = files.isNotEmpty(),
                    onClick = { publish(ModpackMsg.GitSelectAllChangedFiles) },
                ) {
                    Icon(
                        key = AllIconsKeys.Actions.Selectall,
                        contentDescription = "Select all",
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    enabled = selectedCount > 0,
                    onClick = { publish(ModpackMsg.GitClearChangedFileSelection) },
                ) {
                    Icon(
                        key = AllIconsKeys.Actions.Unselectall,
                        contentDescription = "Clear selection",
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    enabled = files.isNotEmpty() && folderIds.isNotEmpty(),
                    onClick = { openFolders = folderIds.toSet() },
                ) {
                    Icon(
                        key = AllIconsKeys.Actions.Expandall,
                        contentDescription = "Expand all",
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    enabled = files.isNotEmpty() && folderIds.isNotEmpty(),
                    onClick = { openFolders = emptySet() },
                ) {
                    Icon(
                        key = AllIconsKeys.Actions.Collapseall,
                        contentDescription = "Collapse all",
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Text(
            text = buildString {
                append("Changes")
                if (files.isNotEmpty()) append(" (${files.size})")
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
            color = JewelTheme.contentColor,
        )

        if (files.isEmpty()) {
            Text(
                text = "No local changes. Edits to tracked files and new files appear here.",
                modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
                style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
                color = JewelTheme.contentColor.copy(alpha = 0.55f),
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(flatRows, key = { row ->
                    when (row) {
                        is GitChangeFlatRow.Folder -> "d:${row.fullPath}"
                        is GitChangeFlatRow.File -> "f:${row.file.path}"
                    }
                }) { row ->
                    GitChangesListRow(
                        row = row,
                        gitState = gitState,
                        subtreeByFolder = subtreeByFolder,
                        currentDiff = currentDiff,
                        onToggleFolder = toggleFolder,
                        publish = publish,
                    )
                }
            }
        }

        CommitPanel(
            gitState = gitState,
            publish = publish,
            canCommit = selectedCount > 0 && gitState.commitMessage.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private sealed class GitChangeFlatRow {
    data class Folder(
        val fullPath: String,
        val displayName: String,
        val depth: Int,
        val expanded: Boolean,
    ) : GitChangeFlatRow()

    data class File(val file: GitFile, val depth: Int) : GitChangeFlatRow()
}

private fun visibleGitChangesRows(root: ChangesDirNode, openFolders: Set<String>): List<GitChangeFlatRow> =
    buildList {
        fun walk(dir: ChangesDirNode, depth: Int) {
            for ((name, sub) in dir.children) {
                val expanded = sub.fullPath in openFolders
                add(GitChangeFlatRow.Folder(sub.fullPath, name, depth, expanded))
                if (expanded) walk(sub, depth + 1)
            }
            for (f in dir.files.sortedBy { it.path }) {
                add(GitChangeFlatRow.File(f, depth))
            }
        }
        walk(root, 0)
    }

@Composable
private fun GitChangesListRow(
    row: GitChangeFlatRow,
    gitState: GitState,
    subtreeByFolder: Map<String, List<GitFile>>,
    currentDiff: DiffContent?,
    onToggleFolder: (String) -> Unit,
    publish: (ModpackMsg) -> Unit,
) {
    when (row) {
        is GitChangeFlatRow.Folder -> {
            val subtree = subtreeByFolder[row.fullPath].orEmpty()
            val selectedPaths = gitState.selectedFiles.map { it.path }.toSet()
            val selectedInSubtree = subtree.count { it.path in selectedPaths }
            val folderCheckState = when {
                subtree.isEmpty() -> ToggleableState.Off
                selectedInSubtree == 0 -> ToggleableState.Off
                selectedInSubtree == subtree.size -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GitChangesRowMinHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(GitChangesIndentPerLevel * row.depth))
                Box(
                    modifier = Modifier
                        .size(GitChangesChevronSlot)
                        .clickable { onToggleFolder(row.fullPath) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        key = if (row.expanded) AllIconsKeys.Gutter.FoldBottom else AllIconsKeys.Gutter.Fold,
                        contentDescription = if (row.expanded) "Collapse" else "Expand",
                        tint = JewelTheme.contentColor.copy(alpha = 0.85f),
                        hints = arrayOf(),
                        modifier = Modifier.size(12.dp),
                    )
                }
                Spacer(Modifier.width(GitChangesChevronGap))
                Box(
                    modifier = Modifier
                        .width(GitChangesCheckSlot)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    TriStateCheckbox(
                        state = folderCheckState,
                        onClick = { publish(ModpackMsg.GitFolderSelectionToggled(row.fullPath)) },
                    )
                }
                Spacer(Modifier.width(GitChangesStatusSlot))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleFolder(row.fullPath) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        key = AllIconsKeys.Nodes.Folder,
                        contentDescription = null,
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = row.displayName,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = JewelTheme.contentColor,
                        style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
                    )
                }
            }
        }

        is GitChangeFlatRow.File -> {
            val file = row.file
            val included = gitState.selectedFiles.any { it.path == file.path }
            val isViewed =
                currentDiff?.newPath == file.path || currentDiff?.oldPath == file.path
            val status = file.status
            val (label, statusColor) = when (status) {
                is GitChange.Added -> "A" to Color(0xFF50FA7B)
                is GitChange.Modified -> "M" to Color(0xFFFFB86C)
                is GitChange.Deleted -> "D" to Color(0xFFFF5555)
                is GitChange.Untracked -> "?" to JewelTheme.contentColor.copy(alpha = 0.55f)
            }
            val pathLabel = file.path.substringAfterLast('/').ifEmpty { file.path }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GitChangesRowMinHeight)
                    .background(
                        if (isViewed) Color(0xFF2F65CA).copy(alpha = 0.35f) else Color.Transparent,
                    )
                    .clickable { publish(ModpackMsg.GitDiffFileSelected(file)) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(GitChangesIndentPerLevel * row.depth))
                Spacer(Modifier.width(GitChangesChevronSlot))
                Spacer(Modifier.width(GitChangesChevronGap))
                Box(
                    modifier = Modifier
                        .width(GitChangesCheckSlot)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Checkbox(
                        checked = included,
                        onCheckedChange = { publish(ModpackMsg.GitFileSelectionToggled(file)) },
                    )
                }
                Box(
                    modifier = Modifier
                        .width(GitChangesStatusSlot)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = statusColor,
                        style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp),
                    )
                }
                Text(
                    text = pathLabel,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = JewelTheme.contentColor,
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
                )
            }
        }
    }
}

/** All changed files that live under each folder path (prefix), for tri-state checkboxes. */
private fun buildSubtreeFileIndex(files: List<GitFile>): Map<String, List<GitFile>> {
    val map = mutableMapOf<String, MutableList<GitFile>>()
    for (f in files) {
        val parts = f.path.split('/').filter { it.isNotEmpty() }
        if (parts.size <= 1) continue
        var acc = parts[0]
        map.getOrPut(acc) { mutableListOf() }.add(f)
        for (i in 1 until parts.lastIndex) {
            acc = "$acc/${parts[i]}"
            map.getOrPut(acc) { mutableListOf() }.add(f)
        }
    }
    return map.mapValues { (_, v) -> v.toList() }
}

private class ChangesDirNode(
    val fullPath: String,
    val children: MutableMap<String, ChangesDirNode> = sortedMapOf(),
    val files: MutableList<GitFile> = mutableListOf(),
)

private fun buildChangesDirRoot(files: List<GitFile>): ChangesDirNode {
    val root = ChangesDirNode("")
    for (file in files) {
        val parts = file.path.split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) continue
        var node = root
        var pathAcc = ""
        for (i in parts.indices) {
            val part = parts[i]
            if (i == parts.lastIndex) {
                node.files.add(file)
            } else {
                pathAcc = if (pathAcc.isEmpty()) part else "$pathAcc/$part"
                node = node.children.getOrPut(part) { ChangesDirNode(pathAcc) }
            }
        }
    }
    return root
}

private fun gitChangesStructureKey(files: List<GitFile>): String =
    files.joinToString("\u0000") { "${it.path}\u0001${it.status}" }

private fun folderIdsForGitFiles(files: List<GitFile>): Set<String> {
    val ids = linkedSetOf<String>()
    for (f in files) {
        val parts = f.path.split('/').filter { it.isNotEmpty() }
        if (parts.size <= 1) continue
        var acc = parts[0]
        ids.add(acc)
        for (i in 1 until parts.lastIndex) {
            acc = "$acc/${parts[i]}"
            ids.add(acc)
        }
    }
    return ids
}

@Composable
private fun CommitPanel(
    gitState: GitState,
    publish: (ModpackMsg) -> Unit,
    canCommit: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(12.dp),
    ) {
        Text(
            text = "Message",
            style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp),
            color = JewelTheme.contentColor.copy(alpha = 0.55f),
        )

        val textFieldState = rememberTextFieldState(gitState.commitMessage)

        LaunchedEffect(gitState.commitMessage) {
            val fieldText = textFieldState.text.toString()
            if (fieldText != gitState.commitMessage) {
                textFieldState.edit {
                    delete(0, length)
                    insert(0, gitState.commitMessage)
                }
            }
        }

        LaunchedEffect(textFieldState.text) {
            publish(ModpackMsg.GitCommitMessageChanged(textFieldState.text.toString()))
        }

        TextField(
            textFieldState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp, max = 120.dp)
                .padding(vertical = 6.dp),
            placeholder = { Text("Summary (required)") },
        )

        Text(
            text = "Commit only checked files (same idea as staging in VS Code or the Commit tool window in IntelliJ).",
            modifier = Modifier.padding(bottom = 8.dp),
            style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp),
            color = JewelTheme.contentColor.copy(alpha = 0.55f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            DefaultButton(
                enabled = canCommit,
                onClick = { publish(ModpackMsg.GitCommitRequested) },
            ) {
                Text("Commit")
            }
        }
    }
}
