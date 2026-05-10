/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffLine

/**
 * Single-column unified diff (like `git diff` / VS Code’s inline view): one row per line, no
 * dual-pane scroll synchronization (which was a major source of jank).
 */
@Composable
fun DiffViewer(
    currentDiff: DiffContent?,
    modifier: Modifier = Modifier,
) {
    if (currentDiff == null) {
        EmptyDiff(modifier)
        return
    }
    val diff = currentDiff
    val listState = rememberLazyListState()
    val gutterWidth = 44.dp
    val hunkHeaderBg = diffHunkHeaderBackground()
    val hunkHeaderFg = diffHunkHeaderForeground()

    SelectionContainer(modifier = modifier.fillMaxSize()) {
        LazyColumn(state = listState) {
            item(key = "header") {
                FileHeader(diff)
            }
            for ((hi, hunk) in diff.hunks.withIndex()) {
                item(key = "hunk-head-$hi") {
                    Text(
                        text = hunk.header,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(hunkHeaderBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = hunkHeaderFg,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
                items(
                    items = hunk.lines,
                    key = { line -> "$hi-${line.number.old}-${line.number.new}-${line.type}-${line.content.hashCode()}" },
                ) { line ->
                    UnifiedDiffRow(
                        line = line,
                        gutterWidth = gutterWidth,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedDiffRow(
    line: DiffLine,
    gutterWidth: androidx.compose.ui.unit.Dp,
) {
    val style = diffLineStyle(line.type)
    val prefix = style.prefix
    val lineBg = style.lineBackground
    val prefixColor = style.prefixColor
    val gutterColor = diffGutterLabelColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(lineBg)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = line.number.old?.toString() ?: "",
            modifier = Modifier.widthIn(min = gutterWidth).padding(horizontal = 6.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = gutterColor,
            maxLines = 1,
        )
        Text(
            text = line.number.new?.toString() ?: "",
            modifier = Modifier.widthIn(min = gutterWidth).padding(horizontal = 6.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = gutterColor,
            maxLines = 1,
        )
        Text(
            text = prefix,
            modifier = Modifier.widthIn(min = 16.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = prefixColor,
        )
        Text(
            text = line.content,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            style = JewelTheme.editorTextStyle,
            softWrap = false,
        )
    }
}
