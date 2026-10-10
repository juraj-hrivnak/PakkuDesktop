/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.plugins

class EdnException(message: String) : IllegalArgumentException(message)

/**
 * Reads a flat EDN map of keywords to strings or integers, e.g. `{:id "x" :host-api 1}`.
 *
 * Supports `;` comments, commas as whitespace and the string escapes `\"`, `\\`, `\n`,
 * `\t`, `\r`. Anything else (vectors, nested maps, symbols, floats, …) is rejected with an
 * [EdnException] naming the line and column.
 *
 * @return keyword names without the leading `:` mapped to [String] or [Long] values,
 *   in source order.
 */
fun readEdnMap(text: String): Map<String, Any> = EdnReader(text).readTopLevelMap()

private class EdnReader(private val text: String) {
    private var pos = 0
    private var line = 1
    private var col = 1

    fun readTopLevelMap(): Map<String, Any> {
        skipWhitespace()
        if (peek() != '{') fail("expected '{'")
        next()
        val result = LinkedHashMap<String, Any>()
        while (true) {
            skipWhitespace()
            when (peek()) {
                null -> fail("unterminated map")
                '}' -> { next(); break }
            }
            val keyLine = line
            val keyCol = col
            val key = readKeyword()
            skipWhitespace()
            if (peek() == null || peek() == '}') fail("missing value for :$key")
            val value = readValue()
            if (result.put(key, value) != null) fail("duplicate key :$key", keyLine, keyCol)
        }
        skipWhitespace()
        if (peek() != null) fail("unexpected content after map")
        return result
    }

    private fun readKeyword(): String {
        if (peek() != ':') fail("expected a keyword")
        next()
        val start = pos
        while (peek()?.let(::isSymbolChar) == true) next()
        if (pos == start) fail("empty keyword")
        return text.substring(start, pos)
    }

    private fun readValue(): Any {
        val c = peek()!!
        return when {
            c == '"' -> readString()
            c.isDigit() || ((c == '-' || c == '+') && peek(1)?.isDigit() == true) -> readInteger()
            c == '[' -> fail("vectors are not supported")
            c == '(' -> fail("lists are not supported")
            c == '{' -> fail("nested maps are not supported")
            c == '#' -> fail("sets and tagged values are not supported")
            c == ':' -> fail("keyword values are not supported")
            else -> fail("unsupported value starting with '$c'")
        }
    }

    private fun readString(): String {
        val startLine = line
        val startCol = col
        next()
        val sb = StringBuilder()
        while (true) {
            val c = peek() ?: fail("unterminated string", startLine, startCol)
            next()
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    val escLine = line
                    val escCol = col - 1
                    sb.append(
                        when (val e = peek()) {
                            '"' -> '"'
                            '\\' -> '\\'
                            'n' -> '\n'
                            't' -> '\t'
                            'r' -> '\r'
                            null -> fail("unterminated string", startLine, startCol)
                            else -> fail("unsupported escape '\\$e'", escLine, escCol)
                        }
                    )
                    next()
                }
                else -> sb.append(c)
            }
        }
    }

    private fun readInteger(): Long {
        val startLine = line
        val startCol = col
        val start = pos
        next()
        while (peek()?.isDigit() == true) next()
        val c = peek()
        if (c != null && !isDelimiter(c)) fail("invalid number", startLine, startCol)
        return text.substring(start, pos).toLongOrNull() ?: fail("integer out of range", startLine, startCol)
    }

    private fun skipWhitespace() {
        while (true) {
            val c = peek() ?: return
            when {
                c == ';' -> while (peek().let { it != null && it != '\n' }) next()
                c.isWhitespace() || c == ',' -> next()
                else -> return
            }
        }
    }

    private fun isSymbolChar(c: Char) = c.isLetterOrDigit() || c in "*+!-_?<>=./"

    private fun isDelimiter(c: Char) = c.isWhitespace() || c == ',' || c == '}' || c == ';'

    private fun peek(offset: Int = 0): Char? = text.getOrNull(pos + offset)

    private fun next() {
        if (text[pos] == '\n') {
            line++
            col = 1
        } else {
            col++
        }
        pos++
    }

    private fun fail(message: String, atLine: Int = line, atCol: Int = col): Nothing =
        throw EdnException("line $atLine, column $atCol: $message")
}
