/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import com.github.michaelbull.result.get
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.klogging.logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.actions.errors.DirectoryNotEmpty
import teksturepako.pakku.api.actions.errors.HashMismatch
import teksturepako.pakku.api.actions.fetch.fetch
import teksturepako.pakku.api.actions.fetch.retrieveProjectFiles
import teksturepako.pakku.api.actions.sync.sync
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.ManualOverride
import teksturepako.pakku.api.overrides.readManualOverrides
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.createHash
import teksturepako.pakku.io.readAndCreateSha1FromBytes
import teksturepako.pakku.io.readPathBytesOrNull
import teksturepako.pakku.io.readPathBytesToResult
import teksturepako.pakku.io.tryOrNull
import teksturepako.pakku.io.tryToResult
import teksturepako.pakkuDesktop.app.ui.model.FetchCleanupPreview
import teksturepako.pakkuDesktop.app.ui.model.FetchDetectResult
import teksturepako.pakkuDesktop.app.ui.model.FetchDownloadPreview
import teksturepako.pakkuDesktop.app.ui.model.FetchOverridePreview
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.io.path.relativeToOrSelf

private val fetchLogger = logger("FetchImpl")

internal fun ManualOverride.toPreview(): FetchOverridePreview = FetchOverridePreview(
    relativeOutputPath = relativeOutputPath.invariantSeparatorsPathString,
    sourcePath = path.absolute().pathString,
    fullOutputPath = fullOutputPath.absolute().pathString,
)

private val DefaultIgnoredPaths = listOf("saves", "screenshots")
private const val BuildDirName = "build"

/** Type dir is the working directory, or a path Pakku ignores, or Gradle `build`. */
private fun ignoredManagedDir(dir: Path): Boolean {
    if (DefaultIgnoredPaths.any { it in dir.pathString }) return true
    val relative = relativeToWorkingPath(dir) ?: return false
    return relative.isEmpty() || relative == "." || BuildDirName in relative.split('/')
}

private fun ignoredManagedPath(path: Path): Boolean {
    if (DefaultIgnoredPaths.any { it in path.pathString }) return true
    val relative = relativeToWorkingPath(path) ?: return false
    return BuildDirName in relative.split('/')
}

private fun relativeToWorkingPath(path: Path): String? =
    try {
        path.toAbsolutePath().normalize()
            .relativeTo(kotlin.io.path.Path(workingPath).toAbsolutePath().normalize())
            .invariantSeparatorsPathString
    } catch (_: IllegalArgumentException) {
        null
    }
private val CleanupExtensions = listOf(".jar", ".zip", ".jar.meta")

/**
 * Scan lock vs disk without writing: missing downloads, pending override copies, leftover cleanup.
 */
suspend fun fetchDetectSuspend(
    lockFile: LockFile,
    configFile: ConfigFile?,
    onToast: suspend (ToastData) -> Unit,
): FetchDetectResult = coroutineScope {
    val results = retrieveProjectFiles(lockFile, Provider.providers)
    val projectFiles = results.mapNotNull { result ->
        result.onErr { error ->
            fetchLogger.error(error.toUiMessage())
            onToast(actionErrorToast(error))
        }.get()
    }

    val toDownload = mutableListOf<FetchDownloadPreview>()
    val hashMismatches = mutableSetOf<String>()

    for (projectFile in projectFiles) {
        val parent = projectFile.getParentProject(lockFile) ?: continue
        val path = projectFile.getPath(parent, configFile) ?: continue
        if (!path.exists()) {
            toDownload += FetchDownloadPreview(project = parent, projectFile = projectFile)
            continue
        }
        val bytes = readPathBytesOrNull(path) ?: continue
        if (projectFile.checkIntegrity(bytes, path) is HashMismatch) {
            hashMismatches += parent.uiKey()
        }
    }

    val manualOverrides = readManualOverrides(configFile)
    val allOverrides = manualOverrides.map { it.toPreview() }
    val toCopy = manualOverrides
        .filter { override -> override.fullOutputPath.notExists() }
        .map { it.toPreview() }

    val toClean = detectCleanupFiles(
        projectFiles = projectFiles,
        lockFile = lockFile,
        configFile = configFile,
        manualOverrides = manualOverrides,
    )

    FetchDetectResult(
        toDownload = toDownload,
        toCopyOverrides = toCopy,
        toClean = toClean,
        hashMismatchKeys = hashMismatches,
        allOverrides = allOverrides,
    )
}

/**
 * Apply a selected fetch preview: download missing files, copy overrides, delete leftovers.
 */
