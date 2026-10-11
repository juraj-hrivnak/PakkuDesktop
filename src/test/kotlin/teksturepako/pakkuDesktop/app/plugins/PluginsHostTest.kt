/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import teksturepako.pakkuDesktop.app.ui.view.plugins.PakkuProSlot
import teksturepako.pakkuDesktop.app.ui.view.plugins.PluginsHost
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.elm.run
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test

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

    @AfterTest
    fun cleanup() {
        loaders.forEach { it.loader.close() }
        loaders.clear()
    }

    private fun loaded(id: String, typed: TypedPlugin<*, *>, sha256: String = ""): LoadedPlugin =
        loadedPluginOf(id, typed).let { plugin ->
            if (sha256.isEmpty()) plugin else LoadedPlugin(
                plugin.id,
                plugin.name,
                plugin.erased,
                plugin.erasedDrivers,
                plugin.initialState,
                plugin.loader,
                plugin.trust,
                sha256,
            )
        }.also { loaders += it }

    private fun registry(vararg plugins: LoadedPlugin, failures: List<Failed> = emptyList()) =
        PluginRegistry(plugins.associateBy { it.id }, failures)

    private fun hostComponent(plugins: PluginRegistry) = component(
        init = appComponent(ProjectsUiData(), plugins).init(),
        update = { msg: AppMsg, model: AppModel -> appUpdate(msg, model, plugins) },
        view = { _, model ->
            PluginsHost(
                plugins = plugins.loadedList,
                failures = model.pluginFailures,
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
    fun settingsListShowsPluginNamesNotViews() {
        val a = loaded("plugin-a", counterPlugin("plugin-a"), sha256 = "abc")
        val b = loaded("plugin-b", counterPlugin("plugin-b"))
        composeRule.setContent {
            JewelTestTheme {
                PluginsHost(
                    plugins = listOf(a, b),
                    failures = persistentListOf(),
                    pluginsRoot = Path("plugins"),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("plugin-a").assertExists()
        composeRule.onNodeWithText("abc").assertExists()
        composeRule.onNodeWithText("plugin-a: 0").assertDoesNotExist()
        composeRule.onNodeWithText("plugin-b: 0").assertDoesNotExist()
    }

    @Test
    fun reorderKeepsRows() {
        val a = loaded("a", counterPlugin("a"))
        val b = loaded("b", counterPlugin("b"))

        composeRule.setContent {
            var order by remember { mutableStateOf(listOf(a, b)) }
            Column {
                PluginsHost(
                    plugins = order,
                    failures = persistentListOf(),
                    pluginsRoot = Path("plugins"),
                )
                BasicText("swap", modifier = Modifier.clickable { order = listOf(b, a) })
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("a").assertExists()
        composeRule.onNodeWithText("b").assertExists()
        composeRule.onNodeWithText("a: 0").assertDoesNotExist()

        composeRule.onNodeWithText("swap").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("a").assertExists()
        composeRule.onNodeWithText("b").assertExists()
    }

    @Test
    fun failureRowsShowDirNameStageAndMessage() {
        val failure = Failed("broken-plugin", Stage.Manifest, "missing manifest.edn")
        composeRule.setContent {
            JewelTestTheme {
                PluginsHost(
                    plugins = emptyList(),
                    failures = persistentListOf(failure),
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
    fun pakkuProViewMountsOnlyInItsSlot() {
        val pro = loaded("pakku-pro", counterPlugin("pakku-pro"))
        val other = loaded("plugin-a", counterPlugin("plugin-a"))
        val plugins = registry(pro, other)
        val states = persistentMapOf(pro.id to pro.initialState, other.id to other.initialState)
        var showSlot by mutableStateOf(false)

        composeRule.setContent {
            JewelTestTheme {
                Column {
                    PluginsHost(
                        plugins = plugins.loadedList,
                        failures = persistentListOf(),
                        pluginsRoot = Path("plugins"),
                    )
                    if (showSlot) PakkuProSlot(plugins, states, persistentListOf(), publish = {})
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("plugin-a").assertExists()
        composeRule.onNodeWithText("plugin-a: 0").assertDoesNotExist()
        composeRule.onNodeWithText("pakku-pro").assertExists()
        composeRule.onNodeWithText("pakku-pro: 0").assertDoesNotExist()

        showSlot = true
        composeRule.waitForIdle()
        composeRule.onNodeWithText("pakku-pro: 0").assertExists()
    }

    @Test
    fun pakkuProSlotShowsLoadFailure() {
        val failure = Failed("pakku-pro", Stage.Trust, "jar digest mismatch")
        composeRule.setContent {
            JewelTestTheme {
                PakkuProSlot(PluginRegistry.Empty, persistentMapOf(), persistentListOf(failure), publish = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("pakku-pro").assertExists()
        composeRule.onNodeWithText("Trust").assertExists()
        composeRule.onNodeWithText("jar digest mismatch", substring = true).assertExists()
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

            composeRule.onNodeWithText("Sample plugin").assertExists()
            composeRule.onNodeWithText(sha256Hex(sampleJar)).assertExists()
            composeRule.onNodeWithText("Sample: 0").assertDoesNotExist()
            composeRule.onNodeWithText("broken", substring = true).assertExists()
            composeRule.onNodeWithText("Manifest", substring = true).assertExists()
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
