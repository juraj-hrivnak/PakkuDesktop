/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import com.github.michaelbull.result.get
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.actions.syncDisplayPaths
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeStatus
import teksturepako.pakkuDesktop.app.ui.model.DiskSnapshot
import teksturepako.pakkuDesktop.app.ui.model.FetchCleanupPreview
import teksturepako.pakkuDesktop.app.ui.model.FetchDownloadPreview
import teksturepako.pakkuDesktop.app.ui.model.FetchOverridePreview
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.SyncKind
import teksturepako.pakkuDesktop.app.ui.model.isOnDisk
import teksturepako.pakkuDesktop.app.ui.model.pathKey

sealed interface PakkuEntry {
    /** In the lock file. [continuation] = not the first link of this project. */
    data class Tracked(
        val project: Project,
        val file: ProjectFile?,
        val mergedFiles: List<ProjectFile>,
        val continuation: Boolean,
    ) : PakkuEntry

    /** A `.pakku/{overrides,server-overrides,client-overrides}` file. */
    data class Override(val relativeOutputPath: String, val sourcePath: String) : PakkuEntry

    /** Found on a platform by sync detect; not in the lock file yet. */
    data class Detected(val project: Project) : PakkuEntry
}

/** Disk relative to Pakku, git-style. */
enum class DiskState { InSync, Missing, Untracked, Modified }

data class FilesLink(
    val path: String,
    val pathKey: String,
    val pakku: PakkuEntry?,
    val onDisk: Boolean,
    val state: DiskState,
    val change: FilesChangeKind?,
    val selectionKey: String?,
    val unrecognized: Boolean = false,
)

enum class FilesDirection { ToDisk, ToPakku }

fun FilesChangeKind.direction(): FilesDirection? = when (this) {
    FilesChangeKind.Fetch, FilesChangeKind.Override, FilesChangeKind.Cleanup -> FilesDirection.ToDisk
    FilesChangeKind.Added, FilesChangeKind.Updated, FilesChangeKind.Removed -> FilesDirection.ToPakku
    FilesChangeKind.HashMismatch -> null
}

fun DiskState.treeStatus(): TreeStatus? = when (this) {
    DiskState.InSync -> null
    DiskState.Missing -> TreeStatus.Deleted
    DiskState.Untracked -> TreeStatus.Untracked
    DiskState.Modified -> TreeStatus.Modified
}

fun typeFolderLabels(config: ConfigFile?): Map<String, String> =
    ProjectType.entries
        .filterNot { it == ProjectType.WORLD }
        .associate { type ->
            val path = type.getPathString(config).replace('\\', '/')
            val key = if (path.endsWith("/")) path else "$path/"
            key to type.prettyName
        }

fun FilesTabModel.filesLinks(): List<FilesLink> {
    val config = configFile?.get()
    val projects = lockFile?.get()?.getAllProjects().snapshotList().orEmpty()
    return buildFilesLinks(
        projects = projects,
        config = config,
        manualOverrides = manualOverrides,
        diskSnapshot = diskSnapshot,
        filesPreviewMode = filesPreviewMode,
        fetchDownloads = fetchDownloads,
        fetchOverrides = fetchOverrides,
        fetchCleanup = fetchCleanup,
        filesHashMismatchKeys = filesHashMismatchKeys,
        syncAdded = syncAdded,
        syncRemoved = syncRemoved,
        syncUpdated = syncUpdated,
    )
}

fun FilesTabModel.filteredFilesLinks(): List<FilesLink> =
    filesLinks().filter { it.matchesFilesFilters(this) }

internal fun buildFilesLinks(
    projects: List<Project>,
    config: ConfigFile?,
    manualOverrides: PersistentList<FetchOverridePreview>,
    diskSnapshot: DiskSnapshot,
    filesPreviewMode: FilesPreviewMode,
    fetchDownloads: PersistentList<FetchDownloadPreview>,
    fetchOverrides: PersistentList<FetchOverridePreview>,
    fetchCleanup: PersistentList<FetchCleanupPreview>,
    filesHashMismatchKeys: PersistentSet<String>,
    syncAdded: PersistentList<Project>,
    syncRemoved: PersistentList<Project>,
    syncUpdated: PersistentList<Project>,
): List<FilesLink> {
    val links = linkedMapOf<String, LinkDraft>()
    addLockBaseLinks(links, projects, config, diskSnapshot, filesHashMismatchKeys)
    addOverrideBaseLinks(links, manualOverrides, diskSnapshot)
    addUntrackedBaseLinks(links, diskSnapshot)

    when (filesPreviewMode) {
        FilesPreviewMode.Fetch -> applyFetchOverlay(
            links,
            config,
            fetchDownloads,
            fetchOverrides,
            fetchCleanup,
            diskSnapshot,
            filesHashMismatchKeys,
        )
        FilesPreviewMode.Sync -> applySyncOverlay(
            links,
            projects,
            config,
            diskSnapshot,
            syncAdded,
            syncRemoved,
            syncUpdated,
        )
        FilesPreviewMode.None -> Unit
    }

    return finalizeLinks(links)
}

