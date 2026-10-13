/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeRow
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeSnapshot
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeStatus
import teksturepako.pakkuDesktop.app.ui.component.tree.mergeExpandedFolders
import teksturepako.pakkuDesktop.app.ui.component.tree.pathFolderIds

// -- Changelist tree --

/**
 * Top-level changelist groups when unversioned files are shown separately. Ids start with a
 * control character so they can never collide with a real repository folder.
 */
enum class GitChangeGroup(val id: String, val label: String) {
    Changes("\u0001changes", "Changes"),
    Unversioned("\u0001unversioned", "Unversioned Files"),
    ;

    companion object {
        fun ofFolder(folderPath: String): GitChangeGroup? = entries.firstOrNull { it.id == folderPath }
    }
}

fun GitChange.toTreeStatus(): TreeStatus = when (this) {
    is GitChange.Added -> TreeStatus.Added
    is GitChange.Modified -> TreeStatus.Modified
    is GitChange.Deleted -> TreeStatus.Deleted
    is GitChange.Untracked -> TreeStatus.Untracked
}

fun GitFile.changeGroup(): GitChangeGroup =
    if (status is GitChange.Untracked) GitChangeGroup.Unversioned else GitChangeGroup.Changes

/** Path of [file] in the changelist tree: under its [GitChangeGroup] when [grouped]. */
fun gitTreePath(file: GitFile, grouped: Boolean): String =
    if (grouped) "${file.changeGroup().id}/${file.path}" else file.path

/** Files under changelist tree folder [folderPath] (a group, or a folder inside one). */
fun gitFilesUnderTreeFolder(files: List<GitFile>, folderPath: String, grouped: Boolean): List<GitFile> =
    files.filter { gitTreePath(it, grouped).startsWith("$folderPath/") }

/** Stable when only [GitFile.status] etc. changes; used to preserve expand/collapse. */
fun gitChangelistPathsKey(files: List<GitFile>): String =
    files.asSequence().map { it.path }.sorted().joinToString("\u0000")

/** Full fingerprint when directory data must rebuild (paths + status). */
fun gitChangelistStructureKey(files: List<GitFile>): String =
    files.joinToString("\u0000") { "${it.path}\u0001${it.status}" }

fun gitFolderIds(files: List<GitFile>, grouped: Boolean): Set<String> =
    pathFolderIds(files.asSequence().map { gitTreePath(it, grouped) })

/**
 * When the set of changed *paths* changes, expand all folders again.
 * Otherwise keep [previousExpanded] intersected with still-valid folder ids.
 */
fun mergeChangelistExpandedFolders(
    previousExpanded: Set<String>,
    previousFiles: List<GitFile>,
    incomingFiles: List<GitFile>,
    grouped: Boolean,
): Set<String> =
    mergeExpandedFolders(
        previousExpanded = previousExpanded,
        previousPaths = previousFiles.map { gitTreePath(it, grouped) },
        incomingPaths = incomingFiles.map { gitTreePath(it, grouped) },
    )

typealias GitChangelistFlatRow = PathTreeRow<GitFile>

class GitChangelistUiSnapshot private constructor(
    private val snapshot: PathTreeSnapshot<GitFile>,
) {
    val folderIds: Set<String> get() = snapshot.folderIds

    fun flatRows(expandedFolderPaths: Set<String>, selectedPaths: Set<String>): List<GitChangelistFlatRow> =
        snapshot.flatRows(expandedFolderPaths, selectedPaths)

    companion object {
        fun fromFiles(files: List<GitFile>, grouped: Boolean): GitChangelistUiSnapshot =
            GitChangelistUiSnapshot(PathTreeSnapshot.of(files) { gitTreePath(it, grouped) })
    }
}

fun gitChangelistUiSnapshot(files: List<GitFile>, grouped: Boolean): GitChangelistUiSnapshot =
    GitChangelistUiSnapshot.fromFiles(files, grouped)
