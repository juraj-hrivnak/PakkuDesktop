/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import com.github.michaelbull.result.fold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.pro.data.Polar

// ---------------------------------------------------------------------------
// licenseDriver — startup activation check + license key submission (IO)
// ---------------------------------------------------------------------------

val licenseDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(Unit) {
        val activated = withContext(Dispatchers.IO) { Polar.isActivated() }
        publish(AppMsg.ProActivationChecked(activated))
    }

    LaunchedEffect(model.pendingLicenseKey) {
        val key = model.pendingLicenseKey ?: return@LaunchedEffect
        val previousActivated = model.isProActivated
        withContext(Dispatchers.IO) {
            Polar.processLicenseKey(key).fold(
                success = {
                    publish(AppMsg.LicenseKeyHandled(activated = true, error = null))
                },
                failure = { err ->
                    publish(AppMsg.LicenseKeyHandled(activated = previousActivated, error = err))
                },
            )
        }
    }

    content()
}

