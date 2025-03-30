package teksturepako.pakkupro.ui.view.children.modpackTabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkupro.ui.component.HorizontalBar
import teksturepako.pakkupro.ui.component.git.diff.DiffViewer
import teksturepako.pakkupro.ui.viewmodel.GitViewModel
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import teksturepako.pakkupro.ui.viewmodel.state.GitChange
import teksturepako.pakkupro.ui.viewmodel.state.GitFile
import kotlin.io.path.Path

@Composable
fun GitTab()
{
    val viewModel = GitViewModel
    val gitState by viewModel.state.collectAsState()

    LaunchedEffect(Unit)
    {
        viewModel.initialize(Path(workingPath))
    }

    Column(Modifier.fillMaxSize()) {
        HorizontalSplitLayout(
            state = ModpackViewModel.projectsTabSplitState,
            first = {
                Column {
                    HorizontalBar {
                        Text("Commit", Modifier.padding(4.dp))
                    }

                    Row {
                        ChangesPanel(
                            files = gitState.gitFiles,
                            selectedFiles = gitState.selectedFiles,
                            onFileSelect = viewModel::toggleFileSelection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        )
                    }
                }
            },
            second = {
                Column {
                    Row {
                        DiffViewer(
                            gitState.currentDiff, modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            firstPaneMinWidth = 200.dp,
            secondPaneMinWidth = 200.dp,
            draggableWidth = 16.dp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChangesPanel(
    files: List<GitFile>,
    selectedFiles: Set<String>,
    onFileSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
)
{
    val viewModel = GitViewModel

    FlowColumn(
        modifier = modifier
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
                        isSelected = selectedFiles.contains(file.status.path),
                        onSelect = { onFileSelect(file.status.path) }
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.Bottom
        ) {
            CommitPanel(
                onCommit = viewModel::commit,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FileRow(
    file: GitFile,
    isSelected: Boolean,
    onSelect: () -> Unit,
)
{
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (isSelected) Color(0xFF2F65CA) else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onSelect() }
        )

        Text(
            text = file.status.displayName, color = when (file.status)
            {
                is GitChange.Added       -> Color(0xFF50FA7B)
                is GitChange.Modified    -> Color(0xFFFFB86C)
                is GitChange.Deleted     -> Color(0xFFFF5555)
                is GitChange.Untracked   -> JewelTheme.contentColor
            }
        )
    }
}

@Composable
private fun CommitPanel(
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
)
{
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = "Commit Message", color = Color(0xFFBBBBBB)
        )

        val textFieldState = rememberTextFieldState()

        TextField(
            textFieldState,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(vertical = 8.dp),
            placeholder = { Text("Enter commit message...") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start
        ) {
            DefaultButton(
                onClick = onCommit,
            ) {
                Text( "Commit")
            }
        }
    }
}

