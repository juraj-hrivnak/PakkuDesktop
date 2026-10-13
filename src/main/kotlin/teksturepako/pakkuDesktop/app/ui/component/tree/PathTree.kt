/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.tree

sealed interface PathTreeRow<out T>
{
    data class Folder(
        val fullPath: String,
        val displayName: String,
        val depth: Int,
        val expanded: Boolean,
        val subtreeItemCount: Int,
        val selectedInSubtree: Int,
    ) : PathTreeRow<Nothing>

    data class Item<T>(
        val item: T,
        val path: String,
        val depth: Int,
        val selected: Boolean,
    ) : PathTreeRow<T>
}

class PathTreeSnapshot<T> private constructor(
    private val root: PathDirNode<T>,
    private val subtreeByFolder: Map<String, List<T>>,
    private val pathOf: (T) -> String,
    val folderIds: Set<String>,
)
{
    fun flatRows(expandedFolderPaths: Set<String>, selectedPaths: Set<String>): List<PathTreeRow<T>> =
        flattenPathTreeRows(root, 0, expandedFolderPaths, subtreeByFolder, selectedPaths, pathOf)

    companion object
    {
        fun <T> of(items: List<T>, pathOf: (T) -> String): PathTreeSnapshot<T>
        {
            val root = buildPathDirRoot(items, pathOf)
            return PathTreeSnapshot(
                root = root,
                subtreeByFolder = buildSubtreeItemIndex(items, pathOf),
                pathOf = pathOf,
                folderIds = pathFolderIds(items.asSequence().map(pathOf)),
            )
        }
    }
}

fun pathFolderIds(paths: Sequence<String>): Set<String> = paths.flatMap(::folderPrefixes).toSet()

/** Every folder prefix of [path] (e.g. `mods/foo/a.jar` → `mods`, `mods/foo`). */
fun folderPathPrefixList(path: String): List<String> = folderPrefixes(path).toList()

/** Folder rows to pin above the current scroll context (ancestors, plus the folder row itself). */
fun stickyFolderPathChain(row: PathTreeRow<*>): List<String> = when (row) {
    is PathTreeRow.Folder -> folderPathPrefixList(row.fullPath) + row.fullPath
    is PathTreeRow.Item -> folderPathPrefixList(row.path)
}

private fun folderPrefixes(path: String): Sequence<String> = sequence {
    val parts = path.split('/').filter { it.isNotEmpty() }
    if (parts.size <= 1) return@sequence
    var acc = parts[0]
    yield(acc)
    for (i in 1 until parts.lastIndex)
    {
        acc = "$acc/${parts[i]}"
        yield(acc)
    }
}

/**
 * When the set of *paths* changes, expand all folders again.
 * Otherwise keep [previousExpanded] intersected with still-valid folder ids.
 */
fun mergeExpandedFolders(
    previousExpanded: Set<String>,
    previousPaths: List<String>,
    incomingPaths: List<String>,
): Set<String>
{
    val newIds = pathFolderIds(incomingPaths.asSequence())
    if (incomingPaths.isEmpty()) return emptySet()
    return if (pathsKey(previousPaths) != pathsKey(incomingPaths))
    {
        newIds
    }
    else
    {
        previousExpanded.intersect(newIds)
    }
}

private fun pathsKey(paths: List<String>): String = paths.sorted().joinToString("\u0000")

private data class PathDirNode<T>(
    val fullPath: String,
    val children: Map<String, PathDirNode<T>> = sortedMapOf(),
    val items: List<T> = emptyList(),
)

private fun <T> buildPathDirRoot(items: List<T>, pathOf: (T) -> String): PathDirNode<T> =
    items.fold(PathDirNode("")) { acc, item -> insertPathItem(acc, item, pathOf) }

private fun <T> insertPathItem(root: PathDirNode<T>, item: T, pathOf: (T) -> String): PathDirNode<T>
{
    val path = pathOf(item)
    val parts = path.split('/').filter { it.isNotEmpty() }
    if (parts.isEmpty()) return root
    return insertParts(root, parts, 0, item, pathOf)
}

private fun <T> insertParts(
    node: PathDirNode<T>,
    parts: List<String>,
    idx: Int,
    item: T,
    pathOf: (T) -> String,
): PathDirNode<T>
{
    if (idx == parts.lastIndex)
    {
        val merged = (node.items + item).sortedBy(pathOf)
        return node.copy(items = merged)
    }
    val segment = parts[idx]
    val childPath = parts.subList(0, idx + 1).joinToString("/")
    val child = node.children[segment] ?: PathDirNode(fullPath = childPath)
    val newChild = insertParts(child, parts, idx + 1, item, pathOf)
    return node.copy(children = (node.children + (segment to newChild)).toSortedMap())
}

private fun <T> buildSubtreeItemIndex(items: List<T>, pathOf: (T) -> String): Map<String, List<T>> =
    items.asSequence().flatMap { item ->
            val path = pathOf(item)
            val parts = path.split('/').filter { it.isNotEmpty() }
            if (parts.size <= 1)
            {
                emptySequence()
            }
            else
            {
                sequence {
                    var acc = parts[0]
                    yield(acc to item)
                    for (i in 1 until parts.lastIndex)
                    {
                        acc = "$acc/${parts[i]}"
                        yield(acc to item)
                    }
                }
            }
        }.groupBy({ it.first }, { it.second }).mapValues { (_, v) -> v }

private fun <T> flattenPathTreeRows(
    dir: PathDirNode<T>,
    depth: Int,
    expandedFolderPaths: Set<String>,
    subtreeByFolder: Map<String, List<T>>,
    selectedPaths: Set<String>,
    pathOf: (T) -> String,
): List<PathTreeRow<T>>
{
    val fromDirs: List<PathTreeRow<T>> = dir.children.flatMap { (name, sub) ->
        val subtree = subtreeByFolder[sub.fullPath].orEmpty()
        val selectedInSubtree = subtree.count { pathOf(it) in selectedPaths }
        val folderRow = PathTreeRow.Folder(
            fullPath = sub.fullPath,
            displayName = name,
            depth = depth,
            expanded = sub.fullPath in expandedFolderPaths,
            subtreeItemCount = subtree.size,
            selectedInSubtree = selectedInSubtree,
        )
        val nested = if (sub.fullPath in expandedFolderPaths)
        {
            flattenPathTreeRows(sub, depth + 1, expandedFolderPaths, subtreeByFolder, selectedPaths, pathOf)
        }
        else
        {
            emptyList()
        }
        listOf(folderRow) + nested
    }
    val itemRows: List<PathTreeRow<T>> = dir.items.sortedBy(pathOf).map { item ->
        val path = pathOf(item)
        PathTreeRow.Item(
            item = item,
            path = path,
            depth = depth,
            selected = path in selectedPaths,
        )
    }
    return fromDirs + itemRows
}
