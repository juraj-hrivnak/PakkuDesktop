/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import io.klogging.noCoLogger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakkuDesktop.plugin.HOST_API
import teksturepako.pakkuDesktop.plugin.PluginId

private val logger = noCoLogger("PluginManifest")

private val ID_PATTERN = Regex("[a-z0-9][a-z0-9._-]*")

@Serializable
internal data class ManifestFile(
    val id: String? = null,
    val name: String? = null,
    @SerialName("main-class") val mainClass: String? = null,
    @SerialName("host-api") val hostApi: Int? = null,
    val jar: String? = null,
)

/** Contents of a plugin folder's `manifest.edn`. */
data class PluginManifest(
    val id: PluginId,
    val name: String,
    val mainClass: String,
    val hostApi: Int,
    val jar: String = "plugin.jar",
)

/**
 * Parses and validates `manifest.edn` text. Throws [IllegalArgumentException] (including
 * [EdnException]) with a user-facing message on any problem.
 *
 * Checks keys, types, the id format and [HOST_API]; does not touch the file system.
 */
fun parseManifest(text: String): PluginManifest {
    val file = Edn.decodeFromString(ManifestFile.serializer(), text)

    val id = file.id ?: throw IllegalArgumentException("missing :id")
    require(ID_PATTERN.matches(id)) { ":id '$id' must match ${ID_PATTERN.pattern}" }
    val name = file.name ?: throw IllegalArgumentException("missing :name")
    val mainClass = file.mainClass ?: throw IllegalArgumentException("missing :main-class")
    val hostApi = file.hostApi ?: throw IllegalArgumentException("missing :host-api")
    require(hostApi == HOST_API) { "host-api $hostApi not supported (host is $HOST_API)" }

    val unknown = Edn.unknownKeysFromLastDecode
    if (unknown.isNotEmpty()) logger.debug("manifest '$id': ignoring unknown keys $unknown")

    return PluginManifest(PluginId(id), name, mainClass, hostApi, file.jar ?: "plugin.jar")
}
