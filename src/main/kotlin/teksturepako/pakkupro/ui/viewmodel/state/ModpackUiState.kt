package teksturepako.pakkupro.ui.viewmodel.state

import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project

data class ModpackUiState(
    val lockFile: LockFile? = null,
    val configFile: ConfigFile? = null,

    val selectedTab: SelectedTab = SelectedTab.MODPACK,

    val selectedProject: Project? = null,
    val editingProject: Project? = null,

    val projectsFilter: (Project) -> Boolean = { true },
)
