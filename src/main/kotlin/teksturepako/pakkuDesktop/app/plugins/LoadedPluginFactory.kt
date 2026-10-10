/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import teksturepako.pakkuDesktop.plugin.PakkuPlugin
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin
import java.net.URLClassLoader

/** In-memory plugin for host tests; no jar or manifest required. */
internal fun loadedPluginOf(id: String, typed: TypedPlugin<*, *>): LoadedPlugin {
    val (erased, erasedDrivers) = typed.erase()
    val loader = URLClassLoader(emptyArray(), PakkuPlugin::class.java.classLoader)
    val initial = typed.component.init()
    return LoadedPlugin(PluginId(id), id, erased, erasedDrivers, initial, loader)
}
