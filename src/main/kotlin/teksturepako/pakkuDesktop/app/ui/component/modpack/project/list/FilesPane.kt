/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

import com.github.michaelbull.result.get
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.actions.syncDisplayPaths
import teksturepako.pakkuDesktop.app.actions.syncKind
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.model.DiskSnapshot
import teksturepako.pakkuDesktop.app.ui.model.FetchCleanupPreview
import teksturepako.pakkuDesktop.app.ui.model.FetchDetectResult
import teksturepako.pakkuDesktop.app.ui.model.FetchDownloadPreview
import teksturepako.pakkuDesktop.app.ui.model.FetchOverridePreview
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ProjectsTabModel
import teksturepako.pakkuDesktop.app.ui.model.SyncKind
import teksturepako.pakkuDesktop.app.ui.model.isOnDisk
import teksturepako.pakkuDesktop.app.ui.model.pathKey

const val FetchSelectionPrefix = "fetch:"
const val OverrideSelectionPrefix = "override:"
const val CleanupSelectionPrefix = "cleanup:"

data class FilesPaneRow(
    val key: String,
    val listKey: String = key,
    val folder: String,
    val fileName: String,
    val relativePath: String,
    val project: Project? = null,
    val projectFile: ProjectFile? = null,
    val mergedFiles: List<ProjectFile> = emptyList(),
    val changeKind: FilesChangeKind? = null,
    val hashMismatch: Boolean = false,
    val selectable: Boolean = false,
    /** Absolute path to reveal; defaults to `workingPath` + [relativePath]. */
    val revealPath: String? = null,
    /** Absolute source path when this row is a pending manual-override copy. */
    val sourcePath: String? = null,
    /** Whether the reveal target is on disk, from [DiskSnapshot]. */
    val onDisk: Boolean = false,
)

fun FetchDownloadPreview.selectionKey(): String = "$FetchSelectionPrefix${project.uiKey()}"

fun FetchOverridePreview.selectionKey(): String = "$OverrideSelectionPrefix$relativeOutputPath"

fun FetchCleanupPreview.selectionKey(): String = "$CleanupSelectionPrefix$absolutePath"

fun FetchDetectResult.selectionKeys(): Set<String> =
    toDownload.map { it.selectionKey() }.toSet() +
        toCopyOverrides.map { it.selectionKey() }.toSet() +
        toClean.map { it.selectionKey() }.toSet()

fun ModpackModel.clearFilesPreview(): ModpackModel = copy(
    filesPreviewMode = FilesPreviewMode.None,
    fetchDownloads = persistentListOf(),
    fetchOverrides = persistentListOf(),
    fetchCleanup = persistentListOf(),
    filesHashMismatchKeys = persistentSetOf(),
    syncAdded = persistentListOf(),
    syncRemoved = persistentListOf(),
    syncUpdated = persistentListOf(),
    filesFilterChangeKinds = persistentSetOf(),
    selectedProjectKeys = persistentSetOf(),
)

fun FilesTabModel.canApplyFetch(): Boolean {
    if (actionName != null) return false
    if (lockFile?.get() == null) return false
    if (filesPreviewMode != FilesPreviewMode.Fetch) return false
    val selected = selectedProjectKeys
    return fetchDownloads.any { it.selectionKey() in selected } ||
        fetchOverrides.any { it.selectionKey() in selected } ||
        fetchCleanup.any { it.selectionKey() in selected }
}

fun FilesTabModel.canApplySync(kind: SyncKind): Boolean {
    if (actionName != null) return false
    if (lockFile?.get() == null) return false
    if (filesPreviewMode != FilesPreviewMode.Sync) return false
    val keys = when (kind) {
        SyncKind.Added -> syncAdded
        SyncKind.Removed -> syncRemoved
        SyncKind.Updated -> syncUpdated
    }.map { it.uiKey() }.toSet()
    return keys.isNotEmpty() && selectedProjectKeys.any { it in keys }
}

