/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import teksturepako.pakkuDesktop.app.data.ProjectsUiData
import teksturepako.pakkuDesktop.app.plugins.PluginLoadState.Failed
import teksturepako.pakkuDesktop.app.plugins.PluginLoadState.Stage
import teksturepako.pakkuDesktop.app.ui.appComponent
import teksturepako.pakkuDesktop.app.ui.appUpdate
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.SettingsTab
import teksturepako.pakkuDesktop.app.ui.view.plugins.PluginsHost
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.elm.RecomposeCounter
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.elm.run
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private data class CounterModel(val n: Int)

private sealed interface CounterMsg {
    data object Inc : CounterMsg
}

private fun counterPlugin(id: String) = TypedPlugin(
    modelClass = CounterModel::class,
    msgClass = CounterMsg::class,
    component = component(
        init = CounterModel(0),
        update = { msg, model ->
            when (msg) {
                CounterMsg.Inc -> model.copy(n = model.n + 1)
            }
        },
        view = { _, model -> BasicText("$id: ${model.n}") },
    ),
)

private fun rememberPlugin(id: String) = TypedPlugin(
    modelClass = Unit::class,
    msgClass = Unit::class,
    component = component(
        init = Unit,
        update = { _, model -> model },
        view = { _, _ ->
            var n by remember { mutableIntStateOf(0) }
            BasicText(
                text = "$id: $n",
                modifier = Modifier.clickable { n += 1 },
            )
        },
    ),
)

private fun assumeAwt(): TestRule = TestRule { base, _ ->
    object : Statement() {
        override fun evaluate() {
            assumeTrue(
                "AWT/Skiko unavailable (X11 libs); Compose UI plugin host tests skipped",
                runCatching { java.awt.Toolkit.getDefaultToolkit() }.isSuccess,
            )
            base.evaluate()
        }
    }
}

class PluginsHostTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(assumeAwt()).around(composeRule)

    private val loaders = mutableListOf<LoadedPlugin>()
    private var probesWereEnabled = false

    @BeforeTest
    fun enableProbes() {
        probesWereEnabled = RecomposeCounter.enabled
        RecomposeCounter.enabled = true
    }

    @AfterTest
    fun cleanup() {
        RecomposeCounter.enabled = probesWereEnabled
        RecomposeCounter.reset()
        loaders.forEach { it.loader.close() }
        loaders.clear()
    }

    private fun loaded(id: String, typed: TypedPlugin<*, *>): LoadedPlugin =
        loadedPluginOf(id, typed).also { loaders += it }

    private fun registry(vararg plugins: LoadedPlugin, failures: List<Failed> = emptyList()) =
        PluginRegistry(plugins.associateBy { it.id }, failures)

    private fun hostComponent(plugins: PluginRegistry) = component(
        init = appComponent(ProjectsUiData(), plugins).init(),
        update = { msg: AppMsg, model: AppModel -> appUpdate(msg, model, plugins) },
        view = { publish, model ->
            PluginsHost(
                plugins = plugins.loadedList,
                states = model.pluginStates,
                failures = model.pluginFailures,
                publish = publish,
                pluginsRoot = Path("plugins"),
            )
        },
    )

    @Composable
    private fun JewelTestTheme(content: @Composable () -> Unit) {
        val textStyle = remember { JewelTheme.createDefaultTextStyle() }
        val theme = remember { JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle) }
        IntUiTheme(theme, ComponentStyling, swingCompatMode = false, content)
    }

    @Test
    fun unrelatedPluginMessageDoesNotRecomposeUntouchedPlugin() {
        val a = loaded("plugin-a", counterPlugin("plugin-a"))
        val b = loaded("plugin-b", counterPlugin("plugin-b"))
        val plugins = registry(a, b)
        lateinit var publish: (AppMsg) -> Unit
        val captureDriver: Driver<AppModel, AppMsg> = { p, _, content -> publish = p; content() }

        composeRule.setContent { run(hostComponent(plugins), listOf(captureDriver)) }
        composeRule.waitForIdle()
        RecomposeCounter.reset()

        publish(AppMsg.Plugin(PluginId("plugin-a"), CounterMsg.Inc))
        composeRule.waitForIdle()

        assertEquals(1, RecomposeCounter["plugin:plugin-a"])
        assertEquals(0, RecomposeCounter["plugin:plugin-b"])
    }

    @Test
    fun unrelatedHostMessageDoesNotRecomposePlugins() {
        val a = loaded("plugin-a", counterPlugin("plugin-a"))
        val b = loaded("plugin-b", counterPlugin("plugin-b"))
        val plugins = registry(a, b)
        lateinit var publish: (AppMsg) -> Unit
        val captureDriver: Driver<AppModel, AppMsg> = { p, _, content -> publish = p; content() }

        composeRule.setContent { run(hostComponent(plugins), listOf(captureDriver)) }
        composeRule.waitForIdle()
        RecomposeCounter.reset()

        publish(AppMsg.SettingsTabSelected(SettingsTab.Plugins))
        composeRule.waitForIdle()

        assertEquals(0, RecomposeCounter["plugin:plugin-a"])
        assertEquals(0, RecomposeCounter["plugin:plugin-b"])
    }

    @Test
    fun reorderKeepsRememberState() {
        val a = loaded("a", rememberPlugin("a"))
        val b = loaded("b", rememberPlugin("b"))
        val states = persistentMapOf(a.id to a.initialState, b.id to b.initialState)

        composeRule.setContent {
            var order by remember { mutableStateOf(listOf(a, b)) }
            Column {
                PluginsHost(
                    plugins = order,
                    states = states,
                    failures = persistentListOf(),
                    publish = {},
                    pluginsRoot = Path("plugins"),
                )
                BasicText("swap", modifier = Modifier.clickable { order = listOf(b, a) })
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("a: 0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("a: 1").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("swap").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("a: 2").assertExists()
        composeRule.onNodeWithText("b: 0").assertExists()
    }

    @Test
    fun failureRowsShowDirNameStageAndMessage() {
        val failure = Failed("broken-plugin", Stage.Manifest, "missing manifest.edn")
        composeRule.setContent {
            JewelTestTheme {
                PluginsHost(
                    plugins = emptyList(),
                    states = persistentMapOf(),
                    failures = persistentListOf(failure),
                    publish = {},
                    pluginsRoot = Path("plugins"),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("broken-plugin", substring = true).assertExists()
        composeRule.onNodeWithText("Manifest", substring = true).assertExists()
        composeRule.onNodeWithText("missing manifest.edn", substring = true).assertExists()
    }

    @Test
    fun samplePluginAndBrokenFixtureShowInHost() {
        val root = createTempDirectory("plugins-host")
        try {
            val sampleSrc = Path(System.getProperty("pakku.test.pluginsDir")).resolve("sample")
            val sampleDir = root.resolve("sample").createDirectories()
            sampleSrc.resolve("manifest.edn").copyTo(sampleDir.resolve("manifest.edn"))
            sampleSrc.resolve("plugin.jar").copyTo(sampleDir.resolve("plugin.jar"))
            root.resolve("broken").createDirectories()

            val sampleJar = sampleDir.resolve("plugin.jar")
            val digests = mapOf(PluginId("sample") to sha256Hex(sampleJar))
            val plugins = PluginLoader.loadAll(root, digests = digests)
            plugins.loadedList.forEach { loaders += it }

            composeRule.setContent {
                JewelTestTheme {
                    run(hostComponent(plugins))
                }
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Sample: 0").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Sample: 1").assertExists()
            composeRule.onNodeWithText("broken", substring = true).assertExists()
            composeRule.onNodeWithText("Manifest", substring = true).assertExists()
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
