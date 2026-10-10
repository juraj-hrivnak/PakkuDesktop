/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.plugin.PakkuPlugin
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin

data class SampleModel(val count: Int)

sealed interface SampleMsg {
    data object Inc : SampleMsg
}

fun sampleUpdate(msg: SampleMsg, model: SampleModel): SampleModel = when (msg) {
    SampleMsg.Inc -> model.copy(count = model.count + 1)
}

/** Opt-in heap profiling: `-Dpakku.debug.tick=true` drives the real plugin hot path. */
private val debugTickDriver: Driver<SampleModel, SampleMsg> = { publish, _, content ->
    LaunchedEffect(Unit) {
        if (System.getProperty("pakku.debug.tick") != "true") return@LaunchedEffect
        while (isActive) {
            delay(16)
            publish(SampleMsg.Inc)
        }
    }
    content()
}

class SamplePlugin : PakkuPlugin {
    override val id = PluginId("sample")

    override fun create(): TypedPlugin<SampleModel, SampleMsg> = TypedPlugin(
        modelClass = SampleModel::class,
        msgClass = SampleMsg::class,
        component = component(
            init = SampleModel(count = 0),
            update = ::sampleUpdate,
            view = { publish, model ->
                BasicText(
                    text = "Sample: ${model.count}",
                    modifier = Modifier.clickable { publish(SampleMsg.Inc) },
                )
            },
        ),
        drivers = listOf(debugTickDriver),
        sampleMessages = listOf(SampleMsg.Inc),
    )
}
