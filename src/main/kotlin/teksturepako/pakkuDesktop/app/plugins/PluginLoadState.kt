/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin
import java.net.URLClassLoader

/** Outcome of loading one plugin folder. */
sealed interface PluginLoadState {
    data class Loaded(val plugin: LoadedPlugin) : PluginLoadState

    /** Holds a message only; the stack trace is logged, never kept (C3). */
    data class Failed(val dirName: String, val stage: Stage, val message: String) : PluginLoadState

    enum class Stage { Manifest, ClassLoad, Instantiate, Validate }
}

/**
 * A plugin that passed every load stage. [loader] stays open for the app's lifetime.
 *
 * @property initialState the validated result of `init()`; seed the host model from this
 *   instead of calling `init()` again.
 */
class LoadedPlugin(
    val id: PluginId,
    val name: String,
    val typed: TypedPlugin<*, *>,
    val initialState: Any?,
    val loader: URLClassLoader,
)

/** Immutable result of [PluginLoader.loadAll]. The only owner of plugin classes (C4). */
class PluginRegistry(
    /** Insertion order = load order (folders sorted by name). */
    val loaded: Map<PluginId, LoadedPlugin>,
    val failed: List<PluginLoadState.Failed>,
) {
    /** One instance for the app's lifetime. */
    val loadedList: List<LoadedPlugin> = loaded.values.toList()

    companion object {
        val Empty = PluginRegistry(emptyMap(), emptyList())
    }
}
