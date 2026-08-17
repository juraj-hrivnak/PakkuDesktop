/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import com.github.michaelbull.result.fold
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.AlreadyAdded
import teksturepako.pakku.api.actions.errors.NotFoundOn
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project

/**
 * Dry-run dependency tree mirroring CLI [teksturepako.pakku.cli.resolveDependencies]:
 * [Project.requestDependencies] → skip if already in pack / already seen →
 * [createAdditionRequest] → recurse.
 *
 * Does not mutate the lock file. Dedupes with [Project.isAlmostTheSameAs] (same as
 * [LockFile.isProjectAdded]) — not pakkuId, which is unique per constructed [Project].
 */
suspend fun Project.previewDependenciesDesktop(
    lockFile: LockFile,
    projectProvider: Provider,
    platforms: List<Platform>,
    /** Roots + deps already shown / would-be-added in this resolve. */
    accounted: MutableList<Project> = mutableListOf(),
): List<DepNode> {
    val dependencies = requestDependencies(projectProvider, lockFile)
    if (dependencies.isEmpty()) return emptyList()

    val nodes = mutableListOf<DepNode>()

    for (result in dependencies) {
        result.fold(
            success = { dep ->
                // Already in the pack — show once as linked, no recurse (CLI only links).
                if (lockFile.isProjectAdded(dep)) {
                    if (accounted.any { it isAlmostTheSameAs dep }) return@fold
                    val existing = lockFile.getProject(dep) ?: dep
                    accounted += existing
                    nodes += DepNode(project = existing, alreadyPresent = true)
                    return@fold
                }

                // Same mod already queued from another platform file / parent (CLI would hit AlreadyAdded).
                if (accounted.any { it isAlmostTheSameAs dep }) return@fold

                val warnings = mutableListOf<ActionError>()
                var validated: Project? = null
                dep.createAdditionRequest(
                    onError = { error ->
                        when (error) {
                            is AlreadyAdded, is NotFoundOn -> Unit
                            else -> warnings += error
                        }
                    },
                    onSuccess = { project, _, _, _ ->
                        validated = project
                    },
                    lockFile = lockFile,
                    platforms = platforms,
                    strict = false,
                )

                val project = validated ?: dep
                if (accounted.any { it isAlmostTheSameAs project }) return@fold
                accounted += project

                val children = project.previewDependenciesDesktop(
                    lockFile, projectProvider, platforms, accounted,
                )
                nodes += DepNode(
                    project = project,
                    children = children,
                    alreadyPresent = false,
                    warnings = warnings.distinctBy { it.fingerprint() },
                )
            },
            failure = { /* apply path surfaces resolve failures */ },
        )
    }
    return nodes
}

/** Soft identity for UI keys / fingerprints — provider ids, slugs, names (never pakkuId). */
fun Project.identityKey(): String {
    val ids = id.values.map { it.lowercase() }.toSortedSet()
    if (ids.isNotEmpty()) return "pid:${ids.joinToString(",")}"
    val slugs = slug.values.map { it.lowercase() }.toSortedSet()
    if (slugs.isNotEmpty()) return "slug:${slugs.joinToString(",")}"
    val names = name.values.map { it.lowercase().filterNot(Char::isWhitespace) }.toSortedSet()
    if (names.isNotEmpty()) return "name:${names.joinToString(",")}"
    return "type:${type.name}"
}

fun ActionError.fingerprint(): String = when (this) {
    is AlreadyAdded -> "AlreadyAdded:${project.identityKey()}"
    is NotFoundOn -> "NotFoundOn:${project.identityKey()}:${provider.serialName}"
    is teksturepako.pakku.api.actions.errors.NoFilesOn ->
        "NoFilesOn:${project.identityKey()}:${provider.serialName}"
    is teksturepako.pakku.api.actions.errors.VersionsDoNotMatch ->
        "VersionsDoNotMatch:${project.identityKey()}"
    is teksturepako.pakku.api.actions.errors.ProjRequiredBy ->
        "ProjRequiredBy:${project.identityKey()}"
    else -> "${this::class.simpleName}:${rawMessage}"
}
