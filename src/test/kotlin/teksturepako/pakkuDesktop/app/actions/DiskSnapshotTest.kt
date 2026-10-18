/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import teksturepako.pakkuDesktop.app.ui.model.pathKey
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiskSnapshotTest {
    @Test
    fun presentUntrackedAndIgnored() {
        val root = createTempDirectory("disk-snapshot")
        try {
            root.resolve("mods/sub").createDirectories()
            root.resolve("mods/a.jar").writeBytes(byteArrayOf(1))
            root.resolve("mods/sub/b.jar").writeBytes(byteArrayOf(2))
            root.resolve("mods/readme.txt").writeBytes(byteArrayOf(3))
            root.resolve("saves").createDirectories()
            root.resolve("saves/x.jar").writeBytes(byteArrayOf(4))
            root.resolve("mods/build").createDirectories()
            root.resolve("mods/build/leak.jar").writeBytes(byteArrayOf(5))
            root.resolve("mods/sodium-build.jar").writeBytes(byteArrayOf(6))

            val expected = mapOf(
                pathKey("mods/a.jar") to "mods/a.jar",
                pathKey("mods/c.jar") to "mods/c.jar",
            )
            val walked = walkManagedFiles(root, configFile = null, excludeAbsolute = emptySet())
            val snapshot = buildDiskSnapshot(root, expected, walked, folders = listOf("mods/"))

            assertEquals(setOf(pathKey("mods/a.jar")), snapshot.present)
            assertEquals(listOf("mods/sodium-build.jar", "mods/sub/b.jar"), snapshot.untracked)
            assertEquals(setOf("mods/"), snapshot.existingFolders)
            assertTrue(snapshot.untracked.none { "readme.txt" in it || it.startsWith("saves/") || "/build/" in it })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun overrideOutputExcludedFromUntracked() {
        val root = createTempDirectory("disk-snapshot-override")
        try {
            root.resolve("mods").createDirectories()
            val override = root.resolve("mods/custom.jar")
            override.writeBytes(byteArrayOf(1))
            root.resolve("mods/other.jar").writeBytes(byteArrayOf(2))

            val walked = walkManagedFiles(
                root,
                configFile = null,
                excludeAbsolute = setOf(override.toAbsolutePath().normalize().invariantSeparatorsPathString),
            )
            val expected = mapOf(pathKey("mods/custom.jar") to "mods/custom.jar")
            val snapshot = buildDiskSnapshot(root, expected, walked, folders = listOf("mods/"))

            assertEquals(setOf(pathKey("mods/custom.jar")), snapshot.present)
            assertEquals(listOf("mods/other.jar"), snapshot.untracked)
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
