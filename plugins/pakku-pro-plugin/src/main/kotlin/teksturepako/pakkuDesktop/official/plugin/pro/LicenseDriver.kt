/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.official.plugin.pro

import androidx.compose.runtime.LaunchedEffect
import com.github.michaelbull.result.fold
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.api.LocalApp
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.InternalMsg
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.official.plugin.pro.data.Polar

/** The two Polar calls the license driver makes. */
internal interface LicenseBackend {
    suspend fun isActivated(): Boolean

    /** Null on success, otherwise the message to show under the key field. */
    suspend fun activate(key: String): String?
}

internal object PolarBackend : LicenseBackend {
    override suspend fun isActivated(): Boolean = withContext(Dispatchers.IO) {
        try {
            Polar.isActivated()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun activate(key: String): String? = withContext(Dispatchers.IO) {
        try {
            Polar.processLicenseKey(key).fold(success = { null }, failure = { it.rawMessage })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message ?: "Failed to activate your license key due to an unknown error."
        }
    }
}

private fun reportActivated(activated: Boolean): AppMsg =
    AppMsg.Internal(InternalMsg.ProActivationReported(activated))

internal suspend fun checkActivation(
    backend: LicenseBackend,
    publish: (ProMsg) -> Unit,
    publishApp: (AppMsg) -> Unit,
) {
    val activated = backend.isActivated()
    publish(ProMsg.ActivationChecked(activated))
    publishApp(reportActivated(activated))
}

/** On failure the host flag is left as it was. */
internal suspend fun submitLicenseKey(
    key: String,
    backend: LicenseBackend,
    publish: (ProMsg) -> Unit,
    publishApp: (AppMsg) -> Unit,
) {
    val error = backend.activate(key)
    publish(ProMsg.KeyHandled(error))
    if (error == null) publishApp(reportActivated(true))
}

internal fun licenseDriver(backend: LicenseBackend): Driver<ProModel, ProMsg> = { publish, model, content ->
    val app = LocalApp.current

    LaunchedEffect(Unit) {
        checkActivation(backend, publish, app.publish)
    }

    LaunchedEffect(model.pendingKey) {
        val key = model.pendingKey ?: return@LaunchedEffect
        submitLicenseKey(key, backend, publish, app.publish)
    }

    content()
}
