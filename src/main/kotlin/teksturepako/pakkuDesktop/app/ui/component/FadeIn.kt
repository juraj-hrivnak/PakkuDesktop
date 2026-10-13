/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()

    var isVisible by remember { mutableStateOf(false) }

    val millis = duration.toInt(DurationUnit.MILLISECONDS)
    // fadeOut only — the default exit is shrinkOut, and its clip hides anything
    // drawn outside the layout (the welcome panel grows upward on scroll).
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(millis)),
        exit = fadeOut(tween(millis)),
    ) {
        content()
    }

    if (!isVisible) {
        coroutineScope.launch {
            if (delay != null) delay(duration.inWholeMilliseconds)
            isVisible = true
        }
    }
}
