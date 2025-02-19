package teksturepako.pakkupro.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.theme.tooltipStyle
import teksturepako.pakkupro.ui.PakkuDesktopConstants

@Composable
fun HoverablePanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    scaleOnHover: Boolean = true,
    onClick: () -> Unit = { },
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    var mousePosition by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isHovered && scaleOnHover -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val glowAlpha by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.4f
            isHovered -> 0.3f
            else -> 0f
        },
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        )
    )

    // Animate the glow position
    val animatedGlowPosition by animateOffsetAsState(
        targetValue = if (isHovered) mousePosition else Offset(boxSize.x / 2, boxSize.y / 2),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 120f
        )
    )

    Box(
        modifier = Modifier.padding(16.dp * scale)
    ) {
        Box(
            modifier = modifier then Modifier
                .padding(4.dp * scale)
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
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
                    elevation = JewelTheme.tooltipStyle.metrics.shadowSize,
                    shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
                    ambientColor = JewelTheme.globalColors.borders.disabled,
                    spotColor = Color.Transparent,
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(JewelTheme.globalColors.panelBackground)
                .drawBehind {
                    // Border
                    drawRoundRect(
                        color = PakkuDesktopConstants.highlightColor.copy(
                            alpha = if (isPressed) 0.4f else 0.3f
                        ),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        style = Stroke(width = 1f)
                    )

                    // Glow effect that follows mouse
                    if (glowAlpha > 0f) {
                        val center = Offset(
                            // Interpolate between center and mouse position
                            size.width / 2 + (animatedGlowPosition.x - size.width / 2) * 0.3f,
                            size.height / 2 + (animatedGlowPosition.y - size.height / 2) * 0.3f
                        )

                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    PakkuDesktopConstants.highlightColor.copy(alpha = glowAlpha),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.maxDimension / 1.5f
                            ),
                            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                        )
                    }
                }
        ) {
            content()
        }
    }
}
