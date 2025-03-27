package teksturepako.pakkupro.ui.component

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

enum class TooltipPosition {
    TOP, BOTTOM, START, END
}

@Composable
fun ImmediateTooltip(
    tooltip: @Composable BoxScope.() -> Unit,
    position: TooltipPosition = TooltipPosition.END,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }

    // Define spacing constants
    val spacing = 4.dp
    val spacingPx = with(LocalDensity.current) { spacing.toPx().toInt() }

    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                contentSize = coordinates.size
            }
            .hoverable(interactionSource)
    ) {
        // Main content
        content(this)

        // Tooltip
        if (isHovered) {
            Popup(
                alignment = when (position) {
                    TooltipPosition.TOP -> Alignment.BottomCenter
                    TooltipPosition.BOTTOM -> Alignment.TopCenter
                    TooltipPosition.START -> Alignment.CenterEnd
                    TooltipPosition.END -> Alignment.CenterStart
                },
                offset = when (position) {
                    TooltipPosition.TOP -> IntOffset(
                        x = contentSize.width / 2 - tooltipSize.width / 2,
                        y = -tooltipSize.height - spacingPx
                    )
                    TooltipPosition.BOTTOM -> IntOffset(
                        x = contentSize.width / 2 - tooltipSize.width / 2,
                        y = contentSize.height + spacingPx
                    )
                    TooltipPosition.START -> IntOffset(
                        x = -tooltipSize.width - spacingPx,
                        y = 0
                    )
                    TooltipPosition.END -> IntOffset(
                        x = contentSize.width + spacingPx,
                        y = 0
                    )
                },
                properties = PopupProperties(focusable = false)
            ) {
                ContentBox(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            tooltipSize = coordinates.size
                        }
                        .padding(8.dp)
                ) {
                    tooltip(this)
                }
            }
        }
    }
}
