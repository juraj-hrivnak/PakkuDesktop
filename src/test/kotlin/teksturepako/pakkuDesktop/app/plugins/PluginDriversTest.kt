/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import org.junit.Rule
import org.junit.rules.RuleChain
import teksturepako.pakkuDesktop.app.data.ProjectsUiData
import teksturepako.pakkuDesktop.app.ui.appComponent
import teksturepako.pakkuDesktop.app.ui.appUpdate
import teksturepako.pakkuDesktop.app.api.LocalApp
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.SettingsTab
import teksturepako.pakkuDesktop.app.ui.model.InternalMsg
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.elm.Event
import teksturepako.pakkuDesktop.elm.LocalMessages
import teksturepako.pakkuDesktop.elm.assumeAwtForComposeUiTests
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.elm.run
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.TypedPlugin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private data class TickModel(val n: Int)

private sealed interface TickMsg {
    data object Inc : TickMsg
}

private fun tickPlugin(driver: Driver<TickModel, TickMsg>) = TypedPlugin(
    modelClass = TickModel::class,
    msgClass = TickMsg::class,
    component = component(
        init = TickModel(0),
        update = { msg, model ->
            when (msg) {
                TickMsg.Inc -> model.copy(n = model.n + 1)
            }
        },
        view = { _, _ -> },
    ),
    drivers = listOf(driver),
)

private const val HOST_VIEW_TAG = "host-view"

private val A = PluginId("plugin-a")
private val B = PluginId("plugin-b")

class PluginDriversTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(assumeAwtForComposeUiTests()).around(composeRule)

    private val loaders = mutableListOf<LoadedPlugin>()

    private lateinit var publish: (AppMsg) -> Unit
    private lateinit var latest: AppModel

    @AfterTest
    fun cleanup() {
        loaders.forEach { it.loader.close() }
        loaders.clear()
    }

    private fun registry(a: Driver<TickModel, TickMsg>, b: Driver<TickModel, TickMsg>): PluginRegistry {
        val pa = loadedPluginOf(A.value, tickPlugin(a)).also { loaders += it }
        val pb = loadedPluginOf(B.value, tickPlugin(b)).also { loaders += it }
        return PluginRegistry(mapOf(pa.id to pa, pb.id to pb), emptyList())
    }

    /** Host capture driver first, plugin drivers appended last, as in `main()`. */
    private fun mount(plugins: PluginRegistry) {
        val host = component(
            init = appComponent(ProjectsUiData(), plugins).init(),
            update = { msg: AppMsg, model: AppModel -> appUpdate(msg, model, plugins) },
            view = { _, _ -> Box(Modifier.testTag(HOST_VIEW_TAG)) },
        )
        val capture: Driver<AppModel, AppMsg> = { p, m, content ->
            publish = p
            latest = m
            content()
        }
        val drivers = listOf(capture) + pluginDrivers(plugins)
        composeRule.setContent { run(host, drivers) }
        composeRule.waitForIdle()
    }

    private fun publishUnrelatedHostMessages(count: Int) = repeat(count) {
        publish(AppMsg.SettingsTabSelected(if (it % 2 == 0) SettingsTab.Heap else SettingsTab.General))
    }

    @Test
    fun driverStartsExactlyOncePerRun() {
        var startsA = 0
        var startsB = 0
        mount(
            registry(
                a = { _, _, content -> LaunchedEffect(Unit) { startsA++ }; content() },
                b = { _, _, content -> LaunchedEffect(Unit) { startsB++ }; content() },
            )
        )

        publishUnrelatedHostMessages(50)
        repeat(50) { publish(AppMsg.Plugin(A, TickMsg.Inc)) }
        repeat(50) { publish(AppMsg.Plugin(B, TickMsg.Inc)) }
        composeRule.waitForIdle()

        assertEquals(50, (latest.pluginStates[A] as TickModel).n)
        assertEquals(1, startsA)
        assertEquals(1, startsB)
    }

    @Test
    fun driverSeesItsSlice() {
        var sliceA: TickModel? = null
        var sliceB: TickModel? = null
        mount(
            registry(
                a = { _, model, content -> sliceA = model; content() },
                b = { _, model, content -> sliceB = model; content() },
            )
        )
        val bInitial = latest.pluginStates[B]
        assertSame(bInitial, sliceB)

        publish(AppMsg.Plugin(A, TickMsg.Inc))
        composeRule.waitForIdle()

        assertSame(latest.pluginStates[A], sliceA)
        assertEquals(TickModel(1), sliceA)
        assertSame(bInitial, sliceB)
    }

    @Test
    fun liftedPublishRoutesToAppMsgPlugin() {
        mount(
            registry(
                a = { p, _, content -> LaunchedEffect(Unit) { p(TickMsg.Inc) }; content() },
                b = { _, _, content -> content() },
            )
        )
        composeRule.waitForIdle()

        assertEquals(TickModel(1), latest.pluginStates[A])
        assertSame(loaders.single { it.id == B }.initialState, latest.pluginStates[B])
    }

    @Test
    fun liftedPublishIsStable() {
        val seen = mutableListOf<Pair<TickModel, (TickMsg) -> Unit>>()
        mount(
            registry(
                a = { p, model, content -> seen += model to p; content() },
                b = { _, _, content -> content() },
            )
        )

        repeat(10) {
            publish(AppMsg.Plugin(A, TickMsg.Inc))
            composeRule.waitForIdle()
        }

        assertEquals((0..10).map(::TickModel), seen.map { it.first })
        assertTrue(seen.all { it.second === seen.first().second })
    }

    @Test
    fun pluginDriverSharesAppAccessMessages() {
        mount(
            registry(
                a = { _, _, content ->
                    assertSame(LocalApp.current.messages, LocalMessages.current)
                    content()
                },
                b = { _, _, content -> content() },
            )
        )
    }

    @Test
    fun pluginDriverDoesNotReceiveNonEventHostMessages() {
        var count = 0
        mount(
            registry(
                a = { _, _, content ->
                    val messages = LocalMessages.current
                    LaunchedEffect(Unit) { messages.collect { count++ } }
                    content()
                },
                b = { _, _, content -> content() },
            )
        )

        publishUnrelatedHostMessages(3)
        publish(AppMsg.HideSettings)
        composeRule.waitForIdle()

        assertEquals(0, count)
    }

    @Test
    fun credentialsLoadedIsNotAnEvent() {
        val loaded = InternalMsg.CredentialsLoaded("k", "t")
        assertTrue(Event::class.java !in loaded.javaClass.interfaces)
    }

    @Test
    fun hostDriverStillReceivesNonEventHostMessages() {
        val hostSeen = mutableListOf<Any?>()
        val plugins = registry(
            a = { _, _, content -> content() },
            b = { _, _, content -> content() },
        )
        val host = component(
            init = appComponent(ProjectsUiData(), plugins).init(),
            update = { msg: AppMsg, model: AppModel -> appUpdate(msg, model, plugins) },
            view = { _, _ -> Box(Modifier.testTag(HOST_VIEW_TAG)) },
        )
        val hostCapture: Driver<AppModel, AppMsg> = { _, _, content ->
            val messages = LocalMessages.current
            LaunchedEffect(Unit) { messages.collect { hostSeen += it } }
            content()
        }
        val capture: Driver<AppModel, AppMsg> = { p, m, content ->
            publish = p
            latest = m
            content()
        }
        composeRule.setContent { run(host, listOf(hostCapture, capture) + pluginDrivers(plugins)) }
        composeRule.waitForIdle()

        publish(AppMsg.HideSettings)
        composeRule.waitForIdle()

        assertEquals(listOf<Any?>(AppMsg.HideSettings), hostSeen)
    }

    @Test
    fun contentRendersOnceWithTwoPluginDrivers() {
        mount(
            registry(
                a = { _, _, content -> content() },
                b = { _, _, content -> content() },
            )
        )

        composeRule.onAllNodesWithTag(HOST_VIEW_TAG).assertCountEquals(1)
    }
}
