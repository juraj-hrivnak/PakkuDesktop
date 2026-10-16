/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlin.test.Test
import kotlin.test.assertEquals

class UnifiedDiffParserTest {
    @Test
    fun `hunk header names the enclosing declaration`() {
        val lines = listOf(
            "class Pack {",
            "    fun name() {",
            "        val a = 1",
            "        val b = 2",
            "    }",
            "}",
        )
        val enriched = UnifiedDiffParser.withEnclosingContext(diff("@@ -3,2 +3,2 @@"), lines)
        assertEquals("@@ -3,2 +3,2 @@ fun name() {", enriched.hunks.single().header)
    }

    @Test
    fun `top level hunk uses the nearest line above it`() {
        val lines = listOf(
            "fun first() {}",
            "fun second() {}",
        )
        val enriched = UnifiedDiffParser.withEnclosingContext(diff("@@ -2,1 +2,1 @@"), lines)
        assertEquals("@@ -2,1 +2,1 @@ fun first() {}", enriched.hunks.single().header)
    }

    @Test
    fun `keeps context git already provided`() {
        val header = "@@ -3,1 +3,1 @@ fun name() {"
        val enriched = UnifiedDiffParser.withEnclosingContext(diff(header), listOf("class Pack {"))
        assertEquals(header, enriched.hunks.single().header)
    }

    @Test
    fun `deleted file uses the old line`() {
        val lines = listOf(
            "fun kept() {",
            "    val gone = 1",
            "}",
        )
        val enriched = UnifiedDiffParser.withEnclosingContext(diff("@@ -2,1 +0,0 @@"), lines)
        assertEquals("@@ -2,1 +0,0 @@ fun kept() {", enriched.hunks.single().header)
    }

    private fun diff(header: String) = DiffContent(
        oldPath = "A.kt",
        newPath = "A.kt",
        hunks = listOf(DiffHunk(header = header, lines = emptyList())),
    )
}
