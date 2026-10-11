/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import kotlin.io.path.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private const val KEY = "pakku.plugins.dir"

class PluginPathsTest {
    private var previous: String? = null
    private val custom = Path("/tmp/pakku-custom-plugins")

    @BeforeTest
    fun saveProperty() {
        previous = System.getProperty(KEY)
        System.clearProperty(KEY)
    }

    @AfterTest
    fun restoreProperty() {
        previous?.let { System.setProperty(KEY, it) } ?: System.clearProperty(KEY)
    }

    @Test
    fun devBuildHonorsPluginsDirOverride() {
        System.setProperty(KEY, custom.toString())
        assertEquals(custom, pluginsRoot(release = false))
    }

    @Test
    fun releaseBuildIgnoresPluginsDirOverride() {
        val configDir = pluginsRoot(release = true)
        System.setProperty(KEY, custom.toString())
        assertEquals(configDir, pluginsRoot(release = true))
        assertNotEquals(custom, configDir)
    }
}
