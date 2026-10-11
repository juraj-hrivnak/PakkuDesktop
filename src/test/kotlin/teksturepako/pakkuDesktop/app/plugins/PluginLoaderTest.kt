/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import teksturepako.pakkuDesktop.app.plugins.PluginLoadState.Failed
import teksturepako.pakkuDesktop.app.plugins.PluginLoadState.Stage
import teksturepako.pakkuDesktop.plugin.PakkuPlugin
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.PluginTrust
import teksturepako.pakku.io.createHash
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val SAMPLE = "teksturepako.pakkuDesktop.sample.SamplePlugin"
private const val PAKKU_PRO = "teksturepako.pakkuDesktop.official.plugin.pro.PakkuProPlugin"
private const val FIXTURES = "teksturepako.pakkuDesktop.sample.fixtures"

internal fun sha256Hex(path: Path): String = path.inputStream().use { createHash("sha256", it) }

class PluginLoaderTest {
    private val stagedPlugins: Path = Path(System.getProperty("pakku.test.pluginsDir"))
    private val sampleJar: Path = stagedPlugins.resolve("sample/plugin.jar")
    private val pakkuProJar: Path = stagedPlugins.resolve("pakku-pro/plugin.jar")
    private val root: Path = createTempDirectory("plugins")
    private val registries = mutableListOf<PluginRegistry>()

    @AfterTest
    fun cleanup() {
        registries.forEach { registry -> registry.loadedList.forEach { it.loader.close() } }
        root.toFile().deleteRecursively()
    }

    private fun manifest(id: String, mainClass: String, extra: String = "") =
        """{:id "$id" :name "$id" :main-class "$mainClass" :host-api 1 $extra}"""

    private fun pluginDir(name: String, manifest: String?, withJar: Boolean = true): Path {
        val dir = root.resolve(name).createDirectories()
        manifest?.let { dir.resolve("manifest.edn").writeText(it) }
        if (withJar) sampleJar.copyTo(dir.resolve("plugin.jar"))
        return dir
    }

    private fun allowAllInRoot(): Map<PluginId, String> {
        if (!root.isDirectory()) return emptyMap()
        val map = linkedMapOf<PluginId, String>()
        for (dir in root.listDirectoryEntries().filter { it.isDirectory() }.sortedBy { it.name }) {
            val manifestFile = dir.resolve("manifest.edn")
            if (!manifestFile.isRegularFile()) continue
            val manifest = runCatching { parseManifest(manifestFile.readText()) }.getOrNull() ?: continue
            val jar = dir.resolve(manifest.jar)
            if (jar.isRegularFile()) {
                map[manifest.id] = sha256Hex(jar)
            }
        }
        return map
    }

    private fun load(
        readText: (Path) -> String = { it.readText() },
        openClassLoader: (Path) -> URLClassLoader = ::testPluginClassLoader,
        digests: Map<PluginId, String>? = null,
        official: Set<PluginId> = emptySet(),
    ): PluginRegistry =
        PluginLoader.loadAll(root, readText, openClassLoader, digests ?: allowAllInRoot(), official)
            .also { registries += it }

    private fun ids(registry: PluginRegistry) = registry.loaded.keys.map { it.value }

    private fun assertSingleFailure(expected: Failed, digests: Map<PluginId, String>? = null) {
        val registry = load(digests = digests)
        assertEquals(emptyList(), ids(registry))
        assertEquals(listOf(expected), registry.failed)
    }

    private fun countingCreates(registry: PluginRegistry): Any? =
        registry.loaded.getValue(PluginId("counting")).loader
            .loadClass("$FIXTURES.CountingPlugin").getMethod("getCreates").invoke(null)

    @Test
    fun goodAndBadPlugins() {
        pluginDir("good", manifest("counting", "$FIXTURES.CountingPlugin"))
        pluginDir("bad", manifest("throwing-init", "$FIXTURES.ThrowingInitPlugin"))

        val registry = load()

        assertEquals(listOf("counting"), ids(registry))
        assertEquals(listOf(Failed("bad", Stage.Validate, "init failed")), registry.failed)
        assertEquals(1, countingCreates(registry))
        assertSame(Unit, registry.loaded.getValue(PluginId("counting")).initialState)
    }

    @Test
    fun sampleMessagesRunAtLoad() {
        pluginDir("sample", manifest("sample", SAMPLE))
        val plugin = load().loadedList.single()
        assertEquals(PluginId("sample"), plugin.id)
        assertEquals("SampleModel(count=0)", plugin.initialState.toString())
    }

    @Test
    fun missingManifest() {
        pluginDir("p", manifest = null)
        assertSingleFailure(Failed("p", Stage.Manifest, "missing manifest.edn"))
    }

    @Test
    fun unparseableManifest() {
        pluginDir("p", """{:id "p" :name""")
        assertSingleFailure(Failed("p", Stage.Manifest, "line 1, column 15: missing value for :name"))
    }

    @Test
    fun manifestMissingKey() {
        pluginDir("p", """{:id "p" :name "p" :host-api 1}""")
        assertSingleFailure(Failed("p", Stage.Manifest, "missing :main-class"))
    }