fun ModpackModel.selectedFetchDownloads(): List<Project> =
    fetchDownloads.filter { it.selectionKey() in selectedProjectKeys }.map { it.project }

fun ModpackModel.selectedFetchOverridePaths(): Set<String> =
    fetchOverrides.filter { it.selectionKey() in selectedProjectKeys }.map { it.relativeOutputPath }.toSet()

fun ModpackModel.selectedFetchCleanupPaths(): List<String> =
    fetchCleanup.filter { it.selectionKey() in selectedProjectKeys }.map { it.absolutePath }

fun folderKeyFromRelative(relative: String): String {
    val normalized = relative.replace('\\', '/')
    val slash = normalized.lastIndexOf('/')
    return if (slash >= 0) normalized.substring(0, slash + 1) else ""
}

fun Project.filesFolderKey(configFile: ConfigFile?): String {
    val path = getPathStringWithSubpath(configFile)
    return if (path.endsWith("/")) path else "$path/"
}

fun FilesTabModel.packFolderKeys(): List<String> {
    val config = configFile?.get()
    val fromTypes = ProjectType.entries
        .filterNot { it == ProjectType.WORLD }
        .map { type ->
            val path = type.getPathString(config)
            if (path.endsWith("/")) path else "$path/"
        }
    val fromOverrides = manualOverrides.map { folderKeyFromRelative(it.relativeOutputPath) }.filter { it.isNotBlank() }
    return (fromTypes + fromOverrides).distinct()
}

fun ProjectsTabModel.syncOf(project: Project): SyncKind? =
    if (filesPreviewMode == FilesPreviewMode.Sync) {
        project.syncKind(syncAdded, syncRemoved, syncUpdated)
    } else {
        null
    }

fun ProjectsTabModel.syncPendingCount(): Int =
    if (filesPreviewMode != FilesPreviewMode.Sync) 0
    else syncAdded.size + syncRemoved.size + syncUpdated.size

fun FilesTabModel.currentFilesRows(): List<FilesPaneRow> {
    val config = configFile?.get()
    val lockProjects = lockFile?.get()?.getAllProjects().snapshotList()
    val downloadKeys = fetchDownloads.map { it.project.uiKey() }.toSet()
    val rows = mutableListOf<FilesPaneRow>()

    for (project in lockProjects) {
        val kind = when (filesPreviewMode) {
            FilesPreviewMode.Sync -> when (project.syncKind(syncAdded, syncRemoved, syncUpdated)) {
                SyncKind.Added -> continue
                SyncKind.Removed -> FilesChangeKind.Removed
                SyncKind.Updated -> FilesChangeKind.Updated
                null -> null
            }
            FilesPreviewMode.Fetch -> when {
                project.uiKey() in downloadKeys -> continue
                project.uiKey() in filesHashMismatchKeys -> FilesChangeKind.HashMismatch
                else -> null
            }
            FilesPreviewMode.None ->
                if (project.uiKey() in filesHashMismatchKeys) FilesChangeKind.HashMismatch else null
        }
        for (cluster in project.filesTabRows()) {
            val primary = cluster.primary
            val relativePath = primary.getRelativePathString(project, config) ?: primary.fileName
            if (pathKey(relativePath) !in diskSnapshot.present) continue
            rows += FilesPaneRow(
                key = cluster.rowKey(),
                listKey = cluster.rowKey(),
                folder = project.filesFolderKey(config),
                fileName = primary.fileName,
                relativePath = relativePath,
                project = project,
                projectFile = primary,
                mergedFiles = cluster.files.filter { it !== primary },
                changeKind = kind,
                hashMismatch = project.uiKey() in filesHashMismatchKeys,
                selectable = false,
                onDisk = true,
            )
        }
    }

    val listedPaths = rows.map { it.relativePath.replace('\\', '/').lowercase() }.toMutableSet()
    val pendingCopyKeys = fetchOverrides.map { it.selectionKey() }.toSet()
    for (override in manualOverrides) {
        val rel = override.relativeOutputPath.replace('\\', '/')
        val destOnDisk = pathKey(rel) in diskSnapshot.present
        // Fetch preview: dest-missing copies belong on New only (blank Current).
        // Source existence is not checked: Pakku only lists overrides whose source exists.
        if (filesPreviewMode == FilesPreviewMode.Fetch && !destOnDisk) continue
        if (rel.lowercase() in listedPaths) continue
        listedPaths += rel.lowercase()
        val pending = override.selectionKey() in pendingCopyKeys || !destOnDisk
        rows += FilesPaneRow(
            key = override.selectionKey(),
            folder = folderKeyFromRelative(override.relativeOutputPath),
            fileName = override.relativeOutputPath.substringAfterLast('/').substringAfterLast('\\'),
            relativePath = override.relativeOutputPath,
            changeKind = if (pending) FilesChangeKind.Override else null,
            selectable = false,
            revealPath = if (destOnDisk) override.fullOutputPath else override.sourcePath,
            sourcePath = override.sourcePath,
            onDisk = destOnDisk || override.sourcePath.isNotBlank(),
        )
    }

    if (filesPreviewMode == FilesPreviewMode.Fetch) {
        for (cleanup in fetchCleanup) {
            rows += cleanup.toPaneRow(selectable = false, onDisk = diskSnapshot.isOnDisk(cleanup.relativePath))
        }
    }

    return rows
}

