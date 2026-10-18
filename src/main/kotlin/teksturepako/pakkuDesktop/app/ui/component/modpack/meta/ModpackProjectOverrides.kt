/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.json
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

/**
 * Sets one property of one `projects` entry. The change is applied to the config being written,
 * not to the value on screen, so queued edits do not overwrite each other.
 * A new entry goes to the top. With [dropEmpty], an entry left without properties is removed, like `pakku cfg prj`.
 */
fun writeProjectOverride(
    projectsField: ModpackField,
    projectKey: String,
    childName: String,
    next: JsonElement?,
    dropEmpty: Boolean = false,
): MetaWrite = writeProjectsMap(projectsField, projectOverrideErrorKey(projectsField, projectKey, childName), { config ->
    val current = projectsMap(config, projectsField)
    val entry = (current[projectKey] ?: JsonObject(emptyMap())).withChild(childName, next)
    val ordered = linkedMapOf<String, JsonElement>()
    if (projectKey !in current) ordered[projectKey] = entry
    current.forEach { (name, value) -> ordered[name] = if (name == projectKey) entry else value }
    if (dropEmpty && entry.isEmpty()) ordered.remove(projectKey)
    JsonObject(ordered)
}) { config, patched ->
    val pathErrors = if (childName == "subpath") projectSubpathOverrideErrors(next?.text) else emptyList()
    pathErrors.ifEmpty { config.jsonFieldErrors(projectsField.serialName, patched) }
}

fun removeProjectOverride(projectsField: ModpackField, projectKey: String): MetaWrite =
    writeProjectsMap(projectsField, projectOverrideErrorKey(projectsField, projectKey, ""), { config ->
        JsonObject(projectsMap(config, projectsField) - projectKey)
    })

fun renameProjectOverride(projectsField: ModpackField, from: String, to: String): MetaWrite =
    writeProjectsMap(projectsField, projectOverrideErrorKey(projectsField, from, ""), { config ->
        val current = projectsMap(config, projectsField)
        if (from !in current || to in current) return@writeProjectsMap current
        JsonObject(buildMap {
            current.forEach { (name, value) -> put(if (name == from) to else name, value) }
        })
    })

private fun projectsMap(config: ConfigFile, projectsField: ModpackField): JsonObject =
    json.encodeToJsonElement(ConfigFile.serializer(), config).jsonObject[projectsField.serialName] as? JsonObject
        ?: JsonObject(emptyMap())

private fun writeProjectsMap(
    projectsField: ModpackField,
    fieldKey: String,
    patch: (ConfigFile) -> JsonObject,
    extraValidate: (ConfigFile, JsonObject) -> List<ActionError> = { config, patched ->
        config.jsonFieldErrors(projectsField.serialName, patched)
    },
): MetaWrite = MetaWrite(
    fieldKey = fieldKey,
    validate = { config, _ -> extraValidate(config, patch(config)) },
    replaceConfig = { config -> config.withJsonField(projectsField.serialName, patch(config)) },
)
