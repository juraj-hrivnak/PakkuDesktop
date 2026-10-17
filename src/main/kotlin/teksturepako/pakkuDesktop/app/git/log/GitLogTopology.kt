/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git.log

/**
 * Every commit reachable from a branch, tag or HEAD, in `git log --date-order` order: children
 * always come before their parents. This is IntelliJ's permanent graph; a node index is the row
 * of the commit in the unfiltered log.
 *
 * Parents and children are stored as compressed adjacency arrays, parents in commit order
 * (first parent first), children top to bottom.
 */
class GitLogTopology private constructor(
    val hashes: Array<String>,
    private val parentStart: IntArray,
    private val parentNodes: IntArray,
    private val childStart: IntArray,
    private val childNodes: IntArray,
    val authors: Array<String>,
    val authorEmails: Array<String>,
    /** Author time, epoch seconds. */
    val authorTimes: LongArray,
) {
    val size: Int get() = hashes.size

    private val indexByHash: HashMap<String, Int> = HashMap<String, Int>(hashes.size * 2).apply {
        hashes.forEachIndexed { index, hash -> put(hash, index) }
    }

    fun indexOf(hash: String): Int? = indexByHash[hash]

    fun parentCount(node: Int): Int = parentStart[node + 1] - parentStart[node]

    fun parent(node: Int, i: Int): Int = parentNodes[parentStart[node] + i]

    fun childCount(node: Int): Int = childStart[node + 1] - childStart[node]

    fun child(node: Int, i: Int): Int = childNodes[childStart[node] + i]

    /** Distinct author names, most commits first. */
    val authorsByCommitCount: List<String> by lazy {
        authors.groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { it.key }
    }

    /** Collects `hash NUL parents NUL author-time NUL author NUL email` lines. */
    class Builder {
        private val hashes = ArrayList<String>()
        private val parents = ArrayList<String>()
        private val authors = ArrayList<String>()
        private val emails = ArrayList<String>()
        private var times = LongArray(1024)
        private val interned = HashMap<String, String>()

        fun add(record: String) {
            val parts = record.split('\u0000')
            if (parts.size < 5 || parts[0].isBlank()) return
            if (hashes.size == times.size) times = times.copyOf(times.size * 2)
            times[hashes.size] = parts[2].trim().toLongOrNull() ?: 0L
            hashes += parts[0].trim()
            parents += parts[1]
            authors += interned.getOrPut(parts[3]) { parts[3] }
            emails += interned.getOrPut(parts[4]) { parts[4] }
        }

        fun build(): GitLogTopology {
            val n = hashes.size
            val index = HashMap<String, Int>(n * 2)
            hashes.forEachIndexed { i, hash -> index[hash] = i }

            val parentStart = IntArray(n + 1)
            val parentList = ArrayList<Int>(n + n / 8)
            val childCounts = IntArray(n)
            for (node in 0 until n) {
                parentStart[node] = parentList.size
                val seen = HashSet<Int>(4)
                for (raw in parents[node].split(' ')) {
                    if (raw.isEmpty()) continue
                    // Shallow clones and pruned grafts can name parents that are not in the log.
                    val parent = index[raw] ?: continue
                    if (parent <= node || !seen.add(parent)) continue
                    parentList += parent
                    childCounts[parent]++
                }
            }
            parentStart[n] = parentList.size
            val parentNodes = parentList.toIntArray()

            val childStart = IntArray(n + 1)
            for (node in 0 until n) childStart[node + 1] = childStart[node] + childCounts[node]
            val fill = childStart.copyOf()
            val childNodes = IntArray(parentNodes.size)
            for (node in 0 until n) {
                for (p in parentStart[node] until parentStart[node + 1]) {
                    val parent = parentNodes[p]
                    childNodes[fill[parent]++] = node
                }
            }

            return GitLogTopology(
                hashes = hashes.toTypedArray(),
                parentStart = parentStart,
                parentNodes = parentNodes,
                childStart = childStart,
                childNodes = childNodes,
                authors = authors.toTypedArray(),
                authorEmails = emails.toTypedArray(),
                authorTimes = times.copyOf(n),
            )
        }
    }

    companion object {
        val EMPTY: GitLogTopology = Builder().build()
    }
}
