package teksturepako.pakkuDesktop.app.ui.modifier

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Modifier.clickableHover(
    enabled: Boolean = true,
    scaleOnHover: Boolean = false,
    pressed: Boolean? = null,
    onClick: () -> Unit,
): Modifier
{
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            pressed ?: isPressed -> 0.98f
            isHovered && scaleOnHover -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    return this
        .hoverable(interactionSource, enabled)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            enabled = enabled
        )
}
