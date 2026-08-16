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
 * Builds a removal plan via [createRemovalRequest] (CLI `pakku rm` prompts).
 * Does not mutate the lock file.
 */
suspend fun buildRemovalPlan(
    lockFile: LockFile,
    projects: Collection<Project>,
): RemovalPlan {
    if (projects.isEmpty()) {
        return RemovalPlan(messages = listOf(RemovalSimpleError("No projects selected to remove.")))
    }

    val entries = mutableListOf<RemovalEntry>()
    val orphaned = mutableListOf<RemovalEntry>()
    val messages = mutableListOf<ActionError>()
    val seenDepIds = mutableSetOf<String>()

    for (project in projects) {
        var lastRequiredBy: ActionError? = null
        project.createRemovalRequest(
            onError = { error ->
                messages += error
                lastRequiredBy = if (error is ProjRequiredBy) error else null
            },
            onRemoval = { proj, isRecommended ->
                entries += RemovalEntry(
                    project = proj,
                    isRecommended = isRecommended,
                    warning = lastRequiredBy,
                )
                lastRequiredBy = null
            },
            onDepRemoval = { dep, isRecommended ->
                val id = dep.pakkuId ?: return@createRemovalRequest
                if (!seenDepIds.add(id)) return@createRemovalRequest
                if (projects.any { it.pakkuId == id }) return@createRemovalRequest
                orphaned += RemovalEntry(
                    project = dep,
                    isRecommended = isRecommended,
                    warning = lastRequiredBy,
                )
                lastRequiredBy = null
            },
            lockFile = lockFile,
        )
    }

    return RemovalPlan(projects = entries, orphanedDeps = orphaned, messages = messages)
}

/**
 * Removes every project in the plan (caller filters to accepted checklist items first).
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

    val removedLabels = mutableListOf<String>()

    suspend fun removeOne(project: Project) {
        val id = project.pakkuId ?: return
        if (lockFile.remove(project) == true) {
            lockFile.removePakkuLinkFromAllProjects(id)
            removedLabels += project.displayLabel()
        }
    }

    for (entry in plan.projects) removeOne(entry.project)
    for (entry in plan.orphanedDeps) removeOne(entry.project)

    when (removedLabels.size) {
        0 -> onToast(actionInfoToast("No projects were removed."))
        1 -> {
            lockFile.write()
            onToast(actionInfoToast("${removedLabels.single()} removed"))
        }
        in 2..3 -> {
            lockFile.write()
            onToast(actionInfoToast("Removed ${removedLabels.joinToString(", ")}"))
        }
        else -> {
            lockFile.write()
            onToast(actionInfoToast("Removed ${removedLabels.size} projects"))
        }
    }
}

/** Auto path: remove selected + recommended orphaned deps (CLI defaults). */
suspend fun removeSuspend(
    lockFile: LockFile,
    projects: Collection<Project>,
    onToast: suspend (ToastData) -> Unit,
) {
    val plan = buildRemovalPlan(lockFile, projects)
    applyRemovalPlan(
        lockFile,
        RemovalPlan(
            projects = plan.projects.filter { it.isRecommended },
            orphanedDeps = plan.orphanedDeps.filter { it.isRecommended },
            messages = plan.messages,
        ),
        onToast,
    )
}