private data class LinkDraft(
    val path: String,
    var pakku: PakkuEntry?,
    var onDisk: Boolean,
    var state: DiskState,
    var change: FilesChangeKind? = null,
    var selectionKey: String? = null,
    var unrecognized: Boolean = false,
)

private fun displayPath(relative: String, diskSnapshot: DiskSnapshot): String {
    val normalized = relative.replace('\\', '/')
    return diskSnapshot.expected[pathKey(normalized)] ?: normalized
}

private fun putLink(
    links: MutableMap<String, LinkDraft>,
    relativePath: String,
    diskSnapshot: DiskSnapshot,
    pakku: PakkuEntry?,
    onDisk: Boolean,
    state: DiskState,
) {
    val path = displayPath(relativePath, diskSnapshot)
    val key = pathKey(path)
    links.putIfAbsent(
        key,
        LinkDraft(path = path, pakku = pakku, onDisk = onDisk, state = state),
    )
}

private fun addLockBaseLinks(
    links: MutableMap<String, LinkDraft>,
    projects: List<Project>,
    config: ConfigFile?,
    diskSnapshot: DiskSnapshot,
    filesHashMismatchKeys: PersistentSet<String>,
) {
    for (project in projects) {
        val mismatch = project.uiKey() in filesHashMismatchKeys
        for (cluster in project.filesTabRows()) {
            val primary = cluster.primary
            val relativePath = primary.getRelativePathString(project, config) ?: primary.fileName
            val onDisk = pathKey(relativePath) in diskSnapshot.present
            val state = when {
                !onDisk -> DiskState.Missing
                mismatch -> DiskState.Modified
                else -> DiskState.InSync
            }
            putLink(
                links,
                relativePath,
                diskSnapshot,
                pakku = PakkuEntry.Tracked(
                    project = project,
                    file = primary,
                    mergedFiles = cluster.files.filter { it !== primary },
                    continuation = false,
                ),
                onDisk = onDisk,
                state = state,
            )
        }
    }
}

private fun addOverrideBaseLinks(
    links: MutableMap<String, LinkDraft>,
    manualOverrides: PersistentList<FetchOverridePreview>,
    diskSnapshot: DiskSnapshot,
) {
    for (override in manualOverrides) {
        val rel = override.relativeOutputPath.replace('\\', '/')
        val onDisk = pathKey(rel) in diskSnapshot.present
        putLink(
            links,
            rel,
            diskSnapshot,
            pakku = PakkuEntry.Override(relativeOutputPath = rel, sourcePath = override.sourcePath),
            onDisk = onDisk,
            state = if (onDisk) DiskState.InSync else DiskState.Missing,
        )
    }
}

private fun addUntrackedBaseLinks(
    links: MutableMap<String, LinkDraft>,
    diskSnapshot: DiskSnapshot,
) {
    for (relative in diskSnapshot.untracked) {
        putLink(
            links,
            relative,
            diskSnapshot,
            pakku = null,
            onDisk = true,
            state = DiskState.Untracked,
        )
    }
}

