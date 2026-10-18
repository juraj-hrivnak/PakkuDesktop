/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.projects.ProjectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModpackConfigFieldsTest {
    private fun config(block: ConfigFile.() -> Unit = {}) = ConfigFile().apply(block)

    private fun ConfigFile.field(name: String) = json.encodeToJsonElement(ConfigFile.serializer(), this).jsonObject[name]

    @Test
    fun `every ConfigFile property is a field, and every hint names one`() {
        val names = ModpackFieldSchema.allFields.filter { it.file == ModpackFile.Config }.map { it.serialName }.toSet()
        val declared = (0 until ConfigFile.serializer().descriptor.elementsCount)
            .map { ConfigFile.serializer().descriptor.getElementName(it) }
            .toSet()

        assertEquals(declared, names)
        assertTrue(ModpackFieldHints.paths(ModpackFile.Config).all { it.substringBefore('.') in declared })
    }

    @Test
    fun `every LockFile property is a field, and every hint names one`() {
        val names = ModpackFieldSchema.allFields.filter { it.file == ModpackFile.Lock }.map { it.serialName }.toSet()
        val declared = (0 until LockFile.serializer().descriptor.elementsCount)
            .map { LockFile.serializer().descriptor.getElementName(it) }
            .toSet()

        assertEquals(declared, names)
        assertTrue(ModpackFieldHints.paths(ModpackFile.Lock).all { it.substringBefore('.') in declared })
        assertTrue(ModpackFieldSchema.lockSetterNames.all { it in declared })
    }

    @Test
    fun `a field with no hint falls back to a default widget and label`() {
        val field = ModpackFieldSchema.allFields.first { it.serialName == "parent_lock_hash" }

        assertEquals(FieldWidget.Text, field.widget)
        assertEquals("Parent lock hash", field.label)
        assertEquals(ModpackIsland.Other, field.island)
    }

    @Test
    fun `an unknown key becomes a raw JSON field`() {
        val field = ModpackFieldSchema.unknownField("future_setting")

        assertEquals(FieldWidget.RawJson, field.widget)
        assertEquals(ModpackIsland.RawJson, field.island)
        assertTrue(field.serialName !in ModpackFieldSchema.allFields.map { it.serialName })
    }

    @Test
    fun `string fields round trip through the serialized form`() {
        val config = config { setName("Pack"); setVersion("1.0"); setAuthor("Ada"); setDescription("A pack") }

        val next = config
            .withJsonField("name", JsonPrimitive("Renamed"))
            .withJsonField("version", JsonPrimitive("2.0"))
            .withJsonField("author", null)
            .withJsonField("description", JsonPrimitive(""))

        assertEquals("Renamed", next.getName())
        assertEquals("2.0", next.getVersion())
        assertEquals("", next.getAuthor())
        assertEquals("", next.getDescription())
    }

    @Test
    fun `override lists round trip without touching other fields`() {
        val config = config {
            setName("Pack")
            setExportServerSideProjectsToClient(true)
            paths["mods"] = "custom/mods"
            excludes.add("jei")
        }.withJsonField("overrides", JsonArray(listOf(JsonPrimitive("config/**"))))

        val next = config
            .withJsonField("server_overrides", JsonArray(listOf("server.properties", "world/**").map(::JsonPrimitive)))
            .withJsonField("client_overrides", JsonArray(listOf(JsonPrimitive("options.txt"))))

        assertEquals(listOf("config/**"), next.field("overrides")?.strings)
        assertEquals(listOf("server.properties", "world/**"), next.field("server_overrides")?.strings)
        assertEquals(listOf("options.txt"), next.field("client_overrides")?.strings)
        assertEquals("Pack", next.getName())
        assertEquals(mapOf("mods" to "custom/mods"), next.paths)
        assertEquals(setOf("jei"), next.excludes)
        assertTrue(next.getExportServerSideProjectsToClient())
    }

    @Test
    fun `clearing an override list leaves it empty`() {
        val config = config()
            .withJsonField("overrides", JsonArray(listOf("config/**", "kubejs/**").map(::JsonPrimitive)))

        val next = config.withJsonField("overrides", JsonArray(emptyList()))

        assertEquals(emptyList(), next.field("overrides")?.strings.orEmpty())
    }

    @Test
    fun `a map field round trips and a blank value removes the key`() {
        val config = config { paths["mods"] = "custom/mods" }

        val next = config.withJsonField("paths", buildJsonObject { put("shader_packs", "shaders") })

        assertEquals(mapOf("shader_packs" to "shaders"), next.paths)
    }

    @Test
    fun `an object map round trips per project property`() {
        val config = config()
            .withJsonField("projects", buildJsonObject { put("jei", buildJsonObject { put("subpath", "lib") }) })

        val next = config.withJsonField(
            "projects",
            buildJsonObject {
                put("jei", buildJsonObject { put("subpath", "lib"); put("export", false) })
            },
        )

        assertEquals("lib", next.projects["jei"]?.subpath)
        assertEquals(false, next.projects["jei"]?.export)
    }

    @Test
    fun `a malformed value is rejected and leaves the file unchanged`() {
        val config = config { setName("Pack") }

        val errors = config.jsonFieldErrors("export_server_side_projects_to_client", JsonPrimitive("yes"))

        assertEquals(1, errors.size)
        assertTrue(errors.single().rawMessage.startsWith("Cannot set 'export_server_side_projects_to_client':"))
        assertEquals("Pack", config.withJsonField("name", JsonPrimitive("Pack")).getName())
    }

    @Test
    fun `malformed JSON text explains itself`() {
        val error = parseJsonValue("{").getError()

        assertNotNull(error)
        assertTrue(error.rawMessage.startsWith("Invalid JSON:"))
    }

    @Test
    fun `lock file setters round trip and keep the CLI checks`() {
        val lock = LockFile()
        val versions = ModpackFieldSchema.allFields.first { it.serialName == "mc_versions" }
        val loaders = ModpackFieldSchema.allFields.first { it.serialName == "loaders" }
        val target = ModpackFieldSchema.allFields.first { it.serialName == "target" }

        assertEquals(
            listOf("At least one Minecraft version is required."),
            versions.write(stringListValue(emptyList())).validate(ConfigFile(), lock).map { it.rawMessage },
        )
        assertEquals(
            listOf("At least one loader is required."),
            loaders.write(stringMapValue(emptyList())).validate(ConfigFile(), lock).map { it.rawMessage },
        )
        assertEquals(
            listOf("Target 'curseforgee' not found"),
            target.write(JsonPrimitive("curseforgee")).validate(ConfigFile(), lock).map { it.rawMessage },
        )

        versions.write(stringListValue(listOf("1.20.1"))).mutate(ConfigFile(), lock)
        loaders.write(stringMapValue(listOf("fabric" to "0.15.0"))).mutate(ConfigFile(), lock)
        target.write(JsonPrimitive("Modrinth")).mutate(ConfigFile(), lock)

        assertEquals(listOf("1.20.1"), lock.getMcVersions())
        assertEquals(listOf("fabric" to "0.15.0"), lock.getLoadersWithVersions())
        assertEquals("modrinth", lock.getProjectProvider().component1()?.serialName)
    }

    @Test
    fun `unknown keys survive a rewrite and can be edited or removed`() {
        val onDisk = buildJsonObject {
            put("name", "Pack")
            put("future_setting", 7)
            put("another", "kept")
        }

        val rewritten = ModpackFieldSchema.configJsonWithUnknownKeys(
            ConfigFile(name = "Renamed"),
            onDisk,
            mapOf("future_setting" to JsonPrimitive("changed")),
        )
        val removed = ModpackFieldSchema.configJsonWithUnknownKeys(
            ConfigFile(),
            rewritten,
            mapOf("another" to null),
        )

        assertEquals("Renamed", rewritten["name"]?.jsonPrimitive?.content)
        assertEquals("changed", rewritten["future_setting"]?.jsonPrimitive?.content)
        assertEquals("kept", rewritten["another"]?.jsonPrimitive?.content)
        assertEquals(listOf("name", "future_setting", "another"), rewritten.keys.toList().filter { it in onDisk })
        assertNull(removed["another"])
        assertNotNull(removed["future_setting"])
    }

    @Test
    fun `a path field reads its placeholder from the project type default`() {
        val paths = ModpackFieldSchema.fields.first { it.serialName == "paths" }

        ProjectType.entries.forEach { type ->
            assertEquals(type.getPathString(null), paths.hint.optionPlaceholders[type.serialName])
        }
    }
}
