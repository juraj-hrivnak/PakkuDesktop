/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import teksturepako.pakku.api.data.ConfigFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModpackConfigFieldsTest {
    @Test
    fun `override lists round trip without touching other fields`() {
        val config = ConfigFile(
            name = "Pack",
            overrides = mutableListOf("config/**"),
            paths = mutableMapOf("mods" to "custom/mods"),
        )
        config.setExportServerSideProjectsToClient(true)
        config.excludes.add("jei")

        val next = config
            .withOverrideEntries(OverrideList.Server, listOf("server.properties", "world/**"))
            .withOverrideEntries(OverrideList.Client, listOf("options.txt"))

        assertEquals(listOf("config/**"), next.overrideEntries(OverrideList.Common))
        assertEquals(listOf("server.properties", "world/**"), next.overrideEntries(OverrideList.Server))
        assertEquals(listOf("options.txt"), next.overrideEntries(OverrideList.Client))
        assertEquals("Pack", next.getName())
        assertEquals(mapOf("mods" to "custom/mods"), next.paths)
        assertEquals(setOf("jei"), next.excludes)
        assertTrue(next.getExportServerSideProjectsToClient())
    }

    @Test
    fun `clearing an override list leaves it empty`() {
        val config = ConfigFile(overrides = mutableListOf("config/**", "kubejs/**"))

        val next = config.withOverrideEntries(OverrideList.Common, emptyList())

        assertEquals(emptyList(), next.overrideEntries(OverrideList.Common))
    }
}
