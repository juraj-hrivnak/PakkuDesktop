/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.tree

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import teksturepako.pakkuDesktop.app.git.GitChange
import teksturepako.pakkuDesktop.app.git.GitFile
import teksturepako.pakkuDesktop.app.git.gitChangelistUiSnapshot
import teksturepako.pakkuDesktop.app.git.mergeChangelistExpandedFolders

class PathTreeTest {
    @Test
    fun pathFolderIdsFromPaths() {
        val ids = pathFolderIds(sequenceOf("a/b/c.txt", "a/d.txt", "e.txt"))
        assertEquals(setOf("a", "a/b"), ids)
    }

    @Test
    fun foldersBeforeItemsCaseSensitiveSort() {
        val snapshot = PathTreeSnapshot.of(
            listOf("z/e.txt", "a/B.txt", "a/b/c.txt", "a/A.txt"),
        ) { it }
        val rows = snapshot.flatRows(setOf("a", "a/b", "z"), emptySet())
        val kinds = rows.map { row ->
            when (row) {
                is PathTreeRow.Folder -> "d:${row.fullPath}"
                is PathTreeRow.Item -> "f:${row.path}"
            }
        }
        assertEquals(
            listOf(
                "d:a",
                "d:a/b",
                "f:a/b/c.txt",
                "f:a/A.txt",
                "f:a/B.txt",
                "d:z",
                "f:z/e.txt",
            ),
            kinds,
        )
    }

    @Test
    fun collapsedFolderHidesSubtree() {
        val snapshot = PathTreeSnapshot.of(listOf("a/b/c.txt", "a/d.txt")) { it }
        val rows = snapshot.flatRows(setOf("a"), emptySet())
        val underCollapsedB = rows.filter { row ->
            when (row) {
                is PathTreeRow.Folder -> row.fullPath.startsWith("a/b/")
                is PathTreeRow.Item -> row.path.startsWith("a/b/")
            }
        }
        assertEquals(emptyList(), underCollapsedB)
        assertEquals(null, rows.filterIsInstance<PathTreeRow.Item<*>>().find { it.path == "a/b/c.txt" })
    }

    @Test
    fun subtreeCountsForNestedFolders() {
        val snapshot = PathTreeSnapshot.of(listOf("a/b/c.txt", "a/b/d.txt", "a/e.txt")) { it }
        val rows = snapshot.flatRows(setOf("a", "a/b"), setOf("a/b/c.txt"))
        val folderA = rows.filterIsInstance<PathTreeRow.Folder>().first { it.fullPath == "a" }
        val folderAb = rows.filterIsInstance<PathTreeRow.Folder>().first { it.fullPath == "a/b" }
        assertEquals(3, folderA.subtreeItemCount)
        assertEquals(1, folderA.selectedInSubtree)
        assertEquals(2, folderAb.subtreeItemCount)
        assertEquals(1, folderAb.selectedInSubtree)
    }

    @Test
    fun mergeExpandedFoldersSamePathsKeepsIntersection() {
        val previous = setOf("a", "a/b", "stale")
        val merged = mergeExpandedFolders(
            previousExpanded = previous,
            previousPaths = listOf("a/x.txt", "a/b/y.txt"),
            incomingPaths = listOf("a/x.txt", "a/b/y.txt"),
        )
        assertEquals(setOf("a", "a/b"), merged)
    }

    @Test
    fun mergeExpandedFoldersNewPathsExpandsAll() {
        val merged = mergeExpandedFolders(
            previousExpanded = setOf("old"),
            previousPaths = listOf("x.txt"),
            incomingPaths = listOf("a/b/c.txt"),
        )
        assertEquals(setOf("a", "a/b"), merged)
    }

    @Test
    fun mergeExpandedFoldersEmptyIncoming() {
        assertEquals(
            emptySet<String>(),
            mergeExpandedFolders(setOf("a"), listOf("a/x.txt"), emptyList()),
        )
    }

    @Test
    fun gitAdapterMatchesLegacyFlatRows() {
        val files = listOf(
            gitFile("mods/A.jar", GitChange.Added("mods/A.jar")),
            gitFile("mods/B.jar", GitChange.Modified("mods/B.jar")),
            gitFile("config/x.toml", GitChange.Untracked("config/x.toml")),
            gitFile("readme.txt", GitChange.Deleted("readme.txt")),
        )
        val expanded = setOf("mods", "config")
        val selected = setOf("mods/B.jar")
        val adapterRows = gitChangelistUiSnapshot(files).flatRows(expanded, selected)
        val legacyRows = legacyGitFlatRows(files, expanded, selected)
        assertEquals(legacyRows, adapterRows.map { it.toLegacyShape() })
    }

