package pakkupro.view.modpack.tabs.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.jewel.foundation.theme.JewelTheme
import pakkupro.view.modpack.tabs.ActionBarView
import pakkupro.view.modpack.tabs.projects.components.ProjectFilter
import pakkupro.view.modpack.tabs.projects.components.ProjectsList
import pakkupro.view.modpack.tabs.projects.components.ProjectsTabSidebar
import pakkupro.viewmodel.MainViewModel
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide

@Composable
fun ProjectsTabView(
    coroutineScope: CoroutineScope,
    onSideSelect: (ProjectSide?) -> Result<ProjectSide?, ActionError?>
)
{
    val searchFilter: MutableState<(Project) -> Boolean> = remember { mutableStateOf({ true }) }
    val selectedProject: MutableState<Project?> = remember { mutableStateOf(null) }

    val projects: MutableState<List<Project>> = mutableStateOf(MainViewModel.lockFile.getAllProjects())

    Column {
        Row {
            ActionBarView("Projects") {
                ProjectFilter(searchFilter)
            }
        }
        Row {
            ProjectsTabSidebar(selectedProject, onSideSelect = onSideSelect)

            Spacer(Modifier.background(JewelTheme.globalColors.borders.disabled).width(1.dp).fillMaxHeight())

            ProjectsList(projects, searchFilter, selectedProject, coroutineScope)
        }
    }
}
