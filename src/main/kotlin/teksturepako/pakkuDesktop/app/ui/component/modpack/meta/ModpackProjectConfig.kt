/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.PropertyWrite

val modpackProjectsField: ModpackField
    get() = ModpackFieldSchema.fields.first { it.serialName == "projects" }

/** Properties of one `pakku.json` project override (shared by Overview cards and the Projects tab). */
val modpackProjectConfigFields: List<ModpackField>
    get() = modpackProjectsField.fields

fun projectPropertyErrorKey(project: Project, field: ModpackField): String {
    val slug = project.slug.values.firstOrNull() ?: project.uiKey()
    return "${modpackProjectsField.key}/$slug/${field.serialName}"
}

fun projectsMapEditErrorKey(mapKey: String): String = "${modpackProjectsField.key}/$mapKey"

fun projectMapEntryErrorKey(mapKey: String, field: ModpackField): String =
    "${modpackProjectsField.key}/$mapKey/${field.serialName}"

/**
 * Key under `pakku.json` → `projects` for [project], matching Pakku lookup (`input in project`).
 * Prefer an existing override entry so renames and multi-slug projects stay consistent.
 */
fun projectConfigMapKey(project: Project, config: ConfigFile): String? =
    config.projects.keys.firstOrNull { it in project }
        ?: project.slug.values.firstOrNull()
        ?: project.name.values.firstOrNull()
        ?: project.id.values.firstOrNull()

/** Client-side checks before queueing a write (invalid input is shown, not saved). */
fun projectOverrideCommitErrors(field: ModpackField, text: String): List<ActionError> = when (field.serialName) {
    "subpath" -> projectSubpathOverrideErrors(text)
    else -> emptyList()
}

/** [ConfigFile.setProjectConfig] mutation for one project property. */
fun propertyWriteForField(field: ModpackField, next: JsonElement?): PropertyWrite = PropertyWrite {
    when (field.serialName) {
        "type" -> type = next?.text?.let { text ->
            ProjectType.entries.first { it.name.equals(text, true) }
        }
        "side" -> side = next?.text?.takeIf { it.isNotBlank() }?.let { text ->
            ProjectSide.entries.first { it.name.equals(text, true) }
        }
        "update_strategy" -> updateStrategy = next?.text?.let { text ->
            UpdateStrategy.entries.first { it.name.equals(text, true) }
        }
        "redistributable" -> redistributable = next?.text?.toBooleanStrictOrNull()
        "subpath" -> {
            // Same assignment as `pakku cfg prj --subpath`. The value lives on the config entry.
            val text = next?.text?.trim().orEmpty()
            subpath = text.ifEmpty { null }
        }
        "aliases" -> aliases = when {
            next == null || next is JsonNull -> null
            next is JsonArray && next.strings.isEmpty() -> mutableSetOf()
            else -> next.strings.toMutableSet()
        }
        "export" -> export = next?.text?.toBooleanStrictOrNull()
        else -> Unit
    }
}

fun projectPropertyWriteMsg(project: Project, field: ModpackField, next: JsonElement?): ModpackMsg =
    ModpackMsg.PropertyWriteRequested(
        projectUiKey = project.uiKey(),
        request = propertyWriteForField(field, next),
        errorFieldKey = projectPropertyErrorKey(project, field),
    )

fun removeProjectOverride(projectsField: ModpackField, projectKey: String): MetaWrite =
    writeProjectsMap(projectsField, projectsMapEditErrorKey(projectKey), { config ->
        JsonObject(projectsMap(config, projectsField) - projectKey)
    })

fun renameProjectOverride(projectsField: ModpackField, from: String, to: String): MetaWrite =
    writeProjectsMap(projectsField, projectsMapEditErrorKey(from), { config ->
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
): MetaWrite = MetaWrite(
    fieldKey = fieldKey,
    validate = { config, _ -> config.jsonFieldErrors(projectsField.serialName, patch(config)) },
    replaceConfig = { config -> config.withJsonField(projectsField.serialName, patch(config)) },
)
