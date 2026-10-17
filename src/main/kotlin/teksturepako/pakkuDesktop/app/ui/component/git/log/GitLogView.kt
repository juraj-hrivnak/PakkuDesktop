/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git.log

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.SplitLayoutState
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalSplitLayout
import teksturepako.pakkuDesktop.app.ui.component.git.diff.DiffViewer
import teksturepako.pakkuDesktop.app.ui.driver.LocalGitLogPrep
import teksturepako.pakkuDesktop.app.ui.model.LogTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.animatedDividerStyle

@Composable
fun GitLogView(
    publish: (ModpackMsg) -> Unit,
    model: LogTabModel,
    modifier: Modifier = Modifier,
) {
    val graph = LocalGitLogPrep.current?.graph
    var branchesOpen by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val log = model.gitLog
    val branchesSplit = remember { SplitLayoutState(0.22f) }
    val detailsSplit = remember { SplitLayoutState(0.62f) }
    val diffSplit = remember { SplitLayoutState(0.68f) }
    // The drag handle is centered on the divider and covers this much of each pane.
    val splitDrag = 16.dp

    fun jumpToHash(hash: String) {
        val row = graph?.rowOf(hash) ?: return
        publish(ModpackMsg.GitLogCommitSelected(hash))
        scope.launch { listState.animateScrollToItem(row) }
    }

    fun jumpToRow(row: Int) {
        val shown = graph ?: return
        if (row !in 0 until shown.rows) return
        publish(ModpackMsg.GitLogCommitSelected(shown.hashAt(row)))
        scope.launch { listState.animateScrollToItem(row) }
    }

    Column(modifier.fillMaxSize()) {
        GitLogFilterBar(
            filters = log.filters,
            refs = model.refs,
            authors = graph?.authors.orEmpty(),
            branchesOpen = branchesOpen,
            onBranchesOpen = { branchesOpen = it },
            publish = publish,
        )
        Divider(Orientation.Horizontal)
        if (graph == null) {
            Text(
                "Reading history…",
                modifier = Modifier.padding(16.dp),
                color = JewelTheme.contentColor.copy(alpha = 0.6f),
            )
            return@Column
        }
        val history: @Composable () -> Unit = {
            val commits: @Composable () -> Unit = {
                if (graph.rows == 0) {
                    Text(
                        if (log.filters.text.isBlank() && log.filters.users.isEmpty() && log.filters.date == null && log.filters.branches == null)
                            "No commits yet." else "No commits match.",
                        modifier = Modifier.padding(16.dp),
                        color = JewelTheme.contentColor.copy(alpha = 0.6f),
                    )
                } else {
                    GitLogTable(
                        graph = graph,
                        subjects = log.subjects,
                        selectedHash = log.selectedHash,
                        busy = model.gitBusy,
                        listState = listState,
                        publish = publish,
                        onJump = ::jumpToRow,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (branchesOpen) {
                HorizontalSplitLayout(
                    state = branchesSplit,
                    dividerStyle = animatedDividerStyle(),
                    first = {
                        Box(Modifier.fillMaxSize().padding(end = splitDrag / 2)) {
                            GitBranchesPane(
                                refs = model.refs,
                                headHash = model.headHash,
                                currentBranch = model.currentBranchName,
                                busy = model.gitBusy,
                                onJump = ::jumpToHash,
                                onFilter = { name ->
                                    publish(
                                        ModpackMsg.GitLogFiltersChanged(
                                            log.filters.copy(branches = persistentSetOf(name)),
                                        ),
                                    )
                                },
                                publish = publish,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                    second = { commits() },
                    modifier = Modifier.fillMaxSize(),
                    firstPaneMinWidth = 160.dp,
                    secondPaneMinWidth = 240.dp,
                    draggableWidth = splitDrag,
                )
            } else {
                commits()
            }
        }
        val logBody: @Composable () -> Unit = {
            HorizontalSplitLayout(
                state = detailsSplit,
                dividerStyle = animatedDividerStyle(),
                first = {
                    Box(Modifier.fillMaxSize().padding(end = splitDrag / 2)) { history() }
                },
                second = {
                    GitLogDetails(
                        details = log.details,
                        containing = log.containingBranches,
                        selectedChange = log.selectedChange,
                        publish = publish,
                    )
                },
                modifier = Modifier.fillMaxSize(),
                firstPaneMinWidth = 280.dp,
                secondPaneMinWidth = 220.dp,
                draggableWidth = splitDrag,
            )
        }
        if (log.selectedChange != null) {
            VerticalSplitLayout(
                state = diffSplit,
                dividerStyle = animatedDividerStyle(),
                first = {
                    Box(Modifier.fillMaxSize().padding(bottom = splitDrag / 2)) { logBody() }
                },
                second = { DiffViewer(log.diff, Modifier.fillMaxSize()) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                firstPaneMinWidth = 160.dp,
                secondPaneMinWidth = 80.dp,
                draggableWidth = splitDrag,
            )
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) { logBody() }
        }
    }
}
