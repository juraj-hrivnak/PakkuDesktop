package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.rememberSplitLayoutState
import org.jetbrains.jewel.ui.component.styling.DividerMetrics
import org.jetbrains.jewel.ui.component.styling.DividerStyle
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent

@Composable
fun DiffViewer(
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
                                        key = { "${line.number.old}:${line.number.new ?: ""}" }) {
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
                                        key = { "${line.number.old ?: ""}:${line.number.new}" }) {
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
