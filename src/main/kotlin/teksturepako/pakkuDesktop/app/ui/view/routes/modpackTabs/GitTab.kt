/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakkuDesktop.app.ui.component.HorizontalBar
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
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

    val splitState = remember { SplitLayoutState(0.2F) }

    Column(Modifier.fillMaxSize()) {
        HorizontalSplitLayout(
            state = splitState,
            first = {
                Column {
                    HorizontalBar {
                        Text("Commit", Modifier.padding(4.dp))
                    }

                    Row {
                        ChangesPanel(
                            files = gitState.gitFiles,
                            selectedFiles = gitState.selectedFiles,
                            currentDiff = diffContent,
                            onFileSelect = { publish(ModpackMsg.GitFileSelectionToggled(it)) },
                            onFileView = { publish(ModpackMsg.GitDiffFileSelected(it)) },
                            publish = publish,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                        )
                    }
                }
            },
            second = {
                Column {
                    Row {
                        DiffViewer(
                            diffContent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            firstPaneMinWidth = 200.dp,
            secondPaneMinWidth = 200.dp,
            draggableWidth = 16.dp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChangesPanel(
    files: List<GitFile>,
    selectedFiles: Set<GitFile>,
    currentDiff: DiffContent?,
    onFileSelect: (GitFile) -> Unit,
    onFileView: (GitFile) -> Unit,
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowColumn(
        modifier = modifier,
    ) {
        Column {
            Text(
                text = "Changes",
                modifier = Modifier.padding(16.dp),
            )

            LazyColumn {
                items(files) { file ->
                    FileRow(
                        file = file,
                        isSelected = selectedFiles.contains(file),
                        isViewed = currentDiff?.newPath == file.status.path,
                        onSelect = { onFileSelect(file) },
                        onView = { onFileView(file) },
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.Bottom,
        ) {
            CommitPanel(
                publish = publish,
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FileRow(
    file: GitFile,
    isSelected: Boolean,
    isViewed: Boolean,
    onSelect: () -> Unit,
    onView: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (isViewed) Color(0xFF2F65CA) else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable { onView() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onSelect() },
        )

        Text(
            text = file.status.displayName,
            color = when (file.status) {
                is GitChange.Added -> Color(0xFF50FA7B)
                is GitChange.Modified -> Color(0xFFFFB86C)
                is GitChange.Deleted -> Color(0xFFFF5555)
                is GitChange.Untracked -> JewelTheme.contentColor
            },
        )
    }
}

@Composable
private fun CommitPanel(
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
    ) {
        Text(
            text = "Commit Message",
            color = Color(0xFFBBBBBB),
        )

        val textFieldState = rememberTextFieldState()

        LaunchedEffect(textFieldState.text) {
            publish(ModpackMsg.GitCommitMessageChanged(textFieldState.text.toString()))
        }

        TextField(
            textFieldState,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(vertical = 8.dp),
            placeholder = { Text("Enter commit message...") },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            DefaultButton(
                onClick = { publish(ModpackMsg.GitCommitRequested) },
            ) {
                Text("Commit")
            }
        }
    }
}
