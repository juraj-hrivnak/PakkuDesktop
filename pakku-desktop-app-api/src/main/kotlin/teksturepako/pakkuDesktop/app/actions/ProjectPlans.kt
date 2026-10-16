/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.projects.Project

/** Plain-text label for toasts / logs (not GUI). Prefer [ProjectRef] in UI. */
fun Project.displayLabel(): String {
    val id = slug.values.firstOrNull()
        ?: name.values.firstOrNull()
        ?: id.values.firstOrNull()
        ?: "?"
    return "${type.name.lowercase()} $id"
}

/**
 * Stable UI / plan selection key from provider identity (ids, slugs, names).
 * Do not use [Project.pakkuId] — it is ephemeral and will go away in a future Pakku version.
 */
fun Project.uiKey(): String = identityKey()

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

/**
 * Unique key for one row in a lazy list. [uiKey] can collide when the lock file holds
 * multiple entries with the same provider identity; [pakkuId] disambiguates those rows.
 */
fun Project.listItemKey(fallbackIndex: Int): String =
    pakkuId ?: "${uiKey()}#$fallbackIndex"

/**
 * Dependency preview node (from Pakku [requestDependencies] + optional [createAdditionRequest]).
 *
 * [cycle] is a back-edge to a project already on the path from the root. It is a leaf:
 * expanding it again would recurse forever and shove the row off the right of the dialog.
 */
data class DepNode(
    val project: Project,
    val children: List<DepNode> = emptyList(),
    val alreadyPresent: Boolean = false,
    val warnings: List<ActionError> = emptyList(),
    val cycle: Boolean = false,
)

/** One root project offered by [createAdditionRequest] (deps previewed; auto-added on apply). */
data class AdditionEntry(
    val project: Project,
    val isRecommended: Boolean,
    val replacing: Project?,
    val warnings: List<ActionError> = emptyList(),
    val deps: List<DepNode> = emptyList(),
) {
    val key: String get() = project.uiKey()
}

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
    /** Orphaned deps freed by removing this project (deduped across the plan). */
    val orphanedChildren: List<RemovalEntry> = emptyList(),
) {
    val key: String get() = project.uiKey()
}

data class RemovalPlan(
    val projects: List<RemovalEntry> = emptyList(),
    val orphanedDeps: List<RemovalEntry> = emptyList(),
    val messages: List<ActionError> = emptyList(),
) {
    val isEmpty: Boolean get() = projects.isEmpty() && orphanedDeps.isEmpty()

    /** Orphans under accepted roots; when [acceptedDepIds] is set, only those keys. */
    fun orphansFor(
        acceptedRootIds: Set<String>,
        acceptedDepIds: Set<String>? = null,
    ): List<RemovalEntry> {
        val fromTrees = projects
            .filter { it.key in acceptedRootIds }
            .flatMap { it.orphanedChildren }
            .distinctBy { it.key }
        return if (acceptedDepIds == null) fromTrees
        else fromTrees.filter { it.key in acceptedDepIds }
    }
}
