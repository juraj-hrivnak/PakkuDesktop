package teksturepako.pakkupro.ui.viewmodel.state

import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project

data class ModpackUiState(
    val lockFile: LockFile? = null,
    val configFile: ConfigFile? = null,

    val selectedTab: SelectedTab = SelectedTab.PROJECTS,

    val selectedProject: Project? = null,
    val editingProject: Boolean = false,

    val projectsFilter: (Project) -> Boolean = { true },
)
