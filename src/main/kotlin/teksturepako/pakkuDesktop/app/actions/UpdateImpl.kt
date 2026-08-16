/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import com.github.michaelbull.result.getOrElse
import io.klogging.logger
import teksturepako.pakku.api.actions.update.updateMultipleProjectsWithFiles
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.model.ProjectUpdateInfo
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

private val updateLogger = logger("UpdateImpl")

/**
 * Same flow as `pakku update` ([teksturepako.pakku.cli.cmd.Update]):
 * fetch via [updateMultipleProjectsWithFiles] (`numberOfFiles = 1`), then
 * [LockFile.update] each result and [LockFile.write].
 *
 * When a status-check preview exists for a project, apply the user-selected
 * file(s) instead of re-fetching that project.
 */
suspend fun updateSuspend(
    lockFile: LockFile,
    configFile: ConfigFile,
    projects: Collection<Project>,
    updatePreviews: Map<String, ProjectUpdateInfo>? = null,
    onToast: suspend (ToastData) -> Unit,
): Set<String> {
    if (projects.isEmpty()) {
        onToast(actionInfoToast("No projects selected to update."))
        return emptySet()
    }

    val platforms = lockFile.getPlatforms().getOrElse { error ->
        onToast(actionErrorToast(error))
        return emptySet()
    }

    val fromPreview = mutableListOf<Project>()
    val needNetwork = mutableListOf<Project>()
    for (project in projects) {
        val preview = project.pakkuId?.let { updatePreviews?.get(it) }
            ?.takeUnless { it.applied }
        if (preview != null) fromPreview += preview.projectWithSelectedFiles()
        else needNetwork += project
    }

    val fromNetwork = if (needNetwork.isEmpty()) {
        emptyList()
    } else {
        updateMultipleProjectsWithFiles(
            onError = { error ->
                updateLogger.error(error.toUiMessage())
                onToast(actionErrorToast(error))
            },
            mcVersions = lockFile.getMcVersions(),
            loaders = lockFile.getLoaders(),
            projects = needNetwork.toMutableSet(),
            configFile = configFile,
            platforms = platforms,
            numberOfFiles = 1,
        )
    }

    val updatedProjects = (fromPreview + fromNetwork).distinctBy { it.pakkuId }

    // Same as CLI: replace by identity (`isAlmostTheSameAs`), never `updateAll`/`equals`.
    for (updatedProject in updatedProjects) {
        lockFile.update(updatedProject)
    }

    when {
        updatedProjects.isEmpty() && projects.isNotEmpty() ->
            onToast(actionInfoToast("All selected projects are up to date."))
        updatedProjects.size == 1 ->
            onToast(actionInfoToast("Updated 1 project."))
        updatedProjects.isNotEmpty() ->
            onToast(actionInfoToast("Updated ${updatedProjects.size} projects."))
    }

    lockFile.write()
    return updatedProjects.mapNotNull { it.pakkuId }.toSet()
}
