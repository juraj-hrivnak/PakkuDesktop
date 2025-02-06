package teksturepako.pakkupro.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

fun Modifier.subtractTopHeight(height: Dp) = layout { measurable, constraints ->
    val reduction = height.roundToPx().coerceAtMost(constraints.maxHeight)
    val newConstraints = constraints.copy(
        maxHeight = (constraints.maxHeight - reduction).coerceAtLeast(0),
        minHeight = constraints.minHeight.coerceAtMost(constraints.maxHeight - reduction)
    )
    measurable.measure(newConstraints).let { placeable ->
        layout(constraints.maxWidth, (constraints.maxHeight - reduction * 2).coerceAtLeast(0)) {
            placeable.placeRelative(0, 0)
        }
    }
}
