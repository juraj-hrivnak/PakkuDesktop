/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffType

@Composable
internal fun DiffTextArea(
    diff: DiffContent,
    modifier: Modifier = Modifier,
) {
    val rendered = remember(diff) { buildRenderedDiff(diff) }
    val state    = remember(rendered.sourceText) { TextFieldState(rendered.sourceText) }
    val scrollState = rememberScrollState()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val editorStyle = JewelTheme.editorTextStyle.copy(
        fontFamily = FontFamily.Monospace,
        color      = JewelTheme.contentColor,
    )
    val gutterFg    = diffGutterLabelColor()
    val gutterStyle = editorStyle.copy(color = gutterFg)

    val addedStyle     = diffLineStyle(DiffType.ADDED)
    val deletedStyle   = diffLineStyle(DiffType.DELETED)
    val unchangedStyle = diffLineStyle(DiffType.UNCHANGED)
    val hunkBg         = diffHunkHeaderBackground()
    val headerFg       = diffHunkHeaderForeground()

    val density = LocalDensity.current

    val lineNumTextWidthPx: Float = remember(gutterStyle) {
        textMeasurer.measure("99999", style = gutterStyle, maxLines = 1).size.width.toFloat()
    }
    val prefixTextWidthPx: Float = remember(editorStyle) {
        textMeasurer.measure("+", style = editorStyle, maxLines = 1).size.width.toFloat()
    }

    val colPadPx: Float       = with(density) { 6.dp.toPx() }
    val lineNumColWidthPx     = colPadPx + lineNumTextWidthPx + colPadPx
    val prefixColWidthPx      = colPadPx + prefixTextWidthPx  + colPadPx
    val contentIndentPx       = 2f * lineNumColWidthPx + prefixColWidthPx
    val contentIndent         = with(density) { contentIndentPx.toDp().toSp() }

    Box(modifier = modifier.fillMaxSize())
    {
        Canvas(modifier = Modifier.matchParentSize())
        {
            val layout  = textLayoutResult ?: return@Canvas
            val scrollY = scrollState.value.toFloat()
            val textLen = layout.layoutInput.text.length

            for (line in rendered.lines)
            {
                val startOff        = line.startOffset.coerceAtMost(textLen)
                val startLayoutLine = layout.getLineForOffset(startOff)

                val endOff = (line.endOffsetExclusive - 1)
                    .coerceAtLeast(line.startOffset)
                    .coerceAtMost(textLen)
                val endLayoutLine = layout.getLineForOffset(endOff)

                val top    = layout.getLineTop(startLayoutLine)  - scrollY
                val bottom = layout.getLineBottom(endLayoutLine) - scrollY
                if (bottom <= 0f || top >= size.height) continue

                val bg = when (line.kind)
                {
                    DiffRenderedLineKind.ADDED       -> addedStyle.lineBackground
                    DiffRenderedLineKind.DELETED     -> deletedStyle.lineBackground
                    DiffRenderedLineKind.UNCHANGED   -> unchangedStyle.lineBackground
                    DiffRenderedLineKind.HUNK_HEADER -> hunkBg
                }
                drawRect(color = bg, topLeft = Offset(0f, top), size = Size(size.width, bottom - top))

                val baselineY = layout.getLineBaseline(startLayoutLine) - scrollY

                if (line.kind == DiffRenderedLineKind.HUNK_HEADER)
                {
                    val l = textMeasurer.measure(line.hunkHeader, style = gutterStyle.copy(color = headerFg), maxLines = 1)
                    drawText(l, topLeft = Offset(colPadPx, baselineY - l.getLineBaseline(0)))
                }
                else
                {
                    val oldText = line.oldNum?.toString() ?: ""
                    if (oldText.isNotEmpty())
                    {
                        val l = textMeasurer.measure(oldText, style = gutterStyle, maxLines = 1)
                        drawText(l, topLeft = Offset(lineNumColWidthPx - colPadPx - l.size.width, baselineY - l.getLineBaseline(0)))
                    }

                    val newText = line.newNum?.toString() ?: ""
                    if (newText.isNotEmpty())
                    {
                        val l = textMeasurer.measure(newText, style = gutterStyle, maxLines = 1)
                        drawText(l, topLeft = Offset(2f * lineNumColWidthPx - colPadPx - l.size.width, baselineY - l.getLineBaseline(0)))
                    }

                    if (line.prefix.isNotBlank())
                    {
                        val prefixColor = when (line.kind)
                        {
                            DiffRenderedLineKind.ADDED   -> addedStyle.prefixColor
                            DiffRenderedLineKind.DELETED -> deletedStyle.prefixColor
                            else                         -> gutterFg
                        }
                        val l = textMeasurer.measure(line.prefix, style = editorStyle.copy(color = prefixColor), maxLines = 1)
                        drawText(l, topLeft = Offset(2f * lineNumColWidthPx + (prefixColWidthPx - l.size.width) / 2f, baselineY - l.getLineBaseline(0)))
                    }
                }
            }
        }

        BasicTextField(
            state        = state,
            readOnly     = true,
            lineLimits   = TextFieldLineLimits.MultiLine(),
            scrollState  = scrollState,
            cursorBrush  = SolidColor(Color.Transparent),
            onTextLayout = { getResult -> textLayoutResult = getResult() },
            textStyle    = editorStyle.copy(
                textIndent = TextIndent(firstLine = contentIndent, restLine = contentIndent),
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}


private enum class DiffRenderedLineKind
{
    HUNK_HEADER,
    ADDED,
    DELETED,
    UNCHANGED,
}

private data class RenderedDiffLine(
    val kind: DiffRenderedLineKind,
    val startOffset: Int,
    val endOffsetExclusive: Int,
    val hunkHeader: String = "",
    val oldNum: Int? = null,
    val newNum: Int? = null,
    val prefix: String = " ",
)

private data class RenderedDiff(
    val sourceText: String,
    val lines: List<RenderedDiffLine>,
)


private fun buildRenderedDiff(diff: DiffContent): RenderedDiff
{
    val sb    = StringBuilder()
    val lines = ArrayList<RenderedDiffLine>(diff.hunks.sumOf { it.lines.size + 1 })

    for (hunk in diff.hunks)
    {
        val hunkStart = sb.length
        sb.append('\n')
        lines += RenderedDiffLine(
            kind              = DiffRenderedLineKind.HUNK_HEADER,
            startOffset       = hunkStart,
            endOffsetExclusive = sb.length,
            hunkHeader        = hunk.header,
        )

        for (line in hunk.lines)
        {
            val lineStart = sb.length
            sb.append(line.content)
            sb.append('\n')
            lines += RenderedDiffLine(
                kind              = line.type.toKind(),
                startOffset       = lineStart,
                endOffsetExclusive = sb.length,
                oldNum            = line.number.old,
                newNum            = line.number.new,
                prefix            = line.type.toPrefix(),
            )
        }
    }

    return RenderedDiff(sourceText = sb.toString(), lines = lines)
}

private fun DiffType.toKind(): DiffRenderedLineKind = when (this) {
    DiffType.ADDED     -> DiffRenderedLineKind.ADDED
    DiffType.DELETED   -> DiffRenderedLineKind.DELETED
    DiffType.UNCHANGED -> DiffRenderedLineKind.UNCHANGED
}

private fun DiffType.toPrefix(): String = when (this) {
    DiffType.ADDED     -> "+"
    DiffType.DELETED   -> "-"
    DiffType.UNCHANGED -> " "
}

