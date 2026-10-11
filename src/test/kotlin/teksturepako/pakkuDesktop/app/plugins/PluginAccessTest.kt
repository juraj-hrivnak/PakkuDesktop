/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.rules.RuleChain
import teksturepako.pakkuDesktop.app.api.AppAccess
import teksturepako.pakkuDesktop.app.api.LocalApp
import teksturepako.pakkuDesktop.app.data.ProjectsUiData
import teksturepako.pakkuDesktop.app.ui.appComponent
import teksturepako.pakkuDesktop.app.ui.appUpdate
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.InternalMsg
import teksturepako.pakkuDesktop.app.ui.model.SettingsCredentials
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.elm.LocalMessages
import teksturepako.pakkuDesktop.elm.assumeAwtForComposeUiTests
import teksturepako.pakkuDesktop.elm.component
import teksturepako.pakkuDesktop.elm.run
import teksturepako.pakkuDesktop.plugin.PluginId
import teksturepako.pakkuDesktop.plugin.PluginTrust
import teksturepako.pakkuDesktop.plugin.TypedPlugin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val ID = PluginId("access")

private fun accessPlugin(driver: Driver<Unit, Unit>) = TypedPlugin(
    modelClass = Unit::class,
    msgClass = Unit::class,
    component = component(init = Unit, update = { _, model -> model }, view = { _, _ -> }),
    drivers = listOf(driver),
)

private val credentials = AppMsg.Internal(InternalMsg.CredentialsLoaded("cf-key", "gh-token"))

class PluginAccessTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(assumeAwtForComposeUiTests()).around(composeRule)

    private val loaded = mutableListOf<LoadedPlugin>()

    private lateinit var publish: (AppMsg) -> Unit
    private lateinit var latest: AppModel
    private lateinit var app: AppAccess

    @AfterTest
    fun cleanup() {
        loaded.forEach { it.loader.close() }
        loaded.clear()
    }

    private fun mount(trust: PluginTrust, driver: Driver<Unit, Unit> = { _, _, content -> content() }) {
        val plugin = loadedPluginOf(ID.value, accessPlugin { p, m, content ->
            app = LocalApp.current
            driver(p, m, content)
        }, trust).also { loaded += it }
        val plugins = PluginRegistry(mapOf(plugin.id to plugin), emptyList())
        val host = component(
            init = appComponent(ProjectsUiData(), plugins).init(),
            update = { msg: AppMsg, model: AppModel -> appUpdate(msg, model, plugins) },
            view = { _, _ -> Box {} },
        )
        val capture: Driver<AppModel, AppMsg> = { p, m, content ->
            publish = p
            latest = m
            content()
        }
        composeRule.setContent { run(host, listOf(capture) + pluginDrivers(plugins)) }
        composeRule.waitForIdle()
    }

    @Test
    fun pluginCanPublishHostMessages() {
        mount(PluginTrust.Standard)
        composeRule.runOnIdle { app.publish(AppMsg.ShowActivation) }
        composeRule.waitForIdle()
        assertEquals(AppScreen.Activation, latest.screen)
    }

    @Test
    fun standardInternalPublishIsDropped() {
        mount(PluginTrust.Standard)
        composeRule.runOnIdle { app.publish(AppMsg.Internal(InternalMsg.ProActivationReported(true))) }
        composeRule.waitForIdle()
        assertEquals(null, latest.isProActivated)
    }

    @Test
    fun officialInternalPublishIsApplied() {
        mount(PluginTrust.Official)
        composeRule.runOnIdle { app.publish(AppMsg.Internal(InternalMsg.ProActivationReported(true))) }
        composeRule.waitForIdle()
        assertEquals(true, latest.isProActivated)
    }

    @Test
    fun standardModelHasNoInternalState() {
        mount(PluginTrust.Standard)
        composeRule.runOnIdle { publish(credentials) }
        composeRule.waitForIdle()

        assertEquals(SettingsCredentials("cf-key", "gh-token"), latest.internal.settingsCredentials)
        assertNull(app.model.value.internal.settingsCredentials)
    }

    @Test
    fun officialModelSeesInternalState() {
        mount(PluginTrust.Official)
        composeRule.runOnIdle { publish(credentials) }
        composeRule.waitForIdle()

        assertEquals(SettingsCredentials("cf-key", "gh-token"), app.model.value.internal.settingsCredentials)
    }

    @Test
    fun standardMessagesHaveNoInternal() {
        val seenApp = mutableListOf<AppMsg>()
        val seenLocal = mutableListOf<Any?>()
        mount(PluginTrust.Standard) { _, _, content ->
            val access = LocalApp.current
            val local = LocalMessages.current
            LaunchedEffect(Unit) { access.messages.collect { seenApp += it } }
            LaunchedEffect(Unit) { local.collect { seenLocal += it } }
            content()
        }
        composeRule.runOnIdle {
            publish(credentials)
            publish(AppMsg.ShowActivation)
        }
        composeRule.waitForIdle()

        assertEquals(listOf<AppMsg>(AppMsg.ShowActivation), seenApp)
        assertEquals(listOf<Any?>(AppMsg.ShowActivation), seenLocal)
    }

    @Test
    fun officialMessagesIncludeInternal() {
        val seen = mutableListOf<AppMsg>()
        mount(PluginTrust.Official) { _, _, content ->
            val access = LocalApp.current
            LaunchedEffect(Unit) { access.messages.collect { seen += it } }
            content()
        }
        composeRule.runOnIdle {
            publish(credentials)
            publish(AppMsg.ShowActivation)
        }
        composeRule.waitForIdle()

        assertEquals(listOf(credentials, AppMsg.ShowActivation), seen)
    }
}
