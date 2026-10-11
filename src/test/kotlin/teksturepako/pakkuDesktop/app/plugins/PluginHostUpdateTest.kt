/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import teksturepako.pakkuDesktop.app.data.ProjectsUiData
import teksturepako.pakkuDesktop.app.plugins.PluginLoadState.Failed
import teksturepako.pakkuDesktop.app.plugins.PluginLoadState.Stage
import teksturepako.pakkuDesktop.app.ui.appComponent
import teksturepako.pakkuDesktop.app.ui.appUpdate
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

private data class ModelA(val n: Int)
private sealed interface MsgA {
    data object Bump : MsgA
    data object Noop : MsgA
}

private data class ModelB(val tag: String)
private sealed interface MsgB {
    data object Ping : MsgB
}

private fun typedPluginA() = TypedPlugin(
    modelClass = ModelA::class,
    msgClass = MsgA::class,
    component = component(
        init = ModelA(0),
        update = { msg, model ->
            when (msg) {
                MsgA.Bump -> model.copy(n = model.n + 1)
                MsgA.Noop -> model
            }
        },
        view = { _, _ -> },
    ),
)

private fun typedPluginB() = TypedPlugin(
    modelClass = ModelB::class,
    msgClass = MsgB::class,
    component = component(
        init = ModelB("initial"),
        update = { _, model -> model },
        view = { _, _ -> },
    ),
)

class PluginHostUpdateTest {
    private val loaders = mutableListOf<LoadedPlugin>()

    @AfterTest
    fun cleanup() {
        loaders.forEach { it.loader.close() }
        loaders.clear()
    }

    private fun registry(): PluginRegistry {
        val a = loadedPluginOf("plugin-a", typedPluginA()).also { loaders += it }
        val b = loadedPluginOf("plugin-b", typedPluginB()).also { loaders += it }
        return PluginRegistry(
            mapOf(a.id to a, b.id to b),
            emptyList(),
        )
    }

    private fun seededModel(plugins: PluginRegistry): AppModel =
        appComponent(ProjectsUiData(), plugins).init()

    @Test
    fun messageToOnePluginLeavesOtherStateIdentical() {
        val plugins = registry()
        val model = seededModel(plugins)
        val bBefore = model.pluginStates[PluginId("plugin-b")]
        val next = appUpdate(AppMsg.Plugin(PluginId("plugin-a"), MsgA.Bump), model, plugins)
        assertSame(bBefore, next.pluginStates[PluginId("plugin-b")])
        assertTrue((next.pluginStates[PluginId("plugin-a")] as ModelA).n == 1)
    }

    @Test
    fun noopPluginMessageReturnsSameModelInstance() {
        val plugins = registry()
        val model = seededModel(plugins)
        val next = appUpdate(AppMsg.Plugin(PluginId("plugin-a"), MsgA.Noop), model, plugins)
        assertSame(model, next)
    }

    @Test
    fun unknownPluginIdReturnsSameModelInstance() {
        val plugins = registry()
        val model = seededModel(plugins)
        val next = appUpdate(AppMsg.Plugin(PluginId("missing"), MsgA.Bump), model, plugins)
        assertSame(model, next)
    }

    @Test
    fun wrongMessageTypeThrowsClassCastException() {
        val plugins = registry()
        val model = seededModel(plugins)
        assertFailsWith<ClassCastException> {
            appUpdate(AppMsg.Plugin(PluginId("plugin-a"), "not-a-msg"), model, plugins)
        }
    }

    @Test
    fun appComponentInitSeedsPluginStatesAndFailures() {
        val a = loadedPluginOf("only", typedPluginA()).also { loaders += it }
        val failure = Failed("bad-dir", Stage.Manifest, "missing manifest.edn")
        val plugins = PluginRegistry(mapOf(a.id to a), listOf(failure))
        val model = appComponent(ProjectsUiData(), plugins).init()
        assertEquals(1, model.pluginStates.size)
        assertSame(a.initialState, model.pluginStates[a.id])
        assertEquals(listOf(failure), model.pluginFailures)
    }
}