fun FilesTabModel.proposedFilesRows(): List<FilesPaneRow> {
    val config = configFile?.get()
    return when (filesPreviewMode) {
        FilesPreviewMode.None -> emptyList()
        FilesPreviewMode.Fetch -> buildList {
            for (download in fetchDownloads) {
                val project = download.project
                val file = download.projectFile
                val relativePath = file.getRelativePathString(project, config) ?: file.fileName
                add(
                    FilesPaneRow(
                        key = download.selectionKey(),
                        folder = project.filesFolderKey(config),
                        fileName = file.fileName,
                        relativePath = relativePath,
                        project = project,
                        projectFile = file,
                        changeKind = FilesChangeKind.Fetch,
                        selectable = true,
                        onDisk = diskSnapshot.isOnDisk(relativePath),
                    ),
                )
            }
            for (override in fetchOverrides) {
                add(override.toPaneRow())
            }
            for (cleanup in fetchCleanup) {
                add(cleanup.toPaneRow(selectable = true, onDisk = diskSnapshot.isOnDisk(cleanup.relativePath)))
            }
        }
        FilesPreviewMode.Sync -> buildList {
            for (project in syncAdded) {
                addAll(project.toSyncRows(config, FilesChangeKind.Added, selectable = true, diskSnapshot))
            }
            for (project in syncUpdated) {
                addAll(project.toSyncRows(config, FilesChangeKind.Updated, selectable = true, diskSnapshot))
            }
            for (project in syncRemoved) {
                addAll(project.toSyncRows(config, FilesChangeKind.Removed, selectable = true, diskSnapshot))
            }
        }
    }
}

data class FilesDiffPair(
    val listKey: String,
    val folder: String,
    val selectionKey: String,
    val selectable: Boolean,
    val changeKind: FilesChangeKind?,
    val left: FilesPaneRow?,
    val right: FilesPaneRow?,
)

