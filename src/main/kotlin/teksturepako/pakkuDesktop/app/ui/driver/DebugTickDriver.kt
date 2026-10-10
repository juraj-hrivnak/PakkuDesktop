/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import teksturepako.pakkuDesktop.app.actions.actionInfoToast
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver

val isDebugTickEnabled: Boolean = System.getProperty("pakku.debug.tick").toBoolean()

/** Debug only: drives real hot-path messages at ~60 Hz for heap profiling. Enable with `-Dpakku.debug.tick=true`. */
val debugTickDriver: Driver<AppModel, AppMsg> = { publish, _, content ->
    LaunchedEffect(Unit) {
        var n = 0L
        while (isActive) {
            delay(16)
            n++
            publish(AppMsg.Modpack(ModpackMsg.FetchProgressUpdated(n % 1000, 1000)))
            if (n % 600 == 0L) publish(AppMsg.Modpack(ModpackMsg.ToastAdded(actionInfoToast("tick $n"))))
        }
    }
    content()
}
