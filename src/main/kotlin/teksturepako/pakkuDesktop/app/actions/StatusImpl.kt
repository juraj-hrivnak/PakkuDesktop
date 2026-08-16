/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import io.klogging.logger
import teksturepako.pakku.api.actions.update.updateMultipleProjectsWithFiles
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakkuDesktop.app.ui.model.ProjectFileChange
import teksturepako.pakkuDesktop.app.ui.model.ProjectUpdateInfo
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

private val statusLogger = logger("StatusImpl")

/** How many newest matching files to keep per provider for the update chooser. */
private const val UPDATE_CANDIDATE_FILES = 10

/**
 * Pakku `status`-equivalent: network check via [updateMultipleProjectsWithFiles]
 * without writing the lock file. Returns update previews keyed by current [Project.pakkuId].
 *
 * Candidate lists come from [Platform.requestMultipleProjectsWithFiles] (and GitHub), because
 * Pakku's update combine currently collapses to one file per provider even when
 * [numberOfFiles] > 1.
 */
suspend fun checkUpdatesSuspend(
    lockFile: LockFile,
    configFile: ConfigFile?,
    onToast: suspend (ToastData) -> Unit,
): Map<String, ProjectUpdateInfo> {
    val platforms = lockFile.getPlatforms().getOrElse { error ->
        onToast(actionErrorToast(error))
        return emptyMap()
    }

    val currentProjects = lockFile.getAllProjects()
    if (currentProjects.isEmpty()) {
        onToast(actionInfoToast("No projects to check."))
        return emptyMap()
    }

    val mcVersions = lockFile.getMcVersions()
    val loaders = lockFile.getLoaders()

    val updatedProjects = updateMultipleProjectsWithFiles(
        onError = { error ->
            statusLogger.error(error.toUiMessage())
            onToast(actionErrorToast(error))
        },
        mcVersions = mcVersions,
        loaders = loaders,
        projects = currentProjects.toMutableSet(),
        configFile = configFile,
        platforms = platforms,
        numberOfFiles = 1,
    )

    val preview = buildUpdatePreview(
        currentProjects = currentProjects,
        updatedProjects = updatedProjects,
        platforms = platforms,
        mcVersions = mcVersions,
        loaders = loaders,
    )
    when {
        preview.isEmpty() -> onToast(actionInfoToast("All projects are up to date."))
        preview.size == 1 -> onToast(actionInfoToast("1 project has an update available."))
        else -> onToast(actionInfoToast("${preview.size} projects have updates available."))
    }
    return preview
}

suspend fun buildUpdatePreview(
    currentProjects: List<Project>,
    updatedProjects: Collection<Project>,
    platforms: List<Platform>,
    mcVersions: List<String>,
    loaders: List<String>,
): Map<String, ProjectUpdateInfo> {
    val map = linkedMapOf<String, ProjectUpdateInfo>()
    for (updated in updatedProjects) {
        val current = currentProjects.firstOrNull { it isAlmostTheSameAs updated } ?: continue
        val id = current.pakkuId ?: continue
        val candidatesByType = fetchCandidateFiles(updated, platforms, mcVersions, loaders)
        val changes = mutableListOf<ProjectFileChange>()
        for (provider in updated.getProviders()) {
            val oldFile = current.getFilesForProvider(provider).firstOrNull() ?: continue
            val newFiles = (candidatesByType[provider.serialName] ?: updated.getFilesForProvider(provider))
                .sortedByDescending { it.datePublished }
                .filter { it != oldFile }
                .distinctBy { it.fileKey() }
            if (newFiles.isEmpty()) continue
            val newest = newFiles.first()
            changes += ProjectFileChange(
                providerShortName = provider.shortName,
                oldFile = oldFile,
                newFiles = newFiles,
                selectedFileId = newest.fileKey(),
            )
        }
        if (changes.isEmpty()) continue
        map[id] = ProjectUpdateInfo(updatedProject = updated, fileChanges = changes)
    }
    return map
}

private suspend fun fetchCandidateFiles(
    project: Project,
    platforms: List<Platform>,
    mcVersions: List<String>,
    loaders: List<String>,
): Map<String, List<ProjectFile>> {
    val out = linkedMapOf<String, List<ProjectFile>>()

    for (platform in platforms) {
        val projectId = project.id[platform.serialName] ?: continue
        val remote = platform.requestMultipleProjectsWithFiles(
            mcVersions,
            loaders,
            mapOf(projectId to project.type),
            UPDATE_CANDIDATE_FILES,
        ).onFailure { error ->
            statusLogger.error(error.toUiMessage())
        }.get()?.firstOrNull { it.slug[platform.serialName] == project.slug[platform.serialName] }
            ?: continue

        out[platform.serialName] = remote.getFilesForPlatform(platform)
            .sortedByDescending { it.datePublished }
    }

    val ghSlug = project.slug[GitHub.serialName]
    if (ghSlug != null) {
        val remote = GitHub.requestProjectWithFiles(
            emptyList(),
            emptyList(),
            ghSlug,
            numberOfFiles = UPDATE_CANDIDATE_FILES,
            projectType = project.type,
        ).onFailure { error ->
            statusLogger.error(error.toUiMessage())
        }.get()
        if (remote != null) {
            out[GitHub.serialName] = remote.getFilesForProvider(GitHub)
                .sortedByDescending { it.datePublished }
        }
    }

    return out
}

internal fun ProjectFile.fileKey(): String = id.ifEmpty { fileName }
