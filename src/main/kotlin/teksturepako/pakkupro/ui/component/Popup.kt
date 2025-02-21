package teksturepako.pakkupro.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import teksturepako.pakkupro.ui.PakkuDesktopConstants
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

data class ToastData(
    val id: String = UUID.randomUUID().toString(),
    val duration: Duration = 4000.milliseconds,
    val content: @Composable BoxScope.() -> Unit,
)

@Composable
fun SonnerPopup(
    toastData: ToastData,
    onDismiss: (String) -> Unit,
    cornerRadius: Dp = 8.dp,
    shadowSize: Dp = 4.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    var mousePosition by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isHovered -> 1.02f
            else -> 1f
        }
    )

    val glowAlpha by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.4f
            isHovered -> 0.3f
            else -> 0f
        }
    )

    val animatedGlowPosition by animateOffsetAsState(
        targetValue = if (isHovered) mousePosition else Offset(boxSize.x / 2, boxSize.y / 2)
    )

    LaunchedEffect(toastData.id) {
        delay(toastData.duration)
        onDismiss(toastData.id)
    }

    Box(
        modifier = Modifier
            .padding(4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onDismiss(toastData.id)
            }
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
            .shadow(
                elevation = shadowSize,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = JewelTheme.globalColors.borders.disabled,
                spotColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(JewelTheme.globalColors.panelBackground)
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
        toastData.content(this)
    }
}

@Composable
fun SonnerToastHost(
    toasts: MutableState<List<ToastData>>,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopCenter,
    spacing: Dp = 4.dp
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(scrollState)
        ) {
            toasts.value.forEachIndexed { index, toast ->
                key(toast.id) {
                    var appeared by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 50L)
                        appeared = true
                    }

                    AnimatedVisibility(
                        visible = appeared,
                        enter = slideInVertically(
                            initialOffsetY = { -40 },
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(200)
                        ),
                        exit = fadeOut(
                            animationSpec = tween(150)
                        ) + shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = tween(150)
                        )
                    ) {
                        Box(
                            modifier = Modifier.animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        ) {
                            SonnerPopup(
                                toastData = toast,
                                onDismiss = { id ->
                                    appeared = false
                                    coroutineScope.launch {
                                        delay(150)
                                        toasts.value = toasts.value.filterNot { it.id == id }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun MutableState<List<ToastData>>.showToast(
    duration: Duration = 3.minutes,
    content: @Composable BoxScope.() -> Unit,
) {
    value = value + ToastData(content = content, duration = duration)
}
