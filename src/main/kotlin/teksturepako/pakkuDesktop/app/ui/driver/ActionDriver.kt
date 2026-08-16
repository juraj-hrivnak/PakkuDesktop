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
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.actions.addSuspend
import teksturepako.pakkuDesktop.app.actions.applyAdditionPlan
import teksturepako.pakkuDesktop.app.actions.checkUpdatesSuspend
import teksturepako.pakkuDesktop.app.actions.exportSuspend
import teksturepako.pakkuDesktop.app.actions.fetchSuspend
import teksturepako.pakkuDesktop.app.actions.updateSuspend
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver

// -- CompositionLocal --

/** Provides a function to launch a named action (suspend block). */
val LocalLaunchAction = compositionLocalOf<(name: String, block: suspend () -> Unit) -> Unit> { { _, _ -> } }

// -- actionDriver --

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

    // React to fetch intent
    LaunchedEffect(model.modpack.wantsFetch) {
        if (!model.modpack.wantsFetch) return@LaunchedEffect
        val lockFile   = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get() ?: return@LaunchedEffect
        launchAction("Fetching") {
            fetchSuspend(lockFile, configFile) { toast ->
                withContext(Dispatchers.Main) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
                }
            }
        }
    }

    // React to update intent
    LaunchedEffect(model.modpack.wantsUpdate) {
        if (!model.modpack.wantsUpdate) return@LaunchedEffect
        val lockFile   = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get() ?: return@LaunchedEffect
        val ids = latestModel.modpack.selectedPakkuIds
        val projects = lockFile.getAllProjects().filter { it.pakkuId in ids }
        launchAction("Updating") {
            val updatedIds = updateSuspend(
                lockFile = lockFile,
                configFile = configFile,
                projects = projects,
                updatePreviews = latestModel.modpack.updatePreviews,
            ) { toast ->
                withContext(Dispatchers.Main) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
                }
            }
            // Reload after update writes the lock file; keep status previews and mark applied.
            val newLockFile = LockFile.readToResult()
            val newConfigFile = ConfigFile.readToResult()
            withContext(Dispatchers.Main) {
                latestPublish(AppMsg.Modpack(ModpackMsg.Loaded(newLockFile, newConfigFile, retainUpdatePreviews = true)))
                if (updatedIds.isNotEmpty()) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.UpdatesApplied(updatedIds)))
                }
            }
        }
    }

    // React to status check (pakku status — no lock write)
    LaunchedEffect(model.modpack.wantsStatusCheck) {
        if (!model.modpack.wantsStatusCheck) return@LaunchedEffect
        val lockFile   = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get()
        launchAction("Checking updates") {
            val previews = checkUpdatesSuspend(lockFile, configFile) { toast ->
                withContext(Dispatchers.Main) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
                }
            }
            withContext(Dispatchers.Main) {
                latestPublish(AppMsg.Modpack(ModpackMsg.StatusCheckCompleted(previews)))
            }
        }
    }

    // DnD / auto add from query
    LaunchedEffect(model.modpack.pendingAddQuery) {
        val query = model.modpack.pendingAddQuery ?: return@LaunchedEffect
        val lockFile = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get() ?: return@LaunchedEffect
        launchAction("Adding") {
            addSuspend(lockFile, configFile, query) { toast ->
                withContext(Dispatchers.Main) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
                }
            }
            val newLockFile = LockFile.readToResult()
            val newConfigFile = ConfigFile.readToResult()
            withContext(Dispatchers.Main) {
                latestPublish(AppMsg.Modpack(ModpackMsg.Loaded(newLockFile, newConfigFile, retainUpdatePreviews = false)))
                latestPublish(AppMsg.Modpack(ModpackMsg.MutationCompleted))
            }
        }
    }

    // Confirmed addition plan from Add popup
    LaunchedEffect(model.modpack.pendingAdditionPlan) {
        val plan = model.modpack.pendingAdditionPlan ?: return@LaunchedEffect
        val lockFile = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get() ?: return@LaunchedEffect
        launchAction("Adding") {
            applyAdditionPlan(lockFile, configFile, plan) { toast ->
                withContext(Dispatchers.Main) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
                }
            }
            val newLockFile = LockFile.readToResult()
            val newConfigFile = ConfigFile.readToResult()
            withContext(Dispatchers.Main) {
                latestPublish(AppMsg.Modpack(ModpackMsg.Loaded(newLockFile, newConfigFile, retainUpdatePreviews = false)))
                latestPublish(AppMsg.Modpack(ModpackMsg.MutationCompleted))
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
