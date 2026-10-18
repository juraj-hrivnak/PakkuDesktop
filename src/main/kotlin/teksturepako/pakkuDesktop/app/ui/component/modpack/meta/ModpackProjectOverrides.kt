/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite

/** [ModpackModel.modpackMetaErrors] key for one project override property. */
fun projectOverrideErrorKey(projectsField: ModpackField, projectKey: String, childName: String): String =
    "${projectsField.key}/$projectKey/$childName"

/** `.` is the current directory, which Pakku treats as no subpath. */
fun subpathEditorText(raw: String): String = raw.trim().let { if (it.isEmpty() || it == ".") "" else it }

/** Client-side checks before queueing a write (invalid input is shown, not saved). */
fun projectOverrideCommitErrors(field: ModpackField, text: String): List<ActionError> = when (field.serialName) {
    "subpath" -> projectSubpathOverrideErrors(text)
    else -> emptyList()
}


fun writeProjectOverride(
    projectsField: ModpackField,
    projects: JsonElement,
    projectKey: String,
    creating: Boolean,
    childName: String,
    next: JsonElement?,
): MetaWrite {
    val current = projects as? JsonObject ?: JsonObject(emptyMap())
    val base = if (creating && projectKey !in current) {
        val ordered = linkedMapOf<String, JsonElement>(projectKey to JsonObject(emptyMap()))
        current.forEach { (name, value) -> ordered[name] = value }
        JsonObject(ordered)
    } else {
        current
    }
    val entry = base[projectKey] ?: JsonObject(emptyMap())
    val meta = projectsField.write(base.withChild(projectKey, entry.withChild(childName, next)))
    val fieldKey = projectOverrideErrorKey(projectsField, projectKey, childName)
    val extraValidate: (ConfigFile, LockFile) -> List<ActionError> = when (childName) {
        "subpath" -> { _, _ -> projectSubpathOverrideErrors(next?.text) }
        else -> { _, _ -> emptyList() }
    }
    return meta.copy(
        fieldKey = fieldKey,
        validate = { config, lock -> meta.validate(config, lock) + extraValidate(config, lock) },
    )
}
