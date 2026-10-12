/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.filesFolderKey
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.filesTabRows
import teksturepako.pakkuDesktop.app.ui.model.DiskSnapshot
import teksturepako.pakkuDesktop.app.ui.model.SyncKind
import teksturepako.pakkuDesktop.app.ui.model.pathKey

/** uiKey → best-effort display path(s) for linking sync preview rows. */
internal fun syncDisplayPaths(
    project: Project,
    config: ConfigFile?,
    snapshot: DiskSnapshot,
    kind: SyncKind,
): List<String> {
    val expected = project.lockExpectedDisplayPaths(config)
    return when (kind) {
        SyncKind.Removed -> expected
        SyncKind.Added -> project.syncAddedOrUpdatedDisplayPaths(config, snapshot, expected, diskOnlyWhenRenamed = false)
        SyncKind.Updated -> project.syncAddedOrUpdatedDisplayPaths(config, snapshot, expected, diskOnlyWhenRenamed = true)
    }
}

private fun Project.lockExpectedDisplayPaths(config: ConfigFile?): List<String> {
    val rows = filesTabRows()
    if (rows.isEmpty()) {
        val path = getPathStringWithSubpath(config).replace('\\', '/')
        return if (path.isBlank()) emptyList() else listOf(path)
    }
    return rows.map { cluster ->
        (cluster.primary.getRelativePathString(this, config) ?: cluster.primary.fileName).replace('\\', '/')
    }
}

private fun Project.syncAddedOrUpdatedDisplayPaths(
    config: ConfigFile?,
    snapshot: DiskSnapshot,
    expected: List<String>,
    diskOnlyWhenRenamed: Boolean,
): List<String> {
    val fileNames = filesTabRows()
        .flatMap { it.files.map { file -> file.fileName } }
        .ifEmpty { files.map { it.fileName } }
        .distinct()
    val diskMatches = fileNames.flatMap { snapshot.pathsWithFileName(it) }.distinct()
    if (diskMatches.isEmpty()) return expected
    if (diskOnlyWhenRenamed) {
        val folder = filesFolderKey(config)
        val fileBaseNames = fileNames.map { it.replace('\\', '/').substringAfterLast('/').lowercase() }.toSet()
        val staleLockOnDisk = snapshot.expected.values.filter { path ->
            val norm = path.replace('\\', '/')
            norm.startsWith(folder) &&
                pathKey(path) in snapshot.present &&
                norm.substringAfterLast('/').lowercase() !in fileBaseNames
        }
        val lockStyleExpected = (expected + staleLockOnDisk).distinct()
        val expectedKeys = lockStyleExpected.map { pathKey(it) }.toSet()
        val diskKeys = diskMatches.map { pathKey(it) }.toSet()
        if (diskKeys != expectedKeys) return diskMatches
    }
    return diskMatches
}

private fun DiskSnapshot.pathsWithFileName(fileName: String): List<String> {
    val target = fileName.replace('\\', '/').substringAfterLast('/').lowercase()
    val basename = { path: String -> path.replace('\\', '/').substringAfterLast('/').lowercase() }
    val fromExpected = present.mapNotNull { key -> expected[key]?.takeIf { basename(it) == target } }
    val fromUntracked = untracked.filter { basename(it) == target }
    return (fromExpected + fromUntracked).distinct()
}
