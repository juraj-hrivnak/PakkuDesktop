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
import kotlin.coroutines.cancellation.CancellationException
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.actions.actionErrorToast
import teksturepako.pakkuDesktop.app.actions.addSuspend
import teksturepako.pakkuDesktop.app.actions.applyAdditionPlan
import teksturepako.pakkuDesktop.app.actions.buildAdditionPlan
import teksturepako.pakkuDesktop.app.actions.checkUpdatesSuspend
import teksturepako.pakkuDesktop.app.actions.exportSuspend
import teksturepako.pakkuDesktop.app.actions.fetchApplySuspend
import teksturepako.pakkuDesktop.app.actions.fetchDetectSuspend
import teksturepako.pakkuDesktop.app.actions.actionInfoToast
import teksturepako.pakkuDesktop.app.actions.syncApplySuspend
import teksturepako.pakkuDesktop.app.actions.syncDetectSuspend
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.actions.updateSuspend
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.selectedFetchCleanupPaths
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.selectedFetchDownloads
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.selectedFetchOverridePaths
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
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            latestPublish(
                                AppMsg.Modpack(
                                    ModpackMsg.ToastAdded(
                                        actionErrorToast(e.message ?: e::class.simpleName ?: "Unexpected error"),
                                    ),
                                ),
                            )
                        }
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

    // Refresh: scan lock vs disk without writing
    LaunchedEffect(model.modpack.wantsFetchDetect) {
        if (!model.modpack.wantsFetchDetect) return@LaunchedEffect
        val lockFile = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get()
        launchAction("Scanning files") {
            val result = fetchDetectSuspend(lockFile, configFile) { toast ->
                withContext(Dispatchers.Main) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
                }
            }
            withContext(Dispatchers.Main) {
                latestPublish(AppMsg.Modpack(ModpackMsg.FetchDetectCompleted(result)))
                if (result.isEmpty) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(actionInfoToast("Nothing to fetch."))))
                }
            }
        }
    }

    // Fetch apply: selected preview rows (download / override copy / cleanup)
    LaunchedEffect(model.modpack.wantsFetch) {
        if (!model.modpack.wantsFetch) return@LaunchedEffect
        val lockFile   = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get() ?: return@LaunchedEffect
        val downloads = latestModel.modpack.selectedFetchDownloads()
        val overrides = latestModel.modpack.selectedFetchOverridePaths()
        val cleanup = latestModel.modpack.selectedFetchCleanupPaths()
        launchAction("Fetching") {
            fetchApplySuspend(
                lockFile = lockFile,
                configFile = configFile,
                downloadProjects = downloads,
                overrideRelativePaths = overrides,
                cleanAbsolutePaths = cleanup,
                onProgress = { completed, total ->
                    withContext(Dispatchers.Main) {
                        latestPublish(AppMsg.Modpack(ModpackMsg.FetchProgressUpdated(completed, total)))
                    }
                },
                onPhase = { name ->
                    withContext(Dispatchers.Main) {
                        latestPublish(AppMsg.Modpack(ModpackMsg.ActionRenamed(name)))
                    }
                },
            ) { toast ->
                withContext(Dispatchers.Main) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
                }
            }
            withContext(Dispatchers.Main) {
                latestPublish(AppMsg.Modpack(ModpackMsg.FilesPreviewCleared))
            }
        }
    }

    // React to update intent
    LaunchedEffect(model.modpack.wantsUpdate) {
        if (!model.modpack.wantsUpdate) return@LaunchedEffect
        val lockFile   = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get() ?: return@LaunchedEffect
        val keys = latestModel.modpack.selectedProjectKeys
        val projects = lockFile.getAllProjects().filter { it.uiKey() in keys }
        launchAction("Updating") {
            val updatedKeys = updateSuspend(
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
                if (updatedKeys.isNotEmpty()) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.UpdatesApplied(updatedKeys)))
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

    // Sync detect (pakku sync — disk fingerprint vs lock, no lock write)
    LaunchedEffect(model.modpack.wantsSyncDetect) {
        if (!model.modpack.wantsSyncDetect) return@LaunchedEffect
        val lockFile = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get()
        launchAction("Detecting files") {
            val result = syncDetectSuspend(lockFile, configFile) { toast ->
                withContext(Dispatchers.Main) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
                }
            }
            withContext(Dispatchers.Main) {
                latestPublish(
                    AppMsg.Modpack(
                        ModpackMsg.SyncDetectCompleted(
                            added = result.added.toList(),
                            removed = result.removed.toList(),
                            updated = result.updated.toList(),
                        ),
                    ),
                )
                if (result.added.isEmpty() && result.removed.isEmpty() && result.updated.isEmpty()) {
                    latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(actionInfoToast("Nothing to sync."))))
                }
            }
        }
    }

    // Sync apply: add / remove / update lock from the last detect result
    LaunchedEffect(model.modpack.pendingSyncApply) {
        if (!model.modpack.pendingSyncApply) return@LaunchedEffect
        val lockFile = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        val configFile = latestModel.modpack.configFile?.get() ?: return@LaunchedEffect
        val added = latestModel.modpack.syncAdded
        val removed = latestModel.modpack.syncRemoved
        val updated = latestModel.modpack.syncUpdated
        val keys = latestModel.modpack.selectedProjectKeys
        launchAction("Syncing") {
            syncApplySuspend(
                lockFile = lockFile,
                configFile = configFile,
                added = added,
                removed = removed,
                updated = updated,
                acceptedKeys = keys,
            ) { toast ->
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

    // Add dialog: resolve plan (Pakku createAdditionRequest + dep preview)
    LaunchedEffect(
        model.modpack.addDialog.pendingResolveQuery,
        model.modpack.addDialog.pendingResolveProjects,
    ) {
        val query = model.modpack.addDialog.pendingResolveQuery
        val projects = model.modpack.addDialog.pendingResolveProjects
        if (query == null && projects == null) return@LaunchedEffect
        val lockFile = latestModel.modpack.lockFile?.get() ?: return@LaunchedEffect
        try {
            val plan = withContext(Dispatchers.IO) {
                val onProgress: suspend (String) -> Unit = { status ->
                    withContext(Dispatchers.Main) {
                        latestPublish(AppMsg.Modpack(ModpackMsg.AddResolveProgress(status)))
                    }
                }
                val onToast: suspend (teksturepako.pakkuDesktop.pkui.component.toast.ToastData) -> Unit = { toast ->
                    withContext(Dispatchers.Main) {
                        latestPublish(AppMsg.Modpack(ModpackMsg.ToastAdded(toast)))
                    }
                }
                if (projects != null) {
                    buildAdditionPlan(
                        lockFile = lockFile,
                        projects = projects,
                        onProgress = onProgress,
                        onToast = onToast,
                    )
                } else {
                    buildAdditionPlan(
                        lockFile = lockFile,
                        query = query.orEmpty(),
                        onProgress = onProgress,
                        onToast = onToast,
                    )
                }
            }
            withContext(Dispatchers.Main) {
                latestPublish(AppMsg.Modpack(ModpackMsg.AddPlanBuilt(plan)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            e.printStackTrace()
            latestPublish(
                AppMsg.Modpack(
                    ModpackMsg.ToastAdded(
                        actionErrorToast(e.message ?: e::class.simpleName ?: "Unexpected error"),
                    ),
                ),
            )
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
