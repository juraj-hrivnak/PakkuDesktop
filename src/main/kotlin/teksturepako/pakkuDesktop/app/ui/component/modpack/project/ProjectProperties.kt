/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg

@Composable
fun ProjectProperties(publish: (AppMsg) -> Unit, model: AppModel) {
    val modpack = model.modpack

    Column(verticalArrangement = Arrangement.SpaceBetween) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1F)) {
                Text("Properties")
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Edit: ")
                    Switch(
                        checked = modpack.editingProject,
                        onCheckedChange = {
                            if (!modpack.editingProject) {
                                publish(AppMsg.Modpack.ProjectEditing(true))
                            } else {
                                publish(AppMsg.Modpack.ProjectEditing(false))
                                // Trigger reload — modpack disk driver will re-read files
                                // In fractal model: the driver periodically reloads on screen change
                                // For explicit reload: we'd need a dedicated message
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
