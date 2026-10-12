/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import com.github.michaelbull.result.Ok
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.ui.model.DiskSnapshot
import teksturepako.pakkuDesktop.app.ui.model.FetchCleanupPreview
import teksturepako.pakkuDesktop.app.ui.model.FetchDownloadPreview
import teksturepako.pakkuDesktop.app.ui.model.FetchOverridePreview
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.pathKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FilesLinksTest {
    private fun modProject(
        fileName: String,
        slug: String = "test-mod",
        extraFile: ProjectFile? = null,
    ): Project {
        val primary = ProjectFile(
            type = "modrinth",
            id = "file-$fileName",
            fileName = fileName,
        )
        val files = buildSet {
            add(primary)
            if (extraFile != null) add(extraFile)
        }
        return Project(
            type = ProjectType.MOD,
            slug = mutableMapOf("modrinth" to slug),
            name = mutableMapOf("en" to "Test Mod"),
            id = mutableMapOf("modrinth" to slug),
            files = files.toMutableSet(),
        )
    }

    private fun lockWith(vararg projects: Project): LockFile =
        LockFile().also { lock -> projects.forEach { lock.add(it) } }

    private data class LinkArgs(
        val projects: List<Project> = emptyList(),
        val manualOverrides: List<FetchOverridePreview> = emptyList(),
        val diskSnapshot: DiskSnapshot = DiskSnapshot(),
        val mode: FilesPreviewMode = FilesPreviewMode.None,
        val fetchDownloads: List<FetchDownloadPreview> = emptyList(),
        val fetchOverrides: List<FetchOverridePreview> = emptyList(),
        val fetchCleanup: List<FetchCleanupPreview> = emptyList(),
        val hashMismatchKeys: Set<String> = emptySet(),
        val syncAdded: List<Project> = emptyList(),
        val syncRemoved: List<Project> = emptyList(),
        val syncUpdated: List<Project> = emptyList(),
    )

    private fun links(args: LinkArgs): List<FilesLink> = buildFilesLinks(
        projects = args.projects,
        config = null,
        manualOverrides = args.manualOverrides.toPersistentList(),
        diskSnapshot = args.diskSnapshot,
        filesPreviewMode = args.mode,
        fetchDownloads = args.fetchDownloads.toPersistentList(),
        fetchOverrides = args.fetchOverrides.toPersistentList(),
        fetchCleanup = args.fetchCleanup.toPersistentList(),
        filesHashMismatchKeys = args.hashMismatchKeys.toPersistentSet(),
        syncAdded = args.syncAdded.toPersistentList(),
        syncRemoved = args.syncRemoved.toPersistentList(),
        syncUpdated = args.syncUpdated.toPersistentList(),
    )

    private fun tabModel(args: LinkArgs): FilesTabModel {
        val lock = lockWith(*args.projects.toTypedArray())
        return FilesTabModel(
            lockFile = Ok(lock),
            configFile = null,
            selectedProject = null,
            selectedProjectKeys = persistentSetOf(),
            lastFilesFab = null,
            filesSplitRatio = 0.5f,
            actionName = null,
            filesPreviewMode = args.mode,
            fetchDownloads = args.fetchDownloads.toPersistentList(),
            fetchOverrides = args.fetchOverrides.toPersistentList(),
            manualOverrides = args.manualOverrides.toPersistentList(),
            diskSnapshot = args.diskSnapshot,
            fetchCleanup = args.fetchCleanup.toPersistentList(),
            filesHashMismatchKeys = args.hashMismatchKeys.toPersistentSet(),
            syncAdded = args.syncAdded.toPersistentList(),
            syncRemoved = args.syncRemoved.toPersistentList(),
            syncUpdated = args.syncUpdated.toPersistentList(),
            filesFilterText = "",
            filesFilterFolders = persistentSetOf(),
            filesFilterChangeKinds = persistentSetOf(),
            wantsFocusProjectsFilter = false,
        )
    }

    private fun linkAt(args: LinkArgs, path: String): FilesLink =
        links(args).single { it.pathKey == pathKey(path) }

    @Test
    fun b1TrackedPresentInSync() {
        val project = modProject("foo.jar")
        val args = LinkArgs(
            projects = listOf(project),
            diskSnapshot = DiskSnapshot(
                expected = persistentMapOf(pathKey("mods/foo.jar") to "mods/foo.jar"),
                present = persistentSetOf(pathKey("mods/foo.jar")),
            ),
        )
        val link = linkAt(args, "mods/foo.jar")
        assertIs<PakkuEntry.Tracked>(link.pakku)
        assertTrue(link.onDisk)
        assertEquals(DiskState.InSync, link.state)
        assertNull(link.change)
    }

    @Test
    fun b2TrackedMissing() {
        val project = modProject("foo.jar")
        val args = LinkArgs(
            projects = listOf(project),
            diskSnapshot = DiskSnapshot(
                expected = persistentMapOf(pathKey("mods/foo.jar") to "mods/foo.jar"),
            ),
        )
        val link = linkAt(args, "mods/foo.jar")
        assertEquals(DiskState.Missing, link.state)
        assertFalse(link.onDisk)
    }

    @Test
    fun b3OverridePresent() {
        val override = FetchOverridePreview(
            relativeOutputPath = "mods/custom.jar",
            sourcePath = "/src/custom.jar",
            fullOutputPath = "/out/mods/custom.jar",
        )
        val args = LinkArgs(
            manualOverrides = listOf(override),
            diskSnapshot = DiskSnapshot(
                expected = persistentMapOf(pathKey("mods/custom.jar") to "mods/custom.jar"),
                present = persistentSetOf(pathKey("mods/custom.jar")),
            ),
        )
        val link = linkAt(args, "mods/custom.jar")
        assertIs<PakkuEntry.Override>(link.pakku)
        assertEquals(DiskState.InSync, link.state)
    }

    @Test
    fun b4OverrideMissing() {
        val override = FetchOverridePreview(
            relativeOutputPath = "mods/custom.jar",
            sourcePath = "/src/custom.jar",
            fullOutputPath = "/out/mods/custom.jar",
        )
        val args = LinkArgs(
            manualOverrides = listOf(override),
            diskSnapshot = DiskSnapshot(
                expected = persistentMapOf(pathKey("mods/custom.jar") to "mods/custom.jar"),
            ),
        )
        val link = linkAt(args, "mods/custom.jar")
        assertEquals(DiskState.Missing, link.state)
    }

    @Test
    fun b5UntrackedFromSnapshot() {
        val args = LinkArgs(
            diskSnapshot = DiskSnapshot(untracked = persistentListOf("mods/stray.jar")),
        )
        val link = linkAt(args, "mods/stray.jar")
        assertNull(link.pakku)
        assertEquals(DiskState.Untracked, link.state)
        assertTrue(link.onDisk)
    }

    @Test
    fun oneLinkPerPathKeyCaseInsensitive() {
        val args = LinkArgs(
            diskSnapshot = DiskSnapshot(
                expected = persistentMapOf(
                    pathKey("mods/A.jar") to "mods/A.jar",
                    pathKey("mods/a.jar") to "mods/a.jar",
                ),
                present = persistentSetOf(pathKey("mods/a.jar")),
                untracked = persistentListOf("mods/A.jar"),
            ),
        )
        assertEquals(1, links(args).count { it.pathKey == pathKey("mods/A.jar") })
    }

    @Test
    fun fetchMissingFileDownload() {
        val project = modProject("foo.jar")
        val file = project.files.first()
        val args = LinkArgs(
            projects = listOf(project),
            diskSnapshot = DiskSnapshot(
                expected = persistentMapOf(pathKey("mods/foo.jar") to "mods/foo.jar"),
            ),
            mode = FilesPreviewMode.Fetch,
            fetchDownloads = listOf(FetchDownloadPreview(project, file)),
        )
        val link = linkAt(args, "mods/foo.jar")
        assertEquals(FilesChangeKind.Fetch, link.change)
        assertEquals("${FetchSelectionPrefix}${project.uiKey()}", link.selectionKey)
        assertEquals(FilesDirection.ToDisk, link.change?.direction())
    }

    @Test
    fun fetchCleanupWithoutBaseLink() {
        val args = LinkArgs(
            mode = FilesPreviewMode.Fetch,
            fetchCleanup = listOf(
                FetchCleanupPreview(
                    relativePath = "mods/meta.jar.meta",
                    absolutePath = "/pack/mods/meta.jar.meta",
                    fileName = "meta.jar.meta",
                ),
            ),
            diskSnapshot = DiskSnapshot(untracked = persistentListOf("mods/meta.jar.meta")),
        )
        val link = linkAt(args, "mods/meta.jar.meta")
        assertNull(link.pakku)
        assertEquals(FilesChangeKind.Cleanup, link.change)
        assertEquals("${CleanupSelectionPrefix}/pack/mods/meta.jar.meta", link.selectionKey)
    }

    @Test
    fun fetchHashMismatchNoSelection() {
        val project = modProject("foo.jar")
        val args = LinkArgs(
            projects = listOf(project),
            diskSnapshot = DiskSnapshot(
                expected = persistentMapOf(pathKey("mods/foo.jar") to "mods/foo.jar"),
                present = persistentSetOf(pathKey("mods/foo.jar")),
            ),
            mode = FilesPreviewMode.Fetch,
            hashMismatchKeys = setOf(project.uiKey()),
        )
        val link = linkAt(args, "mods/foo.jar")
        assertEquals(FilesChangeKind.HashMismatch, link.change)
        assertNull(link.selectionKey)
        assertNull(link.change?.direction())
    }

    @Test
    fun updatedAbsorbsOldLockPath() {
        val lockProject = modProject("sodium-0.5.8.jar", slug = "sodium")
        val updatedProject = modProject("sodium-0.5.9.jar", slug = "sodium")
        val args = LinkArgs(
            projects = listOf(lockProject),
            diskSnapshot = DiskSnapshot(
                expected = persistentMapOf(pathKey("mods/sodium-0.5.8.jar") to "mods/sodium-0.5.8.jar"),
                present = persistentSetOf(pathKey("mods/sodium-0.5.9.jar")),
                untracked = persistentListOf("mods/sodium-0.5.9.jar"),
            ),
            mode = FilesPreviewMode.Sync,
            syncUpdated = listOf(updatedProject),
        )
        val result = links(args)
        assertEquals(1, result.size)
        assertEquals("mods/sodium-0.5.9.jar", result.single().path)
        assertEquals(FilesChangeKind.Updated, result.single().change)
        assertEquals(DiskState.Modified, result.single().state)
    }

    @Test
    fun sameUntrackedPathFetchCleanupAndSyncAdded() {
        val detected = modProject("jei.jar", slug = "jei")
        val snapshot = DiskSnapshot(untracked = persistentListOf("mods/jei.jar"))
        val fetchArgs = LinkArgs(
            mode = FilesPreviewMode.Fetch,
            fetchCleanup = listOf(
                FetchCleanupPreview("mods/jei.jar", "/abs/mods/jei.jar", "jei.jar"),
            ),
            diskSnapshot = snapshot,
        )
        val syncArgs = LinkArgs(
            mode = FilesPreviewMode.Sync,
            syncAdded = listOf(detected),
            diskSnapshot = snapshot,
        )
        val fetchLink = linkAt(fetchArgs, "mods/jei.jar")
        val syncLink = linkAt(syncArgs, "mods/jei.jar")
        assertEquals(fetchLink.path, syncLink.path)
        assertEquals(FilesChangeKind.Cleanup, fetchLink.change)
        assertEquals(FilesChangeKind.Added, syncLink.change)
        assertEquals(FilesDirection.ToDisk, fetchLink.change?.direction())
        assertEquals(FilesDirection.ToPakku, syncLink.change?.direction())
    }

    @Test
    fun sameMissingPathFetchAndSyncRemoved() {
        val project = modProject("gone.jar")
        val snapshot = DiskSnapshot(
            expected = persistentMapOf(pathKey("mods/gone.jar") to "mods/gone.jar"),
        )
        val file = project.files.first()
        val fetchArgs = LinkArgs(
            projects = listOf(project),
            diskSnapshot = snapshot,
            mode = FilesPreviewMode.Fetch,
            fetchDownloads = listOf(FetchDownloadPreview(project, file)),
        )
        val syncArgs = LinkArgs(
            projects = listOf(project),
            diskSnapshot = snapshot,
            mode = FilesPreviewMode.Sync,
            syncRemoved = listOf(project),
        )
        val fetchLink = linkAt(fetchArgs, "mods/gone.jar")
        val syncLink = linkAt(syncArgs, "mods/gone.jar")
        assertEquals(fetchLink.path, syncLink.path)
        assertEquals(FilesChangeKind.Fetch, fetchLink.change)
        assertEquals(FilesChangeKind.Removed, syncLink.change)
    }

    @Test
    fun continuationAcrossTwoClusters() {
        val extra = ProjectFile(type = "curseforge", id = "cf-1", fileName = "other.jar")
        val project = modProject("one.jar", extraFile = extra)
        val args = LinkArgs(
            projects = listOf(project),
            diskSnapshot = DiskSnapshot(
                expected = persistentMapOf(
                    pathKey("mods/one.jar") to "mods/one.jar",
                    pathKey("mods/other.jar") to "mods/other.jar",
                ),
                present = persistentSetOf(pathKey("mods/one.jar"), pathKey("mods/other.jar")),
            ),
        )
        val tracked = links(args).mapNotNull { it.pakku as? PakkuEntry.Tracked }
        assertEquals(2, tracked.size)
        assertFalse(tracked[0].continuation)
        assertTrue(tracked[1].continuation)
    }

    @Test
    fun directionTableForEveryChangeKind() {
        assertEquals(FilesDirection.ToDisk, FilesChangeKind.Fetch.direction())
        assertEquals(FilesDirection.ToDisk, FilesChangeKind.Override.direction())
        assertEquals(FilesDirection.ToDisk, FilesChangeKind.Cleanup.direction())
        assertEquals(FilesDirection.ToPakku, FilesChangeKind.Added.direction())
        assertEquals(FilesDirection.ToPakku, FilesChangeKind.Updated.direction())
        assertEquals(FilesDirection.ToPakku, FilesChangeKind.Removed.direction())
        assertNull(FilesChangeKind.HashMismatch.direction())
    }

    @Test
    fun selectionKeyParityWithFilesDiffPairs() {
        val project = modProject("parity.jar")
        val file = project.files.first()
        val override = FetchOverridePreview(
            relativeOutputPath = "mods/override.jar",
            sourcePath = "/src/override.jar",
            fullOutputPath = "/out/mods/override.jar",
        )
        val snapshot = DiskSnapshot(
            expected = persistentMapOf(
                pathKey("mods/parity.jar") to "mods/parity.jar",
                pathKey("mods/override.jar") to "mods/override.jar",
            ),
            present = persistentSetOf(pathKey("mods/parity.jar")),
            untracked = persistentListOf("mods/extra.jar"),
        )
        val fetchArgs = LinkArgs(
            projects = listOf(project),
            manualOverrides = listOf(override),
            diskSnapshot = snapshot,
            mode = FilesPreviewMode.Fetch,
            fetchDownloads = listOf(FetchDownloadPreview(project, file)),
            fetchOverrides = listOf(override),
            fetchCleanup = listOf(
                FetchCleanupPreview("mods/extra.jar", "/abs/mods/extra.jar", "extra.jar"),
            ),
        )
        assertSelectionParity(fetchArgs)

        val syncProject = modProject("sync-new.jar", slug = "sync-new")
        val syncArgs = LinkArgs(
            projects = listOf(project),
            diskSnapshot = snapshot.copy(
                present = persistentSetOf(pathKey("mods/parity.jar")),
                untracked = persistentListOf("mods/sync-new.jar"),
            ),
            mode = FilesPreviewMode.Sync,
            syncAdded = listOf(syncProject),
            syncRemoved = listOf(project),
            syncUpdated = listOf(project),
        )
        assertSelectionParity(syncArgs)
    }

    private fun assertSelectionParity(args: LinkArgs) {
        val model = tabModel(args)
        val oldKeys = model.filesDiffPairs()
            .filter { it.changeKind?.direction() != null }
            .map { it.selectionKey }
            .toSet()
        val newKeys = model.filesLinks()
            .filter { it.change?.direction() != null }
            .mapNotNull { it.selectionKey }
            .toSet()
        assertEquals(oldKeys, newKeys, "mode=${args.mode}")
    }
}
