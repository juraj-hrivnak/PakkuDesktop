/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git

import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitFile

// ---------------------------------------------------------------------------
// Pure changelist tree — immutable data, folds / maps only (no shared mutation)
// ---------------------------------------------------------------------------

/** Stable when only [GitFile.status] etc. changes; used to preserve expand/collapse. */
fun gitChangelistPathsKey(files: List<GitFile>): String =
    files.asSequence().map { it.path }.sorted().joinToString("\u0000")

/** Full fingerprint when directory data must rebuild (paths + status). */
fun gitChangelistStructureKey(files: List<GitFile>): String =
    files.joinToString("\u0000") { "${it.path}\u0001${it.status}" }

fun gitFolderIds(files: List<GitFile>): Set<String> =
    files.asSequence().flatMap { folderPrefixes(it.path) }.toSet()

private fun folderPrefixes(path: String): Sequence<String> = sequence {
    val parts = path.split('/').filter { it.isNotEmpty() }
    if (parts.size <= 1) return@sequence
    var acc = parts[0]
    yield(acc)
    for (i in 1 until parts.lastIndex) {
        acc = "$acc/${parts[i]}"
        yield(acc)
    }
}

/**
 * When the set of changed *paths* changes, expand all folders again.
 * Otherwise keep [previousExpanded] intersected with still-valid folder ids.
 */
fun mergeChangelistExpandedFolders(
    previousExpanded: Set<String>,
    previousFiles: List<GitFile>,
    incomingFiles: List<GitFile>,
): Set<String> {
    val newIds = gitFolderIds(incomingFiles)
    if (incomingFiles.isEmpty()) return emptySet()
    return if (gitChangelistPathsKey(previousFiles) != gitChangelistPathsKey(incomingFiles)) {
        newIds
    } else {
        previousExpanded.intersect(newIds)
    }
}

private data class ChangesDirNode(
    val fullPath: String,
    val children: Map<String, ChangesDirNode> = sortedMapOf(),
    val files: List<GitFile> = emptyList(),
)

private fun buildChangesDirRoot(files: List<GitFile>): ChangesDirNode =
    files.fold(ChangesDirNode("")) { acc, file -> insertGitFile(acc, file) }

private fun insertGitFile(root: ChangesDirNode, file: GitFile): ChangesDirNode {
    val parts = file.path.split('/').filter { it.isNotEmpty() }
    if (parts.isEmpty()) return root
    return insertParts(root, parts, 0, file)
}

private fun insertParts(node: ChangesDirNode, parts: List<String>, idx: Int, file: GitFile): ChangesDirNode {
    if (idx == parts.lastIndex) {
        val merged = (node.files + file).sortedBy { it.path }
        return node.copy(files = merged)
    }
    val segment = parts[idx]
    val childPath = parts.subList(0, idx + 1).joinToString("/")
    val child = node.children[segment] ?: ChangesDirNode(fullPath = childPath)
    val newChild = insertParts(child, parts, idx + 1, file)
    return node.copy(children = (node.children + (segment to newChild)).toSortedMap())
}

private fun buildSubtreeFileIndex(files: List<GitFile>): Map<String, List<GitFile>> =
    files
        .asSequence()
        .flatMap { f ->
            val parts = f.path.split('/').filter { it.isNotEmpty() }
            if (parts.size <= 1) {
                emptySequence()
            } else {
                sequence {
                    var acc = parts[0]
                    yield(acc to f)
                    for (i in 1 until parts.lastIndex) {
                        acc = "$acc/${parts[i]}"
                        yield(acc to f)
                    }
                }
            }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, v) -> v }

sealed class GitChangelistFlatRow {
    data class Folder(
        val fullPath: String,
        val displayName: String,
        val depth: Int,
        val expanded: Boolean,
        val subtreeFileCount: Int,
        val selectedInSubtree: Int,
    ) : GitChangelistFlatRow()

    data class File(
        val file: GitFile,
        val depth: Int,
        val selectedForCommit: Boolean,
    ) : GitChangelistFlatRow()
}

private fun flattenChangelistRows(
    dir: ChangesDirNode,
    depth: Int,
    expandedFolderPaths: Set<String>,
    subtreeByFolder: Map<String, List<GitFile>>,
    selectedPaths: Set<String>,
): List<GitChangelistFlatRow> {
    val fromDirs: List<GitChangelistFlatRow> =
        dir.children.flatMap { (name, sub) ->
            val subtree = subtreeByFolder[sub.fullPath].orEmpty()
            val selectedInSubtree = subtree.count { it.path in selectedPaths }
            val folderRow =
                GitChangelistFlatRow.Folder(
                    fullPath = sub.fullPath,
                    displayName = name,
                    depth = depth,
                    expanded = sub.fullPath in expandedFolderPaths,
                    subtreeFileCount = subtree.size,
                    selectedInSubtree = selectedInSubtree,
                )
            val nested =
                if (sub.fullPath in expandedFolderPaths) {
                    flattenChangelistRows(sub, depth + 1, expandedFolderPaths, subtreeByFolder, selectedPaths)
                } else {
                    emptyList()
                }
            listOf(folderRow) + nested
        }
    val fileRows: List<GitChangelistFlatRow> =
        dir.files.sortedBy { it.path }.map { f ->
            GitChangelistFlatRow.File(
                file = f,
                depth = depth,
                selectedForCommit = f.path in selectedPaths,
            )
        }
    return fromDirs + fileRows
}

class GitChangelistUiSnapshot private constructor(
    private val root: ChangesDirNode,
    private val subtreeByFolder: Map<String, List<GitFile>>,
    val folderIds: Set<String>,
) {
    fun flatRows(expandedFolderPaths: Set<String>, selectedPaths: Set<String>): List<GitChangelistFlatRow> =
        flattenChangelistRows(root, 0, expandedFolderPaths, subtreeByFolder, selectedPaths)

    companion object {
        fun fromFiles(files: List<GitFile>): GitChangelistUiSnapshot {
            val root = buildChangesDirRoot(files)
            return GitChangelistUiSnapshot(
                root = root,
                subtreeByFolder = buildSubtreeFileIndex(files),
                folderIds = gitFolderIds(files),
            )
        }
    }
}

fun gitChangelistUiSnapshot(files: List<GitFile>): GitChangelistUiSnapshot =
    GitChangelistUiSnapshot.fromFiles(files)
