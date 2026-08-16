/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.github.michaelbull.result.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.actions.applyRemovalPlan
import teksturepako.pakkuDesktop.app.actions.initSuspend
import teksturepako.pakkuDesktop.app.actions.removeSuspend
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.InitSpec
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.Driver

/**
 * Disk mutations for remove / init — write then reload lock+config.
 * Network add runs in [actionDriver].
 */
val projectMutationDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val latestPublish by rememberUpdatedState(publish)
    val latestModel by rememberUpdatedState(model)

    suspend fun reload() {
        val newLockFile = withContext(Dispatchers.IO) { LockFile.readToResult() }
        val newConfigFile = withContext(Dispatchers.IO) { ConfigFile.readToResult() }
        latestPublish(AppMsg.Modpack(ModpackMsg.Loaded(newLockFile, newConfigFile, retainUpdatePreviews = false)))
        latestPublish(AppMsg.Modpack(ModpackMsg.MutationCompleted))
    }

    suspend fun toast(toast: teksturepako.pakkuDesktop.pkui.component.toast.ToastData) {
        withContext(Dispatchers.Main) {
            latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
        }
    }

    // Remove by ids (auto recommended deps) — e.g. fallback paths
    LaunchedEffect(model.modpack.pendingRemovalIds) {
        val ids = model.modpack.pendingRemovalIds ?: return@LaunchedEffect
        val lockFile = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val projects = lockFile.getAllProjects().filter { it.pakkuId in ids }

        withContext(Dispatchers.IO) {
            removeSuspend(lockFile, projects, ::toast)
        }
        latestPublish(AppMsg.Modpack(ModpackMsg.ProjectsCleared()))
        reload()
    }

    // Remove confirmed plan
    LaunchedEffect(model.modpack.pendingRemovalPlan) {
        val plan = model.modpack.pendingRemovalPlan ?: return@LaunchedEffect
        val lockFile = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect

        withContext(Dispatchers.IO) {
            applyRemovalPlan(lockFile, plan, ::toast)
        }
        latestPublish(AppMsg.Modpack(ModpackMsg.ProjectsCleared()))
        reload()
    }

    // Init
    LaunchedEffect(model.modpack.wantsInit, model.modpack.pendingInitSpec) {
        if (!model.modpack.wantsInit) return@LaunchedEffect
        val spec = model.modpack.pendingInitSpec ?: InitSpec(
            name = latestModel.profile.data.currentProfile?.name ?: "Modpack",
            mcVersion = "1.20.1",
            loader = "fabric",
            target = "modrinth",
        )

        withContext(Dispatchers.IO) {
            initSuspend(spec, ::toast)
        }
        latestPublish(AppMsg.HideNewModpack)
        reload()
    }

    content()
}
