/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import teksturepako.pakkuDesktop.app.git.DiffContent
import teksturepako.pakkuDesktop.app.git.DiffHunk
import teksturepako.pakkuDesktop.app.git.DiffLine
import teksturepako.pakkuDesktop.app.git.DiffType

/**
 * Parses unified diff text (as produced by `git diff`) into the structured model consumed by the UI.
 */
object UnifiedDiffParser {

    private val hunkHeader = """^@@\s+-(\d+)(?:,(\d+))?\s+\+(\d+)(?:,(\d+))?\s+@@(.*)$""".toRegex()

    fun parse(unifiedDiff: String, fallbackNewPath: String): DiffContent {
        var oldPath: String? = null
        var newPath = fallbackNewPath
        val hunks = mutableListOf<DiffHunk>()
        var hunkLines = mutableListOf<DiffLine>()
        var hunkHeaderText = ""
        var oldNum = 1
        var newNum = 1

        fun flushHunk() {
            if (hunkLines.isNotEmpty()) {
                hunks += DiffHunk(header = hunkHeaderText, lines = hunkLines.toList())
                hunkLines = mutableListOf()
            }
        }

        for (raw in unifiedDiff.lines()) {
            val line = raw.trimEnd('\r')
            when {
                line.startsWith("--- ") -> {
                    val p = line.removePrefix("--- ").trim()
                    oldPath = when (p) {
                        "/dev/null" -> null
                        else -> p.removePrefix("a/")
                    }
                }
                line.startsWith("+++ ") -> {
                    val p = line.removePrefix("+++ ").trim()
                    newPath = when (p) {
                        "/dev/null" -> fallbackNewPath
                        else -> p.removePrefix("b/")
                    }
                }
                line.startsWith("@@ ") -> {
                    flushHunk()
                    hunkHeaderText = line
                    val m = hunkHeader.find(line)
                    if (m != null) {
                        oldNum = m.groupValues[1].toInt()
                        newNum = m.groupValues[3].toInt()
                    }
                }
                line.startsWith("+") && !line.startsWith("+++") -> {
                    val text = line.drop(1)
                    hunkLines += DiffLine(
                        number = DiffLine.LineNumbers(old = null, new = newNum),
                        content = text,
                        type = DiffType.ADDED,
                    )
                    newNum++
                }
                line.startsWith("-") && !line.startsWith("---") -> {
                    val text = line.drop(1)
                    hunkLines += DiffLine(
                        number = DiffLine.LineNumbers(old = oldNum, new = null),
                        content = text,
                        type = DiffType.DELETED,
                    )
                    oldNum++
                }
                line.startsWith(" ") -> {
                    val text = line.drop(1)
                    hunkLines += DiffLine(
                        number = DiffLine.LineNumbers(old = oldNum, new = newNum),
                        content = text,
                        type = DiffType.UNCHANGED,
                    )
                    oldNum++
                    newNum++
                }
                line.startsWith("\\") -> Unit
                line.isEmpty() && hunkLines.isNotEmpty() -> {
                    hunkLines += DiffLine(
                        number = DiffLine.LineNumbers(old = oldNum, new = newNum),
                        content = "",
                        type = DiffType.UNCHANGED,
                    )
                    oldNum++
                    newNum++
                }
            }
        }
        flushHunk()
        return DiffContent(
            oldPath = oldPath ?: newPath,
            newPath = newPath,
            hunks = hunks,
        )
    }

    /**
     * Appends the enclosing declaration to each hunk header, matching the text Git shows after `@@`.
     * Headers that already carry that text are left as they are.
     */
    fun withEnclosingContext(content: DiffContent, fileLines: List<String>): DiffContent {
        if (fileLines.isEmpty()) return content
        return content.copy(
            hunks = content.hunks.map { hunk ->
                if (alreadyHasCodeContext(hunk.header)) return@map hunk
                val start = hunkStartLine(hunk.header) ?: return@map hunk
                val snippet = enclosingSnippet(fileLines, start) ?: return@map hunk
                hunk.copy(header = hunk.header.trimEnd() + " " + snippet)
            },
        )
    }

    private fun alreadyHasCodeContext(header: String): Boolean {
        val match = hunkHeader.find(header) ?: return true
        return match.groupValues[5].isNotBlank()
    }

    private fun hunkStartLine(header: String): Int? {
        val match = hunkHeader.find(header) ?: return null
        val newStart = match.groupValues[3].toIntOrNull() ?: return null
        val oldStart = match.groupValues[1].toIntOrNull() ?: return null
        return when {
            newStart > 0 -> newStart
            oldStart > 0 -> oldStart
            else -> null
        }
    }

    private fun enclosingSnippet(lines: List<String>, startLine: Int): String? {
        val startIdx = (startLine - 1).coerceIn(0, lines.lastIndex)
        val hunkIndent = indentOf(lines[startIdx])
        var nearest: String? = null
        for (i in startIdx - 1 downTo 0) {
            val trimmed = lines[i].trim()
            if (isSkippableContextLine(trimmed)) continue
            if (nearest == null) nearest = trimmed
            if (hunkIndent == 0 || indentOf(lines[i]) < hunkIndent) return oneLine(trimmed)
        }
        if (nearest != null) return oneLine(nearest)
        for (i in startIdx..lines.lastIndex) {
            val trimmed = lines[i].trim()
            if (!isSkippableContextLine(trimmed)) return oneLine(trimmed)
        }
        return null
    }

    private fun indentOf(line: String): Int {
        var i = 0
        while (i < line.length && (line[i] == ' ' || line[i] == '\t')) i++
        return i
    }

    private fun isSkippableContextLine(trimmed: String): Boolean {
        if (trimmed.isEmpty()) return true
        if (trimmed == "{" || trimmed == "}" || trimmed == "};" || trimmed == ")" || trimmed == "]" || trimmed == "(") {
            return true
        }
        return trimmed.startsWith("//") ||
            trimmed.startsWith("#") ||
            trimmed.startsWith("/*") ||
            trimmed.startsWith("*") ||
            trimmed.startsWith("*/")
    }

    private fun oneLine(trimmed: String): String {
        val collapsed = trimmed.replace(Regex("\\s+"), " ")
        return if (collapsed.length <= 120) collapsed else collapsed.take(117) + "..."
    }
}
