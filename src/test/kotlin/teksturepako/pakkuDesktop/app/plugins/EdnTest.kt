/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
private data class EdnFlatProbe(
    val id: String? = null,
    @SerialName("host-api") val hostApi: Long? = null,
    val n: Long? = null,
    @SerialName("main-class") val mainClass: String? = null,
    val a: Long? = null,
    val b: String? = null,
    val s: String? = null,
)

class EdnTest {
    private fun decode(text: String): EdnFlatProbe =
        Edn.decodeFromString(EdnFlatProbe.serializer(), text)

    private fun assertRejected(text: String, message: String) =
        assertEquals(message, assertFailsWith<EdnException> { decode(text) }.message)

    @Test
    fun flatMapFields() {
        val file = decode("""{:id "x" :host-api 1 :n -2 :main-class "a.B"}""")
        assertEquals("x", file.id)
        assertEquals(1L, file.hostApi)
        assertEquals(-2L, file.n)
        assertEquals("a.B", file.mainClass)
    }

    @Test
    fun stringEscapes() = assertEquals("a\"b\\c\nd\te", decode("""{:s "a\"b\\c\nd\te"}""").s)

    @Test
    fun commentsAndCommasAreWhitespace() {
        val file = decode("; header\n{:a 1,, :b \"x;y\" ; trailing\n}\n; footer")
        assertEquals(1L, file.a)
        assertEquals("x;y", file.b)
    }

    @Test
    fun emptyMapAllowed() = assertEquals(EdnFlatProbe(), decode(" {} "))

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
