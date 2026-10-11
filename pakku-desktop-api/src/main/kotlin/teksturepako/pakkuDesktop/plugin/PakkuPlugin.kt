/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.plugin

/** Manifest `:host-api` must equal this. Bump on any breaking SPI change. */
const val HOST_API: Int = 1

/**
 * Entry point named by `:main-class` in `manifest.edn`.
 *
 * Requirements:
 * - Public no-arg constructor (the loader uses `getDeclaredConstructor().newInstance()`).
 * - [id] must equal manifest `:id` and stay stable across releases.
 * - Manifest `:host-api` must match [HOST_API].
 *
 * Plugins run in the host process with no sandbox. The plugin jar must not bundle this
 * API, `teksturepako.pakkuDesktop.elm`, Compose or the Kotlin stdlib; the host provides them.
 */
interface PakkuPlugin {
    /** Must equal the manifest `:id`. Used for state keys and `AppMsg.Plugin` routing. */
    val id: PluginId

    /**
     * Called exactly once, by the host loader at startup
     * before the UI runs. Never called from `view`, `update`, a driver, or `remember { }`.
     * Return a fully configured [TypedPlugin] (component, drivers, optional [TypedPlugin.sampleMessages]).
     */
    fun create(): TypedPlugin<*, *>
}
