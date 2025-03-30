package teksturepako.pakkupro.ui.view.children.modpackTabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.component.styling.DividerMetrics
import org.jetbrains.jewel.ui.component.styling.DividerStyle
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkupro.ui.component.HorizontalBar
import teksturepako.pakkupro.ui.viewmodel.GitViewModel
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import teksturepako.pakkupro.ui.viewmodel.state.*
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
    val gitState by viewModel.state.collectAsState()

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
                commitMessage = gitState.commitMessage,
                onMessageChange = viewModel::updateCommitMessage,
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
    commitMessage: String,
    onMessageChange: (String) -> Unit,
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

@Composable
private fun DiffViewer(
    currentDiff: DiffContent?,
    modifier: Modifier = Modifier,
)
{
    val diff = currentDiff ?: return EmptyDiff()

    val leftScrollState = rememberLazyListState()
    val rightScrollState = rememberLazyListState()
    val layoutCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Sync scroll states
    LaunchedEffect(leftScrollState.firstVisibleItemScrollOffset) {
        if (leftScrollState.firstVisibleItemScrollOffset != rightScrollState.firstVisibleItemScrollOffset)
        {
            rightScrollState.scrollToItem(
                leftScrollState.firstVisibleItemIndex, leftScrollState.firstVisibleItemScrollOffset
            )
        }
    }

    LaunchedEffect(rightScrollState.firstVisibleItemScrollOffset) {
        if (rightScrollState.firstVisibleItemScrollOffset != leftScrollState.firstVisibleItemScrollOffset)
        {
            leftScrollState.scrollToItem(
                rightScrollState.firstVisibleItemIndex, rightScrollState.firstVisibleItemScrollOffset
            )
        }
    }

    val splitLayoutState = rememberSplitLayoutState(0.5f)

    Box(modifier = modifier) {
        HorizontalSplitLayout(
            dividerStyle = DividerStyle(
                color = JewelTheme.globalColors.borders.normal, metrics = DividerMetrics(20.dp, 10.dp)
            ),
            draggableWidth = 20.dp,
            state = splitLayoutState,
            firstPaneMinWidth = 100.dp,
            secondPaneMinWidth = 100.dp,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    layoutCoordinates.value = coordinates
                },
            first = {
                SelectionContainer(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(state = leftScrollState) {
                        diff.let { diffContent ->
                            item {
                                DisableSelection {
                                    FileHeader(diffContent)
                                }
                            }

                            for (hunk in diffContent.hunks)
                            {
                                for ((lineIndex, line) in hunk.lines.withIndex())
                                {
                                    if (line.number.old == null)
                                    {
                                        continue
                                    }

                                    item(
                                        key = { "${line.number.old ?: ""}:${line.number.new ?: ""}" }) {
                                        DiffLineLeft(line, hunk)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            second = {
                SelectionContainer(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(state = rightScrollState) {
                        diff.let { diffContent ->
                            item {
                                DisableSelection {
                                    FileHeader(diffContent)
                                }
                            }

                            for (hunk in diffContent.hunks)
                            {
                                for ((lineIndex, line) in hunk.lines.withIndex())
                                {
                                    if (line.number.new == null)
                                    {
                                        continue
                                    }

                                    item(
                                        key = { "${line.number.old ?: ""}:${line.number.new ?: ""}" }) {
                                        DiffLineRight(line, hunk)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

private data class Change(
    val startIndex: Int,
    val endIndex: Int
)

private fun findWordDifferences(str1: String, str2: String): List<Change> {
    val changes = mutableListOf<Change>()

    // Find the common prefix
    var i = 0
    while (i < str1.length && i < str2.length && str1[i] == str2[i]) {
        i++
    }

    // Find the common suffix
    var j = str1.length - 1
    var k = str2.length - 1
    while (j > i && k > i && str1[j] == str2[k]) {
        j--
        k--
    }

    // If there's a difference, mark it
    if (i <= j) {
        changes.add(Change(i, j + 1))
    }

    return changes
}

private fun highlightChanges(line: DiffLine, otherLines: List<DiffLine>): AnnotatedString = buildAnnotatedString {
    if (line.type == DiffType.UNCHANGED)
    {
        append(line.content)
        return@buildAnnotatedString
    }

    // Keep any leading whitespace
    val leadingWhitespace = line.content.takeWhile { it.isWhitespace() }

    if (leadingWhitespace.isNotEmpty())
    {
        append(leadingWhitespace)
    }

    val content = line.content.substring(leadingWhitespace.length)

    when (line.type)
    {
        DiffType.DELETED ->
        {
            // Find the most similar added line
            val addedLine = otherLines
                .filter { it.type == DiffType.ADDED }
                .minByOrNull { levenshteinDistance(content, it.content) }

            if (addedLine != null)
            {
                val changes = findWordDifferences(content, addedLine.content)
                var lastIndex = 0

                for (change in changes)
                {
                    // Append unchanged part
                    append(content.substring(lastIndex, change.startIndex))

                    // Append changed part with highlight
                    withStyle(SpanStyle(
                        color = Color(0xFFFF5555), background = Color(0x33FF5555)
                    )) {
                        append(content.substring(change.startIndex, change.endIndex))
                    }

                    lastIndex = change.endIndex
                }

                // Append remaining unchanged part
                if (lastIndex < content.length)
                {
                    append(content.substring(lastIndex))
                }
            }
            else
            {
                withStyle(SpanStyle(color = Color(0xFFFF5555))) {
                    append(content)
                }
            }

            return@buildAnnotatedString
        }
        DiffType.ADDED ->
        {
            // Find the most similar deleted line
            val deletedLine = otherLines
                .filter { it.type == DiffType.DELETED }
                .minByOrNull { levenshteinDistance(content, it.content) }

            if (deletedLine != null)
            {
                val changes = findWordDifferences(content, deletedLine.content)
                var lastIndex = 0

                for (change in changes)
                {
                    // Append unchanged part
                    append(content.substring(lastIndex, change.startIndex))

                    // Append changed part with highlight
                    withStyle(SpanStyle(
                        color = Color(0xFF50FA7B), background = Color(0x3350FA7B)
                    )) {
                        append(content.substring(change.startIndex, change.endIndex))
                    }

                    lastIndex = change.endIndex
                }

                // Append remaining unchanged part
                if (lastIndex < content.length)
                {
                    append(content.substring(lastIndex))
                }
            }
            else
            {
                withStyle(SpanStyle(color = Color(0xFF50FA7B))) {
                    append(content)
                }
            }

            return@buildAnnotatedString
        }
        else -> append(content)
    }
}

// Helper function to calculate string similarity
private fun levenshteinDistance(str1: String, str2: String): Int {
    val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }

    for (i in 0..str1.length) {
        for (j in 0..str2.length) {
            when {
                i == 0 -> dp[i][j] = j
                j == 0 -> dp[i][j] = i
                else -> {
                    dp[i][j] = minOf(
                        dp[i - 1][j - 1] + if (str1[i - 1] == str2[j - 1]) 0 else 1,
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1
                    )
                }
            }
        }
    }

    return dp[str1.length][str2.length]
}

@Composable
private fun DiffLineLeft(
    line: DiffLine,
    hunk: DiffHunk,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(
                when (line.type)
                {
                    DiffType.DELETED -> Color(0xFF40313D).copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
    ) {
        DisableSelection {
            LineNumber(number = line.number.old)
        }

        Text(
            text = when (line.type)
            {
                DiffType.ADDED -> AnnotatedString("")
                else -> highlightChanges(line, hunk.lines)
            },
            style = JewelTheme.editorTextStyle,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun DiffLineRight(
    line: DiffLine,
    hunk: DiffHunk,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(
                when (line.type)
                {
                    DiffType.ADDED -> Color(0xFF294436).copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
    ) {
        DisableSelection {
            LineNumber(number = line.number.new)
        }

        Text(
            text = when (line.type)
            {
                DiffType.DELETED -> AnnotatedString("")
                else -> highlightChanges(line, hunk.lines)
            },
            style = JewelTheme.editorTextStyle,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun LineNumber(number: Int?)
{
    Box(modifier = Modifier.width(50.dp)) {
        Text(
            text = number?.toString()?.padStart(4) ?: "    ",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
            color = Color(0xFF808080)
        )
    }
}

@Composable
private fun FileHeader(diffContent: DiffContent)
{
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = diffContent.newPath,
            color = Color(0xFF4A7A94),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )

        if (diffContent.oldPath != null && diffContent.oldPath != diffContent.newPath)
        {
            Text(
                text = "(was: ${diffContent.oldPath})",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EmptyDiff()
{
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Select a file to view its changes",
        )
    }
}