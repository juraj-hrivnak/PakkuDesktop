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
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@Composable
fun ProjectProperties(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
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
                        checked = model.editingProject,
                        onCheckedChange = {
                            if (!model.editingProject) {
                                publish(ModpackMsg.ProjectEditing(true))
                            } else {
                                publish(ModpackMsg.ProjectEditing(false))
                                // disk driver reloads on its own
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
            projectConfigRef = ConfigFile.ProjectConfig::type,
            publish = publish,
            model = model,
        )
        NullableProjectEnumSelection(
            label = "Side:",
            enumEntries = ProjectSide.entries,
            projectRef = Project::side,
            projectConfigRef = ConfigFile.ProjectConfig::side,
            publish = publish,
            model = model,
        )
        ProjectEnumSelection(
            label = "Update Strategy:",
            enumEntries = UpdateStrategy.entries,
            projectRef = Project::updateStrategy,
            projectConfigRef = ConfigFile.ProjectConfig::updateStrategy,
            publish = publish,
            model = model,
        )
        ProjectBooleanSelection(
            label = "Redistributable:",
            projectRef = Project::redistributable,
            projectConfigRef = ConfigFile.ProjectConfig::redistributable,
            publish = publish,
            model = model,
        )
        NullableProjectStringSelection(
            label = "Subpath:",
            projectRef = Project::getSubpath,
            projectConfigRef = ConfigFile.ProjectConfig::subpath,
            publish = publish,
            model = model,
        )
    }
}
