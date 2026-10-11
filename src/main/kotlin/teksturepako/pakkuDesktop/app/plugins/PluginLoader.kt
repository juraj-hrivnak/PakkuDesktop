/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import io.klogging.noCoLogger
import teksturepako.pakkuDesktop.app.plugins.PluginLoadState.Stage
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.plugin.PakkuPlugin
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.PluginTrust
import teksturepako.pakkuDesktop.plugin.TypedPlugin
import teksturepako.pakku.io.createHash
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.inputStream
import kotlin.io.path.readText

internal const val MAX_PLUGIN_JAR_BYTES = 32L * 1024 * 1024
internal const val MAX_PLUGIN_JAR_ENTRIES = 10_000

private const val HOST_PACKAGE = "teksturepako.pakkuDesktop."

private val FORBIDDEN_JAR_PREFIXES = listOf(
    "teksturepako/pakkuDesktop/elm/",
    "teksturepako/pakkuDesktop/plugin/",
    "teksturepako/pakkuDesktop/app/",
    "teksturepako/pakkuDesktop/pkui/",
    "teksturepako/pakkuDesktop/pro/",
    "androidx/compose/",
)

/**
 * Parent of every plugin class loader. Delegates parent-first, but a class under
 * [HOST_PACKAGE] is returned only if it was loaded from `:pakku-desktop-api` or
 * `:pakku-desktop-app-api`; host internals (drivers, the loader, git) throw
 * [ClassNotFoundException]. This limits what a plugin links by name; it is not a sandbox.
 */
internal class PluginParentClassLoader(
    parent: ClassLoader,
    private val apiLocations: Set<String>,
) : ClassLoader(parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        val cls = super.loadClass(name, resolve)
        if (name.startsWith(HOST_PACKAGE) && cls.codeLocation() !in apiLocations) throw ClassNotFoundException(name)
        return cls
    }
}

private fun Class<*>.codeLocation(): String? = protectionDomain?.codeSource?.location?.toString()

/** If the API modules share a code source with the host, nothing under [HOST_PACKAGE] is delegated. */
private val pluginParent: ClassLoader = run {
    val host = PluginLoader::class.java.codeLocation()
    val api = setOfNotNull(PakkuPlugin::class.java.codeLocation(), AppModel::class.java.codeLocation())
    PluginParentClassLoader(PakkuPlugin::class.java.classLoader, api.filterTo(HashSet()) { it != host })
}

/** Parent-first through [PluginParentClassLoader]: API types resolve to the host's classes. */
internal fun pluginClassLoader(jar: Path): URLClassLoader =
    URLClassLoader(arrayOf(jar.toUri().toURL()), pluginParent)

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
     * @param official ids loaded with [PluginTrust.Official]; every other plugin is [PluginTrust.Standard].
     */
    fun loadAll(
        root: Path,
        readText: (Path) -> String = { it.readText() },
        openClassLoader: (Path) -> URLClassLoader = ::pluginClassLoader,
        digests: Map<PluginId, String> = OfficialPluginDigests.byId,
        official: Set<PluginId> = OfficialPluginDigests.official,
        maxJarBytes: Long = MAX_PLUGIN_JAR_BYTES,
        maxEntries: Int = MAX_PLUGIN_JAR_ENTRIES,
    ): PluginRegistry {
        if (System.getProperty("org.graalvm.nativeimage.imagecode") != null) {
            logger.info("plugins: GraalVM native image; dynamic plugins disabled")
            return PluginRegistry.Empty
        }
        val startedAt = System.nanoTime()
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
            when (val state = loadOne(dir, readText, openClassLoader, loadedFrom, digests, official, maxJarBytes, maxEntries)) {
                is PluginLoadState.Loaded -> {
                    loaded[state.plugin.id] = state.plugin
                    loadedFrom[state.plugin.id] = dir.name
                }
                is PluginLoadState.Failed -> failed += state
            }
        }
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        logger.info(
            "plugins: ${loaded.size} loaded, ${failed.size} failed in ${elapsedMs}ms (root=$root)",
        )
        return PluginRegistry(loaded, failed)
    }

    private fun loadOne(
        dir: Path,
        readText: (Path) -> String,
        openClassLoader: (Path) -> URLClassLoader,
        loadedFrom: Map<PluginId, String>,
        digests: Map<PluginId, String>,
        official: Set<PluginId>,
        maxJarBytes: Long,
        maxEntries: Int,
    ): PluginLoadState {
        var stage = Stage.Manifest
        var loader: URLClassLoader? = null
        return try {
            val manifest = readManifest(dir, readText, loadedFrom)
            val jar = dir.resolve(manifest.jar)
            stage = Stage.Trust
            val size = Files.size(jar)
            check(size in 1..maxJarBytes) { "plugin jar is too large" }
            verifyTrust(manifest.id, jar, digests, maxJarBytes)
            stage = Stage.ClassLoad
            checkJarEntries(dir, jar, maxEntries)
            loader = openClassLoader(jar)
            val cls = try {
                Class.forName(manifest.mainClass, true, loader)
            } catch (e: ExceptionInInitializerError) {
                throw e.cause ?: e
            }
            stage = Stage.Instantiate
            val typed = instantiate(cls, manifest).create()
            stage = Stage.Validate
            val initial = typed.validate()
            val (erased, erasedDrivers) = typed.erase()
            val trust = if (manifest.id in official) PluginTrust.Official else PluginTrust.Standard
            PluginLoadState.Loaded(
                LoadedPlugin(manifest.id, manifest.name, erased, erasedDrivers, initial, loader, trust),
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

    /** Rejects jars that bundle host packages or Compose; the host provides those. */
    private fun checkJarEntries(dir: Path, jar: Path, maxEntries: Int) {
        var bundlesKotlin = false
        var entries = 0
        JarFile(jar.toFile()).use { file ->
            for (entry in file.entries()) {
                entries++
                check(entries <= maxEntries) { "plugin jar has too many entries" }
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

private fun verifyTrust(id: PluginId, jar: Path, digests: Map<PluginId, String>, maxJarBytes: Long) {
    val expectedHex = digests[id] ?: throw IllegalArgumentException("plugin not allowlisted")
    val actualHex = jar.inputStream().use { createHash("sha256", CappedInputStream(it, maxJarBytes)) }
    if (actualHex != expectedHex) {
        throw IllegalArgumentException("jar digest mismatch")
    }
}

/** Stops updating the digest after [maxBytes]; fails if more bytes are read. */
private class CappedInputStream(
    private val delegate: InputStream,
    private val maxBytes: Long,
) : InputStream() {
    private var read = 0L

    override fun read(): Int {
        val b = delegate.read()
        if (b >= 0) count(1)
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = delegate.read(b, off, len)
        if (n > 0) count(n.toLong())
        return n
    }

    private fun count(n: Long) {
        read += n
        check(read <= maxBytes) { "plugin jar is too large" }
    }
}
