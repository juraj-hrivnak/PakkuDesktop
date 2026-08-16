/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.projects.Project

/** Plain-text label for toasts / logs (not GUI). Prefer [ProjectRef] in UI. */
fun Project.displayLabel(): String {
    val slug = slug.values.firstOrNull()
    val name = name.values.firstOrNull()
    val id = slug ?: name ?: pakkuId ?: "?"
    return "${type.name.lowercase()} $id"
}

/** One root project offered by [createAdditionRequest] (deps are auto-added on apply). */
data class AdditionEntry(
    val project: Project,
    val isRecommended: Boolean,
    val replacing: Project?,
    val warnings: List<ActionError> = emptyList(),
)

data class AdditionPlan(
    val entries: List<AdditionEntry> = emptyList(),
    val messages: List<ActionError> = emptyList(),
) {
    val isEmpty: Boolean get() = entries.isEmpty()
}

/** One project or orphaned dep from [createRemovalRequest]. */
data class RemovalEntry(
    val project: Project,
    val isRecommended: Boolean,
    val warning: ActionError? = null,
)

data class RemovalPlan(
    val projects: List<RemovalEntry> = emptyList(),
    val orphanedDeps: List<RemovalEntry> = emptyList(),
    val messages: List<ActionError> = emptyList(),
) {
    val isEmpty: Boolean get() = projects.isEmpty() && orphanedDeps.isEmpty()
}
