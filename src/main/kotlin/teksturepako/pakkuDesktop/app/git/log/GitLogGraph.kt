/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 *
 * Portions adapted from JetBrains/intellij-community: platform/vcs-log/graph/src/com/intellij/vcs/log/graph/impl/print/PrintElementGeneratorImpl.kt, ref 85a7e1b862cecbe12e63128e36eec454fa6735f4
 * Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed
 * by the Apache 2.0 license that can be found in the LICENSE file at https://www.apache.org/licenses/LICENSE-2.0.
 * Modified and adapted for Pakku Desktop by Juraj Hrivnák.
 */

package teksturepako.pakkuDesktop.app.git.log

import teksturepako.pakkuDesktop.app.git.GitLogFilters
import teksturepako.pakkuDesktop.app.git.GitRef
import teksturepako.pakkuDesktop.app.git.GitRefType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** One thing drawn in a commit row: the commit itself, or a piece of an edge. */
data class GitLogElement(
    val row: Int,
    val position: Int,
    val node: Boolean,
    /** Column the other end of an edge aims at. Equal to [position] for a node or a terminal arrow. */
    val otherPosition: Int,
    val down: Boolean,
    val arrow: Boolean,
    val terminal: Boolean,
    val dotted: Boolean,
    /** 0 is the theme content color; any other id resolves with [gitLogColor]. */
    val colorId: Int,
    /** Visible row of the edge's upper end, or -1. */
    val edgeUp: Int,
    /** Visible row of the edge's lower end, or -1. */
    val edgeDown: Int,
    /** The commit HEAD points at. */
    val head: Boolean,
)

/** Compact ref label drawn at the right of the subject, IntelliJ's default. */
data class GitLogLabel(
    val text: String,
    /** Tag-icon colors, left to right. A color used more than once is repeated. */
    val colors: List<Int>,
    /** Detached HEAD: a warning glyph instead of the branch icon. */
    val detached: Boolean,
)

/**
 * The Log's graph: layout, the filtered view of it, and the print elements of each row.
 * Built off the UI thread and read from the table.
 */