suspend fun fetchApplySuspend(
    lockFile: LockFile,
    configFile: ConfigFile,
    downloadProjects: List<Project>,
    overrideRelativePaths: Set<String>,
    cleanAbsolutePaths: List<String>,
    onProgress: suspend (completed: Long, total: Long) -> Unit = { _, _ -> },
    onPhase: suspend (name: String) -> Unit = {},
    onToast: suspend (ToastData) -> Unit,
) {
    val downloadFiles = downloadProjects.mapNotNull { project ->
        project.getLatestFile(Provider.providers)
    }

    var successCount = 0
    if (downloadFiles.isNotEmpty()) {
        downloadFiles.fetch(
            onError = { error ->
                if (error is AlreadyExists) {
                    fetchLogger.info { error.toUiMessage() }
                    return@fetch
                }
                fetchLogger.error(error.toUiMessage())
                onToast(actionErrorToast(error))
            },
            onProgress = onProgress,
            onSuccess = { _, projectFile ->
                successCount++
                fetchLogger.info { "Fetched ${projectFile.fileName}" }
            },
            lockFile = lockFile,
            configFile = configFile,
        ).join()
    }

    val syncedOverrides = mutableListOf<String>()
    if (overrideRelativePaths.isNotEmpty()) {
        onPhase("Syncing manual overrides")
        val selected = readManualOverrides(configFile)
            .filter { it.relativeOutputPath.invariantSeparatorsPathString in overrideRelativePaths }
            .toSet()
        selected.sync(
            onError = { error ->
                if (error is AlreadyExists) return@sync
                fetchLogger.error(error.toUiMessage())
                onToast(actionErrorToast(error))
            },
            onSuccess = { override ->
                fetchLogger.info { "Synced override ${override.fullOutputPath}" }
                syncedOverrides += override.relativeOutputPath.pathString
            },
            syncPrimaryDirectories = true,
        ).joinAll()
    }

    val deleted = mutableListOf<String>()
    if (cleanAbsolutePaths.isNotEmpty()) {
        onPhase("Deleting old files")
        for (absolute in cleanAbsolutePaths) {
            val path = Path(absolute)
            path.tryToResult {
                deleteIfExists()
            }.onOk {
                fetchLogger.info { "$path deleted" }
                deleted += path.name
            }.onErr { error ->
                if (error !is DirectoryNotEmpty) {
                    fetchLogger.error(error.toUiMessage())
                    onToast(actionErrorToast(error))
                }
            }
        }
    }

    onToast(
        actionFetchSummaryToast(
            fetched = successCount,
            overrides = syncedOverrides,
            shelved = emptyList(),
            deleted = deleted,
        ),
    )
}

private suspend fun detectCleanupFiles(
    projectFiles: List<ProjectFile>,
    lockFile: LockFile,
    configFile: ConfigFile?,
    manualOverrides: Set<ManualOverride>,
): List<FetchCleanupPreview> = coroutineScope {
    val fileHashes = projectFiles
        .map { projectFile ->
            async {
                val parentProject = projectFile.getParentProject(lockFile) ?: return@async null
                val path = projectFile.getPath(parentProject, configFile) ?: return@async null
                readPathBytesToResult(path).get()?.let { path to it }
            }
        }
        .awaitAll()
        .filterNotNull()
        .associate { (path, bytes) ->
            path.absolute() to createHash("sha1", bytes)
        }
        .plus(
            manualOverrides.associate { projectOverride ->
                val hash = readPathBytesOrNull(projectOverride.path)?.let { createHash("sha1", it) }
                projectOverride.fullOutputPath.absolute() to hash
            },
        )

    val dirs = ProjectType.entries
        .filterNot { it == ProjectType.WORLD }
        .mapNotNull { projectType ->
            val dir = Path(workingPath, projectType.getPathString(configFile))
            if (dir.notExists() || ignoredManagedDir(dir)) return@mapNotNull null
            dir
        }

    dirs.map { dir ->
        async {
            val paths = dir.tryOrNull { path ->
                path.toFile().walkBottomUp().onEnter { file ->
                    file.toPath() == dir || file.name != BuildDirName
                }.mapNotNull { file: File ->
                    file.toPath().takeIf { it != dir }
                }.toList()
            }.orEmpty()

            paths.mapNotNull { path ->
                if (ignoredManagedPath(path)) return@mapNotNull null
                // Subpath folders (and other dirs) are not leftover downloads; CLI delete
                // only succeeds when they are empty. Do not preview them as Delete.
                if (path.isDirectory()) return@mapNotNull null
                if (CleanupExtensions.none { path.pathString.endsWith(it) }) return@mapNotNull null
                val hash = path.readAndCreateSha1FromBytes()
                if (path.absolute() in fileHashes.keys && hash in fileHashes.values) return@mapNotNull null
                val relative = path.relativeToOrSelf(Path(workingPath)).invariantSeparatorsPathString
                FetchCleanupPreview(
                    relativePath = relative,
                    absolutePath = path.absolute().pathString,
                    fileName = path.name,
                )
            }
        }
    }.awaitAll().flatten().distinctBy { it.absolutePath }
}
