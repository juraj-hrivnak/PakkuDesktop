/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import teksturepako.pakkuDesktop.app.plugins.PluginLoadState.Failed
import teksturepako.pakkuDesktop.app.plugins.PluginLoadState.Stage
import teksturepako.pakkuDesktop.plugin.PakkuPlugin
import teksturepako.pakkuDesktop.plugin.PluginId
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val SAMPLE = "teksturepako.pakkuDesktop.sample.SamplePlugin"
private const val FIXTURES = "teksturepako.pakkuDesktop.sample.fixtures"

class PluginLoaderTest {
    private val sampleJar: Path = Path(System.getProperty("pakku.test.samplePluginDir")).resolve("plugin.jar")
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

    private fun load(readText: (Path) -> String = { it.readText() }) =
        PluginLoader.loadAll(root, readText).also { registries += it }

    private fun ids(registry: PluginRegistry) = registry.loaded.keys.map { it.value }

    private fun assertSingleFailure(expected: Failed) {
        val registry = load()
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

        val registry = PluginLoader.loadAll(root, openClassLoader = { jar ->
            object : URLClassLoader(arrayOf(jar.toUri().toURL()), PakkuPlugin::class.java.classLoader) {
                override fun close() {
                    closedDirs.add(jar.parent.fileName)
                    super.close()
                }
            }.also { opened[jar.parent.fileName] = it }
        }).also { registries += it }

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
}