class GitLogGraph internal constructor(
    val topology: GitLogTopology,
    private val layout: GitLogLayout,
    private val visible: GitLogVisible,
    private val refsAt: Array<List<GitRef>>,
    private val rowOfPermanent: IntArray,
    val headHash: String?,
    private val currentBranch: String?,
    private val upstream: String?,
) {
    val rows: Int get() = visible.size

    /** Text, user or date filter: hovering a line must not light the whole fragment. */
    val hoverEnabled: Boolean get() = !visible.filtered

    val authors: List<String> get() = topology.authorsByCommitCount

    val recommendedWidth: Int by lazy { recommendedWidth() }

    private val pinned: HashSet<Int> = HashSet<Int>().apply {
        for (row in 0 until rows) {
            val node = visible.permanent[row]
            if (refsAt[node].isNotEmpty() || topology.hashes[node] == headHash) add(row)
        }
    }

    private val sortedCache = object : LinkedHashMap<Int, List<RowEl>>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<RowEl>>) = size > CACHE
    }
    private val printCache = object : LinkedHashMap<Int, List<GitLogElement>>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<GitLogElement>>) = size > CACHE
    }

    fun hashAt(row: Int): String = topology.hashes[visible.permanent[row]]

    /** Parent count in the permanent graph, so cherry-pick knows a merge. */
    fun parentCount(row: Int): Int = topology.parentCount(visible.permanent[row])

    fun authorAt(row: Int): String = topology.authors[visible.permanent[row]]

    fun emailAt(row: Int): String = topology.authorEmails[visible.permanent[row]]

    /** Author time, epoch seconds. */
    fun timeAt(row: Int): Long = topology.authorTimes[visible.permanent[row]]

    fun rowOf(hash: String): Int? {
        val node = topology.indexOf(hash) ?: return null
        val row = rowOfPermanent.getOrNull(node) ?: return null
        return row.takeIf { it >= 0 }
    }

    fun labelAt(row: Int): GitLogLabel? = labelFor(visible.permanent[row])

    fun elements(row: Int): List<GitLogElement> = printCache.getOrPut(row) { buildElements(row) }

    /** Columns the graph needs on this row, counting the reserved width IntelliJ keeps free. */
    fun columns(row: Int): Double {
        var maxIndex = 0.0
        for (element in elements(row)) {
            maxIndex = max(maxIndex, element.position.toDouble())
            if (!element.node && !element.terminal) {
                maxIndex = max(maxIndex, (element.position + element.otherPosition) / 2.0)
            }
        }
        return max(maxIndex + 1, min(recommendedWidth.toDouble(), RESERVED_COLUMNS))
    }

    /**
     * Visible rows lit while [element] is hovered. Empty when the element starts no fragment,
     * which is also the case whenever a text, user or date filter is on.
     */
    fun hover(element: GitLogElement): Set<Int> {
        if (!hoverEnabled) return emptySet()
        val fragment = partLongFragment(element) ?: return emptySet()
        return middleNodes(fragment.first, fragment.second)
    }

    /** Row a click on an edge or its arrow jumps to; null for a commit dot. */
    fun clickTarget(element: GitLogElement): Int? {
        if (element.node) return null
        val target = if (element.down) element.edgeDown else element.edgeUp
        return target.takeIf { it >= 0 }
    }

    /** First parent that is still in the table, else the dotted edge's lower end. */
    fun parentRow(row: Int): Int? {
        val node = visible.permanent[row]
        for (p in 0 until topology.parentCount(node)) {
            val parent = rowOfPermanent[topology.parent(node, p)]
            if (parent >= 0) return parent
        }
        return visible.downFrom[row].map { visible.edges[it].down }.minOrNull()
    }

    /** Newest child that is still in the table, else the dotted edge's upper end. */
    fun childRow(row: Int): Int? {
        val node = visible.permanent[row]
        var child = -1
        for (c in 0 until topology.childCount(node)) {
            val candidate = rowOfPermanent[topology.child(node, c)]
            if (candidate >= 0 && (child < 0 || candidate < child)) child = candidate
        }
        if (child >= 0) return child
        return visible.upInto[row].map { visible.edges[it].up }.minOrNull()
    }

    private fun labelFor(node: Int): GitLogLabel? {
        val refs = refsAt[node]
        val headHere = topology.hashes[node] == headHash
        if (refs.isEmpty() && !headHere) return null
        val detached = headHere && currentBranch == null
        val groups = labelGroups(refs, currentBranch, upstream)
        val text = when {
            detached -> "HEAD"
            groups.isEmpty() -> ""
            else -> {
                // Build text from all groups: branch names first, then tag names
                val parts = ArrayList<String>()
                for (group in groups) {
                    if (!group.tagsOnly) {
                        parts.add(shortenRef(group.name))
                    }
                }
                for (group in groups) {
                    if (group.tagsOnly) {
                        parts.add(shortenRef(group.name))
                    }
                }
                parts.joinToString(", ")
            }
        }
        val colors = ArrayList<Int>(4)
        if (headHere) colors.add(REF_HEAD)
        for (group in groups) colors.addAll(group.colors)
        val shown = distinctRepeating(colors)
        if (text.isEmpty() && shown.isEmpty()) return null
        return GitLogLabel(text, shown, detached)
    }

    private fun buildElements(row: Int): List<GitLogElement> {
        val sorted = sortedElements(row)
        val result = ArrayList<GitLogElement>(sorted.size * 2)
        val nodeElements = ArrayList<GitLogElement>(1)
        sorted.forEachIndexed { position, element ->
            when (element) {
                is RowEl.Node -> {
                    val node = visible.permanent[element.index]
                    nodeElements.add(
                        GitLogElement(
                            row = row,
                            position = position,
                            node = true,
                            otherPosition = position,
                            down = false,
                            arrow = false,
                            terminal = false,
                            dotted = false,
                            colorId = layout.colorId(node),
                            edgeUp = -1,
                            edgeDown = -1,
                            head = topology.hashes[node] == headHash,
                        ),
                    )
                    for (id in adjacent(element.index)) {
                        val edge = visible.edges[id]
                        val arrow = arrowAt(edge, row)
                        val down = endPosition(row + 1, id, up = false)
                        val up = endPosition(row - 1, id, up = true)
                        if (down != null) result.add(edgeElement(row, position, down, down = true, arrow == Arrow.Down, false, edge))
                        if (up != null) result.add(edgeElement(row, position, up, down = false, arrow == Arrow.Up, false, edge))
                    }
                }
                is RowEl.Edge -> {
                    val edge = visible.edges[element.id]
                    val arrow = arrowAt(edge, row)
                    val down = endPosition(row + 1, element.id, up = false)
                    val up = endPosition(row - 1, element.id, up = true)
                    if (down != null) result.add(edgeElement(row, position, down, true, arrow == Arrow.Down, false, edge))
                    else if (arrow == Arrow.Down) result.add(edgeElement(row, position, position, true, true, true, edge))
                    if (up != null) result.add(edgeElement(row, position, up, false, arrow == Arrow.Up, false, edge))
                    else if (arrow == Arrow.Up) result.add(edgeElement(row, position, position, false, true, true, edge))
                }
            }
        }
        result.addAll(nodeElements)
        return result
    }

    private fun edgeElement(
        row: Int,
        position: Int,
        other: Int,
        down: Boolean,
        arrow: Boolean,
        terminal: Boolean,
        edge: VEdge,
    ) = GitLogElement(
        row = row,
        position = position,
        node = false,
        otherPosition = other,
        down = down,
        arrow = arrow,
        terminal = terminal,
        dotted = edge.dotted,
        colorId = layout.edgeColorId(visible.permanent[edge.up], visible.permanent[edge.down]),
        edgeUp = edge.up,
        edgeDown = edge.down,
        head = false,
    )

    private fun sortedElements(row: Int): List<RowEl> = sortedCache.getOrPut(row) {
        val list = ArrayList<RowEl>()
        list.add(RowEl.Node(row))
        for (id in visible.edgesThrough(row)) {
            if (edgeVisible(visible.edges[id], row)) list.add(RowEl.Edge(id))
        }
        list.sortWith { a, b -> compareElements(a, b) }
        list
    }

    /** Column of [edgeId] in [target], or of the node at the end the edge is heading toward. */
    private fun endPosition(target: Int, edgeId: Int, up: Boolean): Int? {
        if (target !in 0 until rows) return null
        val elements = sortedElements(target)
        val atEdge = elements.indexOfFirst { it is RowEl.Edge && it.id == edgeId }
        if (atEdge >= 0) return atEdge
        val node = if (up) visible.edges[edgeId].up else visible.edges[edgeId].down
        if (node != target) return null
        val atNode = elements.indexOfFirst { it is RowEl.Node && it.index == node }
        return atNode.takeIf { it >= 0 }
    }

    private fun adjacent(row: Int): IntArray {
        val down = visible.downFrom[row]
        val up = visible.upInto[row]
        if (up.isEmpty()) return down
        if (down.isEmpty()) return up
        return down + up
    }

    private fun edgeVisible(edge: VEdge, row: Int): Boolean {
        val size = edge.down - edge.up
        if (size < LONG_EDGE) return true
        return min(row - edge.up, edge.down - row) <= VISIBLE_PART
    }

    private fun arrowAt(edge: VEdge, row: Int): Arrow? {
        val size = edge.down - edge.up
        if (size < LONG_EDGE) return null
        val upOffset = row - edge.up
        val downOffset = edge.down - row
        if (upOffset == VISIBLE_PART) return Arrow.Down
        if (downOffset == VISIBLE_PART) return Arrow.Up
        return null
    }

    private fun li(row: Int): Int = layout.layoutIndex(visible.permanent[row])

    /** `GraphElementComparatorByLayoutIndex`. */
    private fun compareElements(a: RowEl, b: RowEl): Int = when {
        a is RowEl.Edge && b is RowEl.Edge -> compareEdges(a, b)
        a is RowEl.Edge && b is RowEl.Node -> compareEdgeToNode(a.id, b.index)
        a is RowEl.Node && b is RowEl.Edge -> -compareEdgeToNode(b.id, a.index)
        else -> 0
    }

    private fun compareEdges(a: RowEl.Edge, b: RowEl.Edge): Int {
        val ea = visible.edges[a.id]
        val eb = visible.edges[b.id]
        if (ea.up == eb.up) {
            return if (ea.down < eb.down) -compareEdgeToNode(b.id, ea.down) else compareEdgeToNode(a.id, eb.down)
        }
        return if (ea.up < eb.up) compareEdgeToNode(a.id, eb.up) else -compareEdgeToNode(b.id, ea.up)
    }

    private fun compareEdgeToNode(edgeId: Int, node: Int): Int {
        val edge = visible.edges[edgeId]
        val upLi = li(edge.up)
        val downLi = li(edge.down)
        val nodeLi = li(node)
        val maxLi = max(upLi, downLi)
        if (maxLi != nodeLi) return maxLi - nodeLi
        return edge.up - node
    }

    /** Weighted average width of the first rows, plus one deviation. IntelliJ's recommended width. */
    private fun recommendedWidth(): Int {
        val n = rows
        if (n == 0) return 0
        if (n == 1) return 1
        val sample = min(SAMPLE, n)
        var sum = 0.0
        var sumSquares = 0.0
        var edgesCount = 0
        val current = HashSet<Int>()
        for (i in 0 until sample) {
            var upArrows = 0
            var downArrows = 0
            for (id in adjacent(i)) {
                val edge = visible.edges[id]
                if (edge.down == i) current.remove(id) else current.add(id)
            }
            var fresh = 0
            for (id in current) {
                val edge = visible.edges[id]
                if (edgeVisible(edge, i)) fresh++
                else when (arrowAt(edge, i)) {
                    Arrow.Down -> downArrows++
                    Arrow.Up -> upArrows++
                    null -> {}
                }
            }
            val width = max(edgesCount + upArrows, fresh + downArrows)
            val weight = 2.0 / (sample * (WIDTH_K + 1)) * (1 + (WIDTH_K - 1) * i / (sample - 1))
            sum += width * weight
            sumSquares += width.toDouble() * width.toDouble() * weight
            edgesCount = fresh
        }
        val deviation = sqrt(sumSquares - sum * sum)
        return (sum + deviation).let { kotlin.math.round(it).toInt() }
    }

    /** `LinearFragmentGenerator.getPartLongFragment`, bounded to 500 rows past the start. */
    private fun partLongFragment(element: GitLogElement): Pair<Int, Int>? {
        val relative = relativeFragment(element) ?: return null
        var down = relative.second
        while (down !in pinned) {
            val next = shortFragment(down, downward = true) ?: break
            down = next.second
            if (down - relative.second > FRAGMENT_BOUND) break
        }
        var up = relative.first
        while (up !in pinned) {
            val next = shortFragment(up, downward = false) ?: break
            up = next.first
            if (relative.first - up > FRAGMENT_BOUND) break
        }
        if (up != relative.first || down != relative.second) return up to down
        if (downNeighbors(relative.first).size != 1) return relative
        return null
    }

    private fun relativeFragment(element: GitLogElement): Pair<Int, Int>? {
        var up: Int
        var down: Int
        if (element.node) {
            up = element.row
            down = element.row
        } else {
            up = element.edgeUp
            down = element.edgeDown
            if (up < 0 || down < 0) return null
        }
        repeat(MAX_SEARCH) {
            val fragment = shortFragment(up, downward = true)
            if (fragment != null && fragment.second >= down) return fragment
            val parents = upNeighbors(up)
            if (parents.size != 1) return null
            up = parents[0]
        }
        return null
    }

    /** `LinearFragmentGenerator.getFragment`: a straight run no longer than ten commits. */
    private fun shortFragment(start: Int, downward: Boolean): Pair<Int, Int>? {
        val black = HashSet<Int>()
        black.add(start)
        val gray = HashSet(if (downward) downNeighbors(start) else upNeighbors(start))
        var end = -1
        while (black.size < SHORT_FRAGMENT) {
            var next = -1
            for (candidate in gray.sorted()) {
                val previous = if (downward) upNeighbors(candidate) else downNeighbors(candidate)
                if (black.containsAll(previous)) {
                    next = candidate
                    break
                }
            }
            if (next == -1) return null
            if (gray.size == 1) {
                end = next
                break
            }
            val onward = if (downward) downNeighbors(next) else upNeighbors(next)
            if (onward.isEmpty() || next in pinned) return null
            black.add(next)
            gray.remove(next)
            gray.addAll(onward)
        }
        if (end == -1) return null
        return if (downward) start to end else end to start
    }

    /** Commits on every path between [up] and [down], both ends included. */
    private fun middleNodes(up: Int, down: Int): Set<Int> {
        val downWalk = walk(up, downward = true) { it > down }
        val upWalk = walk(down, downward = false) { it < up }
        downWalk.retainAll(upWalk)
        return downWalk
    }

    private fun walk(start: Int, downward: Boolean, stop: (Int) -> Boolean): HashSet<Int> {
        val seen = HashSet<Int>()
        val stack = ArrayDeque<Int>()
        stack.add(start)
        while (stack.isNotEmpty()) {
            val next = stack.removeLast()
            if (stop(next) || !seen.add(next)) continue
            stack.addAll(if (downward) downNeighbors(next) else upNeighbors(next))
        }
        return seen
    }

    private fun downNeighbors(row: Int): List<Int> = visible.downFrom[row].map { visible.edges[it].down }

    private fun upNeighbors(row: Int): List<Int> = visible.upInto[row].map { visible.edges[it].up }

    private sealed interface RowEl {
        data class Node(val index: Int) : RowEl
        data class Edge(val id: Int) : RowEl
    }

    private enum class Arrow { Up, Down }

    companion object {
        private const val LONG_EDGE = 30
        private const val VISIBLE_PART = 1
        private const val SAMPLE = 20_000
        private const val WIDTH_K = 0.1
        private const val RESERVED_COLUMNS = 6.0
        private const val CACHE = 200
        private const val SHORT_FRAGMENT = 10
        private const val MAX_SEARCH = 10
        private const val FRAGMENT_BOUND = 500

        fun build(
            topology: GitLogTopology,
            refs: List<GitRef>,
            headHash: String?,
            currentBranch: String?,
            upstream: String?,
            filters: GitLogFilters,
            textMatches: Set<String>?,
        ): GitLogGraph {
            val layout = GitLogLayout.build(topology, refs, headHash)
            val visible = buildVisible(topology, refs, filters, textMatches)
            val refsAt = Array(topology.size) { emptyList<GitRef>() }
            val grouped = refs.groupBy { topology.indexOf(it.hash) }
            for ((node, at) in grouped) if (node != null) refsAt[node] = at
            val rowOf = IntArray(topology.size) { -1 }
            visible.permanent.forEachIndexed { row, node -> rowOf[node] = row }
            return GitLogGraph(topology, layout, visible, refsAt, rowOf, headHash, currentBranch, upstream)
        }
    }
}

