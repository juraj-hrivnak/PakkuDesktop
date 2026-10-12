/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeRow
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeSnapshot
import teksturepako.pakkuDesktop.app.ui.component.tree.mergeExpandedFolders
import teksturepako.pakkuDesktop.app.ui.component.tree.pathFolderIds

// -- Changelist tree --

/** Stable when only [GitFile.status] etc. changes; used to preserve expand/collapse. */
fun gitChangelistPathsKey(files: List<GitFile>): String =
    files.asSequence().map { it.path }.sorted().joinToString("\u0000")

/** Full fingerprint when directory data must rebuild (paths + status). */
fun gitChangelistStructureKey(files: List<GitFile>): String =
    files.joinToString("\u0000") { "${it.path}\u0001${it.status}" }

fun gitFolderIds(files: List<GitFile>): Set<String> =
    pathFolderIds(files.asSequence().map { it.path })

/**
 * When the set of changed *paths* changes, expand all folders again.
 * Otherwise keep [previousExpanded] intersected with still-valid folder ids.
 */
fun mergeChangelistExpandedFolders(
    previousExpanded: Set<String>,
    previousFiles: List<GitFile>,
    incomingFiles: List<GitFile>,
): Set<String> =
    mergeExpandedFolders(
        previousExpanded = previousExpanded,
        previousPaths = previousFiles.map { it.path },
        incomingPaths = incomingFiles.map { it.path },
    )

typealias GitChangelistFlatRow = PathTreeRow<GitFile>

class GitChangelistUiSnapshot private constructor(
    private val snapshot: PathTreeSnapshot<GitFile>,
) {
    val folderIds: Set<String> get() = snapshot.folderIds

    fun flatRows(expandedFolderPaths: Set<String>, selectedPaths: Set<String>): List<GitChangelistFlatRow> =
        snapshot.flatRows(expandedFolderPaths, selectedPaths)

    companion object {
        fun fromFiles(files: List<GitFile>): GitChangelistUiSnapshot =
            GitChangelistUiSnapshot(PathTreeSnapshot.of(files) { it.path })
    }
}

fun gitChangelistUiSnapshot(files: List<GitFile>): GitChangelistUiSnapshot =
    GitChangelistUiSnapshot.fromFiles(files)
