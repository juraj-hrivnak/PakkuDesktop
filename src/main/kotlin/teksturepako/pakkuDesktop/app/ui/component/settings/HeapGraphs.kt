/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import teksturepako.pakkuDesktop.app.data.HeapHistogram
import teksturepako.pakkuDesktop.app.data.HeapSample
import teksturepako.pakkuDesktop.app.data.MAX_HEAP_CHART_BARS
import teksturepako.pakkuDesktop.app.data.decodeJvmTypeName
import teksturepako.pakkuDesktop.app.data.formatHeapAxis
import teksturepako.pakkuDesktop.app.data.formatHeapBytes
import teksturepako.pakkuDesktop.app.data.formatHeapChartAge
import teksturepako.pakkuDesktop.app.data.formatHeapShare
import teksturepako.pakkuDesktop.app.data.heapAxisTicks
import teksturepako.pakkuDesktop.app.data.shortClassName
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.button.CopyToClipboardButton

@Composable
fun HeapHistoryChart(samples: List<HeapSample>, modifier: Modifier = Modifier) {
    val rssColor = PakkuDesktopConstants.highlightColor
    val heapColor = PakkuDesktopConstants.green
    val gridColor = JewelTheme.contentColor.copy(alpha = 0.10f)
    val axisColor = JewelTheme.contentColor.copy(alpha = 0.28f)
    val fillColor = rssColor.copy(alpha = 0.16f)
    val axisStyle = JewelTheme.consoleTextStyle.merge(
        TextStyle(color = JewelTheme.contentColor.copy(alpha = 0.50f), fontSize = 11.sp),
    )
    val legendStyle = heapTextStyle(JewelTheme.contentColor.copy(alpha = 0.55f))
    val textMeasurer = rememberTextMeasurer()
    val showHeap = samples.any { it.heapUsedBytes > 0L }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(176.dp)) {
            if (samples.isEmpty()) return@Canvas

            val nowMs = samples.last().epochMs
            val yMaxData = samples.maxOf { sample ->
                maxOf(sample.totalBytes, sample.heapUsedBytes)
            }
            val ticks = heapAxisTicks(yMaxData)
            val yMax = ticks.last().toFloat().coerceAtLeast(1f)
            val yLayouts = ticks.map { tick ->
                tick to textMeasurer.measure(formatHeapAxis(tick), style = axisStyle, maxLines = 1)
            }
            val t0 = samples.first().epochMs
            val t1 = samples.last().epochMs
            val tSpan = (t1 - t0).coerceAtLeast(1L)
            val xTimes = listOf(t0, t0 + tSpan / 2, t1).distinct()
            val xLayouts = xTimes.map { t ->
                t to textMeasurer.measure(formatHeapChartAge(t, nowMs), style = axisStyle, maxLines = 1)
            }

            val left = yLayouts.maxOf { it.second.size.width } + 10f
            val right = size.width - 8f
            val top = 10f
            val bottom = size.height - (xLayouts.maxOf { it.second.size.height } + 8f)
            val width = (right - left).coerceAtLeast(1f)
            val height = (bottom - top).coerceAtLeast(1f)

            fun xAt(epochMs: Long) = left + width * (epochMs - t0).toFloat() / tSpan
            fun yAt(bytes: Long) = bottom - height * (bytes.toFloat() / yMax).coerceIn(0f, 1f)

            yLayouts.forEach { (tick, layout) ->
                val y = yAt(tick)
                drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1f)
                drawText(
                    layout,
                    topLeft = Offset(left - 8f - layout.size.width, y - layout.size.height / 2f),
                )
            }
            xLayouts.forEach { (t, layout) ->
                val x = xAt(t)
                drawLine(gridColor, Offset(x, top), Offset(x, bottom), strokeWidth = 1f)
                val labelX = (x - layout.size.width / 2f).coerceIn(left, right - layout.size.width)
                drawText(layout, topLeft = Offset(labelX, bottom + 6f))
            }

            drawLine(axisColor, Offset(left, top), Offset(left, bottom), strokeWidth = 1.2f)
            drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.2f)

            drawSeries(
                samples = samples,
                xAt = ::xAt,
                yAt = { sample -> yAt(sample.totalBytes) },
                color = rssColor,
                fill = fillColor,
                stroke = 2.4f,
                plotBottom = bottom,
            )
            if (showHeap) {
                drawSeries(
                    samples = samples,
                    xAt = ::xAt,
                    yAt = { sample -> yAt(sample.heapUsedBytes) },
                    color = heapColor,
                    fill = null,
                    stroke = 1.8f,
                    plotBottom = bottom,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ChartLegendSwatch(rssColor)
            BasicText("RSS", style = legendStyle)
            if (showHeap) {
                ChartLegendSwatch(heapColor)
                BasicText("Java heap", style = legendStyle)
            }
        }
    }
}

