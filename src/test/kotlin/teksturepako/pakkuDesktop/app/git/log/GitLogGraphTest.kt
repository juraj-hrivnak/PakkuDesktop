/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git.log

import teksturepako.pakkuDesktop.app.git.GitLogFilters
import teksturepako.pakkuDesktop.app.git.GitRef
import teksturepako.pakkuDesktop.app.git.GitRefType
import java.awt.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitLogGraphTest {
    @Test
    fun linearHistoryStaysInOneLane() {
        val graph = graph(
            parents = listOf("b", "c", ""),
            hashes = listOf("a", "b", "c"),
        )
        assertEquals(3, graph.rows)
        val lanes = (0 until graph.rows).map { row ->
            graph.elements(row).single { it.node }.position
        }
        assertEquals(listOf(0, 0, 0), lanes)
        assertTrue(graph.elements(1).any { !it.node && !it.dotted && it.edgeUp == 0 && it.edgeDown == 1 })
    }

    @Test
    fun branchAndMergeGivesTheSecondParentItsOwnLane() {
        // 0 merges 1 and 2; both parents share root 3.
        val graph = graph(
            hashes = listOf("m", "a", "b", "r"),
            parents = listOf("a b", "r", "r", ""),
            refs = listOf(GitRef("main", GitRefType.LocalBranch, "m")),
        )
        val lane = (0 until graph.rows).associate { row ->
            graph.hashAt(row) to graph.elements(row).single { it.node }.colorId
        }
        assertEquals(lane.getValue("m"), lane.getValue("a"))
        assertTrue(lane.getValue("b") != lane.getValue("m"))
    }

    @Test
    fun octopusMergeKeepsOnlyTheFirstParentOnTheMergeLane() {
        val graph = graph(
            hashes = listOf("m", "a", "b", "c"),
            parents = listOf("a b c", "", "", ""),
        )
        fun lane(hash: String): Int {
            val row = graph.rowOf(hash)!!
            return graph.elements(row).single { it.node }.colorId
        }
        assertEquals(lane("m"), lane("a"))
        assertTrue(lane("b") != lane("m"))
        assertTrue(lane("c") != lane("m") && lane("c") != lane("b"))
    }

    @Test
    fun longEdgeDrawsArrowsOnlyAtItsEnds() {
        val hashes = (0..32).map { "c$it" }
        val parents = hashes.indices.map { index ->
            when (index) {
                0 -> "c1 c31"
                32 -> ""
                else -> "c${index + 1}"
            }
        }
        val graph = graph(hashes, parents)
        val through = (0 until graph.rows).count { row ->
            graph.elements(row).any { !it.node && it.edgeUp == 0 && it.edgeDown == graph.rowOf("c31") }
        }
        val arrows = (0 until graph.rows).flatMap { row ->
            graph.elements(row).filter { it.arrow && it.edgeUp == 0 && it.edgeDown == graph.rowOf("c31") }
        }
        assertTrue(through < 10, "a 30-row edge stays hidden in the middle, saw $through rows")
        assertTrue(arrows.any { it.down })
        assertTrue(arrows.any { !it.down })
    }

    @Test
    fun textFilterConnectsMatchesWithDottedEdges() {
        val hashes = listOf("a", "b", "c", "d", "e")
        val parents = listOf("b", "c", "d", "e", "")
        val graph = graph(
            hashes = hashes,
            parents = parents,
            filters = GitLogFilters(text = "keep"),
            textMatches = setOf("a", "c", "e"),
        )
        assertEquals(listOf("a", "c", "e"), (0 until graph.rows).map { graph.hashAt(it) })
        val dotted = (0 until graph.rows).flatMap { row -> graph.elements(row).filter { it.dotted } }
        assertTrue(dotted.isNotEmpty())
        assertTrue(dotted.all { it.dotted })
        assertEquals(false, graph.hoverEnabled)
    }

    @Test
    fun defaultColorIsTheContentColorAndOthersMatchIntelliJ() {
        assertNull(gitLogColor(0))
        val id = "main".hashCode()
        val actual = gitLogColor(id)
        assertNotNull(actual)
        assertEquals(intellijColor(id), actual)
    }

    @Test
    fun trackedPairLabelReadsOriginAndMain() {
        val graph = graph(
            hashes = listOf("a"),
            parents = listOf(""),
            refs = listOf(
                GitRef("main", GitRefType.LocalBranch, "a"),
                GitRef("origin/main", GitRefType.RemoteBranch, "a"),
            ),
            head = "a",
            current = "main",
            upstream = "origin/main",
        )
        val label = graph.labelAt(0)
        assertNotNull(label)
        assertEquals("origin & main", label.text)
        assertTrue(label.colors.contains(REF_HEAD))
        assertTrue(label.colors.contains(REF_LOCAL))
        assertTrue(label.colors.contains(REF_REMOTE))
    }

    @Test
    fun tagOnlyLabelShowsTagName() {
        val graph = graph(
            hashes = listOf("a"),
            parents = listOf(""),
            refs = listOf(
                GitRef("main", GitRefType.LocalBranch, "a"),
                GitRef("v1.0", GitRefType.Tag, "a"),
            ),
            head = "a",
            current = "main",
        )
        val label = graph.labelAt(0)
        assertNotNull(label)
        assertEquals("main, v1.0", label.text)
        assertTrue(label.colors.contains(REF_HEAD))
        assertTrue(label.colors.contains(REF_LOCAL))
        assertTrue(label.colors.contains(REF_TAG))
    }

    @Test
    fun tagOnlyLabelWithNoBranchShowsTagName() {
        val graph = graph(
            hashes = listOf("a", "b"),
            parents = listOf("b", ""),
            refs = listOf(
                GitRef("main", GitRefType.LocalBranch, "b"),
                GitRef("v1.0", GitRefType.Tag, "a"),
            ),
            head = "b",
            current = "main",
        )
        val label = graph.labelAt(0)
        assertNotNull(label)
        assertEquals("v1.0", label.text)
        assertTrue(label.colors.contains(REF_TAG))
    }

    @Test
    fun multipleTagsOnSameCommitShowAllNames() {
        val graph = graph(
            hashes = listOf("a", "b"),
            parents = listOf("b", ""),
            refs = listOf(
                GitRef("main", GitRefType.LocalBranch, "b"),
                GitRef("v1.0", GitRefType.Tag, "a"),
                GitRef("v1.0-rc1", GitRefType.Tag, "a"),
                GitRef("latest", GitRefType.Tag, "a"),
            ),
            head = "b",
            current = "main",
        )
        val label = graph.labelAt(0)
        assertNotNull(label)
        assertEquals("latest, v1.0, v1.0-rc1", label.text)
        // Three tag icons should be present (REF_TAG appears 3 times, collapsed to 2 by distinctRepeating)
        assertTrue(label.colors.contains(REF_TAG))
    }

    @Test
    fun multipleTagsWithBranchShowAllNames() {
        val graph = graph(
            hashes = listOf("a"),
            parents = listOf(""),
            refs = listOf(
                GitRef("main", GitRefType.LocalBranch, "a"),
                GitRef("v1.0", GitRefType.Tag, "a"),
                GitRef("v2.0", GitRefType.Tag, "a"),
            ),
            head = "a",
            current = "main",
        )
        val label = graph.labelAt(0)
        assertNotNull(label)
        assertEquals("main, v1.0, v2.0", label.text)
        assertTrue(label.colors.contains(REF_HEAD))
        assertTrue(label.colors.contains(REF_LOCAL))
        assertTrue(label.colors.contains(REF_TAG))
    }
}

private fun graph(
    hashes: List<String>,
    parents: List<String>,
    refs: List<GitRef> = emptyList(),
    head: String? = hashes.firstOrNull(),
    current: String? = null,
    upstream: String? = null,
    filters: GitLogFilters = GitLogFilters(),
    textMatches: Set<String>? = null,
): GitLogGraph {
    val builder = GitLogTopology.Builder()
    hashes.forEachIndexed { index, hash ->
        builder.add("$hash\u0000${parents[index]}\u0000${1_000 - index}\u0000Ann\u0000ann@example.com")
    }
    return GitLogGraph.build(builder.build(), refs, head, current, upstream, filters, textMatches)
}

/** `DefaultColorGenerator.calcColor`, saturation 0.4 and brightness 0.65. */
private fun intellijColor(colorId: Int): Int {
    fun rangeFix(n: Int) = abs(n % 100) + 70
    val hsb = Color.RGBtoHSB(rangeFix(colorId * 200 + 30), rangeFix(colorId * 130 + 50), rangeFix(colorId * 90 + 100), null)
    return Color.HSBtoRGB(hsb[0], 0.4f, 0.65f)
}
