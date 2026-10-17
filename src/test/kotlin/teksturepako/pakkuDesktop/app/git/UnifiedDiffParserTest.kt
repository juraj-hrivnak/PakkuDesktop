/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlin.test.Test
import kotlin.test.assertEquals

class UnifiedDiffParserTest {
    @Test
    fun `hunk header drops the function context`() {
        val diff = """
            --- a/A.kt
            +++ b/A.kt
            @@ -3,2 +3,2 @@     fun name() {
            -        val a = 1
            +        val b = 2
        """.trimIndent()
        val parsed = UnifiedDiffParser.parse(diff, "A.kt")
        assertEquals("@@ -3,2 +3,2 @@", parsed.hunks.single().header)
        assertEquals("        val b = 2", parsed.hunks.single().lines.last().content)
    }

    @Test
    fun `hunk header without context stays the range`() {
        val diff = """
            --- a/A.kt
            +++ b/A.kt
            @@ -2,1 +2,1 @@
             fun second() {}
        """.trimIndent()
        val parsed = UnifiedDiffParser.parse(diff, "A.kt")
        assertEquals("@@ -2,1 +2,1 @@", parsed.hunks.single().header)
    }
}
