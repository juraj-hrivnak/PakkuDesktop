/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git.log

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import teksturepako.pakkuDesktop.app.git.log.GitLogElement
import teksturepako.pakkuDesktop.app.git.log.gitLogColor
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

private const val ROW_UNIT = 22f
private const val NODE_WIDTH = 16f
private const val CIRCLE_RADIUS = 4f
private const val LINE = 1.5f
private const val SELECTED_LINE = 2.5f
private const val ARROW_COS2 = 0.7
private const val ARROW_LENGTH = 0.3

/** Draws one row of the commit graph. [lit] rows are the hovered fragment. */
@Composable
fun GitGraphRow(
    elements: List<GitLogElement>,
    lit: Set<Int>,
    contentColor: Color,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val metrics = GraphMetrics(size.height)
        val selected = ArrayList<GitLogElement>()
        for (element in elements) {
            if (element.isLit(lit)) selected.add(element) else drawElement(element, metrics, false, contentColor, background)
        }
        for (element in selected) drawElement(element, metrics, true, contentColor, background, mark = true)
        for (element in selected) drawElement(element, metrics, true, contentColor, background, mark = false)
    }
}

/** Element under [point], nodes before edges, the way IntelliJ hit-tests a cell. */
fun gitGraphHit(elements: List<GitLogElement>, point: Offset, rowHeight: Float): GitLogElement? {
    val metrics = GraphMetrics(rowHeight)
    for (element in elements) {
        if (element.node && metrics.overNode(element.position, point.x, point.y)) return element
    }
    for (element in elements) {
        if (!element.node && metrics.overEdge(element, point.x, point.y)) return element
    }
    return null
}

private fun GitLogElement.isLit(rows: Set<Int>): Boolean {
    if (node) return row in rows
    if (edgeUp !in rows && edgeDown !in rows) return false
    return (edgeUp < 0 || edgeUp in rows) && (edgeDown < 0 || edgeDown in rows)
}

private fun DrawScope.drawElement(
    element: GitLogElement,
    metrics: GraphMetrics,
    selected: Boolean,
    contentColor: Color,
    background: Color,
    mark: Boolean = false,
) {
    val color = when {
        mark -> contentColor
        else -> gitLogColor(element.colorId)?.let { Color(it) } ?: contentColor
    }
    if (element.node) metrics.drawNode(this, element, color, background, selected && !mark)
    else metrics.drawEdge(this, element, color, selected)
}

private class GraphMetrics(val rowHeight: Float) {
    private val scale = rowHeight / ROW_UNIT
    val elementWidth = NODE_WIDTH * scale
    private val centerX = elementWidth / 2f
    private val rowCenter = rowHeight / 2f
    private val line = max(1f, LINE * scale)
    private val selectedLine = max(line + 2f, SELECTED_LINE * scale)
    private val radius = CIRCLE_RADIUS * scale
    private val headOuter = (CIRCLE_RADIUS + 2f) * scale

    fun overNode(position: Int, x: Float, y: Float): Boolean {
        val dx = elementWidth * position + centerX - x
        val dy = rowCenter - y
        return hypot(dx, dy) <= radius + 2f
    }

    fun overEdge(element: GitLogElement, x: Float, y: Float): Boolean {
        val x1 = elementWidth * element.position + centerX
        val y1 = rowCenter
        val x2: Float
        val y2: Float
        if (element.position == element.otherPosition || element.terminal) {
            x2 = x1
            val gap = if (element.terminal) radius / 2f + 1f else 0f
            y2 = if (element.down) rowHeight - gap else gap
        } else {
            x2 = elementWidth * element.otherPosition + centerX
            y2 = if (element.down) rowHeight + rowCenter else rowCenter - rowHeight
        }
        return hypot(x1 - x, y1 - y) + hypot(x2 - x, y2 - y) < hypot(x1 - x2, y1 - y2) + line + 2f
    }

    fun drawNode(scope: DrawScope, element: GitLogElement, color: Color, background: Color, selected: Boolean) {
        val x = snap(elementWidth * element.position + centerX)
        val y = snap(rowCenter)
        if (!element.head) {
            scope.drawCircle(color, if (selected) radius + 1f else radius, Offset(x, y))
            return
        }
        var diameter = headOuter * 2f
        if (selected) diameter += selectedLine - line
        var left = x - diameter / 2f
        var top = y - diameter / 2f
        scope.drawCircle(color, diameter / 2f, Offset(x, y))
        val delta = 2f * scale
        diameter -= delta * 2f
        left += delta
        top += delta
        scope.drawCircle(background, diameter / 2f, Offset(left + diameter / 2f, top + diameter / 2f))
        diameter -= delta * 2f
        scope.drawCircle(color, diameter / 2f, Offset(x, y))
    }

