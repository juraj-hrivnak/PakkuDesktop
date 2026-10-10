/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.SharedFlow

/**
 * Every message passed to `Store.publish`, as a hot stream (no replay).
 *
 * Provided by the host's `run()`. Collect in drivers with `LaunchedEffect(Unit)` and filter by type;
 * do not key effects on model fields that only exist to notify (`broadcast`, `lastEvent`).
 */
val LocalMessages = staticCompositionLocalOf<SharedFlow<*>> {
    error("LocalMessages not provided — run() must wrap the tree")
}
