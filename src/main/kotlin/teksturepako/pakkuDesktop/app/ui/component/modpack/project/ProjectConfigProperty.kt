/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.runtime.Composable
import kotlinx.serialization.json.JsonElement
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackField
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.projectPropertyWriteMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@Composable
fun ProjectConfigProperty(
    project: Project,
    field: ModpackField,
    override: JsonElement,
    editing: Boolean,
    fieldErrors: List<ActionError>,
    publish: (ModpackMsg) -> Unit,
) {
    val display = projectPropertyUiValue(project, field, override)
    if (!editing) {
        if (showProjectPropertyReadOnly(project, field, override)) ProjectPropertyReadOnly(field, display)
    } else {
        ProjectPropertyEditor(
            field = field,
            value = display,
            errors = fieldErrors,
            onSubpathFocus = { publish(ModpackMsg.SubpathFieldFocusChanged(it)) },
        ) { next ->
            publish(projectPropertyWriteMsg(project, field, next))
        }
    }
}
