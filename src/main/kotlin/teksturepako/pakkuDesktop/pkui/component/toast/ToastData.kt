/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component.toast

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

data class ToastData(
    val id: String = UUID.randomUUID().toString(),
    val duration: Duration = 1.minutes,
    val content: @Composable BoxScope.() -> Unit,
)