    @Test
    fun unsupportedHostApi() {
        pluginDir("p", """{:id "sample" :name "s" :main-class "$SAMPLE" :host-api 999}""")
        assertSingleFailure(Failed("p", Stage.Manifest, "host-api 999 not supported (host is 1)"))
    }

    @Test
    fun missingMainClass() {
        pluginDir("p", manifest("p", "does.not.Exist"))
        assertSingleFailure(Failed("p", Stage.ClassLoad, "does.not.Exist"))
    }

    @Test
    fun wrongId() {
        pluginDir("p", manifest("wrong", "$FIXTURES.WrongIdPlugin"))
        assertSingleFailure(Failed("p", Stage.Instantiate, "plugin id 'wrong-id' does not match manifest id 'wrong'"))
    }

    @Test
    fun notAPlugin() {
        pluginDir("p", manifest("p", "$FIXTURES.NotAPlugin"))
        assertSingleFailure(Failed("p", Stage.Instantiate, "main-class does not implement PakkuPlugin"))
    }

    @Test
    fun duplicateIdFirstFolderWins() {
        pluginDir("a", manifest("sample", SAMPLE))
        pluginDir("b", manifest("sample", SAMPLE))

        val registry = load()

        assertEquals(listOf("sample"), ids(registry))
        assertEquals(
            listOf(Failed("b", Stage.Manifest, "duplicate id 'sample' (already loaded from 'a')")),
            registry.failed,
        )
    }

