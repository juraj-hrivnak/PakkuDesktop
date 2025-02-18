package teksturepako.pakkupro.ui.modifier

import androidx.compose.animation.core.*
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import teksturepako.pakkupro.ui.PakkuDesktopConstants

@Composable
fun Modifier.interactiveHover(
    enabled: Boolean = true,
    glowRadius: Float = 100f,
    glowAlpha: Float = 0.2f
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val hoverAlpha by animateFloatAsState(
        targetValue = if (isHovered && enabled) glowAlpha else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    return this
        .hoverable(interactionSource)
        .drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PakkuDesktopConstants.highlightColor.copy(alpha = hoverAlpha),
                        Color.Transparent
                    ),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
}
