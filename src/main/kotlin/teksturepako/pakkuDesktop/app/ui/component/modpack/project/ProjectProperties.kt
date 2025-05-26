package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakkuDesktop.app.ui.component.Switch
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.projectPropSelection.NullableProjectEnumSelection
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.projectPropSelection.NullableProjectStringSelection
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.projectPropSelection.ProjectBooleanSelection
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.projectPropSelection.ProjectEnumSelection
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel

@Composable
fun ProjectProperties()
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1F)
            ) {
                Text("Properties")
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit: ")
                    Switch(
                        checked = modpackUiState.editingProject,
                        onCheckedChange = {
                            if (!modpackUiState.editingProject)
                            {
                                ModpackViewModel.editProject(true)
                            }
                            else
                            {
                                ModpackViewModel.editProject(false)
                                coroutineScope.launch {
                                    ModpackViewModel.loadFromDisk()
                                }
                            }
                        }
                    )
                }
            }
        }

        ProjectEnumSelection(
            label = "Type:",
            enumEntries = ProjectType.entries,
            projectRef = Project::type,
            projectConfigRef = ConfigFile.ProjectConfig::type
        )

        NullableProjectEnumSelection(
            label = "Side:",
            enumEntries = ProjectSide.entries,
            projectRef = Project::side,
            projectConfigRef = ConfigFile.ProjectConfig::side
        )

        ProjectEnumSelection(
            label = "Update Strategy:",
            enumEntries = UpdateStrategy.entries,
            projectRef = Project::updateStrategy,
            projectConfigRef = ConfigFile.ProjectConfig::updateStrategy
        )

        ProjectBooleanSelection(
            label = "Redistributable:",
            projectRef = Project::redistributable,
            projectConfigRef = ConfigFile.ProjectConfig::redistributable
        )

        NullableProjectStringSelection(
            label = "Subpath:",
            projectRef = Project::getSubpath,
            projectConfigRef = ConfigFile.ProjectConfig::subpath
        )
    }
}
