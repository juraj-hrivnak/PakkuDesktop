/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import com.github.michaelbull.result.get
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.model.ProjectsTabModel
import teksturepako.pakkuDesktop.app.ui.model.SortOrder

/** Search text and filter toggles shared by the Projects list and the override picker. */
data class ProjectListQuery(
    val text: String = "",
    val types: Set<ProjectType> = emptySet(),
    val sides: Set<ProjectSide> = emptySet(),
    val missingSide: Boolean = false,
    val providers: Set<String> = emptySet(),
    val redistributable: Boolean? = null,
    val updatesOnly: Boolean = false,
)

fun ProjectsTabModel.projectListQuery(ignoreUpdatesOnly: Boolean = false) = ProjectListQuery(
    text = projectsFilterText,
    types = filterTypes,
    sides = filterSides,
    missingSide = filterMissingSide,
    providers = filterProviders,
    redistributable = filterRedistributable,
    updatesOnly = !ignoreUpdatesOnly && filterUpdatesOnly,
)

fun Project.matchesProjectListQuery(query: ProjectListQuery, hasUpdate: Boolean = false): Boolean {
    val needle = query.text
    val textOk = needle.isEmpty() ||
        name.values.any { needle.lowercase() in it.lowercase() } ||
        needle in this

    val updatesOk = !query.updatesOnly || hasUpdate

    val typeOk = query.types.isEmpty() || type in query.types

    val sideFilterActive = query.sides.isNotEmpty() || query.missingSide
    val sideOk = !sideFilterActive ||
        (side != null && side in query.sides) ||
        (side == null && query.missingSide)

    val providerOk = query.providers.isEmpty() ||
        getProviders().any { it.serialName in query.providers }

    val redistributableOk = query.redistributable == null ||
        redistributable == query.redistributable

    return textOk && updatesOk && typeOk && sideOk && providerOk && redistributableOk
}

fun Project.matchesProjectsListFilters(
    model: ProjectsTabModel,
    ignoreUpdatesOnly: Boolean = false,
): Boolean = matchesProjectListQuery(
    model.projectListQuery(ignoreUpdatesOnly),
    hasUpdate = model.updatePreviews?.containsKey(uiKey()) == true,
)

/** Identity of every input [filteredAndSortedProjects] reads. Used to reuse a list built off the UI thread. */
fun ProjectsTabModel.projectsListPrepKey(): Any = listOf(
    lockFile,
    sortOrder,
    projectsFilterText,
    filterTypes,
    filterSides,
    filterMissingSide,
    filterProviders,
    filterRedistributable,
    filterUpdatesOnly,
    updatePreviews,
)

fun ProjectsTabModel.filteredAndSortedProjects(
    projects: List<Project>,
    ignoreUpdatesOnly: Boolean = false,
): List<Project> {
    val filtered = projects.filter { it.matchesProjectsListFilters(this, ignoreUpdatesOnly) }
    return when (val order = sortOrder) {
        is SortOrder.Name -> if (order.ascending) {
            filtered.sortedBy { it.name.values.firstOrNull() }
        } else {
            filtered.sortedByDescending { it.name.values.firstOrNull() }
        }
        is SortOrder.LastUpdated -> if (order.ascending) {
            filtered.sortedBy { it.getLatestFile(Provider.providers)?.datePublished }
        } else {
            filtered.sortedByDescending { it.getLatestFile(Provider.providers)?.datePublished }
        }
    }
}

/** One list row: a disk file, with platform files merged when they are the same jar. */
data class FilesTabRow(
    val project: Project,
    val files: List<ProjectFile>,
) {
    val primary: ProjectFile get() = files.maxBy { it.datePublished }

    fun rowKey(): String =
        "${project.uiKey()}::${primary.fileName}::${files.map { it.type }.sorted().joinToString()}"
}

fun Project.filesTabRows(): List<FilesTabRow> {
    val latestPerProvider = files.snapshotList()
        .groupBy { it.type }
        .mapNotNull { (_, group) -> group.maxByOrNull { it.datePublished } }
    if (latestPerProvider.isEmpty()) return emptyList()

    val clusters = mutableListOf<MutableList<ProjectFile>>()
    for (file in latestPerProvider) {
        val cluster = clusters.firstOrNull { member -> member.any { it.isSameDiskFileAs(file) } }
        if (cluster != null) cluster += file else clusters += mutableListOf(file)
    }
    return clusters.map { FilesTabRow(this, it) }
}

private fun ProjectFile.isSameDiskFileAs(other: ProjectFile): Boolean {
    if (fileName == other.fileName) return true
    val thisSha = hashes?.entries?.firstOrNull { it.key.equals("sha1", ignoreCase = true) }?.value
    val otherSha = other.hashes?.entries?.firstOrNull { it.key.equals("sha1", ignoreCase = true) }?.value
    return !thisSha.isNullOrBlank() && thisSha == otherSha
}
