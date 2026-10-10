/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.plugin

/** Manifest `:host-api` must equal this. Bump on any breaking SPI change. */
const val HOST_API: Int = 1

/**
 * Entry point named by `:main-class` in `manifest.edn`. Needs a public no-arg constructor.
 *
 * Plugins run in the host process with no sandbox. The plugin jar must not bundle this
 * API, `teksturepako.pakkuDesktop.elm`, Compose or the Kotlin stdlib; the host provides them.
 */
interface PakkuPlugin {
    /** Must equal the manifest `:id`. */
    val id: PluginId

    /**
     * Called exactly once, by the host loader, at startup. Never called again for the
     * lifetime of the process, so build everything the plugin needs here.
     */
    fun create(): TypedPlugin<*, *>
}
