/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.ui.model.DiskSnapshot
import teksturepako.pakkuDesktop.app.ui.model.SyncKind
import teksturepako.pakkuDesktop.app.ui.model.pathKey
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncDisplayPathsTest {
    private fun modProject(fileName: String): Project {
        val projectFile = ProjectFile(
            type = "modrinth",
            id = "file-$fileName",
            fileName = fileName,
        )
        return Project(
            type = ProjectType.MOD,
            slug = mutableMapOf("modrinth" to "test-mod"),
            name = mutableMapOf("en" to "Test Mod"),
            id = mutableMapOf("modrinth" to "test-mod"),
            files = mutableSetOf(projectFile),
        )
    }

    @Test
    fun removedUsesLockExpectedPathWhenAbsentFromDisk() {
        val project = modProject("foo.jar")
        val snapshot = DiskSnapshot(
            expected = persistentMapOf(pathKey("mods/foo.jar") to "mods/foo.jar"),
            present = persistentSetOf(),
        )
        assertEquals(
            listOf("mods/foo.jar"),
            syncDisplayPaths(project, config = null, snapshot, SyncKind.Removed),
        )
    }

    @Test
    fun addedPrefersSnapshotPathByFileName() {
        val project = modProject("bar.jar")
        val snapshot = DiskSnapshot(
            expected = persistentMapOf(pathKey("mods/bar.jar") to "mods/bar.jar"),
            present = persistentSetOf(pathKey("mods/bar.jar")),
        )
        assertEquals(
            listOf("mods/bar.jar"),
            syncDisplayPaths(project, config = null, snapshot, SyncKind.Added),
        )
    }

    @Test
    fun addedFallsBackToLockLayoutWhenNotOnDisk() {
        val project = modProject("bar.jar")
        val snapshot = DiskSnapshot()
        assertEquals(
            listOf("mods/bar.jar"),
            syncDisplayPaths(project, config = null, snapshot, SyncKind.Added),
        )
    }

    @Test
    fun updatedUsesDiskPathWhenFileNameDiffersFromLock() {
        val project = modProject("b.jar")
        val snapshot = DiskSnapshot(
            expected = persistentMapOf(
                pathKey("mods/a.jar") to "mods/a.jar",
                pathKey("mods/b.jar") to "mods/b.jar",
            ),
            present = persistentSetOf(pathKey("mods/a.jar"), pathKey("mods/b.jar")),
            untracked = kotlinx.collections.immutable.persistentListOf("mods/b.jar"),
        )
        assertEquals(
            listOf("mods/b.jar"),
            syncDisplayPaths(project, config = null, snapshot, SyncKind.Updated),
        )
    }
}
