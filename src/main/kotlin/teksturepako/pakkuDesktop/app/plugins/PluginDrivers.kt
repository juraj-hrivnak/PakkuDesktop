/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import io.klogging.noCoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.shareIn
import teksturepako.pakkuDesktop.app.api.AppAccess
import teksturepako.pakkuDesktop.app.api.LocalApp
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.withoutInternal
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.elm.Event
import teksturepako.pakkuDesktop.elm.LocalMessages
import teksturepako.pakkuDesktop.plugin.PluginTrust

private val logger = noCoLogger("PluginAccess")

/** One [AppAccess] per trust level, built once per run. */
internal class PluginAccess(private val official: AppAccess, private val standard: AppAccess) {
    fun of(trust: PluginTrust): AppAccess = when (trust) {
        PluginTrust.Official -> official
        PluginTrust.Standard -> standard
    }
}

/** Host-only. Null outside `run()` (view-only tests); plugins then get no [LocalApp]. */
internal val LocalPluginAccess = staticCompositionLocalOf<PluginAccess?> { null }

/** One shared collector for the process; plugins observe [Event] implementations only. */
internal fun pluginVisibleMessages(upstream: SharedFlow<*>, scope: CoroutineScope): SharedFlow<Event> =
    upstream
        .filterIsInstance<Event>()
        .shareIn(scope, SharingStarted.Eagerly, replay = 0)

/**
 * Every loaded plugin's drivers, lifted to the host model, after the driver that builds
 * [LocalApp]. Build once in `main()` and append to the end of the `run()` driver list so
 * plugin drivers sit inside every host driver.
 *
 * A lifted driver sees only its plugin's `pluginStates` slice, and its `publish` wraps each
 * message in [AppMsg.Plugin]. Under [WithPluginAccess], [LocalApp.messages] and
 * [LocalMessages] are the same trust-filtered stream of [Event]s that also implement
 * [AppMsg]; host drivers must stay before `pluginDrivers()` in the list.
 */
fun pluginDrivers(registry: PluginRegistry): List<Driver<AppModel, AppMsg>> =
    listOf(pluginAccessDriver) +
        registry.loadedList.flatMap { plugin -> plugin.erasedDrivers.map { plugin.lift(it) } }

private val pluginAccessDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val messages = LocalMessages.current
    val scope = rememberCoroutineScope()
    val current = rememberUpdatedState(model)
    val pluginVisible = remember(messages, scope) { pluginVisibleMessages(messages, scope) }
    val access = remember(publish, pluginVisible) { pluginAccess(publish, current, pluginVisible, scope) }
    CompositionLocalProvider(LocalPluginAccess provides access) { content() }
}

private fun pluginAccess(
    publish: (AppMsg) -> Unit,
    model: State<AppModel>,
    pluginVisible: SharedFlow<Event>,
    scope: CoroutineScope,
): PluginAccess {
    val all = pluginVisible.filterIsInstance<AppMsg>().shareIn(scope, SharingStarted.Eagerly, replay = 0)
    val public = all.filterNot { it is AppMsg.Internal }.shareIn(scope, SharingStarted.Eagerly, replay = 0)
    val publicModel = derivedStateOf(referentialEqualityPolicy()) { model.value.withoutInternal() }
    val publicPublish: (AppMsg) -> Unit = { msg ->
        if (msg is AppMsg.Internal) {
            logger.warn("dropped ${msg.msg::class.simpleName} from a ${PluginTrust.Standard} plugin")
        } else {
            publish(msg)
        }
    }
    return PluginAccess(
        official = AppAccess(PluginTrust.Official, model, publish, all),
        standard = AppAccess(PluginTrust.Standard, publicModel, publicPublish, public),
    )
}

/** Plugin drivers and views: [LocalApp] plus matching [LocalMessages] ([AppAccess.messages]). */
@Composable
internal fun WithPluginAccess(trust: PluginTrust, content: @Composable () -> Unit) {
    val access = LocalPluginAccess.current?.of(trust)
    if (access == null) {
        content()
    } else {
        CompositionLocalProvider(
            LocalApp provides access,
            LocalMessages provides access.messages,
            content = content,
        )
    }
}

private fun LoadedPlugin.lift(driver: Driver<Any?, Any?>): Driver<AppModel, AppMsg> {
    val id = id
    val trust = trust
    return { publish, model, content ->
        val lifted = remember(publish) { { msg: Any? -> publish(AppMsg.Plugin(id, msg)) } }
        WithPluginAccess(trust) {
            driver(lifted, model.pluginStates.getValue(id), content)
        }
    }
}