private class LabelGroup(val name: String, val colors: List<Int>, val tagsOnly: Boolean)

/**
 * Groups drawn in one compact label. A local branch and the remote it tracks become
 * `origin & main`. The current branch leads when it is not part of such a pair; tags alone
 * contribute no name.
 */
private fun labelGroups(refs: List<GitRef>, currentBranch: String?, upstream: String?): List<LabelGroup> {
    val locals = refs.filter { it.type == GitRefType.LocalBranch }
        .sortedWith { a, b -> compareLocals(a, b, currentBranch) }
    val remotes = refs.filter { it.type == GitRefType.RemoteBranch }
    val consumed = HashSet<GitRef>()
    val pairs = ArrayList<LabelGroup>()
    for (local in locals) {
        val match = remotes.filter { it !in consumed && tracks(local, it, currentBranch, upstream) }
            .minWithOrNull { a, b -> preferredRemote(a, b, local, currentBranch, upstream) }
        if (match != null) {
            consumed.add(local)
            consumed.add(match)
            val remoteName = match.name.removeSuffix("/${local.name}")
            pairs.add(LabelGroup("$remoteName & ${local.name}", listOf(REF_LOCAL, REF_REMOTE), tagsOnly = false))
        }
    }
    val ordered = ArrayList<LabelGroup>()
    val current = locals.firstOrNull { it.name == currentBranch && it !in consumed }
    if (current != null) {
        consumed.add(current)
        ordered.add(LabelGroup(current.name, listOf(REF_LOCAL), tagsOnly = false))
    }
    ordered.addAll(pairs)
    val otherLocals = locals.filter { it !in consumed }
    if (otherLocals.isNotEmpty()) {
        ordered.add(LabelGroup(otherLocals.first().name, List(otherLocals.size) { REF_LOCAL }, tagsOnly = false))
    }
    val otherRemotes = remotes.filter { it !in consumed }.sortedWith { a, b -> naturalCompare(a.name, b.name) }
    if (otherRemotes.isNotEmpty()) {
        ordered.add(LabelGroup(otherRemotes.first().name, List(otherRemotes.size) { REF_REMOTE }, tagsOnly = false))
    }
    val tags = refs.filter { it.type == GitRefType.Tag }
        .sortedWith(compareByDescending<GitRef> { it.createdAt ?: kotlin.time.Instant.DISTANT_PAST }.thenBy { it.name })
    if (tags.isNotEmpty()) {
        val tagNames = tags.joinToString(", ") { it.name }
        ordered.add(LabelGroup(tagNames, List(tags.size) { REF_TAG }, tagsOnly = true))
    }
    return ordered
}