@Composable
private fun ChartLegendSwatch(color: Color) {
    Canvas(Modifier.size(10.dp, 3.dp)) {
        drawLine(color, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = size.height)
    }
}

private fun DrawScope.drawSeries(
    samples: List<HeapSample>,
    xAt: (Long) -> Float,
    yAt: (HeapSample) -> Float,
    color: Color,
    fill: Color?,
    stroke: Float,
    plotBottom: Float,
) {
    if (samples.size == 1) {
        drawCircle(color, radius = 3.5f, center = Offset(xAt(samples[0].epochMs), yAt(samples[0])))
        return
    }
    val line = Path()
    val area = Path()
    samples.forEachIndexed { i, sample ->
        val x = xAt(sample.epochMs)
        val y = yAt(sample)
        if (i == 0) {
            line.moveTo(x, y)
            area.moveTo(x, plotBottom)
            area.lineTo(x, y)
        } else {
            line.lineTo(x, y)
            area.lineTo(x, y)
        }
    }
    if (fill != null) {
        area.lineTo(xAt(samples.last().epochMs), plotBottom)
        area.close()
        drawPath(area, fill)
    }
    drawPath(line, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    val last = samples.last()
    drawCircle(color, radius = 3.2f, center = Offset(xAt(last.epochMs), yAt(last)))
}

@Composable
fun HeapClassBarChart(histogram: HeapHistogram, modifier: Modifier = Modifier) {
    val bars = histogram.rows.take(MAX_HEAP_CHART_BARS)
    val maxBytes = bars.maxOfOrNull { it.bytes }?.coerceAtLeast(1) ?: 1
    val barColor = PakkuDesktopConstants.highlightColor
    val trackColor = JewelTheme.contentColor.copy(alpha = 0.08f)
    val nameStyle = heapTextStyle(JewelTheme.contentColor)
    val sizeStyle = heapTextStyle(JewelTheme.contentColor.copy(alpha = 0.55f))

    key(histogram.takenAtEpochMs) {
        SelectionContainer(modifier) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                bars.forEach { row ->
                    val fraction = (row.bytes.toFloat() / maxBytes.toFloat()).coerceIn(0.02f, 1f)
                    val short = shortClassName(row.className)
                    val copyText = buildString {
                        append(short)
                        append('\t')
                        append(formatHeapBytes(row.bytes))
                        append('\t')
                        append(formatHeapShare(row.bytes, histogram.totalBytes))
                        val full = decodeJvmTypeName(row.className)
                        if (full != short) {
                            append('\t')
                            append(full)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(short, style = nameStyle, modifier = Modifier.width(132.dp), maxLines = 1)
                        Canvas(Modifier.weight(1f).height(8.dp)) {
                            drawRoundRect(trackColor, cornerRadius = CornerRadius(4f, 4f))
                            drawRoundRect(
                                color = barColor,
                                size = Size(size.width * fraction, size.height),
                                cornerRadius = CornerRadius(4f, 4f),
                            )
                        }
                        BasicText(formatHeapBytes(row.bytes), style = sizeStyle)
                        CopyToClipboardButton(text = copyText, useSimpleTooltip = true, buttonSize = 22.dp, iconSize = 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun heapTextStyle(color: Color): TextStyle =
    JewelTheme.consoleTextStyle.merge(TextStyle(color = color))
