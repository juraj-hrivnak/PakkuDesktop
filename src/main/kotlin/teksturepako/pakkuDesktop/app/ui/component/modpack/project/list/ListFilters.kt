/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import com.github.michaelbull.result.get
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.model.ProjectsTabModel
import teksturepako.pakkuDesktop.app.ui.model.SortOrder

fun Project.matchesProjectsListFilters(
    model: ProjectsTabModel,
    ignoreUpdatesOnly: Boolean = false,
): Boolean {
    val textOk = model.projectsFilterText.isEmpty() ||
        name.values.any { model.projectsFilterText.lowercase() in it.lowercase() } ||
        model.projectsFilterText in this

    val updatesOk = ignoreUpdatesOnly ||
        !model.filterUpdatesOnly ||
        model.updatePreviews?.containsKey(uiKey()) == true

    val typeOk = model.filterTypes.isEmpty() || type in model.filterTypes

    val sideFilterActive = model.filterSides.isNotEmpty() || model.filterMissingSide
    val sideOk = !sideFilterActive ||
        (side != null && side in model.filterSides) ||
        (side == null && model.filterMissingSide)

    val providerOk = model.filterProviders.isEmpty() ||
        getProviders().any { it.serialName in model.filterProviders }

    val redistributableOk = model.filterRedistributable == null ||
        redistributable == model.filterRedistributable

    return textOk && updatesOk && typeOk && sideOk && providerOk && redistributableOk
}

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
