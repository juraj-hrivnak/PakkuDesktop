package teksturepako.pakkupro.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import teksturepako.pakkupro.ui.PakkuDesktopConstants

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    var mousePosition by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(Offset.Zero) }

    val transition = updateTransition(checked, label = "switch")

    val thumbPosition by transition.animateFloat(
        label = "thumbPosition",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    ) { if (it) 1f else 0f }

    val backgroundAlpha by transition.animateFloat(
        label = "backgroundAlpha",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    ) { if (it) 1f else 0f }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isHovered -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val animatedGlowPosition by animateOffsetAsState(
        targetValue = if (isHovered) mousePosition else Offset(boxSize.x / 2, boxSize.y / 2),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 120f
        )
    )

    Box(
        modifier = modifier
            .padding(8.dp * scale)
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(32.dp)
                .hoverable(interactionSource, enabled)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = { onCheckedChange(!checked) }
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Move -> {
                                    val position = event.changes.first().position
                                    mousePosition = position
                                    boxSize = Offset(size.width.toFloat(), size.height.toFloat())
                                }
                                PointerEventType.Exit -> {
                                    mousePosition = Offset(boxSize.x / 2, boxSize.y / 2)
                                }
                            }
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = JewelTheme.globalColors.borders.disabled,
                    spotColor = Color.Transparent
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            PakkuDesktopConstants.highlightColor.copy(alpha = backgroundAlpha * 0.8f),
                            PakkuDesktopConstants.highlightColor.copy(alpha = backgroundAlpha)
                        )
                    )
                )
                .drawBehind {
                    if (isHovered && enabled) {
                        val center = Offset(
                            size.width / 2 + (animatedGlowPosition.x - size.width / 2) * 0.3f,
                            size.height / 2 + (animatedGlowPosition.y - size.height / 2) * 0.3f
                        )

                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    PakkuDesktopConstants.highlightColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.maxDimension / 1.5f
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    }
                }
        ) {
            // Thumb
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .offset(x = 2.dp + 20.dp * thumbPosition)
                    .align(Alignment.CenterStart)
                    .shadow(
                        elevation = 2.dp,
                        shape = CircleShape,
                        ambientColor = JewelTheme.globalColors.borders.disabled,
                        spotColor = Color.Transparent
                    )
                    .background(
                        color = JewelTheme.globalColors.panelBackground,
                        shape = CircleShape
                    )
            )
        }
    }
}
