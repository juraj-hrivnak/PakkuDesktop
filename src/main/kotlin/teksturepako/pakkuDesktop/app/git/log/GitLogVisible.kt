/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 *
 * Portions adapted from JetBrains/intellij-community: platform/vcs-log/graph/src/com/intellij/vcs/log/graph/collapsing/DottedFilterEdgesGenerator.kt, ref 85a7e1b862cecbe12e63128e36eec454fa6735f4
 * Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed
 * by the Apache 2.0 license that can be found in the LICENSE file at https://www.apache.org/licenses/LICENSE-2.0.
 * Modified and adapted for Pakku Desktop by Juraj Hrivnák.
 */

package teksturepako.pakkuDesktop.app.git.log

import teksturepako.pakkuDesktop.app.git.GitLogFilters
import teksturepako.pakkuDesktop.app.git.GitRef
import java.util.BitSet
import kotlin.math.max
import kotlin.math.min

/** One edge of the filtered graph. [up] and [down] are visible-row indices, [up] above [down]. */
internal class VEdge(val up: Int, val down: Int, val dotted: Boolean)

/**
 * The graph the table draws: commits that pass the branch filter and, when a text, user or date
 * filter is on, only the matching ones. Hidden stretches become dotted edges, the way IntelliJ's
 * `FilteredController` draws them.
 */
internal class GitLogVisible(
    val permanent: IntArray,
    val edges: List<VEdge>,
    /** Text, user or date filter is on, so hover must not highlight a fragment. */
    val filtered: Boolean,
) {
    val size: Int get() = permanent.size

    /** Edge indices leaving a row downward (this row is the up end). */
    val downFrom: Array<IntArray>
    /** Edge indices arriving at a row from above (this row is the down end). */
    val upInto: Array<IntArray>

    init {
        val down = Array(size) { ArrayList<Int>(1) }
        val up = Array(size) { ArrayList<Int>(1) }
        edges.forEachIndexed { index, edge ->
            if (edge.up in 0 until size) down[edge.up].add(index)
            if (edge.down in 0 until size) up[edge.down].add(index)
        }
        downFrom = Array(size) { down[it].toIntArray() }
        upInto = Array(size) { up[it].toIntArray() }
    }

    /** Edges that pass through [row] without ending on it: `up < row < down`. */
    fun edgesThrough(row: Int): IntArray {
        val block = row / BLOCK * BLOCK
        var ids = blockCache.getOrPut(block) { passing(block) }
        if (row == block) return ids
        val set = HashSet<Int>(ids.size * 2 + 4)
        ids.forEach { set.add(it) }
        for (r in block until row) {
            for (e in downFrom[r]) set.add(e)
            val next = r + 1
            if (next < size) for (e in upInto[next]) set.remove(e)
        }
        return set.toIntArray()
    }

    private val blockCache = HashMap<Int, IntArray>()

    /** Edges crossing the boundary above [block], computed from a checkpoint [WALK] rows earlier. */
    private fun passing(block: Int): IntArray {
        val start = max(0, block - WALK)
        val set = HashSet<Int>()
        for (r in start until block) {
            for (e in downFrom[r]) set.add(e)
            val next = r + 1
            if (next < size) for (e in upInto[next]) set.remove(e)
        }
        return set.toIntArray()
    }

    private companion object {
        const val BLOCK = 40
        const val WALK = 1000
    }
}

internal fun buildVisible(
    topology: GitLogTopology,
    refs: List<GitRef>,
    filters: GitLogFilters,
    textMatches: Set<String>?,
): GitLogVisible {
    val n = topology.size
    val shown = BitSet(n)
    if (n == 0) return GitLogVisible(IntArray(0), emptyList(), filtered = false)

    val reachable = reachableFrom(topology, branchTips(topology, refs, filters))
    val textActive = filters.text.isNotBlank()
    val userActive = filters.users.isNotEmpty()
    val dateActive = filters.date != null
    val filtered = textActive || userActive || dateActive

    val textHits = if (!textActive) null else {
        val hits = BitSet(n)
        textMatches?.forEach { hash -> topology.indexOf(hash)?.let { hits.set(it) } }
        hits
    }
    val users = if (userActive) filters.users else null
    val since = filters.date?.since?.epochSeconds
    val until = filters.date?.until?.epochSeconds

    val permanent = ArrayList<Int>(n)
    for (node in 0 until n) {
        if (!reachable.get(node)) continue
        if (textHits != null && !textHits.get(node)) continue
        if (users != null && topology.authors[node] !in users) continue
        if (since != null && topology.authorTimes[node] < since) continue
        if (until != null && topology.authorTimes[node] > until) continue
        shown.set(node)
        permanent.add(node)
    }
    val rows = IntArray(n) { -1 }
    permanent.forEachIndexed { row, node -> rows[node] = row }
    val nodes = permanent.toIntArray()

    val solid = ArrayList<VEdge>()
    val solidPairs = HashSet<Long>()
    for (row in nodes.indices) {
        val node = nodes[row]
        for (p in 0 until topology.parentCount(node)) {
            val parentRow = rows[topology.parent(node, p)]
            if (parentRow < 0) continue
            solid.add(VEdge(row, parentRow, dotted = false))
            solidPairs.add(pairKey(row, parentRow))
        }
    }
    if (!filtered) return GitLogVisible(nodes, solid, filtered = false)

    val dotted = ArrayList<VEdge>()
    for ((upNode, downNode) in dottedPairs(topology, shown)) {
        val up = rows[upNode]
        val down = rows[downNode]
        if (up < 0 || down < 0 || up == down) continue
        val key = pairKey(up, down)
        if (!solidPairs.add(key)) continue
        dotted.add(VEdge(min(up, down), max(up, down), dotted = true))
    }
    return GitLogVisible(nodes, solid + dotted, filtered = true)
}

