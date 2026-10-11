/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.api

import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.SharedFlow
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.plugin.PluginTrust

/**
 * The host app, as one plugin sees it. The host provides a separate instance around each
 * plugin's drivers and view; read it with `LocalApp.current`.
 *
 * Below [PluginTrust.Official]:
 * - [model] has an empty [AppModel.internal],
 * - [publish] drops [AppMsg.Internal],
 * - [messages] emits only types that implement [teksturepako.pakkuDesktop.elm.Event] and
 *   [AppMsg]; standard trust also drops [AppMsg.Internal]. Matches `LocalMessages` in
 *   plugin drivers and views.
 */
class AppAccess(
    val trust: PluginTrust,
    /** Latest host model. Read `.value` where you need it so only that scope recomposes. */
    val model: State<AppModel>,
    /** Runs a host message through the host's `update`, like any host view or driver. */
    val publish: (AppMsg) -> Unit,
    /** Host messages after `update`, hot, no replay. */
    val messages: SharedFlow<AppMsg>,
)

val LocalApp = staticCompositionLocalOf<AppAccess> {
    error("LocalApp not provided — only plugin drivers and views get it")
}
