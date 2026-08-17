/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.data

import com.github.michaelbull.result.get
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.decodeToResult
import teksturepako.pakku.io.writeToFile
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.SortOrder
import kotlin.io.path.Path

/** Persisted Projects-tab list prefs (sort, filters, split ratio). */
@Serializable
data class ProjectsUiData(
    @SerialName("sort_by") val sortBy: String = SORT_NAME,
    @SerialName("sort_ascending") val sortAscending: Boolean = true,
    @SerialName("filter_text") val filterText: String = "",
    /** [ProjectType.name] values; empty = no type filter. */
    @SerialName("filter_types") val filterTypes: Set<String> = emptySet(),
    /** [ProjectSide.name] values; empty = no side filter. */
    @SerialName("filter_sides") val filterSides: Set<String> = emptySet(),
    /** Include projects with no side set. */
    @SerialName("filter_missing_side") val filterMissingSide: Boolean = false,
    /** Provider [teksturepako.pakku.api.platforms.Provider.serialName] values; empty = no provider filter. */
    @SerialName("filter_providers") val filterProviders: Set<String> = emptySet(),
    /**
     * null = no redistributable filter;
     * true = only redistributable; false = only not redistributable.
     */
    @SerialName("filter_redistributable") val filterRedistributable: Boolean? = null,
    /** First-pane weight for the Projects list|inspector split (list share, 0–1). */
    @SerialName("split_ratio") val splitRatio: Float = DEFAULT_SPLIT_RATIO,
    /**
     * 1 = legacy detail-first split (ratio was detail weight).
     * 2 = list-first split (ratio is list weight).
     */
    @SerialName("split_version") val splitVersion: Int = SPLIT_VERSION_LIST_FIRST,
) {
    fun toSortOrder(): SortOrder = when (sortBy) {
        SORT_LAST_UPDATED -> SortOrder.LastUpdated(ascending = sortAscending)
        else -> SortOrder.Name(ascending = sortAscending)
    }

    fun toProjectTypes(): Set<ProjectType> =
        filterTypes.mapNotNull { runCatching { ProjectType.valueOf(it) }.getOrNull() }.toSet()

    fun toProjectSides(): Set<ProjectSide> =
        filterSides.mapNotNull { runCatching { ProjectSide.valueOf(it) }.getOrNull() }.toSet()

    /** List pane weight for the current layout. */
    fun listSplitRatio(): Float {
        val coerced = splitRatio.coerceIn(0.05f, 0.95f)
        return if (splitVersion < SPLIT_VERSION_LIST_FIRST) {
            (1f - coerced).coerceIn(0.05f, 0.95f)
        } else {
            coerced
        }
    }

    companion object {
        const val FILE_NAME = "projects-ui-data.json"
        const val SORT_NAME = "name"
        const val SORT_LAST_UPDATED = "last_updated"
        /** Default list share when the inspector is open. */
        const val DEFAULT_SPLIT_RATIO = 0.68f
        const val SPLIT_VERSION_LIST_FIRST = 2

        suspend fun readOrNew(): ProjectsUiData =
            decodeToResult<ProjectsUiData>(Path(FILE_NAME), format = jsonEncodeDefaults).get()
                ?: ProjectsUiData()

        fun readOrNewBlocking(): ProjectsUiData = runBlocking { readOrNew() }
    }

    suspend fun write(): ActionError? =
        writeToFile<ProjectsUiData>(this, FILE_NAME, format = jsonEncodeDefaults)
}

fun SortOrder.toProjectsUiSortBy(): String = when (this) {
    is SortOrder.LastUpdated -> ProjectsUiData.SORT_LAST_UPDATED
    is SortOrder.Name -> ProjectsUiData.SORT_NAME
}

fun ModpackModel.toProjectsUiData(): ProjectsUiData = ProjectsUiData(
    sortBy = sortOrder.toProjectsUiSortBy(),
    sortAscending = sortOrder.ascending,
    filterText = projectsFilterText,
    filterTypes = filterTypes.map { it.name }.toSet(),
    filterSides = filterSides.map { it.name }.toSet(),
    filterMissingSide = filterMissingSide,
    filterProviders = filterProviders,
    filterRedistributable = filterRedistributable,
    splitRatio = projectsSplitRatio.coerceIn(0.05f, 0.95f),
    splitVersion = ProjectsUiData.SPLIT_VERSION_LIST_FIRST,
)

fun ModpackModel.withProjectsUi(data: ProjectsUiData): ModpackModel = copy(
    sortOrder = data.toSortOrder(),
    projectsFilterText = data.filterText,
    filterTypes = data.toProjectTypes(),
    filterSides = data.toProjectSides(),
    filterMissingSide = data.filterMissingSide,
    filterProviders = data.filterProviders,
    filterRedistributable = data.filterRedistributable,
    projectsSplitRatio = data.listSplitRatio(),
)
