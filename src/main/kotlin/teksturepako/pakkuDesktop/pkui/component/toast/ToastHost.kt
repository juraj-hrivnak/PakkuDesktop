package teksturepako.pakkuDesktop.pkui.component.toast

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Composable
fun ToastHost(
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
                            Toast(
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
