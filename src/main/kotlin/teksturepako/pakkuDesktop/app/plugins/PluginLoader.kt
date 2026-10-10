/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import io.klogging.noCoLogger
import teksturepako.pakkuDesktop.app.plugins.PluginLoadState.Stage
import teksturepako.pakkuDesktop.plugin.PakkuPlugin
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

private val FORBIDDEN_JAR_PREFIXES = listOf(
    "teksturepako/pakkuDesktop/elm/",
    "teksturepako/pakkuDesktop/plugin/",
    "androidx/compose/",
)

/** Parent-first: `Component`, `Driver` and `PakkuPlugin` resolve to the host's classes. */
private fun pluginClassLoader(jar: Path): URLClassLoader =
    URLClassLoader(arrayOf(jar.toUri().toURL()), PakkuPlugin::class.java.classLoader)

/**
 * Loads every plugin folder under a root directory into a [PluginRegistry].
 *
 * The only place in the host that loads classes or instantiates plugins. Each folder is
 * loaded inside its own `try/catch`, so one broken plugin never stops another or the app.
 * Runs synchronously; call it after `loggingConfiguration { }`.
 */
object PluginLoader {
    private val logger = noCoLogger("PluginLoader")

    /**
     * @param readText reads `manifest.edn`; called exactly once per plugin folder.
     * @param openClassLoader creates the plugin's class loader from its jar.
     */
    fun loadAll(
        root: Path,
        readText: (Path) -> String = { it.readText() },
        openClassLoader: (Path) -> URLClassLoader = ::pluginClassLoader,
    ): PluginRegistry {
        if (!root.isDirectory()) {
            logger.info("plugins: root $root does not exist, none loaded")
            return PluginRegistry.Empty
        }
        val dirs = try {
            root.listDirectoryEntries().filter { it.isDirectory() }.sortedBy { it.name }
        } catch (e: IOException) {
            logger.error(e, "plugins: cannot list root $root, none loaded")
            return PluginRegistry.Empty
        }

        val loaded = LinkedHashMap<PluginId, LoadedPlugin>()
        val loadedFrom = HashMap<PluginId, String>()
        val failed = ArrayList<PluginLoadState.Failed>()
        for (dir in dirs) {
            when (val state = loadOne(dir, readText, openClassLoader, loadedFrom)) {
                is PluginLoadState.Loaded -> {
                    loaded[state.plugin.id] = state.plugin
                    loadedFrom[state.plugin.id] = dir.name
                }
                is PluginLoadState.Failed -> failed += state
            }
        }
        logger.info("plugins: ${loaded.size} loaded, ${failed.size} failed (root=$root)")
        return PluginRegistry(loaded, failed)
    }

    private fun loadOne(
        dir: Path,
        readText: (Path) -> String,
        openClassLoader: (Path) -> URLClassLoader,
        loadedFrom: Map<PluginId, String>,
    ): PluginLoadState {
        var stage = Stage.Manifest
        var loader: URLClassLoader? = null
        return try {
            val manifest = readManifest(dir, readText, loadedFrom)
            stage = Stage.ClassLoad
            val jar = dir.resolve(manifest.jar)
            checkJarEntries(dir, jar)
            loader = openClassLoader(jar)
            val cls = Class.forName(manifest.mainClass, true, loader)
            stage = Stage.Instantiate
            val typed = instantiate(cls, manifest).create()
            stage = Stage.Validate
            val initial = typed.validate()
            val (erased, erasedDrivers) = typed.erase()
            PluginLoadState.Loaded(
                LoadedPlugin(manifest.id, manifest.name, erased, erasedDrivers, initial, loader),
            )
        } catch (e: VirtualMachineError) {
            throw e
        } catch (e: Throwable) {
            runCatching { loader?.close() }
            logger.error(e, "plugin '${dir.name}' failed at $stage")
            PluginLoadState.Failed(dir.name, stage, e.message ?: e::class.simpleName.orEmpty())
        }
    }

    private fun readManifest(
        dir: Path,
        readText: (Path) -> String,
        loadedFrom: Map<PluginId, String>,
    ): PluginManifest {
        val file = dir.resolve("manifest.edn")
        require(file.isRegularFile()) { "missing manifest.edn" }
        val manifest = parseManifest(readText(file))
        loadedFrom[manifest.id]?.let {
            throw IllegalArgumentException("duplicate id '${manifest.id.value}' (already loaded from '$it')")
        }
        val folder = dir.normalize()
        val jar = folder.resolve(manifest.jar).normalize()
        require(jar.startsWith(folder)) { ":jar '${manifest.jar}' is outside the plugin folder" }
        require(jar.isRegularFile()) { "missing jar '${manifest.jar}'" }
        return manifest
    }

    /** Rejects jars that bundle the API, `elm` or Compose; the host provides those. */
    private fun checkJarEntries(dir: Path, jar: Path) {
        var bundlesKotlin = false
        JarFile(jar.toFile()).use { file ->
            for (entry in file.entries()) {
                val name = entry.name
                val forbidden = FORBIDDEN_JAR_PREFIXES.firstOrNull { name.startsWith(it) }
                check(forbidden == null) { "${jar.name} bundles host class '$name'" }
                if (name.startsWith("kotlin/")) bundlesKotlin = true
            }
        }
        if (bundlesKotlin) logger.warn("plugin '${dir.name}': ${jar.name} bundles kotlin/ classes (fat jar?)")
    }

    private fun instantiate(cls: Class<*>, manifest: PluginManifest): PakkuPlugin {
        require(PakkuPlugin::class.java.isAssignableFrom(cls)) { "main-class does not implement PakkuPlugin" }
        val plugin = try {
            cls.asSubclass(PakkuPlugin::class.java).getDeclaredConstructor().newInstance()
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
        check(plugin.id == manifest.id) {
            "plugin id '${plugin.id.value}' does not match manifest id '${manifest.id.value}'"
        }
        return plugin
    }

    /** Runs `init()` and [TypedPlugin.sampleMessages] once; returns the initial model. */
    private fun <M : Any, S : Any> TypedPlugin<M, S>.validate(): M {
        val initial = component.init()
        check(modelClass.isInstance(initial)) {
            "init() returned ${initial::class.qualifiedName}, expected ${modelClass.qualifiedName}"
        }
        for (msg in sampleMessages) {
            val next = component.update(msg, initial)
            check(modelClass.isInstance(next)) { "update($msg) returned the wrong model type" }
        }
        return initial
    }
}
