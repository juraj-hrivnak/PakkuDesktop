package teksturepako.pakkupro.ui.viewmodel.state

import teksturepako.pakku.api.projects.Project

data class ModpackUiState(
    val selectedTab: SelectedTab = SelectedTab.PROJECTS,
    val selectedProject: Project? = null,
)
