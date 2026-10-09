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

// -- windowDiskDriver --

fun windowDiskDriver(
    onQuit: () -> Unit,
    initialWindowData: WindowData,
): Driver<AppModel, AppMsg> = { publish, model, content ->
    val windowState = LocalWindowState.current

    LaunchedEffect(Unit) {
        publish(AppMsg.WindowLoaded(initialWindowData))
    }

    LaunchedEffect(model.wantsQuit) {
        if (!model.wantsQuit) return@LaunchedEffect
        val data = snapshotWindowData(windowState)
        withContext(Dispatchers.IO) { data.write() }
        publish(AppMsg.QuitReady)
        onQuit()
    }

    content()
}