    @Test
    fun mergeChangelistExpandedFoldersUsesPathKey() {
        val f1 = gitFile("a/b.txt", GitChange.Modified("a/b.txt"))
        val f2 = gitFile("a/b.txt", GitChange.Added("a/b.txt"))
        val kept = mergeChangelistExpandedFolders(setOf("a", "gone"), listOf(f1), listOf(f2))
        assertEquals(setOf("a"), kept)
    }

    private fun gitFile(path: String, status: GitChange): GitFile =
        GitFile(
            path = path,
            status = status,
            lastModified = LocalDateTime(2026, 1, 1, 0, 0),
            size = 1L,
        )
}

// --- Legacy git changelist tree (pre–PathTreeSnapshot) for parity ---

private sealed class LegacyGitFlatRow {
    data class Folder(
        val fullPath: String,
        val displayName: String,
        val depth: Int,
        val expanded: Boolean,
        val subtreeFileCount: Int,
        val selectedInSubtree: Int,
    ) : LegacyGitFlatRow()

    data class File(
        val path: String,
        val depth: Int,
        val selectedForCommit: Boolean,
    ) : LegacyGitFlatRow()
}

private fun PathTreeRow<GitFile>.toLegacyShape(): LegacyGitFlatRow =
    when (this) {
        is PathTreeRow.Folder ->
            LegacyGitFlatRow.Folder(
                fullPath = fullPath,
                displayName = displayName,
                depth = depth,
                expanded = expanded,
                subtreeFileCount = subtreeItemCount,
                selectedInSubtree = selectedInSubtree,
            )
        is PathTreeRow.Item ->
            LegacyGitFlatRow.File(
                path = path,
                depth = depth,
                selectedForCommit = selected,
            )
    }

private fun legacyGitFlatRows(
    files: List<GitFile>,
    expandedFolderPaths: Set<String>,
    selectedPaths: Set<String>,
): List<LegacyGitFlatRow> {
    val root = legacyBuildRoot(files)
    val subtreeByFolder = legacySubtreeIndex(files)
    return legacyFlatten(root, 0, expandedFolderPaths, subtreeByFolder, selectedPaths)
}

private data class LegacyDirNode(
    val fullPath: String,
    val children: Map<String, LegacyDirNode> = sortedMapOf(),
    val files: List<GitFile> = emptyList(),
)

private fun legacyBuildRoot(files: List<GitFile>): LegacyDirNode =
    files.fold(LegacyDirNode("")) { acc, file -> legacyInsert(acc, file) }

private fun legacyInsert(root: LegacyDirNode, file: GitFile): LegacyDirNode {
    val parts = file.path.split('/').filter { it.isNotEmpty() }
    if (parts.isEmpty()) return root
    return legacyInsertParts(root, parts, 0, file)
}

private fun legacyInsertParts(
    node: LegacyDirNode,
    parts: List<String>,
    idx: Int,
    file: GitFile,
): LegacyDirNode {
    if (idx == parts.lastIndex) {
        return node.copy(files = (node.files + file).sortedBy { it.path })
    }
    val segment = parts[idx]
    val childPath = parts.subList(0, idx + 1).joinToString("/")
    val child = node.children[segment] ?: LegacyDirNode(fullPath = childPath)
    val newChild = legacyInsertParts(child, parts, idx + 1, file)
    return node.copy(children = (node.children + (segment to newChild)).toSortedMap())
}

private fun legacySubtreeIndex(files: List<GitFile>): Map<String, List<GitFile>> =
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

private fun legacyFlatten(
    dir: LegacyDirNode,
    depth: Int,
    expandedFolderPaths: Set<String>,
    subtreeByFolder: Map<String, List<GitFile>>,
    selectedPaths: Set<String>,
): List<LegacyGitFlatRow> {
    val fromDirs =
        dir.children.flatMap { (name, sub) ->
            val subtree = subtreeByFolder[sub.fullPath].orEmpty()
            val selectedInSubtree = subtree.count { it.path in selectedPaths }
            val folderRow =
                LegacyGitFlatRow.Folder(
                    fullPath = sub.fullPath,
                    displayName = name,
                    depth = depth,
                    expanded = sub.fullPath in expandedFolderPaths,
                    subtreeFileCount = subtree.size,
                    selectedInSubtree = selectedInSubtree,
                )
            val nested =
                if (sub.fullPath in expandedFolderPaths) {
                    legacyFlatten(sub, depth + 1, expandedFolderPaths, subtreeByFolder, selectedPaths)
                } else {
                    emptyList()
                }
            listOf(folderRow) + nested
        }
    val fileRows =
        dir.files.sortedBy { it.path }.map { f ->
            LegacyGitFlatRow.File(
                path = f.path,
                depth = depth,
                selectedForCommit = f.path in selectedPaths,
            )
        }
    return fromDirs + fileRows
}
