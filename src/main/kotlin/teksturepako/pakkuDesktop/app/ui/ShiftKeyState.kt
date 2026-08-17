/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Mutable shift-key state — stable reference for [LocalShiftKeyState] so key events don't recompose the whole tree. */
@Stable
class ShiftKeyState {
    var pressed by mutableStateOf(false)
}
