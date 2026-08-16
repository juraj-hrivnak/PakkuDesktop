/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.runtime.compositionLocalOf
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope

/** Window/application scope lives outside the ELM model; `mainWindowDriver` provides this local. */
val LocalPakkuApplicationScope = compositionLocalOf<PakkuApplicationScope> {
    error("LocalPakkuApplicationScope not provided — mainWindowDriver must wrap the tree")
}

/** True while Shift is held (updated from the main window key preview). */
val LocalShiftPressed = compositionLocalOf { false }
