/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package teksturepako.pakkuDesktop.app.plugins

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

class EdnException(message: String) : IllegalArgumentException(message)

/**
 * Decodes a flat EDN map into kotlinx.serialization types. Encoding is not supported.
 */
object Edn : kotlinx.serialization.StringFormat {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    /** Keyword names (without `:`) skipped during the last [decodeFromString] call. */
    var unknownKeysFromLastDecode: Set<String> = emptySet()
        private set

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        unknownKeysFromLastDecode = emptySet()
        val unknown = mutableSetOf<String>()
        val lexer = EdnLexer(string)
        val decoder = EdnMapDecoder(lexer, unknown)
        return try {
            decoder.decodeSerializableValue(deserializer).also {
                unknownKeysFromLastDecode = unknown.toSet()
            }
        } catch (e: SerializationException) {
            throw e.cause?.takeIf { it is EdnException || it is IllegalArgumentException } ?: e
        }
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String =
        throw UnsupportedOperationException()
}

private class EdnMapDecoder(
    private val lexer: EdnLexer,
    private val unknownKeys: MutableSet<String>,
) : AbstractDecoder(), CompositeDecoder {

    override val serializersModule: SerializersModule = EmptySerializersModule()

    private var currentKey: String? = null
    private val decodedIndices = mutableSetOf<Int>()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        require(descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT) {
            "only class-like descriptors are supported"
        }
        while (true) {
            lexer.skipWhitespace()
            when (lexer.peek()) {
                null -> lexer.fail("unterminated map")
                '}' -> {
                    lexer.next()
                    lexer.skipWhitespace()
                    if (lexer.peek() != null) lexer.fail("unexpected content after map")
                    return CompositeDecoder.DECODE_DONE
                }
            }
            val keyLine = lexer.line
            val keyCol = lexer.col
            val key = lexer.readKeyword()
            lexer.skipWhitespace()
            if (lexer.peek() == null || lexer.peek() == '}') lexer.fail("missing value for :$key")

            when (val index = descriptor.getElementIndex(key)) {
                CompositeDecoder.UNKNOWN_NAME -> {
                    lexer.readAndDiscardValue()
                    unknownKeys.add(key)
                }
                else -> {
                    if (index in decodedIndices) lexer.fail("duplicate key :$key", keyLine, keyCol)
                    decodedIndices.add(index)
                    currentKey = key
                    return index
                }
            }
        }
    }

    override fun decodeString(): String = lexer.readStringValue(currentKey)

    override fun decodeInt(): Int {
        val long = lexer.readIntegerValue(currentKey)
        return long.toIntExactOrNull(currentKey) ?: lexer.fail("integer out of range")
    }

    override fun decodeLong(): Long = lexer.readIntegerValue(currentKey)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        lexer.skipWhitespace()
        if (lexer.peek() != '{') lexer.fail("expected '{'")
        lexer.next()
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // Top-level map closing is handled in decodeElementIndex.
    }

    override fun decodeNotNullMark(): Boolean = true

    override fun decodeNull(): Nothing = error("null values are not supported")
}

private class EdnLexer(private val text: String) {
    var pos = 0
        private set
    var line = 1
        private set
    var col = 1
        private set

    fun peek(offset: Int = 0): Char? = text.getOrNull(pos + offset)

    fun next() {
        if (text[pos] == '\n') {
            line++
            col = 1
        } else {
            col++
        }
        pos++
    }

    fun fail(message: String, atLine: Int = line, atCol: Int = col): Nothing =
        throw EdnException("line $atLine, column $atCol: $message")

    fun skipWhitespace() {
        while (true) {
            val c = peek() ?: return
            when {
                c == ';' -> while (peek().let { it != null && it != '\n' }) next()
                c.isWhitespace() || c == ',' -> next()
                else -> return
            }
        }
    }

    fun readKeyword(): String {
        if (peek() != ':') fail("expected a keyword")
        next()
        val start = pos
        while (peek()?.let(::isSymbolChar) == true) next()
        if (pos == start) fail("empty keyword")
        return text.substring(start, pos)
    }

    fun readAndDiscardValue() {
        when (val c = peek() ?: fail("missing value")) {
            '"' -> readString()
            in '0'..'9', '-', '+' -> readIntegerRaw()
            '[' -> fail("vectors are not supported")
            '(' -> fail("lists are not supported")
            '{' -> fail("nested maps are not supported")
            '#' -> fail("sets and tagged values are not supported")
            ':' -> fail("keyword values are not supported")
            else -> fail("unsupported value starting with '$c'")
        }
    }

    fun readStringValue(elementKey: String?): String {
        if (peek() != '"') {
            if (peek()?.let { it.isDigit() || it == '-' || it == '+' } == true) {
                readIntegerRaw()
                throw IllegalArgumentException(":$elementKey must be a string")
            }
            readAndDiscardValue()
            error("unreachable")
        }
        return readString()
    }

    fun readIntegerValue(elementKey: String?): Long {
        val c = peek() ?: fail("missing value")
        if (c == '"') {
            readString()
            throw IllegalArgumentException(":$elementKey must be an integer")
        }
        if (c.isDigit() || c == '-' || c == '+') return readIntegerRaw()
        readAndDiscardValue()
        error("unreachable")
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
                        },
                    )
                    next()
                }
                else -> sb.append(c)
            }
        }
    }

    private fun readIntegerRaw(): Long {
        val startLine = line
        val startCol = col
        val start = pos
        next()
        while (peek()?.isDigit() == true) next()
        val c = peek()
        if (c != null && !isDelimiter(c)) fail("invalid number", startLine, startCol)
        return text.substring(start, pos).toLongOrNull() ?: fail("integer out of range", startLine, startCol)
    }

    private fun isSymbolChar(c: Char) = c.isLetterOrDigit() || c in "*+!-_?<>=./"

    private fun isDelimiter(c: Char) = c.isWhitespace() || c == ',' || c == '}' || c == ';'
}

private fun Long.toIntExactOrNull(elementKey: String?): Int? =
    if (this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) toInt() else null
