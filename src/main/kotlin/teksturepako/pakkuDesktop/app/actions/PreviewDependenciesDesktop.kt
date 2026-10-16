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
 *
 * A dependency that points back at an ancestor is kept as a [DepNode.cycle] leaf so the
 * dialog can show the loop without walking it again.
 */
suspend fun Project.previewDependenciesDesktop(
    lockFile: LockFile,
    projectProvider: Provider,
    platforms: List<Platform>,
    /** Roots + deps already shown / would-be-added in this resolve. */
    accounted: MutableList<Project> = mutableListOf(),
    /** Projects on the path from the review root to this node, nearest ancestor last. */
    ancestors: List<Project> = emptyList(),
): List<DepNode> {
    val dependencies = requestDependencies(projectProvider, lockFile)
    if (dependencies.isEmpty()) return emptyList()

    val path = ancestors + this
    val nodes = mutableListOf<DepNode>()

    for (result in dependencies) {
        result.fold(
            success = { dep ->
                // Back-edge: show the project once more as a leaf, then stop.
                path.firstOrNull { it isAlmostTheSameAs dep }?.let { prior ->
                    nodes += DepNode(project = prior, cycle = true)
                    return@fold
                }
                if (path.size >= MaxDependencyDepth) {
                    nodes += DepNode(project = dep, cycle = true)
                    return@fold
                }

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
                path.firstOrNull { it isAlmostTheSameAs project }?.let { prior ->
                    nodes += DepNode(project = prior, cycle = true)
                    return@fold
                }
                if (accounted.any { it isAlmostTheSameAs project }) return@fold
                accounted += project

                val children = project.previewDependenciesDesktop(
                    lockFile, projectProvider, platforms, accounted, path,
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

/** Stop walking if identity comparison ever fails to catch a loop. */
private const val MaxDependencyDepth = 32

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
