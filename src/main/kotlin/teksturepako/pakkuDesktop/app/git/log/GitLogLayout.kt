/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git.log

import teksturepako.pakkuDesktop.app.git.GitRef
import teksturepako.pakkuDesktop.app.git.GitRefType
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max

/**
 * Layout indices of [GitLogTopology], assigned the way IntelliJ's `GraphLayoutBuilder` does:
 * a depth-first walk from heads, heads ordered by `GitBranchLayoutComparator`.
 *
 * Index 0 means "not laid out". Assigned indices start at 1. The first parent of a commit
 * keeps the commit's index; each other parent starts a new one.
 */
class GitLogLayout(
    val topology: GitLogTopology,
    private val layoutIndex: IntArray,
    private val headNodes: IntArray,
    private val bestRefName: Array<String?>,
) {
    private val headLayoutIndex: IntArray = IntArray(headNodes.size) { layoutIndex[headNodes[it]] }

    fun layoutIndex(node: Int): Int = layoutIndex[node]

    /** Head whose walk painted [node] (`GraphLayoutImpl.getOneOfHeadNodeIndex`). */
    fun headOf(node: Int): Int {
        val order = headOrder(layoutIndex[node])
        return headNodes[order]
    }

    /**
     * Color id of [node]: the head fragment uses its best ref name's hash, every other fragment
     * uses its layout index. Id 0 is IntelliJ's default color (the theme content color).
     */
    fun colorId(node: Int): Int {
        val li = layoutIndex[node]
        if (li == 0) return 0
        val head = headOf(node)
        if (layoutIndex[head] == li) return bestRefName[head]?.hashCode() ?: 0
        return li
    }

    /** Edge color: the up node's when its layout index is greater, otherwise the down node's. */
    fun edgeColorId(up: Int, down: Int): Int {
        val upLi = layoutIndex[up]
        val downLi = layoutIndex[down]
        return if (upLi >= downLi) colorId(up) else colorId(down)
    }

    private fun headOrder(li: Int): Int {
        val i = headLayoutIndex.binarySearch(li)
        return if (i < 0) max(0, -i - 2) else i
    }

    companion object {
        fun build(topology: GitLogTopology, refs: List<GitRef>, headHash: String?): GitLogLayout {
            val n = topology.size
            val refsAt = Array(n) { ArrayList<GitRef>(1) }
            for (ref in refs) {
                val node = topology.indexOf(ref.hash) ?: continue
                refsAt[node].add(ref)
            }
            val headNode = headHash?.let { topology.indexOf(it) }
            val best = arrayOfNulls<String>(n)
            val kinds = arrayOfNulls<LayoutKind>(n)
            for (node in 0 until n) {
                val tip = bestTip(refsAt[node], node == headNode)
                best[node] = tip?.name
                kinds[node] = tip?.kind
            }

            val heads = ArrayList<Int>()
            for (node in 0 until n) {
                if (topology.childCount(node) == 0 || refsAt[node].isNotEmpty() || node == headNode) heads.add(node)
            }
            heads.sortWith { a, b -> compareHeads(a, b, best, kinds) }

            val layout = IntArray(n)
            val important = ArrayList<Int>()
            var nextIndex = 1
            for (head in heads) {
                if (layout[head] != 0) continue
                important.add(head)
                val stack = ArrayDeque<Int>()
                stack.add(head)
                while (stack.isNotEmpty()) {
                    val node = stack.last()
                    val firstVisit = layout[node] == 0
                    if (firstVisit) layout[node] = nextIndex
                    var child = -1
                    for (p in 0 until topology.parentCount(node)) {
                        val parent = topology.parent(node, p)
                        if (layout[parent] == 0) {
                            child = parent
                            break
                        }
                    }
                    if (child == -1) {
                        if (firstVisit) nextIndex++
                        stack.removeLast()
                    } else {
                        stack.add(child)
                    }
                }
            }
            return GitLogLayout(topology, layout, important.toIntArray(), best)
        }
    }
}

/** `origin/main` before other remotes, before `main`, before other locals, before tags, before HEAD. */
private enum class LayoutKind { OriginMain, Remote, Main, Local, Tag, Current, Head, Other }

private fun layoutKind(ref: GitRef): LayoutKind = when (ref.type) {
    GitRefType.Tag -> LayoutKind.Tag
    GitRefType.LocalBranch -> if (ref.name == "master" || ref.name == "main") LayoutKind.Main else LayoutKind.Local
    GitRefType.RemoteBranch ->
        if (ref.name == "origin/master" || ref.name == "origin/main") LayoutKind.OriginMain else LayoutKind.Remote
}

private class Tip(val kind: LayoutKind, val name: String)

/** Ref that wins `GitBranchLayoutComparator` at this commit; a synthetic HEAD when nothing beats it. */
private fun bestTip(refs: List<GitRef>, headHere: Boolean): Tip? {
    var best: Tip? = null
    for (ref in refs) {
        val tip = Tip(layoutKind(ref), ref.name)
        if (best == null || tipBeats(tip, best)) best = tip
    }
    if (headHere && (best == null || LayoutKind.Head.ordinal < best.kind.ordinal)) return Tip(LayoutKind.Head, "HEAD")
    return best
}

private fun tipBeats(candidate: Tip, incumbent: Tip): Boolean {
    val byKind = candidate.kind.ordinal - incumbent.kind.ordinal
    if (byKind != 0) return byKind < 0
    return naturalCompare(candidate.name, incumbent.name) < 0
}

/** Heads with no ref come last; otherwise the better tip ref wins, then the node index. */
private fun compareHeads(a: Int, b: Int, names: Array<String?>, kinds: Array<LayoutKind?>): Int {
    val ka = kinds[a]
    val kb = kinds[b]
    if (ka == null && kb == null) return a - b
    if (ka == null) return 1
    if (kb == null) return -1
    val byKind = ka.ordinal - kb.ordinal
    if (byKind != 0) return byKind
    return naturalCompare(names[a]!!, names[b]!!)
}

/** Case-sensitive natural order, the way IntelliJ orders ref names. */
internal fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        if (a[i].isDigit() && b[j].isDigit()) {
            val startA = i
            val startB = j
            while (i < a.length && a[i].isDigit()) i++
            while (j < b.length && b[j].isDigit()) j++
            var sa = startA
            var sb = startB
            while (sa < i - 1 && a[sa] == '0') sa++
            while (sb < j - 1 && b[sb] == '0') sb++
            val len = (i - sa) - (j - sb)
            if (len != 0) return len
            for (k in 0 until i - sa) {
                val diff = a[sa + k] - b[sb + k]
                if (diff != 0) return diff
            }
        } else {
            val diff = a[i].code - b[j].code
            if (diff != 0) return diff
            i++
            j++
        }
    }
    return (a.length - i) - (b.length - j)
}

/**
 * IntelliJ's `DefaultColorGenerator`: id 0 stays null (drawn with the content color), every other
 * id goes through the r/g/b `rangeFix` mix and HSB saturation 0.4, brightness 0.65.
 */
fun gitLogColor(colorId: Int): Int? {
    if (colorId == 0) return null
    val hsb = Color.RGBtoHSB(rangeFix(colorId * 200 + 30), rangeFix(colorId * 130 + 50), rangeFix(colorId * 90 + 100), null)
    return Color.HSBtoRGB(hsb[0], SATURATION, BRIGHTNESS)
}

private fun rangeFix(n: Int): Int = abs(n % 100) + 70

private const val SATURATION = 0.4f
private const val BRIGHTNESS = 0.65f
