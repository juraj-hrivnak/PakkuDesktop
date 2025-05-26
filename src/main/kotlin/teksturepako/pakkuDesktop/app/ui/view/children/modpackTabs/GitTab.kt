package teksturepako.pakkuDesktop.app.ui.view.children.modpackTabs

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.app.ui.component.HorizontalBar
import teksturepako.pakkuDesktop.pro.ui.component.diff.DiffViewer
import teksturepako.pakkuDesktop.pro.ui.viewmodel.GitDiffViewModel
import teksturepako.pakkuDesktop.pro.ui.viewmodel.GitViewModel
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitChange
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile
import kotlin.io.path.Path

@Composable
fun GitTab()
{
    val diffState by GitDiffViewModel.state.collectAsState()
    val gitState by GitViewModel.gitState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(gitState)
    {
        GitDiffViewModel.init(Path(workingPath))
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
                            currentDiff = diffState.currentDiff,
                            onFileSelect = {
                                coroutineScope.launch(Dispatchers.Main) {
                                    GitViewModel.toggleFileSelection(it)
                                }
                            },
                            onFileView = {
                                coroutineScope.launch(Dispatchers.Main) {
                                    GitDiffViewModel.selectDiff(it)
                                }
                            },
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
                            diffState.currentDiff, modifier = Modifier.weight(1f)
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
    selectedFiles: Set<GitFile>,
    currentDiff: DiffContent?,
    onFileSelect: (GitFile) -> Unit,
    onFileView: (GitFile) -> Unit,
    modifier: Modifier = Modifier,
)
{
    val coroutineScope = rememberCoroutineScope()

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
                        isSelected = selectedFiles.contains(file),
                        isViewed = currentDiff?.newPath == file.status.path,
                        onSelect = { onFileSelect(file) },
                        onView = { onFileView(file) }
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.Bottom
        ) {
            CommitPanel(
                onCommit = {
                    coroutineScope.launch(Dispatchers.IO) {
                        GitViewModel.commit()
                    }
                },
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
    isViewed: Boolean,
    onSelect: () -> Unit,
    onView: () -> Unit,
)
{
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (isViewed) Color(0xFF2F65CA) else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable { onView() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onSelect() }
        )

        Text(
            text = file.status.displayName, color = when (file.status)
            {
                is GitChange.Added     -> Color(0xFF50FA7B)
                is GitChange.Modified  -> Color(0xFFFFB86C)
                is GitChange.Deleted   -> Color(0xFFFF5555)
                is GitChange.Untracked -> JewelTheme.contentColor
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

        LaunchedEffect(textFieldState.text)
        {
            GitViewModel.updateCommitMessage(textFieldState.text.toString())
        }

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

