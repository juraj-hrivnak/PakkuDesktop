/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import teksturepako.pakkuDesktop.plugin.PluginId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PluginManifestTest {
    private fun assertRejected(text: String, message: String) =
        assertEquals(message, assertFailsWith<IllegalArgumentException> { parseManifest(text) }.message)

    @Test
    fun defaultsJarAndIgnoresUnknownKeys() = assertEquals(
        PluginManifest(PluginId("sample"), "Sample", "a.B", hostApi = 1),
        parseManifest("""{:id "sample" :name "Sample" :main-class "a.B" :host-api 1 :future "x"}"""),
    )

    @Test
    fun explicitJar() = assertEquals(
        "lib/p.jar",
        parseManifest("""{:id "p" :name "P" :main-class "a.B" :host-api 1 :jar "lib/p.jar"}""").jar,
    )

    @Test
    fun missingKey() = assertRejected("""{:id "p" :name "P" :host-api 1}""", "missing :main-class")

    @Test
    fun wrongType() = assertRejected("""{:id "p" :name "P" :main-class "a.B" :host-api "1"}""", ":host-api must be an integer")

    @Test
    fun invalidId() = assertRejected(
        """{:id "My Plugin" :name "P" :main-class "a.B" :host-api 1}""",
        ":id 'My Plugin' must match [a-z0-9][a-z0-9._-]*",
    )

    @Test
    fun unsupportedHostApi() = assertRejected(
        """{:id "p" :name "P" :main-class "a.B" :host-api 2}""",
        "host-api 2 not supported (host is 1)",
    )
}
