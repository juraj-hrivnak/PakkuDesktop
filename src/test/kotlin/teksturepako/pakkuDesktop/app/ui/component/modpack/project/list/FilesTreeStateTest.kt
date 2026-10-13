/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import kotlinx.collections.immutable.persistentSetOf
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeRow
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeSnapshot
import teksturepako.pakkuDesktop.app.ui.component.tree.StickyFolderIndex
import teksturepako.pakkuDesktop.app.ui.component.tree.VisibleTreeRow
import teksturepako.pakkuDesktop.app.ui.component.tree.stickyFolderPathChain
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.app.ui.model.pathKey
import teksturepako.pakkuDesktop.app.ui.modpackUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FilesTreeStateTest {
    private fun link(path: String, key: String? = "fetch:$path"): FilesLink =
        FilesLink(
            path = path,
            pathKey = pathKey(path),
            pakku = null,
            onDisk = true,
            state = DiskState.InSync,
            change = FilesChangeKind.Fetch,
            selectionKey = key,
        )

    @Test
    fun folderExpansionToggleTwiceReturnsOriginalSet() {
        val start = ModpackModel()
        val collapsed = modpackUpdate(ModpackMsg.FilesFolderExpansionToggled("mods"), start)
        val restored = modpackUpdate(ModpackMsg.FilesFolderExpansionToggled("mods"), collapsed)
        assertEquals(persistentSetOf("mods"), collapsed.filesCollapsedFolders)
        assertEquals(persistentSetOf(), restored.filesCollapsedFolders)
        assertEquals(start.filesCollapsedFolders, restored.filesCollapsedFolders)
    }

    @Test
    fun expandAndCollapseAllNoOps() {
        val start = ModpackModel()
        assertSame(start, modpackUpdate(ModpackMsg.FilesExpandAllFolders, start))
        val ids = setOf("mods", "resourcepacks")
        val collapsed = modpackUpdate(ModpackMsg.FilesCollapseAllFolders(ids), start)
        assertSame(collapsed, modpackUpdate(ModpackMsg.FilesCollapseAllFolders(ids), collapsed))
    }

    @Test
    fun collapsedFoldersSurviveTabSwitch() {
        val collapsed = modpackUpdate(
            ModpackMsg.FilesFolderExpansionToggled("mods"),
            ModpackModel(),
        )
        val onProjects = modpackUpdate(ModpackMsg.TabSelected(SelectedTab.PROJECTS), collapsed)
        val backOnFiles = modpackUpdate(ModpackMsg.TabSelected(SelectedTab.FILES), onProjects)
        assertTrue("mods" in backOnFiles.filesCollapsedFolders)
    }

    @Test
    fun folderCheckboxKeysExcludeInSyncLinks() {
        val selectableA = link("mods/a.jar", "fetch:a")
        val selectableB = link("mods/b.jar", "fetch:b")
        val inSync = link("mods/c.jar", key = null)
        val keys = filesSelectableKeysUnderFolder(
            listOf(selectableA, selectableB, inSync),
            "mods",
        )
        assertEquals(setOf("fetch:a", "fetch:b"), keys)
    }

    @Test
    fun stickyFolderChainIncludesAncestorsAndNestedFolder() {
        val fileRow = PathTreeRow.Item(
            item = link("mods/fabric/a.jar"),
            path = "mods/fabric/a.jar",
            depth = 2,
            selected = false,
        )
        assertEquals(listOf("mods", "mods/fabric"), stickyFolderPathChain(fileRow))
        val folderRow = PathTreeRow.Folder(
            fullPath = "mods/fabric",
            displayName = "fabric",
            depth = 1,
            expanded = true,
            subtreeItemCount = 1,
            selectedInSubtree = 0,
        )
        assertEquals(listOf("mods", "mods/fabric"), stickyFolderPathChain(folderRow))
    }

    @Test
    fun stickyHeadersStayInPlaceAndGetPushedByNextRow() {
        val links = listOf(
            link("mods/a.jar", "fetch:a"),
            link("mods/fabric/b.jar", "fetch:b"),
        )
        val tree = PathTreeSnapshot.of(links) { it.path }
        // mods, mods/fabric, mods/fabric/b.jar, mods/a.jar
        val rows = tree.flatRows(tree.folderIds, emptySet())
        val index = StickyFolderIndex(rows)
        val h = 30
        fun headersAt(scroll: Int) =
            index.headers(rows.indices.map { VisibleTreeRow(it, it * h - scroll, h) }, h)
                .map { it.folder.fullPath to it.top }

        assertEquals(emptyList(), headersAt(0))
        assertEquals(listOf("mods" to 0, "mods/fabric" to h), headersAt(10))
        assertEquals(listOf("mods" to 0, "mods/fabric" to 20), headersAt(40))
        assertEquals(listOf("mods" to 0), headersAt(61))
    }

    @Test
    fun rangeSelectSkipsFoldersAndNonSelectableLinks() {
        val links = listOf(
            link("mods/a.jar", "fetch:a"),
            link("mods/b.jar", key = null),
            link("mods/c.jar", "fetch:c"),
            link("mods/d.jar", "fetch:d"),
        )
        val tree = PathTreeSnapshot.of(links) { it.path }
        val rows = tree.flatRows(expandedFolderPaths = tree.folderIds, selectedPaths = emptySet())
        val flat = filesFlatSelectableLinks(rows)
        assertEquals(listOf("fetch:a", "fetch:c", "fetch:d"), flat.mapNotNull { it.selectionKey })
        val keys = filesRangeSelectionKeys(flat, start = 0, end = 2)
        assertEquals(setOf("fetch:a", "fetch:c", "fetch:d"), keys)
    }
}
