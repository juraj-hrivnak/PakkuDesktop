/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.pro.data.Polar

// ---------------------------------------------------------------------------
// licenseDriver — checks pro activation once on startup
// ---------------------------------------------------------------------------

val licenseDriver: Driver<AppModel, AppMsg> = { publish, _, content ->
    LaunchedEffect(Unit) {
        val activated = withContext(Dispatchers.IO) { Polar.isActivated() }
        publish(AppMsg.ProActivationChecked(activated))
    }
    content()
}

