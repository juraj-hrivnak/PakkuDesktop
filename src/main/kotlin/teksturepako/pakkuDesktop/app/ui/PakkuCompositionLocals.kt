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

/** Shift-key tracker — read [ShiftKeyState.pressed]; do not push the boolean through CompositionLocal directly. */
val LocalShiftKeyState = compositionLocalOf<ShiftKeyState> {
    error("LocalShiftKeyState not provided — mainWindowDriver must wrap the tree")
}
