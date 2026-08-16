/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import com.github.michaelbull.result.fold
import teksturepako.pakku.api.actions.RequestHandlers
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project

/**
 * Desktop port of CLI [teksturepako.pakku.cli.resolveDependencies] without Mordant [Terminal].
 * Dependencies are added automatically (same as CLI — no per-dep yes/no).
 */
suspend fun Project.resolveDependenciesDesktop(
    reqHandlers: RequestHandlers,
    lockFile: LockFile,
    projectProvider: Provider,
    platforms: List<Platform>,
    onInfo: suspend (String) -> Unit = {},
    onDependencyReq: suspend (
        project: Project, provider: Provider, lockfile: LockFile
    ) -> List<com.github.michaelbull.result.Result<Project, ActionError>> =
        { project, provider, lf -> project.requestDependencies(provider, lf) },
) {
    val dependencies = onDependencyReq(this, projectProvider, lockFile)
    if (dependencies.isEmpty()) return

    onInfo("Resolving dependencies…")

    for (result in dependencies) {
        result.fold(
            success = { dep ->
                if (lockFile.isProjectAdded(dep)) {
                    lockFile.getProject(dep)?.pakkuId?.let { pakkuId ->
                        lockFile.addPakkuLink(pakkuId, this)
                    }
                } else {
                    dep.createAdditionRequest(
                        onError = reqHandlers.onError,
                        onSuccess = { dependency, _, _, depReqHandlers ->
                            lockFile.add(dependency)
                            lockFile.addPakkuLink(dependency.pakkuId!!, this@resolveDependenciesDesktop)
                            dependency.resolveDependenciesDesktop(
                                depReqHandlers, lockFile, projectProvider, platforms, onInfo, onDependencyReq
                            )
                            onInfo("${dependency.displayLabel()} added")
                        },
                        lockFile = lockFile,
                        platforms = platforms,
                    )
                }
            },
            failure = { reqHandlers.onError(it) },
        )
    }
}
