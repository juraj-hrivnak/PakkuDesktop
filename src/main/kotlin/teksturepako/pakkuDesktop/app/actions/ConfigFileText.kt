/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import kotlinx.collections.immutable.toPersistentMap
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.io.writeToFile
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackFieldSchema
import teksturepako.pakkuDesktop.app.ui.model.ConfigFileText
import kotlin.io.path.Path
import kotlin.io.path.readText

private fun configFilePath() = Path(workingPath, ConfigFile.FILE_NAME)

/** `pakku.json` of the open modpack exactly as on disk; null when it cannot be read. */
fun readConfigFileText(): ConfigFileText?
{
    val text = runCatching { configFilePath().readText() }.getOrNull() ?: return null
    val unknownKeys = parseObject(text)?.let(ModpackFieldSchema::unknownKeys).orEmpty()
    return ConfigFileText(text, unknownKeys.toPersistentMap())
}

/**
 * Writes [this] like [ConfigFile.write], which drops the keys [ConfigFile] does not declare. Those
 * keys are carried over from the file on disk, with [unknownKeys] applied (null removes a key).
 */
suspend fun ConfigFile.writeKeepingUnknownKeys(unknownKeys: Map<String, JsonElement?>): ActionError?
{
    val onDisk = runCatching { configFilePath().readText() }.getOrNull()?.let(::parseObject)
    if (unknownKeys.isEmpty() && onDisk?.let(ModpackFieldSchema::unknownKeys).isNullOrEmpty()) return write()

    val content = ModpackFieldSchema.configJsonWithUnknownKeys(this, onDisk, unknownKeys)
    return writeToFile(content, configFilePath().toString(), overrideText = true, format = json)
}

private fun parseObject(text: String): JsonObject? =
    runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
