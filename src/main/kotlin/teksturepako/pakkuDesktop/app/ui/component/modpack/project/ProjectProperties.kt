/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.component.Switch
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldSchema
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldValues
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.child
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.text
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.projectOverrideErrorKey
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.writeProjectOverride
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
        val projectsField = ModpackFieldSchema.fields.first { it.serialName == "projects" }
        val projects = ModpackFieldValues(config, lock, emptyMap())[projectsField] as? JsonObject ?: JsonObject(emptyMap())
        val existingKey = projects.keys.firstOrNull { key -> key in project }
        val entry = existingKey?.let { projects[it] } ?: JsonObject(emptyMap())

        modpackProjectConfigFields.forEach { field ->
            val override = entry.child(field.serialName)
            val display = projectPropertyUiValue(project, field, override)
            val configKey = existingKey ?: project.slug.values.firstOrNull()
                ?: project.pakkuId
                ?: project.name.values.firstOrNull()
            val fieldErrors = if (configKey != null) {
                model.metaErrors[projectOverrideErrorKey(projectsField, configKey, field.serialName)]?.orEmpty()
                    ?: emptyList()
            } else {
                emptyList()
            }
            if (field.serialName == "subpath" && override.text.trim() == "." && configKey != null) {
                LaunchedEffect(configKey) {
                    publish(
                        ModpackMsg.MetaWriteRequested(
                            writeProjectOverride(
                                projectsField,
                                projects,
                                configKey,
                                existingKey == null,
                                field.serialName,
                                null,
                            ),
                        ),
                    )
                }
            }
            if (!model.editingProject) {
                if (!showProjectPropertyReadOnly(project, field, override)) return@forEach
                ProjectPropertyReadOnly(field, display)
            } else {
                ProjectPropertyEditor(field, display, fieldErrors) { next ->
                    val key = configKey ?: return@ProjectPropertyEditor
                    publish(
                        ModpackMsg.MetaWriteRequested(
                            writeProjectOverride(
                                projectsField,
                                projects,
                                key,
                                existingKey == null,
                                field.serialName,
                                next,
                            ),
                        ),
                    )
                }
            }
        }
    }
}
