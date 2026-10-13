/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

/** The three `pakku.json` override lists, by their serial names. */
enum class OverrideList(val serialName: String, val title: String, val description: String)
{
    Common("overrides", "Overrides", "Copied into every export."),
    Server("server_overrides", "Server overrides", "Copied into server exports only."),
    Client("client_overrides", "Client overrides", "Copied into client exports only."),
}

/** Plain Pakku-style message when no dedicated [ActionError] exists (matches CLI `set` output). */
data class PackMetaSettingError(override val rawMessage: String) : ActionError()

/*
 * ConfigFile exposes override lists only glob-expanded, and has no setters for the server and
 * client lists, so these go through its serialized form.
 */

fun ConfigFile.overrideEntries(list: OverrideList): List<String> =
    json.encodeToJsonElement(ConfigFile.serializer(), this).jsonObject[list.serialName]
        ?.jsonArray
        ?.map { it.jsonPrimitive.content }
        .orEmpty()

fun ConfigFile.withOverrideEntries(list: OverrideList, entries: List<String>): ConfigFile
{
    val current = json.encodeToJsonElement(ConfigFile.serializer(), this).jsonObject
    val next = JsonObject(current + (list.serialName to JsonArray(entries.map(::JsonPrimitive))))
    return json.decodeFromJsonElement(ConfigFile.serializer(), next)
}

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
