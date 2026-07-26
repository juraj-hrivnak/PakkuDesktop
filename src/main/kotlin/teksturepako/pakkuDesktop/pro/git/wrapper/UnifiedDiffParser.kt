/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.git.wrapper

import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffHunk
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffLine
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffType

/**
 * Parses unified diff text (as produced by `git diff`) into the structured model consumed by the UI.
 */
object UnifiedDiffParser {

    private val hunkHeader = """^@@\s+-(\d+)(?:,(\d+))?\s+\+(\d+)(?:,(\d+))?\s+@@""".toRegex()

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
}
