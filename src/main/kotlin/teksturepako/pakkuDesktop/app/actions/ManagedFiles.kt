/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.ui.model.DiskSnapshot
import teksturepako.pakkuDesktop.app.ui.model.pathKey
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo

/** Same names Pakku's detectProjects skips when they appear in a project-type directory path. */
private val managedFolderIgnoredNames = listOf("saves", "screenshots")

/** Gradle output. Not a modpack folder; jars under it are not projects. */
private const val BuildDirName = "build"

/**
 * Mirrors Pakku's detectProjects folder rules (DetectProjects.kt, pakku 1.5.0.343):
 * project types except worlds, skip missing dirs and any dir whose path contains
 * `saves` / `screenshots`, skip directories and non-matching extensions, and skip
 * [excludeAbsolute] (manual override outputs).
 *
 * [extensions] defaults to the cleanup list. Sync detect passes Pakku's `.jar` / `.zip`.
 */
internal fun walkManagedFiles(
    root: Path,
    configFile: ConfigFile?,
    excludeAbsolute: Set<String>,
    extensions: List<String> = listOf(".jar", ".zip", ".jar.meta"),
): List<Path> {
    val excluded = excludeAbsolute.map { absolutePathKey(Path(it)) }.toSet()
    return managedTypeDirs(root, configFile).flatMap { dir ->
        val listed = runCatching {
            dir.toFile().walkTopDown().onEnter { file ->
                file.toPath() == dir || file.name != BuildDirName
            }.mapNotNull { file ->
                val candidate = file.toPath()
                if (candidate == dir || candidate.isDirectory()) return@mapNotNull null
                val text = candidate.invariantSeparatorsPathString
                if (extensions.none { text.endsWith(it) }) return@mapNotNull null
                if (absolutePathKey(candidate) in excluded) return@mapNotNull null
                candidate
            }.toList()
        }.getOrNull()
        listed.orEmpty()
    }
}

internal fun buildDiskSnapshot(
    root: Path,
    expected: Map<String, String>,
    walked: List<Path>,
    folders: List<String>,
): DiskSnapshot {
    val base = root.toAbsolutePath().normalize()
    val present = expected.mapNotNull { (key, display) ->
        val path = base.resolve(display.replace('\\', '/'))
        if (path.exists()) key else null
    }.toPersistentSet()
    val untracked = walked.mapNotNull { path ->
        val relative = path.relativeDisplay(base) ?: return@mapNotNull null
        if (pathKey(relative) in expected) null else relative
    }.distinct().sorted().toPersistentList()
    val existingFolders = folders
        .map { asSnapshotFolderKey(it) }
        .distinct()
        .filter { key ->
            val relative = key.trimEnd('/')
            val dir = if (relative.isEmpty()) base else base.resolve(relative)
            dir.exists() && dir.isDirectory()
        }
        .toPersistentSet()
    return DiskSnapshot(
        expected = expected.toPersistentMap(),
        present = present,
        untracked = untracked,
        existingFolders = existingFolders,
    )
}

private fun managedTypeDirs(root: Path, configFile: ConfigFile?): List<Path> =
    ProjectType.entries
        .filterNot { it == ProjectType.WORLD }
        .mapNotNull { type ->
            val dir = root.resolve(type.getPathString(configFile)).normalize()
            if (!dir.isDirectory()) return@mapNotNull null
            val relative = dir.relativeDisplay(root) ?: return@mapNotNull null
            if (relative.isEmpty() || relative == ".") return@mapNotNull null
            if (managedFolderIgnoredNames.any { it in relative.split('/') }) return@mapNotNull null
            if (BuildDirName in relative.split('/')) return@mapNotNull null
            dir
        }

private fun absolutePathKey(path: Path): String =
    path.toAbsolutePath().normalize().invariantSeparatorsPathString

private fun Path.relativeDisplay(root: Path): String? =
    try {
        toAbsolutePath().normalize()
            .relativeTo(root.toAbsolutePath().normalize())
            .invariantSeparatorsPathString
    } catch (_: IllegalArgumentException) {
        null
    }

private fun asSnapshotFolderKey(folder: String): String {
    val normalized = folder.replace('\\', '/').removePrefix("./")
    if (normalized.isEmpty() || normalized == "/") return ""
    return if (normalized.endsWith("/")) normalized else "$normalized/"
}
