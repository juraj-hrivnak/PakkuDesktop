/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.SideEffect
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

// ---------------------------------------------------------------------------
// publishBridgeDriver — wires the window's onCloseRequest to the ELM loop
// ---------------------------------------------------------------------------

fun publishBridgeDriver(onPublish: ((AppMsg) -> Unit) -> Unit): Driver<AppModel, AppMsg> =
    { publish, _, content ->
        SideEffect { onPublish(publish) }
        content()
    }

