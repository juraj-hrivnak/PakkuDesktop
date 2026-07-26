/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.application.window.LocalWindowState
import teksturepako.pakkuDesktop.app.ui.application.window.snapshotWindowData
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

// ---------------------------------------------------------------------------
// windowDiskDriver — loads window data once; saves + quits when wantsQuit
// ---------------------------------------------------------------------------

fun windowDiskDriver(
    onQuit: () -> Unit,
): Driver<AppModel, AppMsg> = { publish, model, content ->
    val windowState = LocalWindowState.current

    LaunchedEffect(Unit) {
        val data = withContext(Dispatchers.IO) { WindowData.readOrNew() }
        publish(AppMsg.WindowLoaded(data))
    }

    LaunchedEffect(model.wantsQuit) {
        if (!model.wantsQuit) return@LaunchedEffect
        val data = snapshotWindowData(windowState)
        withContext(Dispatchers.IO) { data.write() }
        onQuit()
        publish(AppMsg.QuitReady)
    }

    content()
}

