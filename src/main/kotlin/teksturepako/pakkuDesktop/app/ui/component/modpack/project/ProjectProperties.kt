/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import kotlinx.serialization.json.JsonObject
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.component.Switch
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldValues
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.child
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.modpackProjectConfigFields
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.modpackProjectsField
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.projectConfigMapKey
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.projectPropertyErrorKey
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProjectsTabModel

@Composable
fun ProjectProperties(publish: (ModpackMsg) -> Unit, model: ProjectsTabModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupHeader("Properties", modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Edit: ")
                Switch(
                    checked = model.editingProject,
                    onCheckedChange = { publish(ModpackMsg.ProjectEditing(!model.editingProject)) },
                )
            }
        }

        val project = model.selectedProject ?: return@Column
        val config = model.configFile?.get() ?: return@Column
        val lock = model.lockFile?.get() ?: return@Column
        if (project.slug.values.isEmpty()) return@Column
        val projects = ModpackFieldValues(config, lock, emptyMap())[modpackProjectsField] as? JsonObject
            ?: JsonObject(emptyMap())
        val mapKey = projectConfigMapKey(project, config) ?: return@Column
        val entry = projects[mapKey] ?: JsonObject(emptyMap())

        key(project.uiKey()) {
            modpackProjectConfigFields.forEach { field ->
                key(field.serialName) {
                    ProjectConfigProperty(
                        project = project,
                        field = field,
                        override = entry.child(field.serialName),
                        editing = model.editingProject,
                        fieldErrors = model.metaErrors[projectPropertyErrorKey(project, field)].orEmpty(),
                        publish = publish,
                    )
                }
            }
        }
    }
}
