/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.SortOrder

fun Project.matchesProjectsListFilters(model: ModpackModel): Boolean {
    val textOk = model.projectsFilterText.isEmpty() ||
        name.values.any { model.projectsFilterText.lowercase() in it.lowercase() } ||
        model.projectsFilterText in this

    val updatesOk = !model.filterUpdatesOnly ||
        model.updatePreviews?.containsKey(uiKey()) == true

    val typeOk = model.filterTypes.isEmpty() || type in model.filterTypes

    val sideOk = model.filterSides.isEmpty() || side in model.filterSides

    val providerOk = model.filterProviders.isEmpty() ||
        getProviders().any { it.serialName in model.filterProviders }

    return textOk && updatesOk && typeOk && sideOk && providerOk
}

fun ModpackModel.filteredAndSortedProjects(projects: List<Project>): List<Project> {
    val filtered = projects.filter { it.matchesProjectsListFilters(this) }
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
