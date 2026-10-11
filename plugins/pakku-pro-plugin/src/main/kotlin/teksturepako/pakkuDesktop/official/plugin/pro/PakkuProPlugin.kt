/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.official.plugin.pro

import androidx.compose.foundation.text.BasicText
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.plugin.PakkuPlugin
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin

object ProModel

sealed interface ProMsg

@Suppress("UNUSED_PARAMETER")
private fun proUpdate(msg: ProMsg, model: ProModel): ProModel = model

class PakkuProPlugin : PakkuPlugin {
    override val id = PluginId("pakku-pro")

    override fun create(): TypedPlugin<ProModel, ProMsg> = TypedPlugin(
        modelClass = ProModel::class,
        msgClass = ProMsg::class,
        component = component(
            init = ProModel,
            update = ::proUpdate,
            view = { _, _ ->
                BasicText(text = "Pakku Pro")
            },
        ),
    )
}
