package teksturepako.pakkupro.ui.viewmodel.state

import com.github.michaelbull.result.Result
import kotlinx.coroutines.Job
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project

data class ModpackUiState(
    val lockFile: Result<LockFile, ActionError>? = null,
    val configFile: Result<ConfigFile, ActionError>? = null,

    val selectedTab: SelectedTab = SelectedTab.PROJECTS,

    val selectedProject: Project? = null,
    val editingProject: Boolean = false,

    val selectedProjectsMap: Map<String, (Project) -> Boolean> = emptyMap(),

    val projectsFilter: (Project) -> Boolean = { true },

    val action: Pair<String?, Job?> = null to null,
)
