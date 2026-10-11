/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.official.plugin.pro

import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.official.plugin.pro.ui.LicenseKeyField
import teksturepako.pakkuDesktop.plugin.PakkuPlugin
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin

data class ProModel(
    val activated: Boolean? = null,
    /** Set on submit; cleared by the license driver. Non-null while a submit is in flight. */
    val pendingKey: String? = null,
    val error: String? = null,
)

sealed interface ProMsg {
    data class ActivationChecked(val activated: Boolean) : ProMsg
    data class SubmitKey(val key: String) : ProMsg
    /** [error] is null when the key was activated. */
    data class KeyHandled(val error: String?) : ProMsg
}

internal fun proUpdate(msg: ProMsg, model: ProModel): ProModel = when (msg) {
    is ProMsg.ActivationChecked ->
        if (model.activated == msg.activated) model else model.copy(activated = msg.activated)
    is ProMsg.SubmitKey ->
        if (model.pendingKey != null) model else model.copy(pendingKey = msg.key, error = null)
    is ProMsg.KeyHandled -> model.copy(
        activated = if (msg.error == null) true else model.activated,
        pendingKey = null,
        error = msg.error,
    )
}

class PakkuProPlugin : PakkuPlugin {
    override val id = PluginId("pakku-pro")

    override fun create(): TypedPlugin<ProModel, ProMsg> = TypedPlugin(
        modelClass = ProModel::class,
        msgClass = ProMsg::class,
        component = component(
            init = ProModel(),
            update = ::proUpdate,
            view = { publish, model ->
                LicenseKeyField(
                    isProActivated = model.activated,
                    busy = model.pendingKey != null,
                    licenseKeyError = model.error,
                    onSubmitLicenseKey = { key -> publish(ProMsg.SubmitKey(key)) },
                )
            },
        ),
        drivers = listOf(licenseDriver(PolarBackend)),
        sampleMessages = listOf(ProMsg.ActivationChecked(false), ProMsg.KeyHandled(error = null)),
    )
}
