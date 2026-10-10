/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import io.klogging.noCoLogger
import teksturepako.pakkuDesktop.plugin.HOST_API
import teksturepako.pakkuDesktop.plugin.PluginId

private val logger = noCoLogger("PluginManifest")

private val ID_PATTERN = Regex("[a-z0-9][a-z0-9._-]*")

private val KNOWN_KEYS = setOf("id", "name", "main-class", "host-api", "jar")

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
    val map = readEdnMap(text)

    fun string(key: String): String? = when (val v = map[key]) {
        null -> null
        is String -> v
        else -> throw IllegalArgumentException(":$key must be a string")
    }

    fun requireString(key: String): String = requireNotNull(string(key)) { "missing :$key" }

    val id = requireString("id")
    require(ID_PATTERN.matches(id)) { ":id '$id' must match ${ID_PATTERN.pattern}" }
    val name = requireString("name")
    val mainClass = requireString("main-class")
    val hostApi = when (val v = map["host-api"]) {
        null -> throw IllegalArgumentException("missing :host-api")
        is Long -> v
        else -> throw IllegalArgumentException(":host-api must be an integer")
    }
    require(hostApi == HOST_API.toLong()) { "host-api $hostApi not supported (host is $HOST_API)" }
    val jar = string("jar") ?: "plugin.jar"

    val unknown = map.keys - KNOWN_KEYS
    if (unknown.isNotEmpty()) logger.debug("manifest '$id': ignoring unknown keys $unknown")

    return PluginManifest(PluginId(id), name, mainClass, hostApi.toInt(), jar)
}