    fun drawEdge(scope: DrawScope, element: GitLogElement, color: Color, selected: Boolean) {
        val x1 = snap(elementWidth * element.position + centerX)
        val y1 = snap(rowCenter)
        val vertical = element.position == element.otherPosition || element.terminal
        val x2: Float
        val y2: Float
        if (vertical) {
            x2 = x1
            val gap = if (element.terminal) radius / 2f + 1f else 0f
            y2 = if (element.down) rowHeight - gap else gap
        } else {
            x2 = snap(elementWidth * element.otherPosition + centerX)
            y2 = if (element.down) rowHeight + rowCenter else rowCenter - rowHeight
        }
        val thickness = if (selected) selectedLine else line
        val stroke = if (element.dotted) {
            val length = if (vertical) rowHeight.toDouble() else hypot((x1 - x2).toDouble(), (y1 - y2).toDouble())
            Stroke(thickness, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(dash(length), 0f))
        } else {
            Stroke(thickness, cap = StrokeCap.Round)
        }
        scope.drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = thickness, cap = StrokeCap.Round, pathEffect = stroke.pathEffect)
        if (element.arrow) {
            val ax = if (vertical) x2 else (x1 + x2) / 2f
            val ay = if (vertical) y2 else (y1 + y2) / 2f
            val fromX = if (vertical) x1 else x1
            val fromY = if (vertical) y1 else y1
            drawArrow(scope, color, thickness, fromX, fromY, ax, ay)
        }
    }

    private fun dash(edgeLength: Double): FloatArray {
        val count = max(1, floor(edgeLength / rowHeight).toInt())
        val space = rowHeight / 2f - 2f
        val dash = (edgeLength / count - space).toFloat()
        return floatArrayOf(dash.coerceAtLeast(1f), space.coerceAtLeast(1f))
    }

    private fun drawArrow(scope: DrawScope, color: Color, thickness: Float, x: Float, y: Float, tipX: Float, tipY: Float) {
        val cos = sqrt(ARROW_COS2)
        val sin = sqrt(1 - ARROW_COS2)
        val length = ARROW_LENGTH * rowHeight
        val (x1, y1) = rotate(x.toDouble(), y.toDouble(), tipX.toDouble(), tipY.toDouble(), cos, sin, length.toDouble())
        val (x2, y2) = rotate(x.toDouble(), y.toDouble(), tipX.toDouble(), tipY.toDouble(), cos, -sin, length.toDouble())
        scope.drawLine(color, Offset(tipX, tipY), Offset(x1, y1), strokeWidth = thickness, cap = StrokeCap.Round)
        scope.drawLine(color, Offset(tipX, tipY), Offset(x2, y2), strokeWidth = thickness, cap = StrokeCap.Round)
    }
}

private fun snap(value: Float): Float = floor(value) + 0.5f

private fun rotate(x: Double, y: Double, centerX: Double, centerY: Double, cos: Double, sin: Double, length: Double): Pair<Float, Float> {
    val tx = x - centerX
    val ty = y - centerY
    val d = hypot(tx, ty).coerceAtLeast(0.001)
    val sx = length * tx / d
    val sy = length * ty / d
    return ((sx * cos - sy * sin) + centerX).toFloat() to ((sx * sin + sy * cos) + centerY).toFloat()
}

/** One tag of a ref label, in IntelliJ's 16-unit tag shape. */
fun DrawScope.drawRefTag(color: Color, background: Color, offsetX: Float, size: Float, dot: Boolean) {
    val scale = size / 16f
    val path = Path().apply {
        moveTo(offsetX + 13.4495f * scale, 7.5001f * scale)
        lineTo(offsetX + 7.01028f * scale, 13.9394f * scale)
        cubicTo(
            offsetX + 6.81502f * scale, 14.1347f * scale,
            offsetX + 6.49843f * scale, 14.1347f * scale,
            offsetX + 6.30317f * scale, 13.9394f * scale,
        )
        lineTo(offsetX + 2.06053f * scale, 9.69679f * scale)
        cubicTo(
            offsetX + 1.86527f * scale, 9.50152f * scale,
            offsetX + 1.86527f * scale, 9.18494f * scale,
            offsetX + 2.06053f * scale, 8.98968f * scale,
        )
        lineTo(offsetX + 8.49978f * scale, 2.55035f * scale)
        cubicTo(
            offsetX + 8.61106f * scale, 2.43907f * scale,
            offsetX + 8.76824f * scale, 2.38667f * scale,
            offsetX + 8.92404f * scale, 2.40893f * scale,
        )
        lineTo(offsetX + 12.6363f * scale, 2.93926f * scale)
        cubicTo(
            offsetX + 12.8563f * scale, 2.97069f * scale,
            offsetX + 13.0292f * scale, 3.14354f * scale,
            offsetX + 13.0606f * scale, 3.36352f * scale,
        )
        lineTo(offsetX + 13.5909f * scale, 7.07584f * scale)
        cubicTo(
            offsetX + 13.6132f * scale, 7.23163f * scale,
            offsetX + 13.5608f * scale, 7.38882f * scale,
            offsetX + 13.4495f * scale, 7.5001f * scale,
        )
        close()
    }
    drawPath(path, background)
    drawPath(path, color, style = Stroke(width = scale.coerceAtLeast(1f)))
    if (dot) drawCircle(color, scale, Offset(offsetX + 10.5f * scale, 5.5f * scale))
}