fun FilesTabModel.filesDiffPairs(): List<FilesDiffPair> {
    val currentGroups = currentFilesRows().groupBy { it.diffGroupKey() }
    val proposedGroups = proposedFilesRows().groupBy { it.diffGroupKey() }
    val pairs = mutableListOf<FilesDiffPair>()

    for (key in currentGroups.keys + proposedGroups.keys) {
        val lefts = currentGroups[key].orEmpty()
        val rights = proposedGroups[key].orEmpty()
        val kind = rights.firstOrNull()?.changeKind ?: lefts.firstOrNull()?.changeKind
        val deletion = kind == FilesChangeKind.Removed || kind == FilesChangeKind.Cleanup

        pairs += when {
            deletion -> {
                val rows = lefts.ifEmpty { rights }
                rows.mapIndexed { index, row ->
                    val right = rights.getOrNull(index)
                    FilesDiffPair(
                        listKey = "del:${row.listKey}",
                        folder = row.folder,
                        selectionKey = right?.key ?: row.key,
                        selectable = true,
                        changeKind = kind,
                        left = row,
                        right = null,
                    )
                }
            }
            lefts.isEmpty() -> rights.map { right ->
                FilesDiffPair(
                    listKey = "add:${right.listKey}",
                    folder = right.folder,
                    selectionKey = right.key,
                    selectable = true,
                    changeKind = kind ?: right.changeKind,
                    left = null,
                    right = right,
                )
            }
            rights.isEmpty() -> lefts.map { left ->
                val scanned = filesPreviewMode != FilesPreviewMode.None
                val mirroredKind = left.changeKind.takeIf { it == FilesChangeKind.HashMismatch }
                FilesDiffPair(
                    listKey = "keep:${left.listKey}",
                    folder = left.folder,
                    selectionKey = left.project?.uiKey() ?: left.key,
                    selectable = scanned,
                    changeKind = if (scanned) mirroredKind else null,
                    left = left.copy(selectable = scanned),
                    right = if (scanned) {
                        left.copy(changeKind = mirroredKind, selectable = scanned)
                    } else {
                        null
                    },
                )
            }
            else -> {
                val n = maxOf(lefts.size, rights.size)
                List(n) { index ->
                    val left = lefts.getOrNull(index)
                    val right = rights.getOrNull(index)
                    FilesDiffPair(
                        listKey = "pair:${left?.listKey ?: "_"}:${right?.listKey ?: "_"}:$index",
                        folder = left?.folder ?: right!!.folder,
                        selectionKey = right?.key ?: left?.project?.uiKey() ?: left!!.key,
                        selectable = true,
                        changeKind = right?.changeKind ?: left?.changeKind ?: kind,
                        left = left,
                        right = right,
                    )
                }
            }
        }
    }

    return pairs.sortedWith(
        compareBy<FilesDiffPair> { it.folder.lowercase() }
            .thenBy { (it.right?.fileName ?: it.left?.fileName ?: "").lowercase() },
    )
}

fun FilesTabModel.filteredFilesDiffPairs(): List<FilesDiffPair> =
    filesDiffPairs().filter { it.matchesFilesFilters(this) }

private fun FilesPaneRow.diffGroupKey(): String =
    project?.uiKey() ?: "path:${relativePath.replace('\\', '/').lowercase()}"

private fun FilesDiffPair.matchesFilesFilters(model: FilesTabModel): Boolean {
    val rows = listOfNotNull(left, right)
    val q = model.filesFilterText.lowercase()
    val textOk = q.isEmpty() || rows.any { row ->
        q in row.fileName.lowercase() ||
            q in row.relativePath.lowercase() ||
            q in row.folder.lowercase() ||
            row.project?.name?.values?.any { q in it.lowercase() } == true
    }
    val folderOk = model.filesFilterFolders.isEmpty() || folder in model.filesFilterFolders
    val kindOk = model.filesFilterChangeKinds.isEmpty() || changeKind in model.filesFilterChangeKinds
    return textOk && folderOk && kindOk
}

private fun FetchOverridePreview.toPaneRow(): FilesPaneRow = FilesPaneRow(
    key = selectionKey(),
    folder = folderKeyFromRelative(relativeOutputPath),
    fileName = relativeOutputPath.substringAfterLast('/').substringAfterLast('\\'),
    relativePath = relativeOutputPath,
    changeKind = FilesChangeKind.Override,
    selectable = true,
    revealPath = sourcePath,
    sourcePath = sourcePath,
    onDisk = sourcePath.isNotBlank(),
)

