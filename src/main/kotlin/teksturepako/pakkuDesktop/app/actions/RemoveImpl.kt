/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import teksturepako.pakku.api.actions.createRemovalRequest
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.ProjRequiredBy
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

private class RemovalSimpleError(override val rawMessage: String) : ActionError()

/**
 * Builds a removal plan via Pakku [createRemovalRequest] + [LockFile.getLinkedProjects].
 * Does not mutate the lock file. Identity uses [Project.isAlmostTheSameAs], not pakkuId.
 *
 * Dep “still required” uses [LockFile.getLinkedProjects], ignoring the removal batch
 * (parent + other selected roots). Older Pakku builds forgot to ignore the parent for deps.
 */
suspend fun buildRemovalPlan(
    lockFile: LockFile,
    projects: Collection<Project>,
): RemovalPlan {
    if (projects.isEmpty()) {
        return RemovalPlan(messages = listOf(RemovalSimpleError("No projects selected to remove.")))
    }

    val selected = projects.toList()
    val entries = mutableListOf<RemovalEntry>()
    val orphanedFlat = mutableListOf<RemovalEntry>()
    val messages = mutableListOf<ActionError>()
    val seenMessageKeys = mutableSetOf<String>()

    fun remainingDependants(of: Project, ignoring: Collection<Project>): List<Project> {
        val id = lockFile.getProject(of)?.pakkuId ?: of.pakkuId ?: return emptyList()
        return lockFile.getLinkedProjects(id).filter { dependant ->
            ignoring.none { it isAlmostTheSameAs dependant }
        }
    }

    for (project in projects) {
        var pendingRequiredBy: ProjRequiredBy? = null
        val children = mutableListOf<RemovalEntry>()
        val ignoringForThis = selected // whole batch leaves together

        project.createRemovalRequest(
            onError = { error ->
                when (error) {
                    is ProjRequiredBy -> pendingRequiredBy = error
                    else -> {
                        val key = error::class.simpleName + ":" + error.rawMessage
                        if (seenMessageKeys.add(key)) messages += error
                    }
                }
            },
            onRemoval = { proj, _ ->
                // Recompute with LockFile links; ignore other selected roots too.
                val remaining = remainingDependants(proj, ignoringForThis)
                val warning = if (remaining.isEmpty()) null else ProjRequiredBy(proj, remaining)
                entries += RemovalEntry(
                    project = proj,
                    isRecommended = remaining.isEmpty(),
                    warning = warning,
                )
                pendingRequiredBy = null
            },
            onDepRemoval = { dep, _ ->
                pendingRequiredBy = null
                if (selected.any { it isAlmostTheSameAs dep }) return@createRemovalRequest
                if (orphanedFlat.any { it.project isAlmostTheSameAs dep }) return@createRemovalRequest

                val remaining = remainingDependants(dep, ignoringForThis)
                val warning = if (remaining.isEmpty()) null else ProjRequiredBy(dep, remaining)
                val orphan = RemovalEntry(
                    project = dep,
                    isRecommended = remaining.isEmpty(),
                    warning = warning,
                )
                children += orphan
                orphanedFlat += orphan
            },
            lockFile = lockFile,
        )

        val lastIdx = entries.indexOfLast { it.project isAlmostTheSameAs project }
        if (lastIdx >= 0 && children.isNotEmpty()) {
            entries[lastIdx] = entries[lastIdx].copy(orphanedChildren = children)
        }
    }

    return RemovalPlan(
        projects = entries,
        orphanedDeps = orphanedFlat,
        messages = messages,
    )
}

/**
 * Removes every project in the plan (caller filters to accepted roots + their orphans).
 * Emits one summary toast of what was removed (modal-style [ProjectRef]s).
 */
suspend fun applyRemovalPlan(
    lockFile: LockFile,
    plan: RemovalPlan,
    onToast: suspend (ToastData) -> Unit,
) {
    if (plan.isEmpty) {
        onToast(actionInfoToast(plan.messages.firstOrNull()?.toUiMessage() ?: "No projects to remove."))
        return
    }

    val removed = mutableListOf<Project>()

    suspend fun removeOne(project: Project) {
        // Link cleanup still goes through current LockFile API (pakkuLinks) while it exists;
        // identity for finding the project is isAlmostTheSameAs via [LockFile.getProject] / [LockFile.remove].
        val linkId = lockFile.getProject(project)?.pakkuId
        if (lockFile.remove(project) == true) {
            linkId?.let { lockFile.removePakkuLinkFromAllProjects(it) }
            removed += project
        }
    }

    for (entry in plan.projects) removeOne(entry.project)
    for (entry in plan.orphanedDeps) removeOne(entry.project)

    if (removed.isEmpty()) {
        onToast(actionInfoToast("No projects were removed."))
        return
    }

    lockFile.write()
    onToast(actionRemovedToast(removed.distinctBy { it.uiKey() }))
}

/** Auto path: remove selected + recommended orphaned deps (CLI defaults). */
suspend fun removeSuspend(
    lockFile: LockFile,
    projects: Collection<Project>,
    onToast: suspend (ToastData) -> Unit,
) {
    val plan = buildRemovalPlan(lockFile, projects)
    val acceptedRoots = plan.projects.filter { it.isRecommended }
    val acceptedIds = acceptedRoots.map { it.key }.toSet()
    val recommendedDepIds = plan.projects
        .filter { it.key in acceptedIds }
        .flatMap { it.orphanedChildren }
        .filter { it.isRecommended }
        .map { it.key }
        .toSet()
    applyRemovalPlan(
        lockFile,
        RemovalPlan(
            projects = acceptedRoots,
            orphanedDeps = plan.orphansFor(acceptedIds, recommendedDepIds),
            messages = plan.messages,
        ),
        onToast,
    )
}