private fun compareLocals(a: GitRef, b: GitRef, currentBranch: String?): Int {
    fun rank(ref: GitRef) = when {
        ref.name == currentBranch -> 0
        ref.name == "main" || ref.name == "master" -> 1
        else -> 2
    }
    val byRank = rank(a) - rank(b)
    return if (byRank != 0) byRank else naturalCompare(a.name, b.name)
}

/** `origin/main` tracks `main`; `origin/feature/main` does not. The current upstream wins ties. */
private fun tracks(local: GitRef, remote: GitRef, currentBranch: String?, upstream: String?): Boolean {
    if (local.name == currentBranch && upstream != null && remote.name == upstream) return true
    return remote.name.substringAfter('/', "") == local.name
}

private fun preferredRemote(a: GitRef, b: GitRef, local: GitRef, currentBranch: String?, upstream: String?): Int {
    fun rank(ref: GitRef) = if (local.name == currentBranch && ref.name == upstream) 0 else 1
    val byRank = rank(a) - rank(b)
    return if (byRank != 0) byRank else naturalCompare(a.name, b.name)
}

/** A color used once stays once; a color used more than once is drawn twice. */
private fun distinctRepeating(colors: List<Int>): List<Int> {
    val counts = LinkedHashMap<Int, Int>()
    for (color in colors) counts[color] = (counts[color] ?: 0) + 1
    val result = ArrayList<Int>(counts.size * 2)
    for ((color, count) in counts) {
        result.add(color)
        if (count > 1) result.add(color)
    }
    return result
}

private fun shortenRef(name: String): String {
    if (name.length <= MAX_REF_LENGTH) return name
    val separator = name.indexOf('/')
    val shortened = if (separator > "..".length) ".." + name.substring(separator) else name
    if (shortened.length <= MAX_REF_LENGTH) return shortened
    return shortened.take(MAX_REF_LENGTH - 1) + "…"
}

internal const val REF_HEAD = 0xFFE1C731.toInt()
internal const val REF_LOCAL = 0xFF3CB45C.toInt()
internal const val REF_REMOTE = 0xFF9F79B5.toInt()
internal const val REF_TAG = 0xFF999999.toInt()

private const val MAX_REF_LENGTH = 22
