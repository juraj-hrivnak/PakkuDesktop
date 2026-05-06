/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.github.michaelbull.result.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.actions.exportSuspend
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver

// ---------------------------------------------------------------------------
// CompositionLocal — provided to the whole view tree by actionDriver
// ---------------------------------------------------------------------------

/** Provides a function to launch a named action (suspend block). */
val LocalLaunchAction = compositionLocalOf<(name: String, block: suspend () -> Unit) -> Unit> { { _, _ -> } }

// ---------------------------------------------------------------------------
// actionDriver — owns the running action coroutine
// ---------------------------------------------------------------------------

val actionDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val latestPublish by rememberUpdatedState(publish)
    val latestModel by rememberUpdatedState(model)
    val coroutineScope = rememberCoroutineScope()
    val currentJob = remember { mutableStateOf<Job?>(null) }

    val launchAction: (String, suspend () -> Unit) -> Unit = remember {
        { name, block ->
            if (currentJob.value?.isActive != true) {
                currentJob.value = coroutineScope.launch {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ActionStarted(name)))
                    try {
                        withContext(Dispatchers.IO) { block() }
                    } finally {
                        currentJob.value = null
                        latestPublish(AppMsg.Modpack(ModpackMsg.ActionFinished))
                    }
                }
            }
        }
    }

    // React to export intent
    LaunchedEffect(model.modpack.wantsExport) {
        if (!model.modpack.wantsExport) return@LaunchedEffect
        val lockFile   = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get() ?: return@LaunchedEffect
        launchAction("Exporting") {
            exportSuspend(lockFile, configFile) { toast ->
                withContext(Dispatchers.Main) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
                }
            }
        }
    }

    // React to terminate request
    LaunchedEffect(model.modpack.wantsTerminateAction) {
        if (!model.modpack.wantsTerminateAction) return@LaunchedEffect
        currentJob.value?.cancelAndJoin()
        currentJob.value = null
        latestPublish(AppMsg.Modpack(ModpackMsg.ActionFinished))
    }

    CompositionLocalProvider(LocalLaunchAction provides launchAction) {
        content()
    }
}

