/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.sample.fixtures

import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Component
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.plugin.PakkuPlugin
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin

// Host loader test fixtures. They share the sample plugin jar; each test folder's
// manifest points `:main-class` at one of them.

private fun unitPlugin(component: Component<Unit, Unit>) =
    TypedPlugin(modelClass = Unit::class, msgClass = Unit::class, component = component)

/** Valid. Counts [create] calls per class loader. */
class CountingPlugin : PakkuPlugin {
    override val id = PluginId("counting")

    override fun create(): TypedPlugin<Unit, Unit> {
        creates++
        return unitPlugin(component(init = Unit, update = { _, _ -> }, view = { _, _ -> }))
    }

    companion object {
        @JvmStatic
        var creates = 0
    }
}

/** `init()` throws. */
class ThrowingInitPlugin : PakkuPlugin {
    override val id = PluginId("throwing-init")

    override fun create(): TypedPlugin<Unit, Unit> =
        unitPlugin(Component(init = { error("init failed") }, update = { _, _ -> }, view = { _, _ -> }))
}

/** [id] is `"wrong-id"`; manifests using this class declare a different id. */
class WrongIdPlugin : PakkuPlugin {
    override val id = PluginId("wrong-id")

    override fun create(): TypedPlugin<Unit, Unit> =
        unitPlugin(component(init = Unit, update = { _, _ -> }, view = { _, _ -> }))
}

/** Doesn't implement [PakkuPlugin]. */
class NotAPlugin

/** Class init resolves a host-only class; the plugin parent loader refuses it. */
class HostInternalPlugin : PakkuPlugin {
    override val id = PluginId("host-internal")

    override fun create(): TypedPlugin<Unit, Unit> =
        unitPlugin(component(init = Unit, update = { _, _ -> }, view = { _, _ -> }))

    companion object {
        init {
            Class.forName("teksturepako.pakkuDesktop.app.ui.driver.CredentialsDriverKt")
        }
    }
}

/** Links [AppMsg] from `:pakku-desktop-app-api` in [create]; the plugin parent delegates it. */
class AppApiPlugin : PakkuPlugin {
    override val id = PluginId("app-api")

    override fun create(): TypedPlugin<Unit, Unit> {
        linked = AppMsg.ShowActivation
        return unitPlugin(component(init = Unit, update = { _, _ -> }, view = { _, _ -> }))
    }

    companion object {
        @JvmStatic
        var linked: AppMsg? = null
    }
}
