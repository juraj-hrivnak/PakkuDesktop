/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EdnTest {
    private fun assertRejected(text: String, message: String) =
        assertEquals(message, assertFailsWith<EdnException> { readEdnMap(text) }.message)

    @Test
    fun flatMapInSourceOrder() {
        val map = readEdnMap("""{:id "x" :host-api 1 :n -2 :main-class "a.B"}""")
        assertEquals(listOf("id", "host-api", "n", "main-class"), map.keys.toList())
        assertEquals(mapOf("id" to "x", "host-api" to 1L, "n" to -2L, "main-class" to "a.B"), map)
    }

    @Test
    fun stringEscapes() =
        assertEquals("a\"b\\c\nd\te", readEdnMap("""{:s "a\"b\\c\nd\te"}""").getValue("s"))

    @Test
    fun commentsAndCommasAreWhitespace() =
        assertEquals(
            mapOf("a" to 1L, "b" to "x;y"),
            readEdnMap("; header\n{:a 1,, :b \"x;y\" ; trailing\n}\n; footer"),
        )

    @Test
    fun emptyMapAllowed() = assertEquals(emptyMap<String, Any>(), readEdnMap(" {} "))

    @Test
    fun nestedVectorRejected() = assertRejected("{:a [1 2]}", "line 1, column 5: vectors are not supported")

    @Test
    fun nestedMapRejected() = assertRejected("{:a {:b 1}}", "line 1, column 5: nested maps are not supported")

    @Test
    fun symbolValueRejected() = assertRejected("{:a b}", "line 1, column 5: unsupported value starting with 'b'")

    @Test
    fun positionOnLaterLine() = assertRejected("{:a 1\n :b }", "line 2, column 5: missing value for :b")

    @Test
    fun unterminatedStringPointsAtOpeningQuote() =
        assertRejected("{:a\n  \"abc", "line 2, column 3: unterminated string")

    @Test
    fun unsupportedEscape() = assertRejected("""{:a "x\u"}""", "line 1, column 7: unsupported escape '\\u'")

    @Test
    fun floatRejected() = assertRejected("{:a 1.5}", "line 1, column 5: invalid number")

    @Test
    fun duplicateKeyRejected() = assertRejected("{:a 1 :a 2}", "line 1, column 7: duplicate key :a")

    @Test
    fun unterminatedMap() = assertRejected("{:a 1", "line 1, column 6: unterminated map")

    @Test
    fun notAMap() = assertRejected("[1]", "line 1, column 1: expected '{'")

    @Test
    fun contentAfterMap() = assertRejected("{} {}", "line 1, column 4: unexpected content after map")
}
