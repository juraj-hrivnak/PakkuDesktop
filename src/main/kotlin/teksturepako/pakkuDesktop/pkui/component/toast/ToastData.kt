/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component.toast

import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * What a toast shows, as plain immutable data. Rendered by the `content` slot of [ToastHost].
 * Implementations must not hold lambdas or [Throwable]s.
 */
interface ToastBody

data class ToastData(
    val id: String = UUID.randomUUID().toString(),
    val duration: Duration = 1.minutes,
    val body: ToastBody,
)
