/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import androidx.compose.runtime.remember
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

/**
 * Every loaded plugin's drivers, lifted to the host model. Build once in `main()` and append
 * to the end of the `run()` driver list so plugin drivers sit inside every host driver.
 *
 * A lifted driver sees only its plugin's `pluginStates` slice, and its `publish` wraps each
 * message in [AppMsg.Plugin].
 */
fun pluginDrivers(registry: PluginRegistry): List<Driver<AppModel, AppMsg>> =
    registry.loadedList.flatMap { plugin -> plugin.erasedDrivers.map { plugin.lift(it) } }

private fun LoadedPlugin.lift(driver: Driver<Any?, Any?>): Driver<AppModel, AppMsg> {
    val id = id
    return { publish, model, content ->
        val lifted = remember(publish) { { msg: Any? -> publish(AppMsg.Plugin(id, msg)) } }
        driver(lifted, model.pluginStates.getValue(id), content)
    }
}
