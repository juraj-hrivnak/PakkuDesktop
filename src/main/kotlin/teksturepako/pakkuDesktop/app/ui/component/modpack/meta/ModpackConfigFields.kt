/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.NoFilesOn
import teksturepako.pakku.api.actions.errors.NotFoundOn
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform

/** Plain Pakku-style message when no dedicated [ActionError] exists (matches CLI `set` output). */
data class PackMetaSettingError(override val rawMessage: String) : ActionError()

/*
 * Most ConfigFile properties are private and have no setters, so generic field access goes
 * through the serialized form. A null value removes the key, which decodes to the default.
 */

fun ConfigFile.withJsonField(key: String, value: JsonElement?): ConfigFile =
    withJsonField(ConfigFile.serializer(), this, key, value)

fun LockFile.withJsonField(key: String, value: JsonElement?): LockFile =
    withJsonField(LockFile.serializer(), this, key, value)

private fun <T> withJsonField(serializer: KSerializer<T>, value: T, key: String, field: JsonElement?): T
{
    val current = json.encodeToJsonElement(serializer, value).jsonObject
    val next = if (field == null || field is JsonNull) current - key else current + (key to field)
    return json.decodeFromJsonElement(serializer, JsonObject(next))
}

/** Why [ConfigFile.withJsonField] would reject [value], or empty when it decodes. */
fun ConfigFile.jsonFieldErrors(key: String, value: JsonElement?): List<ActionError> =
    decodeErrors(key) { withJsonField(key, value) }

fun LockFile.jsonFieldErrors(key: String, value: JsonElement?): List<ActionError> =
    decodeErrors(key) { withJsonField(key, value) }

private fun decodeErrors(key: String, decode: () -> Unit): List<ActionError> =
    runCatching(decode).exceptionOrNull()?.let { error ->
        val reason = error.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "invalid value"
        listOf(PackMetaSettingError("Cannot set '$key': $reason"))
    }.orEmpty()

/** Same rules as `pakku set --target` / [teksturepako.pakku.api.actions.createAdditionRequest]. */
fun LockFile.targetChangeErrors(target: String): List<ActionError>
{
    val platforms = when (target.lowercase()) {
        "curseforge" -> listOf(CurseForge)
        "modrinth" -> listOf(Modrinth)
        "multiplatform" -> Multiplatform.platforms
        else -> return listOf(LockFile.TargetNotFound(target))
    }

    val errors = mutableListOf<ActionError>()
    for (project in getAllProjects()) {
        if (project.slug.keys.size <= 1 && project.slug.keys.firstOrNull() == GitHub.serialName) continue
        for (platform in platforms) {
            if (project.isNotOnPlatform(platform)) {
                errors += NotFoundOn(project, platform)
            }
            if (project.hasNoFilesOnPlatform(platform)) {
                errors += NoFilesOn(project, platform)
            }
        }
    }
    return errors
}

/** Same rule as `pakku set --mc-versions`. */
fun LockFile.mcVersionsChangeErrors(versions: List<String>): List<ActionError>
{
    if (versions.isEmpty()) {
        return listOf(PackMetaSettingError("At least one Minecraft version is required."))
    }

    val errors = mutableListOf<ActionError>()
    for (project in getAllProjects()) {
        for (file in project.files) {
            if (file.mcVersions.isNotEmpty() && file.mcVersions.none { it in versions }) {
                val name = project.name.values.firstOrNull() ?: project.slug.values.firstOrNull() ?: "?"
                errors += PackMetaSettingError(
                    "Cannot set to $versions, because $name (${file.type}) requires ${file.mcVersions}.",
                )
            }
        }
    }
    return errors
}

/** Same rule as `pakku set --loaders`. */
fun LockFile.loadersChangeErrors(loaders: Collection<String>): List<ActionError>
{
    if (loaders.isEmpty()) {
        return listOf(PackMetaSettingError("At least one loader is required."))
    }

    val errors = mutableListOf<ActionError>()
    for (project in getAllProjects()) {
        for (file in project.files) {
            if (file.loaders.isNotEmpty() && file.loaders.none { it in loaders || it in Platform.validLoaders }) {
                val name = project.name.values.firstOrNull() ?: project.slug.values.firstOrNull() ?: "?"
                errors += PackMetaSettingError(
                    "Cannot set to $loaders, because $name (${file.type}) requires ${file.loaders}.",
                )
            }
        }
    }
    return errors
}