/** Null means every commit; an empty array means the branch filter matched no ref. */
private fun branchTips(topology: GitLogTopology, refs: List<GitRef>, filters: GitLogFilters): IntArray? {
    val names = filters.branches ?: return null
    return names.mapNotNull { name ->
        val hash = refs.firstOrNull { it.name == name }?.hash ?: return@mapNotNull null
        topology.indexOf(hash)
    }.toIntArray()
}

private fun reachableFrom(topology: GitLogTopology, tips: IntArray?): BitSet {
    val seen = BitSet(topology.size)
    if (tips == null) {
        seen.set(0, topology.size)
        return seen
    }
    val stack = ArrayDeque<Int>()
    for (tip in tips) {
        if (tip !in 0 until topology.size || seen.get(tip)) continue
        seen.set(tip)
        stack.add(tip)
    }
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        for (p in 0 until topology.parentCount(node)) {
            val parent = topology.parent(node, p)
            if (seen.get(parent)) continue
            seen.set(parent)
            stack.add(parent)
        }
    }
    return seen
}

/** Dotted edges of `DottedFilterEdgesGenerator` over the whole permanent graph. Pairs are node indices. */
private fun dottedPairs(topology: GitLogTopology, visible: BitSet): List<Pair<Int, Int>> {
    val n = topology.size
    if (n == 0) return emptyList()
    val numbers = IntArray(n)
    val pairs = LinkedHashSet<Long>()
    fun add(a: Int, b: Int) {
        if (a !in 0 until n || b !in 0 until n || a == b) return
        if (!visible.get(a) || !visible.get(b)) return
        pairs.add(pairKey(min(a, b), max(a, b)))
    }

    for (node in 0 until n) {
        if (visible.get(node)) {
            var nearlyUp = Int.MIN_VALUE
            var maxAdj = Int.MIN_VALUE
            for (c in 0 until topology.childCount(node)) {
                val up = topology.child(node, c)
                if (visible.get(up)) maxAdj = max(maxAdj, numbers[up]) else nearlyUp = max(nearlyUp, numbers[up])
            }
            numbers[node] = if (nearlyUp == maxAdj || nearlyUp == Int.MIN_VALUE) maxAdj else nearlyUp.also { add(node, it) }
        } else {
            var nearlyUp = Int.MIN_VALUE
            for (c in 0 until topology.childCount(node)) {
                val up = topology.child(node, c)
                nearlyUp = if (visible.get(up)) max(nearlyUp, up) else max(nearlyUp, numbers[up])
            }
            numbers[node] = nearlyUp
        }
    }
    for (i in 0 until n) numbers[i] = Int.MAX_VALUE
    for (node in n - 1 downTo 0) {
        if (visible.get(node)) {
            var nearlyDown = Int.MAX_VALUE
            var minAdj = Int.MAX_VALUE
            for (p in 0 until topology.parentCount(node)) {
                val down = topology.parent(node, p)
                if (visible.get(down)) minAdj = min(minAdj, numbers[down]) else nearlyDown = min(nearlyDown, numbers[down])
            }
            numbers[node] = if (nearlyDown == minAdj || nearlyDown == Int.MAX_VALUE) minAdj else nearlyDown.also { add(node, it) }
        } else {
            var nearlyDown = Int.MAX_VALUE
            for (p in 0 until topology.parentCount(node)) {
                val down = topology.parent(node, p)
                nearlyDown = if (visible.get(down)) min(nearlyDown, down) else min(nearlyDown, numbers[down])
            }
            numbers[node] = nearlyDown
        }
    }
    return pairs.map { key -> (key ushr 32).toInt() to key.toInt() }
}

private fun pairKey(up: Int, down: Int): Long = up.toLong() shl 32 or (down.toLong() and 0xffffffffL)
