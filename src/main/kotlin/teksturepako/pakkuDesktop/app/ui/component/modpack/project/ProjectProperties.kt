/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackField
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldSchema
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldValues
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.child
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.isOverride
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.withChild
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.write
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite
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
            val value = entry.child(field.serialName)
            if (!model.editingProject) {
                if (!value.isOverride()) return@forEach
                ProjectPropertyReadOnly(field, value)
            } else {
                ProjectPropertyEditor(field, value) { next ->
                    val key = existingKey ?: project.slug.values.firstOrNull()
                        ?: project.pakkuId
                        ?: project.name.values.firstOrNull()
                        ?: return@ProjectPropertyEditor
                    publish(
                        ModpackMsg.MetaWriteRequested(
                            writeProjectOverride(projectsField, projects, key, existingKey == null, field.serialName, next),
                        ),
                    )
                }
            }
        }
    }
}

private fun writeProjectOverride(
    projectsField: ModpackField,
    projects: JsonElement,
    key: String,
    creating: Boolean,
    childName: String,
    next: JsonElement?,
): MetaWrite {
    val current = projects as? JsonObject ?: JsonObject(emptyMap())
    val base = if (creating && key !in current) {
        val ordered = linkedMapOf<String, JsonElement>(key to JsonObject(emptyMap()))
        current.forEach { (name, value) -> ordered[name] = value }
        JsonObject(ordered)
    } else {
        current
    }
    val entry = base[key] ?: JsonObject(emptyMap())
    return projectsField.write(base.withChild(key, entry.withChild(childName, next)))
}