private fun applyFetchOverlay(
    links: MutableMap<String, LinkDraft>,
    config: ConfigFile?,
    fetchDownloads: PersistentList<FetchDownloadPreview>,
    fetchOverrides: PersistentList<FetchOverridePreview>,
    fetchCleanup: PersistentList<FetchCleanupPreview>,
    diskSnapshot: DiskSnapshot,
    filesHashMismatchKeys: PersistentSet<String>,
) {
    for (download in fetchDownloads) {
        val project = download.project
        val file = download.projectFile
        val relativePath = file.getRelativePathString(project, config)
            ?: file.fileName
        val key = pathKey(relativePath)
        val draft = links[key]
        if (draft != null) {
            draft.change = FilesChangeKind.Fetch
            draft.selectionKey = download.selectionKey()
        } else {
            links[key] = LinkDraft(
                path = displayPath(relativePath, diskSnapshot),
                pakku = PakkuEntry.Tracked(project, file, emptyList(), continuation = false),
                onDisk = diskSnapshot.isOnDisk(relativePath),
                state = DiskState.Missing,
                change = FilesChangeKind.Fetch,
                selectionKey = download.selectionKey(),
            )
        }
    }

    for (override in fetchOverrides) {
        val rel = override.relativeOutputPath.replace('\\', '/')
        val key = pathKey(rel)
        val draft = links[key]
        if (draft != null) {
            draft.change = FilesChangeKind.Override
            draft.selectionKey = override.selectionKey()
        } else {
            links[key] = LinkDraft(
                path = displayPath(rel, diskSnapshot),
                pakku = PakkuEntry.Override(rel, override.sourcePath),
                onDisk = pathKey(rel) in diskSnapshot.present,
                state = DiskState.Missing,
                change = FilesChangeKind.Override,
                selectionKey = override.selectionKey(),
            )
        }
    }

    for (cleanup in fetchCleanup) {
        val rel = cleanup.relativePath.replace('\\', '/')
        val key = pathKey(rel)
        val draft = links[key]
        if (draft != null) {
            draft.change = FilesChangeKind.Cleanup
            draft.selectionKey = cleanup.selectionKey()
            draft.onDisk = true
            if (draft.state == DiskState.Untracked) {
                // keep Untracked for cleanup targets
            }
        } else {
            links[key] = LinkDraft(
                path = displayPath(rel, diskSnapshot),
                pakku = null,
                onDisk = true,
                state = DiskState.Untracked,
                change = FilesChangeKind.Cleanup,
                selectionKey = cleanup.selectionKey(),
            )
        }
    }

    for (draft in links.values) {
        val tracked = draft.pakku as? PakkuEntry.Tracked ?: continue
        if (tracked.project.uiKey() in filesHashMismatchKeys && draft.onDisk && draft.state == DiskState.Modified) {
            draft.change = FilesChangeKind.HashMismatch
            draft.selectionKey = null
        }
    }
}

private fun applySyncOverlay(
    links: MutableMap<String, LinkDraft>,
    lockProjects: List<Project>,
    config: ConfigFile?,
    diskSnapshot: DiskSnapshot,
    syncAdded: PersistentList<Project>,
    syncRemoved: PersistentList<Project>,
    syncUpdated: PersistentList<Project>,
) {
    val lockKeys = lockProjects.map { it.uiKey() }.toSet()

    for (project in syncRemoved) {
        val key = project.uiKey()
        val matching = links.filter { (_, draft) ->
            (draft.pakku as? PakkuEntry.Tracked)?.project?.uiKey() == key
        }
        if (matching.isEmpty()) {
            addSyncFallbackLink(links, project, config, diskSnapshot, FilesChangeKind.Removed, key)
        } else {
            for ((_, draft) in matching) {
                draft.change = FilesChangeKind.Removed
                draft.selectionKey = key
                if (draft.onDisk && draft.state != DiskState.Missing) {
                    draft.state = DiskState.Modified
                }
            }
        }
    }

    for (project in syncUpdated) {
        applySyncProjectPaths(
            links,
            project,
            config,
            diskSnapshot,
            SyncKind.Updated,
            FilesChangeKind.Updated,
            lockKeys,
        )
    }

    for (project in syncAdded) {
        if (project.uiKey() in lockKeys) continue
        applySyncProjectPaths(
            links,
            project,
            config,
            diskSnapshot,
            SyncKind.Added,
            FilesChangeKind.Added,
            lockKeys,
        )
    }

    dropStaleUpdatedPaths(links, syncUpdated, config, diskSnapshot)

    // Projects only in sync lists as Added but also in lock (edge): overlay Added on matching paths
    for (project in syncAdded) {
        if (project.uiKey() !in lockKeys) continue
        applySyncProjectPaths(
            links,
            project,
            config,
            diskSnapshot,
            SyncKind.Added,
            FilesChangeKind.Added,
            lockKeys,
        )
    }

}

private fun dropStaleUpdatedPaths(
    links: MutableMap<String, LinkDraft>,
    syncUpdated: PersistentList<Project>,
    config: ConfigFile?,
    diskSnapshot: DiskSnapshot,
) {
    for (project in syncUpdated) {
        val displayPaths = syncDisplayPaths(project, config, diskSnapshot, SyncKind.Updated)
            .map { pathKey(it) }
            .toSet()
        if (displayPaths.isEmpty()) continue
        val staleKeys = links.filter { (_, draft) ->
            (draft.pakku as? PakkuEntry.Tracked)?.project?.uiKey() == project.uiKey() &&
                pathKey(draft.path) !in displayPaths &&
                draft.change != FilesChangeKind.Removed
        }.keys
        for (key in staleKeys) {
            links.remove(key)
        }
    }
}

