/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component

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
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.elm.animatedColor

@Composable
fun HoverablePanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    scaleOnHover: Boolean = true,
    /** Hover scale — home cards use 1.02; small FABs need more to read the same. */
    hoverScale: Float = 1.02f,
    pressedScale: Float = 0.98f,
    enabled: Boolean = true,
    /** Outer spacing that grows with [scale] (layout size change on hover). */
    surroundPadding: Dp = 16.dp,
    contentPadding: Dp = 4.dp,
    onClick: () -> Unit = { },
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val panelBackground = animatedColor(JewelTheme.globalColors.panelBackground)
    val shadowColor = animatedColor(JewelTheme.globalColors.borders.disabled)

    var mousePosition by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> pressedScale
            isHovered && scaleOnHover -> hoverScale
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val glowAlpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0f
            isPressed -> 0.4f
            isHovered -> 0.3f
            else -> 0f
        },
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        )
    )

    val animatedGlowPosition by animateOffsetAsState(
        targetValue = if (isHovered && enabled) mousePosition else Offset(boxSize.x / 2, boxSize.y / 2),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 120f
        )
    )

    Box(
        modifier = Modifier.padding(surroundPadding * scale)
    ) {
        Box(
            modifier = modifier then Modifier
                .padding(contentPadding * scale)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    alpha = if (enabled) 1f else 0.55f
                }
                .hoverable(interactionSource, enabled)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
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
                .shadow(
                    elevation = JewelTheme.tooltipStyle.metrics.shadowSize,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = shadowColor,
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(panelBackground)
                .drawBehind {
                    drawRoundRect(
                        color = PakkuDesktopConstants.highlightColor.copy(
                            alpha = if (isPressed) 0.4f else 0.3f
                        ),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        style = Stroke(width = 1f)
                    )

                    if (glowAlpha > 0f) {
                        val center = Offset(
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