private fun FetchCleanupPreview.toPaneRow(selectable: Boolean, onDisk: Boolean): FilesPaneRow = FilesPaneRow(
    key = selectionKey(),
    folder = folderKeyFromRelative(relativePath),
    fileName = fileName,
    relativePath = relativePath,
    changeKind = FilesChangeKind.Cleanup,
    selectable = selectable,
    onDisk = onDisk,
)

private fun Project.toSyncRows(
    config: ConfigFile?,
    kind: FilesChangeKind,
    selectable: Boolean,
    diskSnapshot: DiskSnapshot,
): List<FilesPaneRow> {
    val syncKind = when (kind) {
        FilesChangeKind.Added -> SyncKind.Added
        FilesChangeKind.Updated -> SyncKind.Updated
        FilesChangeKind.Removed -> SyncKind.Removed
        else -> error("Not a sync change kind: $kind")
    }
    val displayPaths = syncDisplayPaths(this, config, diskSnapshot, syncKind)
    val clusters = filesTabRows()
    if (displayPaths.isEmpty()) {
        val relativePath = getPathStringWithSubpath(config)
        val folder = filesFolderKey(config)
        return listOf(
            FilesPaneRow(
                key = uiKey(),
                listKey = "${uiKey()}::empty",
                folder = folder,
                fileName = name.values.firstOrNull() ?: uiKey(),
                relativePath = relativePath,
                project = this,
                changeKind = kind,
                selectable = selectable,
                onDisk = folder in diskSnapshot.existingFolders || diskSnapshot.isOnDisk(relativePath),
            ),
        )
    }
    return displayPaths.map { relativePath ->
        val fileName = relativePath.replace('\\', '/').substringAfterLast('/')
        val cluster = clusters.firstOrNull { row ->
            row.files.any { it.fileName.equals(fileName, ignoreCase = true) }
        } ?: clusters.firstOrNull()
        val primary = cluster?.primary
        FilesPaneRow(
            key = uiKey(),
            listKey = cluster?.rowKey() ?: "${uiKey()}::$relativePath",
            folder = filesFolderKey(config),
            fileName = primary?.fileName ?: fileName,
            relativePath = relativePath,
            project = this,
            projectFile = primary,
            mergedFiles = cluster?.files?.filter { it !== primary }.orEmpty(),
            changeKind = kind,
            selectable = selectable,
            onDisk = diskSnapshot.isOnDisk(relativePath),
        )
    }
}

internal fun expectedDiskPaths(
    projects: List<Project>,
    configFile: ConfigFile?,
    overrides: List<FetchOverridePreview>,
): Map<String, String> {
    val expected = linkedMapOf<String, String>()
    for (project in projects) {
        for (cluster in project.filesTabRows()) {
            val display = cluster.primary.getRelativePathString(project, configFile)
                ?.replace('\\', '/')
                ?: continue
            expected.putIfAbsent(pathKey(display), display)
        }
    }
    for ((relativeOutputPath) in overrides) {
        val display = relativeOutputPath.replace('\\', '/')
        expected.putIfAbsent(pathKey(display), display)
    }
    return expected
}

internal fun diskSnapshotFolderKeys(
    configFile: ConfigFile?,
    projects: List<Project>,
    overrides: List<FetchOverridePreview>,
): List<String> {
    val fromTypes = ProjectType.entries
        .filterNot { it == ProjectType.WORLD }
        .map { type ->
            val path = type.getPathString(configFile).replace('\\', '/')
            if (path.endsWith("/")) path else "$path/"
        }
    val fromProjects = projects.map { it.filesFolderKey(configFile) }
    val fromOverrides = overrides.map { folderKeyFromRelative(it.relativeOutputPath) }
    return (fromTypes + fromProjects + fromOverrides).distinct()
}

/** Copy a live Pakku list so composition is not iterating it while fetch mutates it. */
internal fun <T> Iterable<T>?.snapshotList(): List<T> {
    val source = this ?: return emptyList()
    repeat(3) {
        try {
            return source.toList()
        } catch (_: ConcurrentModificationException) {
            // Worker thread mutated LockFile; retry the copy.
        }
    }
    return emptyList()
}
