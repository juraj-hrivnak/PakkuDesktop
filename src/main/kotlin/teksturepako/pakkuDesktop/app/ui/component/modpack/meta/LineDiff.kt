/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import teksturepako.pakkuDesktop.app.git.DiffContent
import teksturepako.pakkuDesktop.app.git.DiffHunk
import teksturepako.pakkuDesktop.app.git.DiffLine
import teksturepako.pakkuDesktop.app.git.DiffType
import kotlin.math.max
import kotlin.math.min

/**
 * Line-based unified diff (Myers) between two texts, as the [DiffContent] the Git diff viewer
 * renders. A moved line shows as a deletion plus an addition.
 */
object LineDiff
{
    const val DEFAULT_CONTEXT = 3

    /**
     * Above this many trace cells, the changed middle is reported as one replacement instead.
     * Edits made in the app are local, so the common prefix and suffix are trimmed first.
     */
    private const val MAX_TRACE_CELLS = 4_000_000L

    fun unified(old: String, new: String, path: String, context: Int = DEFAULT_CONTEXT): DiffContent =
        DiffContent(oldPath = path, newPath = path, hunks = hunks(lines(old), lines(new), context))

    private fun lines(text: String): List<String> =
        if (text.isEmpty()) emptyList() else text.removeSuffix("\n").split('\n').map { it.removeSuffix("\r") }

    private fun hunks(a: List<String>, b: List<String>, context: Int): List<DiffHunk>
    {
        val script = editScript(a, b)
        val hunks = mutableListOf<DiffHunk>()
        var i = 0
        while (i < script.size) {
            while (i < script.size && script[i].type == DiffType.UNCHANGED) i++
            if (i >= script.size) break

            val start = max(0, i - context)
            var end = i
            var j = i
            while (j < script.size) {
                if (script[j].type != DiffType.UNCHANGED) {
                    end = ++j
                    continue
                }
                var run = j
                while (run < script.size && script[run].type == DiffType.UNCHANGED) run++
                // Join the next change when the unchanged gap fits in both contexts.
                if (run < script.size && run - j <= 2 * context) j = run else break
            }
            val stop = min(script.size, end + context)
            hunks += hunkOf(script.subList(start, stop), script.subList(0, start))
            i = stop
        }
        return hunks
    }

    private fun hunkOf(lines: List<DiffLine>, before: List<DiffLine>): DiffHunk
    {
        val oldCount = lines.count { it.number.old != null }
        val newCount = lines.count { it.number.new != null }
        val oldStart = lines.firstNotNullOfOrNull { it.number.old } ?: before.count { it.number.old != null }
        val newStart = lines.firstNotNullOfOrNull { it.number.new } ?: before.count { it.number.new != null }
        return DiffHunk(header = "@@ -$oldStart,$oldCount +$newStart,$newCount @@", lines = lines)
    }

    /** Every line of [a] and [b] in order, numbered from 1. */
    private fun editScript(a: List<String>, b: List<String>): List<DiffLine>
    {
        var prefix = 0
        while (prefix < a.size && prefix < b.size && a[prefix] == b[prefix]) prefix++
        var suffix = 0
        while (suffix < a.size - prefix && suffix < b.size - prefix && a[a.size - 1 - suffix] == b[b.size - 1 - suffix]) {
            suffix++
        }

        val script = ArrayList<DiffLine>(a.size + b.size)
        for (k in 0 until prefix) script += unchanged(a[k], k, k)
        middle(a.subList(prefix, a.size - suffix), b.subList(prefix, b.size - suffix), prefix, prefix, script)
        for (k in suffix downTo 1) script += unchanged(a[a.size - k], a.size - k, b.size - k)
        return script
    }

    private fun middle(a: List<String>, b: List<String>, oldOffset: Int, newOffset: Int, out: MutableList<DiffLine>)
    {
        val ops = myers(a, b)
        if (ops == null) {
            a.forEachIndexed { k, line -> out += deleted(line, oldOffset + k) }
            b.forEachIndexed { k, line -> out += added(line, newOffset + k) }
            return
        }
        for (op in ops) {
            out += when (op.type) {
                DiffType.UNCHANGED -> unchanged(a[op.x], oldOffset + op.x, newOffset + op.y)
                DiffType.DELETED -> deleted(a[op.x], oldOffset + op.x)
                DiffType.ADDED -> added(b[op.y], newOffset + op.y)
            }
        }
    }

    private class Op(val type: DiffType, val x: Int, val y: Int)

    /** Shortest edit script, or null when it would need more than [MAX_TRACE_CELLS] of trace. */
    private fun myers(a: List<String>, b: List<String>): List<Op>?
    {
        val n = a.size
        val m = b.size
        val max = n + m
        if (max == 0) return emptyList()
        val offset = max + 1
        val width = 2 * max + 3
        val v = IntArray(width)
        val trace = ArrayList<IntArray>()

        var found = -1
        for (d in 0..max) {
            if ((d + 1).toLong() * width > MAX_TRACE_CELLS) return null
            trace += v.copyOf()
            var k = -d
            while (k <= d) {
                var x = if (k == -d || (k != d && v[offset + k - 1] < v[offset + k + 1])) {
                    v[offset + k + 1]
                } else {
                    v[offset + k - 1] + 1
                }
                var y = x - k
                while (x < n && y < m && a[x] == b[y]) {
                    x++
                    y++
                }
                v[offset + k] = x
                if (x >= n && y >= m) {
                    found = d
                    break
                }
                k += 2
            }
            if (found >= 0) break
        }

        val ops = ArrayList<Op>(max)
        var x = n
        var y = m
        for (d in found downTo 0) {
            val previous = trace[d]
            val k = x - y
            val prevK = if (k == -d || (k != d && previous[offset + k - 1] < previous[offset + k + 1])) k + 1 else k - 1
            val prevX = previous[offset + prevK]
            val prevY = prevX - prevK
            while (x > prevX && y > prevY) {
                x--
                y--
                ops += Op(DiffType.UNCHANGED, x, y)
            }
            if (d > 0) {
                if (x == prevX) ops += Op(DiffType.ADDED, x, prevY) else ops += Op(DiffType.DELETED, prevX, y)
            }
            x = prevX
            y = prevY
        }
        ops.reverse()
        return ops
    }

    private fun unchanged(text: String, oldIndex: Int, newIndex: Int) =
        DiffLine(DiffLine.LineNumbers(old = oldIndex + 1, new = newIndex + 1), text, DiffType.UNCHANGED)

    private fun deleted(text: String, oldIndex: Int) =
        DiffLine(DiffLine.LineNumbers(old = oldIndex + 1, new = null), text, DiffType.DELETED)

    private fun added(text: String, newIndex: Int) =
        DiffLine(DiffLine.LineNumbers(old = null, new = newIndex + 1), text, DiffType.ADDED)
}
