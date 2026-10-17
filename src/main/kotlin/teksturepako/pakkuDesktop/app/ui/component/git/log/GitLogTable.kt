/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git.log

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.treeStyle
import teksturepako.pakkuDesktop.app.git.log.GitLogElement
import teksturepako.pakkuDesktop.app.git.log.GitLogGraph
import teksturepako.pakkuDesktop.app.git.log.GitLogLabel
import teksturepako.pakkuDesktop.app.integration.writeClipboardText
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.PkUiContextMenuArea
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val LogRowHeight = 22.dp
private val AuthorWidth = 140.dp
private val DateWidth = 148.dp

@Composable
fun GitLogTable(
    graph: GitLogGraph,
    subjects: Map<String, String>,
    selectedHash: String?,
    busy: Boolean,
    listState: LazyListState,
    publish: (ModpackMsg) -> Unit,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var hover by remember { mutableStateOf<GitLogElement?>(null) }
    val lit = remember(hover, graph) { hover?.let { graph.hover(it) }.orEmpty() }
    val selectedBg = JewelTheme.treeStyle.colors.backgroundSelected
    val content = JewelTheme.contentColor
    val panel = JewelTheme.globalColors.panelBackground

    LaunchedEffect(listState) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) IntRange.EMPTY else visible.first().index..visible.last().index
        }.distinctUntilChanged().collect { publish(ModpackMsg.GitLogVisibleRowsChanged(it)) }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val current = selectedHash?.let { graph.rowOf(it) } ?: 0
                val next = when (event.key) {
                    Key.DirectionDown -> current + 1
                    Key.DirectionUp -> current - 1
                    else -> return@onPreviewKeyEvent false
                }
                if (next !in 0 until graph.rows) return@onPreviewKeyEvent false
                publish(ModpackMsg.GitLogCommitSelected(graph.hashAt(next)))
                scope.launch { listState.animateScrollToItem(next) }
                true
            },
    ) {
        items(count = graph.rows, key = { graph.hashAt(it) }) { row ->
            val hash = graph.hashAt(row)
            GitLogTableRow(
                graph = graph,
                row = row,
                subject = subjects[hash] ?: hash.take(8),
                selected = hash == selectedHash,
                selectedBg = selectedBg,
                content = content,
                panel = panel,
                lit = lit,
                onSelect = { publish(ModpackMsg.GitLogCommitSelected(hash)) },
                onHover = { hover = it },
                onJump = onJump,
                menu = {
                    gitLogCommitMenu(graph, row, busy, publish, { text ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { writeClipboardText(text) }
                    }, onJump)
                },
            )
        }
    }
}

@Composable
private fun GitLogTableRow(
    graph: GitLogGraph,
    row: Int,
    subject: String,
    selected: Boolean,
    selectedBg: Color,
    content: Color,
    panel: Color,
    lit: Set<Int>,
    onSelect: () -> Unit,
    onHover: (GitLogElement?) -> Unit,
    onJump: (Int) -> Unit,
    menu: () -> List<androidx.compose.foundation.ContextMenuItem>,
) {
    val elements = graph.elements(row)
    val graphWidth = (graph.columns(row) * 16 + 2).dp
    val label = graph.labelAt(row)
    PkUiContextMenuArea(items = menu) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(LogRowHeight)
                .background(if (selected) selectedBg else Color.Transparent)
                .clickable(onClick = onSelect),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(graphWidth)
                    .fillMaxHeight()
                    .clipToBounds()
                    .pointerInput(elements, row) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Exit) {
                                    onHover(null)
                                    continue
                                }
                                val change = event.changes.firstOrNull() ?: continue
                                val hit = gitGraphHit(elements, change.position, size.height.toFloat())
                                onHover(hit)
                                if (event.type == PointerEventType.Press) {
                                    if (hit != null && !hit.node) {
                                        change.consume()
                                        val target = graph.clickTarget(hit)
                                        if (target != null) onJump(target)
                                    } else {
                                        onSelect()
                                    }
                                }
                            }
                        }
                    },
            ) {
                GitGraphRow(elements, lit, content, panel, Modifier.fillMaxSize())
            }
            Text(
                subject,
                modifier = Modifier.weight(1f).padding(end = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = content,
            )
            if (label != null) GitRefLabel(label, content)
            Text(
                graph.authorAt(row),
                modifier = Modifier.width(AuthorWidth).padding(horizontal = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = content.copy(alpha = 0.8f),
            )
            Text(
                formatCommitDate(graph.timeAt(row)),
                modifier = Modifier.width(DateWidth).padding(end = 8.dp),
                maxLines = 1,
                color = content.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun GitRefLabel(label: GitLogLabel, content: Color) {
    val icon = 13.dp
    val count = label.colors.size.coerceAtLeast(1)
    val width = icon + 2.dp * (count - 1)
    Row(
        Modifier
            .padding(end = 4.dp)
            .background(content.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Canvas(Modifier.width(width).height(icon)) {
            val size = this.size.height
            val step = size / 16f * 2f
            for (index in label.colors.lastIndex downTo 0) {
                drawRefTag(
                    color = Color(label.colors[index]),
                    background = content.copy(alpha = 0.08f),
                    offsetX = step * index,
                    size = size,
                    dot = index == label.colors.lastIndex,
                )
            }
        }
        if (label.text.isNotEmpty()) {
            val style = JewelTheme.defaultTextStyle
            Text(
                label.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = content,
                style = style.copy(fontSize = style.fontSize * 0.92f, fontWeight = FontWeight.Normal),
            )
        }
    }
}

private val shortDateTime: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

internal fun formatCommitDate(epochSeconds: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = java.time.Instant.ofEpochSecond(epochSeconds).atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val time = "%02d:%02d".format(dateTime.hour, dateTime.minute)
    return when (dateTime.toLocalDate()) {
        today -> "Today $time"
        today.minusDays(1) -> "Yesterday $time"
        else -> shortDateTime.format(dateTime)
    }
}