    @Test
    fun jarOutsidePluginFolder() {
        sampleJar.copyTo(root.resolve("escape.jar"))
        pluginDir("p", manifest("sample", SAMPLE, """:jar "../escape.jar""""), withJar = false)
        assertSingleFailure(Failed("p", Stage.Manifest, ":jar '../escape.jar' is outside the plugin folder"))
    }

    @Test
    fun missingJar() {
        pluginDir("p", manifest("sample", SAMPLE), withJar = false)
        assertSingleFailure(Failed("p", Stage.Manifest, "missing jar 'plugin.jar'"))
    }

    @Test
    fun jarBundlingHostClassRejected() {
        val dir = pluginDir("p", manifest("sample", SAMPLE), withJar = false)
        val entry = "teksturepako/pakkuDesktop/elm/Component.class"
        JarFile(sampleJar.toFile()).use { src ->
            JarOutputStream(dir.resolve("plugin.jar").outputStream()).use { dst ->
                for (e in src.entries()) {
                    dst.putNextEntry(JarEntry(e.name))
                    src.getInputStream(e).use { it.copyTo(dst) }
                    dst.closeEntry()
                }
                dst.putNextEntry(JarEntry(entry))
                dst.closeEntry()
            }
        }
        assertSingleFailure(Failed("p", Stage.ClassLoad, "plugin.jar bundles host class '$entry'"))
    }

    @Test
    fun manifestReadOncePerFolder() {
        pluginDir("a", manifest("sample", SAMPLE))
        pluginDir("b", manifest("counting", "$FIXTURES.CountingPlugin"))
        pluginDir("c", manifest("throwing-init", "$FIXTURES.ThrowingInitPlugin"))
        val reads = mutableMapOf<Path, Int>()

        val registry = load(readText = { path -> reads.merge(path, 1, Int::plus); path.readText() })

        assertEquals(listOf("a", "b", "c"), reads.keys.map { it.parent.fileName.toString() }.sorted())
        assertTrue(reads.values.all { it == 1 }, reads.toString())
        assertEquals(1, countingCreates(registry))
    }

    @Test
    fun failedPluginLoaderIsClosed() {
        pluginDir("good", manifest("counting", "$FIXTURES.CountingPlugin"))
        pluginDir("bad", manifest("throwing-init", "$FIXTURES.ThrowingInitPlugin"))
        val closedDirs = mutableSetOf<Path>()
        val opened = mutableMapOf<Path, URLClassLoader>()

        val registry = PluginLoader.loadAll(
            root,
            openClassLoader = { jar ->
                object : URLClassLoader(arrayOf(jar.toUri().toURL()), PakkuPlugin::class.java.classLoader) {
                    override fun close() {
                        closedDirs.add(jar.parent.fileName)
                        super.close()
                    }
                }.also { opened[jar.parent.fileName] = it }
            },
            digests = allowAllInRoot(),
        ).also { registries += it }

        assertEquals(setOf(Path("bad")), closedDirs)
        assertSame(opened.getValue(Path("good")), registry.loadedList.single().loader)
        assertFailsWith<ClassNotFoundException> { opened.getValue(Path("bad")).loadClass("$FIXTURES.NotAPlugin") }
    }

    @Test
    fun missingRootIsEmpty() {
        val registry = PluginLoader.loadAll(root.resolve("missing"))
        assertSame(PluginRegistry.Empty, registry)
    }

    @Test
    fun scanOrderIsFolderNameAndFilesAreIgnored() {
        pluginDir("b", manifest("counting", "$FIXTURES.CountingPlugin"))
        pluginDir("a", manifest("sample", SAMPLE))
        root.resolve("stray.txt").writeText("not a plugin")

        val registry = load()

        assertEquals(listOf("sample", "counting"), ids(registry))
        assertEquals(listOf("sample", "counting"), registry.loadedList.map { it.id.value })
        assertEquals(emptyList(), registry.failed)
    }

    @Test
    fun trustAllowsMatchingDigest() {
        val dir = pluginDir("p", manifest("counting", "$FIXTURES.CountingPlugin"))
        val jar = dir.resolve("plugin.jar")
        val registry = load(digests = mapOf(PluginId("counting") to sha256Hex(jar)))
        assertEquals(listOf("counting"), ids(registry))
        assertEquals(emptyList(), registry.failed)
    }

    @Test
    fun trustLevelComesFromOfficialSet() {
        pluginDir("a", manifest("counting", "$FIXTURES.CountingPlugin"))
        pluginDir("b", manifest("sample", SAMPLE))
        val registry = load(official = setOf(PluginId("sample")))
        assertEquals(PluginTrust.Standard, registry.loaded.getValue(PluginId("counting")).trust)
        assertEquals(PluginTrust.Official, registry.loaded.getValue(PluginId("sample")).trust)
    }

    @Test
    fun trustRejectsTamperedJar() {
        val dir = pluginDir("p", manifest("counting", "$FIXTURES.CountingPlugin"))
        val jar = dir.resolve("plugin.jar")
        val goodHex = sha256Hex(jar)
        val bytes = jar.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
        jar.writeBytes(bytes)
        var classLoaderOpens = 0
        val registry = load(
            openClassLoader = {
                classLoaderOpens++
                testPluginClassLoader(it)
            },
            digests = mapOf(PluginId("counting") to goodHex),
        )
        assertEquals(0, classLoaderOpens)
        assertEquals(emptyList(), ids(registry))
        assertEquals(listOf(Failed("p", Stage.Trust, "jar digest mismatch")), registry.failed)
    }

    @Test
    fun trustRejectsWrongExpectedHex() {
        pluginDir("p", manifest("counting", "$FIXTURES.CountingPlugin"))
        val wrong = "0".repeat(64)
        assertSingleFailure(Failed("p", Stage.Trust, "jar digest mismatch"), digests = mapOf(PluginId("counting") to wrong))
    }

    @Test
    fun trustRejectsUnlistedId() {
        pluginDir("p", manifest("counting", "$FIXTURES.CountingPlugin"))
        assertSingleFailure(Failed("p", Stage.Trust, "plugin not allowlisted"), digests = emptyMap())
    }

    @Test
    fun defaultDigestsAllowRepoSampleInDevBuilds() {
        if (PluginBuildMode.release) return
        pluginDir("sample", manifest("sample", SAMPLE))
        val registry = PluginLoader.loadAll(root).also { registries += it }
        assertEquals(listOf("sample"), ids(registry))
        assertEquals(emptyList(), registry.failed)
    }

    private fun pakkuProManifest() =
        """{:id "pakku-pro" :name "Pakku Pro" :main-class "$PAKKU_PRO" :host-api 1}"""

    @Test
    fun defaultDigestsAllowPakkuPro() {
        val dir = pluginDir("pro", pakkuProManifest(), withJar = false)
        pakkuProJar.copyTo(dir.resolve("plugin.jar"))
        val registry = PluginLoader.loadAll(root).also { registries += it }
        assertEquals(listOf("pakku-pro"), ids(registry))
        assertEquals(emptyList(), registry.failed)
    }

    @Test
    fun trustRejectsTamperedPakkuProJar() {
        val dir = pluginDir("pro", pakkuProManifest(), withJar = false)
        pakkuProJar.copyTo(dir.resolve("plugin.jar"))
        val jar = dir.resolve("plugin.jar")
        val goodHex = sha256Hex(jar)
        val bytes = jar.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
        jar.writeBytes(bytes)
        var classLoaderOpens = 0
        val registry = load(
            openClassLoader = {
                classLoaderOpens++
                testPluginClassLoader(it)
            },
            digests = mapOf(PluginId("pakku-pro") to goodHex),
        )
        assertEquals(0, classLoaderOpens)
        assertEquals(emptyList(), ids(registry))
        assertEquals(listOf(Failed("pro", Stage.Trust, "jar digest mismatch")), registry.failed)
    }

    @Test
    fun goodAndUnlistedPlugins() {
        pluginDir("good", manifest("counting", "$FIXTURES.CountingPlugin"))
        pluginDir("bad", manifest("sample", SAMPLE))
        val goodDir = root.resolve("good")
        val goodJar = goodDir.resolve("plugin.jar")
        val registry = load(digests = mapOf(PluginId("counting") to sha256Hex(goodJar)))
        assertEquals(listOf("counting"), ids(registry))
        assertEquals(listOf(Failed("bad", Stage.Trust, "plugin not allowlisted")), registry.failed)
    }

}

private fun testPluginClassLoader(jar: Path): URLClassLoader =
    URLClassLoader(arrayOf(jar.toUri().toURL()), PakkuPlugin::class.java.classLoader)
