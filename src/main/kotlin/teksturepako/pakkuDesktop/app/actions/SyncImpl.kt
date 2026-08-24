/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.klogging.logger
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.errors.AlreadyAdded
import teksturepako.pakku.api.actions.sync.SyncResult
import teksturepako.pakku.api.actions.sync.syncProjects
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.overrides.readManualOverrides
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.model.SyncKind
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

private val syncLogger = logger("SyncImpl")

suspend fun syncDetectSuspend(
    lockFile: LockFile,
    configFile: ConfigFile?,
    onToast: suspend (ToastData) -> Unit,
): SyncResult {
    val platforms = lockFile.getPlatforms().getOrElse { error ->
        onToast(actionErrorToast(error))
        return SyncResult(emptySet(), emptySet(), emptySet())
    }
    val manualOverrides = readManualOverrides(configFile)
    return syncProjects(
        onError = { error ->
            syncLogger.error(error.toUiMessage())
            onToast(actionErrorToast(error))
        },
        lockFile = lockFile,
        configFile = configFile,
        platforms = platforms,
        manualOverrides = manualOverrides,
    )
}

suspend fun syncApplySuspend(
    lockFile: LockFile,
    configFile: ConfigFile,
    added: List<Project>,
    removed: List<Project>,
    updated: List<Project>,
    acceptedKeys: Set<String>,
    onToast: suspend (ToastData) -> Unit,
) {
    val addSel = added.filter { acceptedKeys.isEmpty() || it.uiKey() in acceptedKeys }
    val remSel = removed.filter { acceptedKeys.isEmpty() || it.uiKey() in acceptedKeys }
    val updSel = updated.filter { acceptedKeys.isEmpty() || it.uiKey() in acceptedKeys }

    if (addSel.isEmpty() && remSel.isEmpty() && updSel.isEmpty()) {
        onToast(actionInfoToast("Nothing to sync."))
        return
    }

    val platforms = lockFile.getPlatforms().getOrElse { error ->
        onToast(actionErrorToast(error))
        return
    }

    if (addSel.isNotEmpty()) {
        for (projectIn in addSel) {
            projectIn.createAdditionRequest(
                onError = { error ->
                    if (error !is AlreadyAdded) {
                        syncLogger.error(error.toUiMessage())
                        onToast(actionErrorToast(error))
                    }
                },
                onSuccess = { project, _, replacing, _ ->
                    if (replacing == null) lockFile.add(project) else lockFile.update(project)
                    lockFile.linkProjectToDependents(project)
                },
                lockFile = lockFile,
                platforms = platforms,
                strict = false,
            )
        }
        lockFile.write()
        configFile.write()
        onToast(actionInfoToast("Added ${addSel.size} project${if (addSel.size == 1) "" else "s"}."))
    }

    if (remSel.isNotEmpty()) {
        val plan = buildRemovalPlan(lockFile, remSel)
        val acceptedRoots = plan.projects.filter {
            acceptedKeys.isEmpty() || it.key in acceptedKeys
        }.ifEmpty { plan.projects.filter { it.isRecommended } }
        val acceptedIds = acceptedRoots.map { it.key }.toSet()
        val recommendedDepIds = acceptedRoots
            .flatMap { it.orphanedChildren }
            .filter { it.isRecommended }
            .map { it.key }
            .toSet()
        applyRemovalPlan(
            lockFile,
            RemovalPlan(
                projects = acceptedRoots,
                orphanedDeps = plan.orphansFor(acceptedIds, recommendedDepIds),
                messages = plan.messages,
            ),
            onToast,
        )
    }

    if (updSel.isNotEmpty()) {
        for (project in updSel) {
            lockFile.update(project)
            project.getSubpath()?.onSuccess { subpath ->
                configFile.setProjectConfig(project, lockFile) { _ ->
                    this.subpath = subpath
                }
            }?.onFailure { error ->
                onToast(actionErrorToast(error))
            }
        }
        lockFile.write()
        configFile.write()
        onToast(actionInfoToast("Updated ${updSel.size} project${if (updSel.size == 1) "" else "s"}."))
    }
}

fun Project.syncKind(
    added: List<Project>,
    removed: List<Project>,
    updated: List<Project>,
): SyncKind? = when {
    added.any { it isAlmostTheSameAs this } -> SyncKind.Added
    removed.any { it isAlmostTheSameAs this } -> SyncKind.Removed
    updated.any { it isAlmostTheSameAs this } -> SyncKind.Updated
    else -> null
}
