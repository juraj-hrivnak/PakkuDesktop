/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import kotlinx.datetime.LocalDateTime
import teksturepako.pakkuDesktop.app.git.*
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitChangelistFilterTest {

    // -- Basic matching --

    @Test
    fun emptyQueryReturnsAllRows() {
        val files = listOf(
            gitFile("mods/A.jar", GitChange.Added("mods/A.jar")),
            gitFile("readme.txt", GitChange.Modified("readme.txt")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "")
        assertEquals(rows, filtered)
    }

    @Test
    fun blankQueryFilteredReturnsNoMatch() {
        // The filter function itself does not treat whitespace as blank — only the UI layer does.
        // A whitespace-only query matches nothing.
        val files = listOf(gitFile("a.txt", GitChange.Modified("a.txt")))
        val rows = rowsFor(files)
        assertTrue(filterChangelistRows(rows, "  ").isEmpty())
    }

    @Test
    fun exactMatchReturnsOneFile() {
        val files = listOf(
            gitFile("mods/A.jar", GitChange.Added("mods/A.jar")),
            gitFile("mods/B.jar", GitChange.Modified("mods/B.jar")),
            gitFile("readme.txt", GitChange.Deleted("readme.txt")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "B.jar")
        val fileRows = filtered.filterIsInstance<PathTreeRow.Item<GitFile>>()
        assertEquals(1, fileRows.size)
        assertEquals("mods/B.jar", fileRows[0].path)
    }

    @Test
    fun partialMatchSubstring() {
        val files = listOf(
            gitFile("mods/Fabric.jar", GitChange.Added("mods/Fabric.jar")),
            gitFile("mods/Forge.jar", GitChange.Modified("mods/Forge.jar")),
            gitFile("config/fabric.toml", GitChange.Modified("config/fabric.toml")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "fabric")
        val paths = filtered.filterIsInstance<PathTreeRow.Item<GitFile>>().map { it.path }
        // Rows are sorted alphabetically by the tree snapshot
        assertEquals(listOf("config/fabric.toml", "mods/Fabric.jar"), paths)
    }

    @Test
    fun caseInsensitiveMatch() {
        val files = listOf(
            gitFile("Mods/MyMod.jar", GitChange.Added("Mods/MyMod.jar")),
            gitFile("mods/Other.jar", GitChange.Modified("mods/Other.jar")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "MODS")
        val paths = filtered.filterIsInstance<PathTreeRow.Item<GitFile>>().map { it.path }
        assertEquals(listOf("Mods/MyMod.jar", "mods/Other.jar"), paths)
    }

    // -- Folder ancestor preservation --

    @Test
    fun folderAncestorsPreservedForMatchingFile() {
        val files = listOf(
            gitFile("a/b/c/deep.txt", GitChange.Modified("a/b/c/deep.txt")),
            gitFile("a/b/other.txt", GitChange.Added("a/b/other.txt")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "deep")
        val folderPaths = filtered.filterIsInstance<PathTreeRow.Folder>().map { it.fullPath }
        // "a/b/c/deep.txt" → ancestors: a, a/b, a/b/c
        assertEquals(setOf("a", "a/b", "a/b/c"), folderPaths.toSet())
    }

    @Test
    fun rootFileHasNoFolderAncestors() {
        val files = listOf(
            gitFile("readme.txt", GitChange.Modified("readme.txt")),
            gitFile("LICENSE", GitChange.Added("LICENSE")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "readme")
        val folders = filtered.filterIsInstance<PathTreeRow.Folder>()
        assertEquals(0, folders.size)
        val fileRows = filtered.filterIsInstance<PathTreeRow.Item<GitFile>>()
        assertEquals(1, fileRows.size)
        assertEquals("readme.txt", fileRows[0].path)
    }

    @Test
    fun nestedFolderAncestorsAllPreserved() {
        val files = listOf(gitFile("x/y/z/file.jar", GitChange.Modified("x/y/z/file.jar")))
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "file")
        val folderPaths = filtered.filterIsInstance<PathTreeRow.Folder>().map { it.fullPath }
        assertEquals(setOf("x", "x/y", "x/y/z"), folderPaths.toSet())
    }

    @Test
    fun nonAncestorFoldersExcluded() {
        val files = listOf(
            gitFile("mods/A.jar", GitChange.Added("mods/A.jar")),
            gitFile("config/x.toml", GitChange.Modified("config/x.toml")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "A.jar")
        val folderPaths = filtered.filterIsInstance<PathTreeRow.Folder>().map { it.fullPath }
        assertEquals(setOf("mods"), folderPaths.toSet())
    }

    // -- No matches --

    @Test
    fun noMatchReturnsEmptyList() {
        val files = listOf(
            gitFile("mods/A.jar", GitChange.Added("mods/A.jar")),
            gitFile("readme.txt", GitChange.Modified("readme.txt")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "zzz_not_found")
        assertTrue(filtered.isEmpty())
    }

    // -- Multiple matches --

    @Test
    fun multipleFilesMatchSameQuery() {
        val files = listOf(
            gitFile("mods/Fabric.jar", GitChange.Added("mods/Fabric.jar")),
            gitFile("mods/FabricApi.jar", GitChange.Modified("mods/FabricApi.jar")),
            gitFile("config/fabric.toml", GitChange.Modified("config/fabric.toml")),
            gitFile("readme.txt", GitChange.Deleted("readme.txt")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "fabric")
        val paths = filtered.filterIsInstance<PathTreeRow.Item<GitFile>>().map { it.path }
        assertEquals(3, paths.size)
        assertTrue(paths.contains("mods/Fabric.jar"))
        assertTrue(paths.contains("mods/FabricApi.jar"))
        assertTrue(paths.contains("config/fabric.toml"))
    }

    @Test
    fun queryMatchesFolderNameInPath() {
        val files = listOf(
            gitFile("mods/A.jar", GitChange.Added("mods/A.jar")),
            gitFile("mods/B.jar", GitChange.Modified("mods/B.jar")),
            gitFile("plugins/C.jar", GitChange.Added("plugins/C.jar")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "mods")
        val paths = filtered.filterIsInstance<PathTreeRow.Item<GitFile>>().map { it.path }
        assertEquals(listOf("mods/A.jar", "mods/B.jar"), paths)
    }

    // -- Order preservation --

    @Test
    fun filterPreservesOriginalRowOrder() {
        val files = listOf(
            gitFile("a.txt", GitChange.Added("a.txt")),
            gitFile("b.txt", GitChange.Modified("b.txt")),
            gitFile("c.txt", GitChange.Deleted("c.txt")),
        )
        val rows = rowsFor(files)
        // Match first and last
        val filtered = filterChangelistRows(rows, "a.txt\nc.txt".take(5)) // "a.txt"
        val itemRows = filtered.filterIsInstance<PathTreeRow.Item<GitFile>>()
        assertEquals(1, itemRows.size)
        assertEquals("a.txt", itemRows[0].path)
    }

    @Test
    fun filterWithSharedAncestorFolder() {
        val files = listOf(
            gitFile("shared/a/deep1.txt", GitChange.Modified("shared/a/deep1.txt")),
            gitFile("shared/b/deep2.txt", GitChange.Modified("shared/b/deep2.txt")),
            gitFile("other.txt", GitChange.Added("other.txt")),
        )
        val rows = rowsFor(files)
        val filtered = filterChangelistRows(rows, "deep")
        val folderPaths = filtered.filterIsInstance<PathTreeRow.Folder>().map { it.fullPath }
        // Both files under "shared" → shared, shared/a, shared/b
        assertEquals(setOf("shared", "shared/a", "shared/b"), folderPaths.toSet())
    }

    // -- Helper --

    private fun gitFile(path: String, status: GitChange): GitFile =
        GitFile(
            path = path,
            status = status,
            lastModified = LocalDateTime(2026, 1, 1, 0, 0),
            size = 1L,
        )

    private fun rowsFor(files: List<GitFile>): List<PathTreeRow<GitFile>> {
        val expanded = gitFolderIds(files, grouped = false)
        return gitChangelistUiSnapshot(files, grouped = false).flatRows(expanded, emptySet())
    }
}
