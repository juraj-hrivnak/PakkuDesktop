/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import kotlinx.collections.immutable.persistentSetOf
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeRow
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeSnapshot
import teksturepako.pakkuDesktop.app.ui.component.tree.pinnedFolderRowsForScroll
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
    fun pinnedFoldersKeepParentsWhenScrollingNestedSubtree() {
        val links = listOf(
            link("mods/a.jar", "fetch:a"),
            link("mods/fabric/b.jar", "fetch:b"),
        )
        val tree = PathTreeSnapshot.of(links) { it.path }
        val expanded = tree.folderIds
        val rows = tree.flatRows(expanded, emptySet())
        val folderIndexByPath = rows.mapIndexedNotNull { index, row ->
            (row as? PathTreeRow.Folder)?.fullPath?.let { it to index }
        }.toMap()
        val folderRowByPath = rows.filterIsInstance<PathTreeRow.Folder>().associateBy { it.fullPath }
        val fileIndex = rows.indexOfFirst { it is PathTreeRow.Item && it.path == "mods/fabric/b.jar" }
        val pinned = pinnedFolderRowsForScroll(
            rows,
            fileIndex,
            anchorScrollOffsetPx = 0,
            folderIndexByPath,
            folderRowByPath,
        )
        assertEquals(listOf("mods", "mods/fabric"), pinned.map { it.fullPath })
        val nestedFolderIndex = rows.indexOfFirst { it is PathTreeRow.Folder && it.fullPath == "mods/fabric" }
        val pinnedAtNestedFolder = pinnedFolderRowsForScroll(
            rows,
            nestedFolderIndex,
            anchorScrollOffsetPx = 0,
            folderIndexByPath,
            folderRowByPath,
        )
        assertEquals(listOf("mods"), pinnedAtNestedFolder.map { it.fullPath })
        val pinnedWhileScrollingFolderAway = pinnedFolderRowsForScroll(
            rows,
            nestedFolderIndex,
            anchorScrollOffsetPx = 4,
            folderIndexByPath,
            folderRowByPath,
        )
        assertEquals(listOf("mods", "mods/fabric"), pinnedWhileScrollingFolderAway.map { it.fullPath })
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