private fun applySyncProjectPaths(
    links: MutableMap<String, LinkDraft>,
    project: Project,
    config: ConfigFile?,
    diskSnapshot: DiskSnapshot,
    syncKind: SyncKind,
    change: FilesChangeKind,
    lockKeys: Set<String>,
) {
    val displayPaths = syncDisplayPaths(project, config, diskSnapshot, syncKind)
    if (displayPaths.isEmpty()) {
        addSyncFallbackLink(links, project, config, diskSnapshot, change, project.uiKey())
        return
    }
    val clusters = project.filesTabRows()
    for (relativePath in displayPaths) {
        val path = displayPath(relativePath, diskSnapshot)
        val key = pathKey(path)
        val fileName = path.substringAfterLast('/')
        val cluster = clusters.firstOrNull { row ->
            row.files.any { it.fileName.equals(fileName, ignoreCase = true) }
        } ?: clusters.firstOrNull()
        val primary = cluster?.primary
        val tracked = if (project.uiKey() in lockKeys) {
            PakkuEntry.Tracked(
                project = project,
                file = primary,
                mergedFiles = cluster?.files?.filter { it !== primary }.orEmpty(),
                continuation = false,
            )
        } else {
            PakkuEntry.Detected(project)
        }
        val onDisk = diskSnapshot.isOnDisk(relativePath)
        links[key] = LinkDraft(
            path = path,
            pakku = tracked,
            onDisk = onDisk,
            state = if (onDisk) DiskState.Modified else DiskState.Missing,
            change = change,
            selectionKey = project.uiKey(),
        )
    }
}

private fun addSyncFallbackLink(
    links: MutableMap<String, LinkDraft>,
    project: Project,
    config: ConfigFile?,
    diskSnapshot: DiskSnapshot,
    change: FilesChangeKind,
    selectionKey: String,
) {
    val clusters = project.filesTabRows()
    val relativePath = if (clusters.isNotEmpty()) {
        val primary = clusters.first().primary
        primary.getRelativePathString(project, config) ?: primary.fileName
    } else {
        val folder = project.filesFolderKey(config).trimEnd('/')
        val name = project.files.firstOrNull()?.fileName
            ?: project.name.values.firstOrNull()
            ?: selectionKey
        "$folder/$name"
    }
    val path = displayPath(relativePath, diskSnapshot)
    val key = pathKey(path)
    val onDisk = pathKey(relativePath) in diskSnapshot.present ||
        folderKeyFromRelative(relativePath) in diskSnapshot.existingFolders
    links.putIfAbsent(
        key,
        LinkDraft(
            path = path,
            pakku = PakkuEntry.Tracked(
                project = project,
                file = clusters.firstOrNull()?.primary,
                mergedFiles = emptyList(),
                continuation = false,
            ),
            onDisk = onDisk,
            state = if (onDisk) DiskState.Modified else DiskState.Missing,
            change = change,
            selectionKey = selectionKey,
        ),
    )
    links[key]?.let { draft ->
        draft.change = change
        draft.selectionKey = selectionKey
    }
}

private fun finalizeLinks(links: Map<String, LinkDraft>): List<FilesLink> {
    val continuationByProject = mutableMapOf<String, Boolean>()
    val sorted = links.values.sortedWith(
        compareBy<LinkDraft> { folderKeyFromRelative(it.path).lowercase() }
            .thenBy { it.path.lowercase() },
    )
    return sorted.map { draft ->
        val pakku = when (val entry = draft.pakku) {
            is PakkuEntry.Tracked -> {
                val projectKey = entry.project.uiKey()
                val cont = continuationByProject[projectKey] == true
                continuationByProject[projectKey] = true
                entry.copy(continuation = cont)
            }
            else -> entry
        }
        FilesLink(
            path = draft.path,
            pathKey = pathKey(draft.path),
            pakku = pakku,
            onDisk = draft.onDisk,
            state = draft.state,
            change = draft.change,
            selectionKey = draft.selectionKey,
            unrecognized = draft.unrecognized,
        )
    }
}

private fun FilesLink.matchesFilesFilters(model: FilesTabModel): Boolean {
    val folder = folderKeyFromRelative(path)
    val fileName = path.substringAfterLast('/')
    val q = model.filesFilterText.lowercase()
    val projectName = when (val entry = pakku) {
        is PakkuEntry.Tracked -> entry.project.name.values
        is PakkuEntry.Detected -> entry.project.name.values
        else -> emptyList()
    }
    val textOk = q.isEmpty() ||
        q in fileName.lowercase() ||
        q in path.lowercase() ||
        q in folder.lowercase() ||
        projectName.any { q in it.lowercase() }
    val folderOk = model.filesFilterFolders.isEmpty() || folder in model.filesFilterFolders
    val kindOk = model.filesFilterChangeKinds.isEmpty() || change in model.filesFilterChangeKinds
    return textOk && folderOk && kindOk
}
