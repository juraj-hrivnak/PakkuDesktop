package teksturepako.pakkupro.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun FadeIn(
    duration: Duration = 2.seconds,
    delay: Duration? = null,
    content: @Composable () -> Unit
)
{
    var isVisible by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(duration.toInt(DurationUnit.MILLISECONDS))),
    ) {
        content()
    }

    if (!isVisible) {
        LaunchedEffect(Unit) {
            if (delay != null) delay(duration.inWholeMilliseconds)
            isVisible = true
        }
    }
}
