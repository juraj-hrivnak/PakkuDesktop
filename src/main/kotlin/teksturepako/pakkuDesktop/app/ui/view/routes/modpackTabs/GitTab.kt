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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakkuDesktop.app.ui.component.HorizontalBar
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pro.git.GitState
import teksturepako.pakkuDesktop.pro.ui.component.diff.DiffViewer
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitChange
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile

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

    Column(modifier) {
        HorizontalBar {
            Text(
                text = "SOURCE CONTROL",
                modifier = Modifier.padding(4.dp),
                style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp),
                color = JewelTheme.contentColor.copy(alpha = 0.55f),
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                enabled = files.isNotEmpty(),
                onClick = { publish(ModpackMsg.GitSelectAllChangedFiles) },
            ) {
                Text("Select all")
            }
            OutlinedButton(
                enabled = selectedCount > 0,
                onClick = { publish(ModpackMsg.GitClearChangedFileSelection) },
            ) {
                Text("Clear")
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
                items(files, key = { it.path }) { file ->
                    ChangeFileRow(
                        file = file,
                        included = gitState.selectedFiles.any { it.path == file.path },
                        isViewed = currentDiff?.newPath == file.path ||
                            currentDiff?.oldPath == file.path,
                        onIncludeToggled = { publish(ModpackMsg.GitFileSelectionToggled(file)) },
                        onOpenDiff = { publish(ModpackMsg.GitDiffFileSelected(file)) },
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

@Composable
private fun ChangeFileRow(
    file: GitFile,
    included: Boolean,
    isViewed: Boolean,
    onIncludeToggled: () -> Unit,
    onOpenDiff: () -> Unit,
) {
    val status = file.status
    val (label, statusColor) = when (status) {
        is GitChange.Added -> "A" to Color(0xFF50FA7B)
        is GitChange.Modified -> "M" to Color(0xFFFFB86C)
        is GitChange.Deleted -> "D" to Color(0xFFFF5555)
        is GitChange.Untracked -> "?" to JewelTheme.contentColor.copy(alpha = 0.55f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isViewed) Color(0xFF2F65CA).copy(alpha = 0.35f) else Color.Transparent)
            .clickable { onOpenDiff() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Checkbox(
            checked = included,
            onCheckedChange = { onIncludeToggled() },
        )
        Text(
            text = label,
            modifier = Modifier.width(18.dp),
            color = statusColor,
            style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp),
        )
        Text(
            text = file.path,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = JewelTheme.contentColor,
            style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
        )
    }
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
